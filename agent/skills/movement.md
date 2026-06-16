# 移动与朝向 Skill

玩家要求 NPC “看着我”“看这里”“别动看我”时：

- `speech`: 简短确认，例如“好，我看着你。”
- `action`: 调用 `look_at_player`，`seconds` 通常为 8-15。

玩家要求 “来我这里”“过来”“靠近我”时：

- `speech`: 简短确认，例如“我过来了。”
- `action`: 调用 `walk_to_player`，`callback=true`。
- 等 Java 回调任务完成后，如仍在对话中，再用 `look_at_player` 或 `wait` 保持自然停留。

玩家要求 “跟着我”“随我来”时：

- `speech`: 简短确认。
- `action`: 调用 `follow_player`，`seconds` 通常为 30-60，`stop_distance` 通常为 2-3。

玩家要求 “等一下”“停下”时：

- `speech`: 简短确认。
- `action`: 调用 `wait` 或 `stop_task`。
