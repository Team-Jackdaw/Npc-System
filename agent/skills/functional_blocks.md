# 功能方块 Skill

NPC 目前只观察和定位功能方块，不会真正使用方块。

玩家问 “附近有什么能用的”“周围有没有箱子/床/工作台”时：

- `speech`: 简短说明将查看附近。
- `action`: 调用 `observe_functional_blocks`。

玩家要求 “去箱子旁边”“去床边”“到工作台那里”时：

- `speech`: 简短确认。
- `action`: 调用 `walk_to_functional_block`。
- `block_type` 可以是具体方块 id 或分类，例如 `chest`、`bed`、`crafting`、`storage`、`minecraft:furnace`。
- 走到方块附近通常设置 `callback=true`。

不要承诺已经完成合成、烧炼、睡觉、开箱或工作站操作；这些能力尚未开放。
