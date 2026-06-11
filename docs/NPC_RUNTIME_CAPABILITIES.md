# NPC 运行能力说明

Last updated: 2026-06-11 15:18:07 CST

本文按当前代码实现说明 NPC 在游戏中可以预期完成的任务、运行循环、玩家交互、外部 agent 协议、可调用接口和记忆存储方式。

## 当前可预期完成的任务

在不接入外部 agent 时，NPC 已经具备最小默认行为：空闲时会间隔 3-8 秒尝试看向附近实体、随机闲逛或等待。该默认行为不进入持久任务队列，任何玩家任务或 agent 任务都可以打断它。

在接入对话或外部 agent 后，NPC 可以通过 tool 下发低层 Minecraft task：

- 说话：在聊天栏和/或头顶气泡显示短消息。
- 看向玩家或 NPC：按名称或 UUID 找到目标并持续看向目标。
- 走向玩家或 NPC：导航到目标附近，达到停止距离或超时后结束。
- 跟随玩家：在限定时间内跟随目标玩家并保持距离。
- 等待：停止导航并持续若干秒。
- 停止任务：取消当前任务并清空任务队列。

当前还不能可靠完成睡觉、使用工作方块、使用方块、拿取/丢弃物品、战斗、采集、背包管理、路径规划链式任务等复杂行为。`MeetPlayerTask`、`MeetNPCTask`、`FollowChatTargetTask` 目前只是占位类，不应视为可用 task。

## 已实现的 Sensor

`NpcSensorState` 是当前被动感知层，由 `NPCEntity.tick()` 每 20 tick 更新一次。已记录的信息包括：

- 自身位置、维度、生物群系、天气、世界时间。
- 生命值、最大生命值、是否在地面、是否在水中。
- 当前任务状态和任务队列长度。
- 感知范围内最多 8 个玩家、8 个 NPC、8 个其他存活实体。
- 实体名称、类型、距离、是否有视线。
- 玩家是否正在看向该 NPC。
- 最近听到的最多 8 条玩家聊天。

感知范围使用 `Config.range`，默认 10 格。

## 已实现的 Observe

`ObservationCollector` 比较上一次和当前 sensor 快照，生成结构化 `ObservationEvent`。Observe 不调用 LLM，只负责把 sensor 变化转为事件。事件字段为：

- `type`
- `importance`
- `text`
- `gameTime`
- `facts`

已实现事件类型包括：

- 自身状态变化、位置变化。
- 天气变化。
- 生命值变化。
- 玩家进入/离开范围。
- 玩家看向 NPC。
- NPC 进入/离开范围。
- 听到聊天。
- 任务开始、完成、失败、取消。

NPC 的 `recentEvents` 最多保留 32 条，`importance >= 7` 的事件还会进入 `importantEvents`，同样最多 32 条。

## 已实现的 Tool 与 Task

当前默认注册的 NPC 可用 tool：

| 名称 | 类型 | 作用 |
| --- | --- | --- |
| `say` | task | 让 NPC 说一句话，对应 `SpeakTask` |
| `look_at_player` | task | 看向在线玩家，对应 `LookAtEntityTask` |
| `look_at_npc` | task | 看向 NPC，对应 `LookAtEntityTask` |
| `walk_to_player` | task | 走到玩家附近，对应 `WalkToEntityTask` |
| `walk_to_npc` | task | 走到 NPC 附近，对应 `WalkToEntityTask` |
| `follow_player` | task | 跟随玩家，对应 `FollowEntityTask` |
| `wait` | task | 等待，对应 `WaitTask` |
| `stop_task` | task | 停止当前任务并清空队列 |
| `rag_query` | tool | 查询本地文本记忆 |
| `rag_record` | tool | 写入本地文本记忆 |
| `end_conversation` | tool | 结束当前对话 |

另外还有 `call_command`，但它的权限等级为 3，默认 NPC 权限不能调用。`NoCallableFunction` 可从 `config/npc-system/functions/*.json` 动态加载数据包函数桥接 tool。

所有 tool 结果统一返回：

```json
{
  "status": "success",
  "code": "task_started",
  "message": "Task started.",
  "data": {},
  "retryable": false
}
```

## 任务控制与优先级

`NpcTaskController` 当前是“当前任务 + FIFO 队列”模型。任务来源与默认优先级：

- `PLAYER`: 100
- `AGENT`: 80
- `SYSTEM`: 50
- `DEFAULT`: 10

