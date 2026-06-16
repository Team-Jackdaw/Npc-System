# 外部 Agent 接口

最后更新：2026-06-16 CST

状态：Java 端已经具备协议 DTO、HTTP 客户端和 action 执行器；Python 端已经具备 FastAPI、Pydantic schema、Pydantic AI runner 和 per-NPC 上下文管理。启用 `Config.agentEnabled` 后，Java 会通过 HTTP 调用外部 agent。本文是当前线协议参考。

本文定义 Minecraft mod 与外部 NPC agent 之间的 JSON 边界。传输方式当前是 HTTP。认证、超时、重试由配置和实现处理，不在单个响应 schema 中表达。

Python agent 可通过环境变量选择模型供应商：`ollama`、`deepseek`、`openai-compatible`。具体启动方式见 `agent/README.md`。

## 协议规则

- `version` / `v` 当前为 `1`。
- 响应必须回填请求中的 `request_id` / `rid`。
- `mode` 为 `fast` 或 `deliberate`。
- 普通文字回复统一放在 `speech` 字段，不占 action 数量。
- 每次响应最多包含 1 个真实工具或任务调用。
- action 调用的函数来自 Java `FunctionManager`。
- `kind: "task"` 表示启动 Minecraft 内 NPC 任务；`kind: "tool"` 表示其他工具。
- `npc.kind` 为 `npc` 或 `master`。
- `npc.permission` 对应 Java 工具权限；只有 `kind=master` 且 `permission>=3` 才能调用 `call_command`。
- Java 是 Minecraft 世界事实来源和执行器；外部 agent 负责上下文、模型调用和决策。

## Fast 模式

Fast 模式用于短回复、附近事件反应和紧急打断。它使用短字段降低 token 消耗。

请求示例：

```json
{
  "v": 1,
  "rid": "request-uuid",
  "mode": "fast",
  "npc": {
    "id": "npc-uuid",
    "name": "npc-name",
    "kind": "npc",
    "permission": 1,
    "task": "idle",
    "hp": 20.0,
    "pos": [10, 64, -5],
    "dim": "minecraft:overworld"
  },
  "evt": [
    ["CHAT_HEARD", 7, "Steve: 你好", 12345]
  ],
  "near": {
    "p": ["Steve@3.2"],
    "n": ["Bob@5.1"],
    "e": ["Zombie@8.0"]
  },
  "tools": ["look_at_player", "walk_to_player", "follow_player", "pickup_nearby_item", "observe_functional_blocks", "wait", "stop_task"],
  "limits": {
    "max_reply_chars": 60
  }
}
```

响应示例：

```json
{
  "v": 1,
  "rid": "request-uuid",
  "mode": "fast",
  "a": "none",
  "kind": null,
  "name": null,
  "args": {},
  "callback": false,
  "speech": "你好。",
  "note": "reply"
}
```

`speech` 是 NPC/Master 的普通回复，由 Java 端直接显示在聊天栏和/或头顶气泡。Fast 模式使用 `a/kind/name/args/callback` 表示单个真实工具或任务调用；如果没有动作，`a` 为 `none`。

## Deliberate 模式

Deliberate 模式用于复杂计划、记忆参与和较长交互。它使用完整字段名和更丰富上下文。

请求示例：

```json
{
  "version": 1,
  "request_id": "request-uuid",
  "mode": "deliberate",
  "npc": {
    "uuid": "npc-uuid",
    "name": "npc-name",
    "kind": "npc",
    "permission": 1,
    "instruction": "Minecraft NPC instruction",
    "status": {
      "task": "running follow_entity",
      "health": 20.0,
      "max_health": 20.0,
      "position": {"x": 10, "y": 64, "z": -5},
      "dimension": "minecraft:overworld",
      "biome": "minecraft:plains",
      "weather": "clear",
      "inventory": {
        "occupied_slots": 1,
        "total_slots": 8,
        "items": [{"id": "minecraft:apple", "count": 2}]
      },
      "functional_blocks": [
        {
          "blockId": "minecraft:chest",
          "category": "storage",
          "pos": {"x": 11, "y": 64, "z": -7},
          "distance": 3.0
        }
      ]
    }
  },
  "observations": {
    "summary": "full state summary",
    "recent_events": [],
    "important_events": []
  },
  "conversation": {
    "speaker": "Steve",
    "message": "你能跟着我吗？",
    "history": []
  },
  "memory": {
    "recent": [],
    "relevant": []
  },
  "available_tools": [
    {
      "name": "follow_player",
      "kind": "task",
      "description": "Follow a player.",
      "parameters": {},
      "required": ["player"]
    }
  ],
  "limits": {
    "max_reply_chars": 200
  }
}
```

响应示例：

```json
{
  "version": 1,
  "request_id": "request-uuid",
  "mode": "deliberate",
  "action": {
    "type": "call",
    "kind": "task",
    "name": "follow_player",
    "arguments": {
      "player": "Steve",
      "seconds": 30
    },
    "callback": true,
    "label": "follow_request"
  },
  "speech": "好，我跟着你。",
  "memory_updates": [],
  "reasoning_summary": "玩家请求 NPC 跟随。"
}
```

