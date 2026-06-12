# NPC System 实机测试清单

Last updated: 2026-06-12 09:34:13 CST

本文面向服务器管理员，用于安装、运行和逐项验证 NPC System。

## 安装与准备

1. 准备 Minecraft 26.1.2 Fabric 服务端。
2. 安装与项目匹配的 Fabric Loader 和 Fabric API。
3. 使用 Java 25 启动服务端。
4. 将构建产物放入服务端 `mods/` 目录：
   - 本项目构建命令：`./gradlew build`
   - 产物位置：`build/libs/`
5. 首次启动服务端，让插件生成配置目录：
   - `config/npc-system/config.json`
   - `config/npc-system/memory/`
6. 如需外部 agent，先启动 Python agent 服务：
   - `PYTHONPATH=agent python -m npc_agent.server`
   - 默认地址：`http://127.0.0.1:8765`
   - 默认上下文目录：`config/npc-system/agent-state`

## 关键配置

配置文件：`config/npc-system/config.json`

常用字段：

- `enabled`: 是否启用插件。
- `debug`: 是否开启详细交互日志。
- `agentEnabled`: 是否启用外部 agent。
- `agentBaseUrl`: 外部 agent 地址。
- `agentMode`: 普通 NPC 使用的 `fast` 或 `deliberate`；Master 对话固定使用 deliberate。
- `range`: NPC 聊天和感知范围。
- `isBubble`: 是否显示头顶气泡。
- `isChatBar`: 是否在聊天栏显示 NPC 发言。

## 管理命令

- `/npc`: 查看插件状态、debug 状态、agent 地址、显示设置。
- `/npc spawn`: 在管理员当前位置生成 NPC。
- `/npc debug`: 查询当前维度中距离命令源最近的 NPC 调试信息。
- `/npc debug on`: 开启详细日志，记录玩家/NPC/agent 交互。
- `/npc debug off`: 关闭详细日志，只保留失败日志。
- `/npc master <message>`: 与 Master agent 对话。
- `/npc saveAll`: 保存插件状态。
- `/npc group ...`: 管理 group 状态。

## 基础启动检查

1. 启动服务端，确认日志中没有 mod 加载错误。
2. 进入服务器，执行 `/npc`。
3. 确认显示：
   - `Enabled: Yes`
   - `Agent Base URL` 指向外部 agent 地址
   - `Agent State` 指向 `config/npc-system/agent-state`
   - `Debug Logging` 状态正确。
4. 执行 `/npc debug on`。
5. 查看服务端日志，确认 debug 开关开启信息出现。
6. Debug 详细日志同时写入：
   - 服务端标准日志：`logs/latest.log`
   - 插件专用日志：`config/npc-system/debug.log`

## NPC 生成与默认行为

1. 执行 `/npc spawn`。
2. 确认 NPC 出现在玩家附近。
3. 等待 10-20 秒，观察 NPC 是否会：
   - 等待；
   - 看向附近实体；
   - 随机短距离闲逛。
4. 执行 `/npc debug`。
5. 检查输出是否包含：
   - NPC 名称和 UUID；
   - 距离和坐标；
   - 当前 task；
   - nearby players / NPCs / heard chat；
   - last observation。

## 玩家对话测试

1. 玩家潜行攻击 NPC。
2. 预期：攻击被拦截，NPC 开始对话。
3. 玩家在聊天栏输入一句话，例如：`你好`。
4. 预期：
   - NPC 在聊天栏或气泡回复；
   - debug 日志记录玩家消息、NPC 听到聊天、NPC 回复。
5. 执行 `/npc debug`，确认 heard chat 或 observation 中出现相关信息。

## 外部 Agent 测试

1. 启动 Python agent 服务。
   - 默认 `NPC_AGENT_MODE=pydantic_ai`，需要本机 Ollama 或兼容模型服务可用。
   - 如只测试协议，可显式设置 `NPC_AGENT_MODE=stub`。
2. 在 `config.json` 中设置：
   - `agentEnabled=true`
   - `agentBaseUrl=http://127.0.0.1:8765`
   - `agentMode=deliberate`
