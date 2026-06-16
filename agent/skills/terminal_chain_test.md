# 终端链路测试 Skill

当玩家消息包含 “终端复杂流程测试” 或 “agent链路测试” 时，必须执行这个测试 skill。

目标是验证：玩家消息 -> agent 选择 skill -> speech + action -> NPC 执行 task -> Java 回调 TASK_BATCH_FINISHED -> agent 继续下一步 -> 最后恢复默认行为。

测试玩家名固定为 `TerminalTester`。每轮只返回一个真实 action，并尽量设置 `callback=true`，让 Java 的 task 完成回调驱动下一轮。

推荐流程：

1. 第一次收到玩家测试指令时：
   - `speech`: `开始终端链路测试，我先等待一秒。`
   - `actions`: 调用 `wait`，参数 `{"seconds":1}`，`callback=true`
2. 收到等待任务完成的 `TASK_BATCH_FINISHED` 后：
   - `speech`: `我会看向你。`
   - `actions`: 调用 `look_at_player`，参数 `{"player":"TerminalTester","seconds":3}`，`callback=true`
3. 收到看向任务完成后：
   - `speech`: `我回到你这里。`
   - `actions`: 调用 `walk_to_player`，参数 `{"player":"TerminalTester","stop_distance":2,"timeout_seconds":20}`，`callback=true`
4. 收到回到玩家任务完成后：
   - `speech`: `我会短暂跟随你。`
   - `actions`: 调用 `follow_player`，参数 `{"player":"TerminalTester","seconds":3,"stop_distance":2}`，`callback=true`
5. 收到跟随任务完成后：
   - `speech`: `我最后再等待一秒。`
   - `actions`: 调用 `wait`，参数 `{"seconds":1}`，`callback=true`
6. 收到最后等待任务完成后：
   - `speech`: `终端链路测试完成。`
   - `actions`: 调用 `resume_default_behavior`，`callback=false`

不要把多轮动作一次性全部返回。不要调用 `say`。普通回复必须放在 `speech` 字段。
