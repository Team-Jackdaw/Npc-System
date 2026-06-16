# NPC 系统总览

最后更新：2026-06-16 CST

本文是项目架构与实现状态的滚动总览。后续功能变更应同步更新本文。

## 当前架构

```text
Minecraft NPC Entity
  -> Sensor State
  -> Observation Events
  -> NPC Context Buffers
  -> External Agent Request
  -> Agent-side Conversation Context
  -> Tool / Task Action
  -> Priority Task Queue
  -> NpcTaskController
  -> Default Behavior
```

本项目是面向 Minecraft 26.1.2 的 Java 25 Fabric mod。Minecraft 侧代码位于 `src/main/java/team/jackdaw/npcsystem`，可选 Python 外部 agent 脚手架位于 `agent/`。

关键分层：

- **实体层**：`NPCEntity` 持有 sensor 状态、任务控制器、聊天显示和 NPC agent 对象桥接。
- **Sensor 层**：`NpcSensorState` 周期性记录附近玩家、NPC、实体、天气、生物群系、生命值、位置、任务状态、背包、附近功能方块和最近聊天。
- **Observe 层**：`ObservationCollector` 比较 sensor 快照并生成结构化 `ObservationEvent`。
- **AI/context 层**：`NPC` 保存最近事件和重要事件，并向 conversation 与外部 agent 请求暴露上下文。
- **外部 agent 层**：Java DTO 定义 fast/deliberate JSON 协议；Python FastAPI + Pydantic AI 接收请求并管理每个 NPC 的上下文。
- **Tool 层**：`FunctionManager` 暴露可调用工具和 descriptor，tool result 使用稳定的 `status/code/message/data/retryable` 结构。
- **Task 层**：`NpcTaskController` 同时只运行一个低层 Minecraft task，并用优先级和 FIFO 队列管理等待任务。
- **默认行为层**：空闲 NPC 可在不调用外部 agent 的情况下看向周围、闲逛或等待。

## 已实现

- 已迁移到 MC 26.1.2 / Fabric，使用 Java 25 与 Gradle 9.5.1。
- Java 侧本地 LLM 调用和本地长期记忆已移除；持久上下文由外部 agent 管理。
- 基础 NPC sensor 状态与 observe 事件缓冲。
- 玩家、NPC、聊天、天气、生命值、任务状态和功能方块发现等事件类型。
- 项目自有 `NpcTask` 与 `NpcTaskController`。
- 任务来源与优先级：`PLAYER`、`AGENT`、`SYSTEM`、`DEFAULT`。
- 玩家/agent/system 任务 FIFO 队列；默认行为不进入持久队列。
- 自定义 NPC 禁用原版村民 Brain 行为，由项目任务控制器接管导航和看向控制。
- 基础 task：
  - 看向实体
  - 走向实体
  - 跟随实体
  - 等待
  - 空闲看向周围
  - 随机闲逛
  - 走向方块
  - 捡起附近掉落物
- NPC action tool：
  - `look_at_player`
  - `look_at_npc`
  - `walk_to_player`
  - `walk_to_npc`
  - `follow_player`
  - `wait`
  - `stop_task`
  - `resume_default_behavior`
  - `inspect_inventory`
  - `pickup_nearby_item`
  - `drop_item`
  - `give_item`
  - `observe_functional_blocks`
  - `walk_to_block`
  - `walk_to_functional_block`
- 外部 agent 协议：
  - 紧凑 `fast` 模式
  - 完整 `deliberate` 模式
  - `speech` 字段用于普通回复
  - 每次响应最多 2 个真实 action
  - AGENT 任务批次完成回调
