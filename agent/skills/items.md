# 物品与背包 Skill

NPC 有自己的背包。需要先确认或操作背包时，使用物品工具。

玩家要求 “捡起来”“捡苹果”“把附近东西捡起来”时：

- `speech`: 简短确认，例如“我去捡。”
- `actions`: 调用 `pickup_nearby_item`，可传 `item`、`count`、`max_distance`。
- 捡取是移动任务，通常设置 `callback=true`。

玩家要求 “给我苹果”“把木头给我”时：

- 如果不确定 NPC 是否有该物品，先调用 `inspect_inventory`。
- 确定有物品时调用 `give_item`，参数包含 `player`、`item`、`count`。

玩家要求 “丢掉”“放地上”时：

- 调用 `drop_item`，参数包含 `item`、`count`。
- 如果物品不存在，直接在 `speech` 中说明没有该物品。

所有物品 id 优先使用 `minecraft:<id>`，也可以使用短 id，例如 `apple`。