规则：

- 高优先级任务可以打断低优先级任务。
- 同优先级任务默认排队，不抢占。
- agent/system/player 任务进入持久队列。
- default 任务不进入持久队列，只在完全空闲时临时生成。
- 玩家任务打断 agent/system 任务时，如果被打断任务允许恢复，会放回队列头部。
- default 任务被打断后直接丢弃。
- `stop_task` 会停止当前任务、清空队列并停止导航。

当前 tool 下发的 NPC 行为默认来源是 `AGENT`。玩家交互优先级入口已经在任务控制器层支持，但现有玩家直接交互主要还是触发对话，不会自动创建 `PLAYER` 来源 task。

## 完整 Tick Loop

`NPCEntity` 继承自 `Villager`，但覆盖了 `customServerAiStep(ServerLevel)` 并保持空实现，避免原版 Villager Brain 抢占自定义 navigation/look control。`tick()` 仍调用 `super.tick()`，保留实体、导航、渲染等基础机制。

服务端每 tick 的核心流程：

1. 调用 `super.tick()`。
2. 客户端侧直接返回。
3. 每 20 tick 更新一次 `NpcSensorState`。
4. 用 `ObservationCollector` 比较前后快照，生成 observation events。
5. 更新 `lastObservationSummary`。
6. 如果 NPC AI 对象存在且事件非空，调用 `npc.observe(events)` 写入短期事件缓冲。
7. 调用 `DefaultBehaviorController.tick(this)`，仅在当前任务和队列都为空时尝试生成默认任务。
8. 调用 `NpcTaskController.tick(this)`，驱动当前 task，完成后从队列取下一个 task。

异步对话和外部 HTTP 调用不直接在实体 tick 中执行，而是通过 `AsyncTask` 与服务器 tick 末尾的任务队列衔接。

## 玩家交互方式

当前已实现的玩家交互入口：

- 潜行攻击 NPC：触发 `AttackEntityCallback`，启动玩家与 NPC 的对话窗口，并阻止原攻击行为。
- 玩家聊天：`PlayerSendMessageMixin` 在聊天广播后触发回调。所有 NPC 会记录范围内聊天到 sensor；如果已有以该玩家为目标的对话窗口，消息会送入该窗口并触发 NPC 回复。
- NPC 说话显示：`NPCEntity.sendMessage` 可按配置显示头顶气泡和聊天栏消息。

限制：

- 玩家聊天本身不会自动创建新对话，通常需要先通过潜行攻击开启会话。
- 玩家交互目前没有把“玩家命令式请求”直接转换成 `PLAYER` 优先级 task；它会先进入对话/agent，再由 agent 或 Ollama tool 调用生成 `AGENT` task。

## 与后端 Agent 的交互

外部 agent 由配置控制：

- `Config.agentEnabled`: 是否启用，默认 `false`。
- `Config.agentBaseUrl`: 默认 `http://127.0.0.1:8765`。
- `Config.agentMode`: `fast` 或 `deliberate`，默认 `fast`。
- `Config.agentAuthToken`: 可选 Authorization。
- `Config.agentFallbackToOllama`: agent 失败时是否回退 Ollama，默认 `true`。

交互时机：

- 玩家开启或继续 NPC 对话时，`ConversationWindow.chat(...)` 会优先尝试外部 agent。
- 如果 `agentMode=fast`，POST 到 `/agent/fast`。
- 如果 `agentMode=deliberate`，POST 到 `/agent/deliberate`。
- agent 响应中的 action 会由 `AgentActionExecutor` 调用 `FunctionManager` 执行。
- 当前每次 agent 响应最多执行 1 个 action；任务队列已支持连续任务，但 wire protocol 还未支持一次返回多个 action。

### Fast 请求字段

Fast 模式使用短字段以节省 token：

```json
{
  "v": 1,
  "rid": "request-uuid",
  "mode": "fast",
  "npc": {
    "id": "npc-uuid",
    "name": "NPC名称",
    "task": "running agent follow_entity queue=0",
    "hp": 20.0,
    "pos": [10.0, 64.0, -5.0],
    "dim": "minecraft:overworld"
  },
  "evt": [
    ["CHAT_HEARD", 7, "听到 Steve 说：“你好”。", 12345]
  ],
  "near": {
    "p": ["Steve@3.2"],
    "n": ["Alice@5.1"],
    "e": ["minecraft:zombie@8.0"]
  },
  "tools": ["say", "look_at_player", "walk_to_player"],
  "limits": {
    "max_actions": 1,
    "max_reply_chars": 60
  }
}
```