- Python 外部 agent 脚手架：
  - FastAPI server
  - Pydantic schemas
  - 可选模型供应商：`ollama`、`deepseek`、`openai-compatible`
  - 每个 NPC/Master 独立上下文目录：`config/npc-system/agent-state`
  - 首次创建上下文时复制 `agent/templates`
  - 通过 `messages.json` 持久化 Pydantic AI `message_history`
  - 用大模型生成 `SUMMARY.md` 当前会话自然语言摘要
  - 会话结束时用大模型写入 `MEMORY.md` 长期自然语言记忆
  - `agent/skills/*.md` 作为共享 skill 注入 prompt
  - deterministic stub 决策路径
  - 可选 Pydantic AI 模型路径
- Java 外部 agent 集成：
  - `ExternalAgentClient`
  - `AgentRequestBuilder`
  - `AgentActionExecutor`
  - NPC 对话路由到外部 agent，不再 fallback 到 Java 本地 LLM。
- Master 外部 agent 集成：
  - 复用 NPC 的 fast/deliberate endpoint
  - `kind=master`、`permission=3`
  - 稳定 Java UUID 和 `agent-state/master/default` 上下文目录
  - 普通管理员对话使用 `speech`
  - 高权限 `call_command` 由 Java 权限和 agent 身份共同约束
- 文本气泡：
  - `TextBubbleEntity` 使用 TextDisplay
  - 支持背景色和透视配置
  - 已设置 billboard，使气泡朝向玩家视角
- 终端测试入口：
  - `scripts/server-smoke.sh` 可启动无客户端 dedicated server smoke test。
  - `scripts/agent-chain-smoke.sh` 可使用真实外部 agent 测试 NPC-agent-task callback 链路。
  - `/npc spawnAt <x> <y> <z> [count]` 支持 console 生成 NPC。
  - `/npc perf <count> [seconds]` 和 `/npc perf status` 支持基础 tick 性能采样。
  - `/npc test chat <message>` 和 `/npc test status` 支持模拟玩家与 NPC 对话。

## 待实现

- 更完整的实机验证：
  - mixin 描述符
  - 文本气泡显示
  - 任务移动与看向控制
  - 外部 agent HTTP 闭环
  - 禁用原版 Brain 后的长期行为稳定性
- 更多 sensor：
  - 装备
  - 光照
  - 敌对威胁详情
  - 物品实体
  - 状态效果
- 更多 observe 事件：
  - 背包变化
  - 看到/拾取物品
  - 危险升级
  - 路径失败详情
- 更多 task/tool：
  - 睡觉 / 起床
  - 使用方块
  - 使用手持物品
  - 带路
  - 复杂多步采集/合成/烧炼
- Agent loop 改进：
  - deliberate 模式触发策略
  - 更完整的工具执行结果反馈
  - NPC mailbox/outbox，让 agent 可以在没有 Java 主动请求时排队动作
- Python agent 改进：
  - 更强 prompt
  - 对真实模型后端的集成测试
  - auth 测试覆盖
  - 模型输出修复和 fallback 策略
- 测试设施改进：
  - 假玩家 bot 集成，用于覆盖潜行攻击、聊天栏、玩家移动等服务端交互。

## 当前默认值

- 外部 agent 默认关闭：`Config.agentEnabled = false`。
- 默认 agent endpoint：`http://127.0.0.1:8765`。
- 默认 Java agent 模式：`fast`。
- 默认 Python agent 模式：`pydantic_ai`。
- 默认 Python 模型供应商：`ollama`。
- Java 不直接调用本地 LLM。外部 agent 禁用或不可用时，NPC 使用固定 fallback 文本并恢复默认行为。

## 文档索引

- `docs/AGENT_INTERFACE.md`：外部 agent JSON 协议。
- `docs/FOUNDATION_PLAN.md`：sensor / observe / tool / task 基础能力设计。
- `docs/MIGRATION_REPORT.md`：MC 26.1.2 迁移报告。
- `docs/NPC_RUNTIME_CAPABILITIES.md`：当前游戏内 NPC 运行能力。
- `CHECK_LIST.md`：实机与终端 smoke 测试清单。
- `agent/README.md`：Python 外部 agent 脚手架使用说明。