Deliberate 模式只使用单个 `action` 字段。复杂多步流程通过 `callback=true` 的任务完成回调逐步推进。`reasoning_summary` 是简短决策摘要，不应包含隐藏思维链。

## Agent 侧上下文

Python agent 默认把上下文保存在 `config/npc-system/agent-state`，可用 `NPC_AGENT_STATE_DIR` 覆盖。

目录结构：

```text
agent-state/
  npc/<uuid>/
  master/default/
```

每个目录包含：

- `AGENTS.md`：身份、行为边界和权限说明。
- `SOLU.md`：当前计划和未解决目标。
- `SUMMARY.md`：当前会话的 LLM 自然语言摘要。
- `MEMORY.md`：会话结束时写入的长期自然语言记忆。
- `messages.json`：当前 Pydantic AI message history。
- `history.jsonl`：调试和审计日志，不作为 prompt 主上下文。

首次创建目录时，Markdown 文件会从 `agent/templates` 复制。普通 NPC 使用 `agent/templates/npc/AGENTS.md`，Master 使用 `agent/templates/master/AGENTS.md`，公共文件来自 `agent/templates/common`。已有运行时文件不会被覆盖。

Pydantic AI 调用会使用 `message_history`，并通过 `result.all_messages_json()` 持久化当前会话。当 `messages.json` 超过 `NPC_AGENT_MAX_HISTORY_BYTES`，agent 会调用当前配置的大模型，把上下文总结为自然语言写入 `SUMMARY.md`，然后重置 `messages.json`。`SUMMARY.md` 不应包含原始 JSON、消息 dump 或调试日志。

## 会话结束

Java 在会话显式结束或窗口被移除时通知 agent。

端点：

```text
POST /agent/conversation/end
```

请求示例：

```json
{
  "version": 1,
  "request_id": "request-uuid",
  "npc": {
    "uuid": "npc-uuid",
    "id": "npc-uuid",
    "name": "npc-name",
    "kind": "npc",
    "permission": 1
  },
  "reason": "end_conversation_tool",
  "snapshot": {
    "task": "idle",
    "last_observation": "..."
  }
}
```

响应示例：

```json
{
  "version": 1,
  "request_id": "request-uuid",
  "status": "ok",
  "memory_updated": true
}
```

会话结束时，agent 会调用当前配置的大模型，根据 `messages.json` 与 `SUMMARY.md` 生成长期自然语言记忆并写入 `MEMORY.md`，然后重置当前会话上下文。如果总结失败，agent 会在 `history.jsonl` 记录失败并保留当前上下文以便重试，不会把原始 JSON 写入记忆文件。

## 任务完成回调

AGENT 级任务批次完成后，Java 会向同一个 fast 或 deliberate 端点发送 follow-up 请求。Fast 模式追加紧凑事件：

```json
["TASK_BATCH_FINISHED", 8, "Agent task batch ... finished: follow_entity:finished", 12345]
```

Deliberate 模式追加结构化 `recent_events`：

```json
{
  "type": "TASK_BATCH_FINISHED",
  "importance": 8,
  "text": "Agent task batch ... finished: ...",
  "game_time": 12345,
  "facts": {
    "batch_id": "batch-uuid",
    "status": "finished",
    "tasks": ["follow_entity:finished"]
  }
}
```

回调期间 NPC 会短暂进入 SYSTEM wait。agent 如果返回新 action，它们会作为新的 AGENT 批次入队；如果控制结束，agent 应调用 `resume_default_behavior`。

```json
{
  "type": "call",
  "kind": "tool",
  "name": "resume_default_behavior",
  "arguments": {}
}
```

## Master Agent

Master 复用普通 NPC 的协议，但 Java 当前把 Master 对话固定路由到 `/agent/deliberate`，便于管理员监控和命令决策使用完整上下文。Master 使用稳定 Java UUID，Python 端上下文固定在 `master/default`，不会因重启或 UUID 变化分裂记忆。Master 没有 Minecraft 实体，不能执行 NPC task。

Master 请求身份：

```json
{
  "kind": "master",
  "permission": 3,
  "status": {
    "entity": "none",
    "task": "idle"
  }
}
```

Master 普通回复使用 `speech`。只有当管理员明确要求 Minecraft 管理命令时，且 `npc.kind=master`、`npc.permission>=3`，agent 才应调用 `call_command(command)`。Java 端也会通过 `FunctionManager` 做权限校验，普通 NPC 即使伪造响应也不能执行管理员命令。

## 工具结果

所有 tool 和 task function 返回统一结构：

```json
{
  "status": "success",
  "code": "task_started",
  "message": "Task started.",
  "data": {
    "task": "follow_entity",
    "target": "Steve"
  },
  "retryable": false
}
```

失败示例：

```json
{
  "status": "failure",
  "code": "target_not_found",
  "message": "Player Steve was not found.",
  "data": {},
  "retryable": true
}
```

`status`、`code`、`message`、`data`、`retryable` 总是存在。