Fast 响应字段：

```json
{
  "v": 1,
  "rid": "request-uuid",
  "mode": "fast",
  "a": "call",
  "kind": "task",
  "name": "say",
  "args": {
    "message": "你好。"
  },
  "note": "reply"
}
```

`a` 为 `call` 时执行 tool；其他值或空响应会视为无动作。

### Deliberate 请求字段

Deliberate 模式使用完整字段，适合更长推理：

```json
{
  "version": 1,
  "request_id": "request-uuid",
  "mode": "deliberate",
  "npc": {
    "uuid": "npc-uuid",
    "name": "NPC名称",
    "instruction": "Your are a Minecraft NPC...",
    "status": {
      "task": "idle queue=0",
      "health": 20.0,
      "max_health": 20.0,
      "position": {"x": 10, "y": 64, "z": -5},
      "dimension": "minecraft:overworld",
      "biome": "minecraft:plains",
      "weather": "clear"
    }
  },
  "observations": {
    "summary": "完整状态摘要",
    "recent_events": [],
    "important_events": []
  },
  "conversation": {
    "speaker": "Steve",
    "message": "跟着我",
    "history": []
  },
  "memory": {
    "recent": [],
    "relevant": []
  },
  "available_tools": [],
  "limits": {
    "max_actions": 1,
    "max_reply_chars": 200
  }
}
```

Deliberate 响应字段：

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
    }
  },
  "speech": "好，我跟着你。",
  "memory_updates": [],
  "reasoning_summary": "玩家请求 NPC 跟随。"
}
```

当前 Java 端会使用 `speech` 或 action 中 `say.message` 作为对话文本。`memory_updates` 字段已在协议中存在，但尚未自动写入本地记忆。

## Agent 可调用的 NPC 接口

agent 实际可调用接口来自当前 NPC agent 的 tool 列表，并由 `FunctionManager` 转成 descriptor。默认 NPC 可调用：

- `say(message)`
- `look_at_player(player, seconds?)`
- `look_at_npc(npc, seconds?)`
- `walk_to_player(player, stop_distance?, timeout_seconds?)`
- `walk_to_npc(npc, stop_distance?, timeout_seconds?)`
- `follow_player(player, seconds?, stop_distance?)`
- `wait(seconds)`
- `stop_task()`
- `rag_query(context)`
- `rag_record(context)`
- `end_conversation()`

这些接口隐藏 Minecraft 内部类，只暴露 JSON 参数。task 类接口会在 Java 端解析目标实体、创建 `NpcTask`，并交给 `NpcTaskController`。

## 记忆储存与读取

当前记忆模块仍保留 `rag` 包名和 `rag_query`/`rag_record` tool 名称，但实现已经不是 embedding/vector RAG。实际存储是本地 JSON 文本记录：

- 存储目录：`config/npc-system/rag`
- 每个 NPC 一个 JSON 文件：文件名来自 NPC UUID；Master 使用 `Master.json`。
- 写入时调用 `RAG.record(text, className)`。
- 文本按 `RAG.CHUNK_SIZE = 150` 做简单空白分词 chunk。
- JSON 记录包含 `id`、原始 `text`、`chunks`、`createdAt`。
- 查询时调用 `RAG.query(text, topK, className)`。
- 查询使用本地关键词计数打分：中文按单字 token，英文/数字按词 token。
- 如果查询文本没有 token，则返回最近记录的 chunk。

Ollama embedding 已不参与记忆写入和查询。`RAG.completion(...)` 仍会把查询结果拼入 prompt 后调用 Ollama completion，但普通记忆查询本身不依赖外部模型。

## 当前运行边界

当前版本可以支撑“玩家打开对话 -> NPC 感知上下文 -> external agent/Ollama 选择一个 tool -> NPC 执行一个基础 Minecraft 动作 -> tick loop 推进任务”的闭环。

最需要继续补齐的是：

- agent 一次返回多 action 或计划队列的协议。
- task 结果反馈给 agent 的二次回调。
- 玩家交互直接映射到 `PLAYER` 优先级任务。
- 背包、物品、方块、威胁、睡觉、工作站等 Minecraft 核心玩法能力。
- `memory_updates` 自动写入本地记忆。