3. 重启服务端或重新加载配置后进入游戏。
4. 执行 `/npc debug on`。
5. 潜行攻击 NPC 开启对话。
6. 输入：`跟着我`
7. 预期：
   - agent 返回 `say + follow_player`；
   - NPC 先说话；
   - NPC 开始跟随玩家；
   - 服务端日志记录 agent request、response、action result。
8. 检查 agent 状态目录：
   - `config/npc-system/agent-state/npc/<uuid>/messages.json`
   - `AGENTS.md`、`SOLU.md`、`SUMMARY.md`、`MEMORY.md`
   - `history.jsonl`
9. 首次生成时确认上述 Markdown 文件来自模板：
   - `agent/templates/npc/AGENTS.md`
   - `agent/templates/common/SOLU.md`
   - `agent/templates/common/SUMMARY.md`
   - `agent/templates/common/MEMORY.md`

## Master Agent 测试

1. 保持 Python agent 服务运行。
2. 设置：
   - `agentEnabled=true`
   - `agentMode=deliberate`
3. 执行 `/npc debug on`。
4. 执行 `/npc master 你好，介绍一下你能做什么`。
5. 预期：
   - 服务端日志记录 Master external agent request；
   - 请求中 `kind=master`、`permission=3`；
   - Master 回复管理员。
6. 执行 `/npc master /time set day`。
7. 预期：
   - agent 返回 `call_command`；
   - 服务端执行 `/time set day`；
   - 日志中出现 `call_command` action result。
8. 验证普通 NPC 不具备该权限：
   - 与普通 NPC 对话请求执行命令；
   - 预期普通 NPC 不应调用 `call_command`；
   - 如果 agent 错误返回，Java 侧应返回 `permission_denied`。

## Task Batch 回调测试

1. 继续上一项 follow 测试。
2. 等待 follow 任务超时或完成。
3. 预期：
   - NPC 进入短暂 SYSTEM wait；
   - Java 向 agent 发送 `TASK_BATCH_FINISHED` follow-up；
   - agent 返回新 action 或无动作；
   - 如果无有效后续任务，NPC 恢复默认行为。
4. 查看日志，确认出现：
   - `completed agent batch`
   - `Agent follow-up result`
   - 若失败，应出现失败原因和恢复默认行为日志。

## Agent 超时/失败恢复测试

1. 保持 `agentEnabled=true`。
2. 停止 Python agent 服务。
3. 触发一次 NPC 对话或等待已有 AGENT task 完成。
4. 预期：
   - 服务端记录 agent 请求失败或超时；
   - NPC 不应永久停留在 SYSTEM wait；
   - NPC 应恢复默认行为。
5. 执行 `/npc debug` 检查当前 task 是否回到 idle/default 相关状态。

## Agent Context 测试

1. 与 NPC 对话，请求它记住一条信息。
2. 如果外部 agent 返回 `memory_updates`，检查 agent 主线记忆：
   - `config/npc-system/agent-state/npc/<uuid>/MEMORY.md`
3. 调用 `end_conversation` 或等待 conversation timeout。
4. 预期：
   - Java 请求 `/agent/conversation/end`；
   - agent 将当前 `messages.json` 和 `SUMMARY.md` 总结进 `MEMORY.md`；
   - `messages.json` 被重置为 `[]`。
5. 确认 Java 端没有生成 `config/npc-system/memory/` 作为长期记忆来源。

## 显示测试

1. 设置 `isBubble=true`，确认 NPC 回复时出现头顶气泡。
2. 设置 `isChatBar=true`，确认聊天栏出现 `<NPC名> 消息`。
3. 分别关闭两项，确认显示行为符合配置。

## 回归检查

每次反馈问题时，请记录：

- Minecraft 服务端版本。
- Fabric Loader / Fabric API 版本。
- Java 版本。
- 本 mod jar 文件名。
- `config/npc-system/config.json` 中 agent 相关字段。
- 复现步骤。
- `/npc debug` 输出。
- 服务端日志中对应的 `[npc-system]` 片段。

## 当前已知限制

- 还没有睡觉、工作站、物品、背包、战斗等复杂 Minecraft 行为。
- 玩家交互目前主要走对话和 agent，不直接生成 `PLAYER` 优先级 task。
- 外部 agent 每次最多返回 2 个 action，主要用于 `say + do`。
- agent 主动下发 action 的 polling mailbox/outbox 尚未实现；当前仍依赖 Java 主动请求和 task callback。
