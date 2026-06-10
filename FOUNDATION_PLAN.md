# NPC 基础能力架构计划

## 目标

当前系统已经具备基础对话、工具调用、本地文本记忆和 Minecraft 事件接入能力。下一阶段重点不再扩展对话本身，而是补齐 NPC 在 Minecraft 世界中的基础感知与行动能力，让外部 AI Agent 能够基于足够多的信息做决策，并通过稳定的工具接口驱动 NPC 行为。

## 核心分层

### Sensor

Sensor 是最底层的被动感知层，按 tick 定时更新，不直接调用 LLM。它只负责收集 Minecraft 世界中的原始状态，例如：

- 附近玩家
- 附近 NPC
- 可见实体
- 当前坐标、维度、方块环境
- 时间、天气、亮度
- 生命值、状态效果
- 最近听到的聊天内容

当前代码中已有 `NearestNPCSensor`，但它只是注册了 `SensorType`，还没有可靠接入 NPC brain，因此实际运行能力不足。短期建议不要优先依赖 Minecraft Brain Sensor，而是在 `NPCEntity.tick()` 中维护项目自己的 `NpcSensorState`。

### Observe

Observe 是主动观察层，基于 Sensor 的原始信息生成结构化观察或自然语言观察。它应该把底层状态转成 AI Agent 可理解的信息，例如：

```text
你在村庄附近。附近有玩家 Steve，距离 4 格，正在看着你。
附近有另一个 NPC Alice，距离 8 格。
当前是白天，天气晴朗。
你刚刚听到 Steve 说：“你好”。
```

Observe 不应该直接执行 Minecraft 行为。它的职责是为外部 Agent、记忆系统和 Planner 提供输入。

### Tool

Tool 是可供 NPC 或外部 Agent 调用的函数/指令接口。它偏系统能力，例如：

- 查询记忆
- 记录记忆
- 结束对话
- 执行命令
- 请求 NPC 执行某个基础动作

当前项目中 Tool 层实现相对完整，已有：

- `rag_query`
- `rag_record`
- `end_conversation`
- `call_command`
- 动态 JSON function 加载

后续应把 Minecraft 行为能力也封装成 Tool，例如 `walk_to`、`look_at`、`say`、`drop_item`，供外部 Agent 调用。

### Task

Task 是 NPC 在 Minecraft 世界中的低层动作，不直接代表 LLM 推理。它负责实际执行操作，例如：

- 走到某个实体或方块
- 看向某个实体
- 跟随某个目标
- 说话
- 丢出物品
- 等待
- 使用物品或交互方块

当前自定义 task 在 MC 26.1.2 迁移后已经变成占位类：

- `FollowChatTargetTask`
- `MeetNPCTask`
- `MeetPlayerTask`
- `NPCTaskListProvider`

这些类目前不可用。短期不建议直接恢复旧 Villager Brain task，而应实现项目自己的 `NpcTask` 和 `NpcTaskController`，由 `NPCEntity.tick()` 驱动。

## 推荐实现路线

### 1. 实现 `NpcSensorState`

在 `NPCEntity` 中维护一个传感器状态对象，每 20 tick 更新一次。

建议记录：

- nearby players
- nearby NPCs
- visible entities
- current position
- current block/location description
- health and status effects
- world time and weather
- recently heard chat

### 2. 实现 `ObservationCollector`

新增观察收集器，将前后两次 `NpcSensorState` 快照转换为事件，而不是每秒保存完整重复文本。

事件包含：

- `type`
- `importance`
- `text`
- `gameTime`
- `facts`

第一阶段事件类型包括玩家进入/离开范围、玩家看向 NPC、NPC 进入/离开范围、聊天、天气变化、生命值变化、任务开始/完成/失败/取消等。然后调用 `NPC.observe(events)`。

### 3. 补齐 `NPC.observe`

`NPC.observe` 当前为空。后续应实现：

- 保存短期 observation event buffer
- 保存高重要性 event buffer
- 不在每秒 observe 中调用 LLM
- 为外部 Agent 提供最近观察上下文

### 4. 实现 `NpcTask` 和 `NpcTaskController`

建议定义项目自己的 task 接口：

```java
interface NpcTask {
    boolean canStart(NPCEntity npc);
    void start(NPCEntity npc);
    void tick(NPCEntity npc);
    boolean isFinished(NPCEntity npc);
    void stop(NPCEntity npc);
}
```

`NpcTaskController` 负责：

- 持有当前 task
- 每 tick 驱动 task
- 处理 task 完成、失败、取消
- 提供 task 队列或优先级

### 5. 先实现基础 Task

建议按以下顺序实现：

1. `LookAtEntityTask`
2. `WalkToEntityTask`
3. `FollowEntityTask`
4. `SpeakTask`
5. `WaitTask`
6. `DropItemTask`
7. `MoveToBlockTask`
8. `UseItemTask`

这些基础 task 可以直接调用 Minecraft 的 navigation、look control 和实体 API，不必先接入 Villager Brain。

### 6. 用 Tool 包装 Task

外部 Agent 不应该直接操作 Minecraft tick 行为，而应该调用 Tool。Tool 接收参数后创建 Task，例如：

```text
walk_to_player(player_name)
look_at_entity(entity_id)
say(message)
drop_item(item, count)
follow_player(player_name)
```

Tool 的职责是校验参数、找到目标、创建 task，并交给 `NpcTaskController` 执行。

### 7. 恢复主动行为

在 Sensor、Observe 和 Task 稳定之后，再恢复主动行为：

- 看到玩家后主动打招呼
- 看到 NPC 后主动交流
- 根据最近观察决定是否记录记忆
- 根据外部 Agent 决策选择下一步 task
- 将任务执行结果反馈给 Agent

## 与外部 Agent 的关系

后续 AI 层计划调用外部 Agent 来完成高层决策。项目内应尽量提供稳定、清晰、可组合的输入输出：

- 输入：Sensor/Observe/Memory/Status/Conversation
- 输出：Tool 调用或高层 action
- 执行：Tool 将 action 转换为 Task
- 反馈：Task 执行结果回写 Observation 或 Conversation

外部 Agent 不应直接依赖 Minecraft 内部类。它应该只看到结构化上下文和可调用工具列表。

## 当前优先级

短期优先级：

1. `NpcSensorState`
2. `ObservationCollector`
3. `NPC.observe`
4. `NpcTask`
5. `NpcTaskController`
6. 基础移动、看向、说话、等待 task
7. Task Tool 封装

暂缓事项：

- 直接恢复旧 Villager Brain task
- 完整 Planner
- RelationTable
- 世界事件系统
- 复杂行动链

## 设计结论

后续应明确采用以下架构：

```text
Sensor: tick 被动感知
Observe: 主动解释感知并形成观察
Memory: 记录重要观察和总结
External Agent: 基于上下文进行高层决策
Tool: Agent 可调用的能力入口
Task: Minecraft 世界中的低层动作执行
```

当前项目已经实现了 `Tool + Conversation + Memory` 的基础能力。下一阶段应集中补齐 `Sensor + Observe + Task`，让外部 Agent 拥有足够的信息输入和足够稳定的操作出口。
