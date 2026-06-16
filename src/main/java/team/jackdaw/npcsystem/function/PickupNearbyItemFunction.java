package team.jackdaw.npcsystem.function;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.task.PickupNearbyItemTask;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class PickupNearbyItemFunction extends NpcTaskFunction {
    public PickupNearbyItemFunction() {
        description = "Make this NPC pick up a nearby dropped item into its inventory.";
        properties = Map.of(
                "item", Map.of("description", "Optional item id, for example minecraft:apple or apple.", "type", "string"),
                "count", Map.of("description", "Maximum count to pick up.", "type", "integer"),
                "max_distance", Map.of("description", "Search radius in blocks.", "type", "number"),
                "timeout_seconds", Map.of("description", "Maximum seconds to try picking up.", "type", "integer")
        );
        required = new String[]{};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        Optional<NPCEntity> npc = currentNpc(conversation);
        if (npc.isEmpty()) {
            return failure("npc_not_found", "No NPC is associated with this conversation.", false);
        }
        String itemId = stringArg(args, "item", "item_id", "target_item");
        Item item = null;
        if (itemId != null) {
            Optional<Item> parsed = NpcItemUtil.item(itemId);
            if (parsed.isEmpty()) {
                return failure("invalid_item", "Unknown item: " + itemId, false);
            }
            item = parsed.get();
        }
        double maxDistance = number(args.get("max_distance"), 8.0);
        int count = Math.max(1, integer(args.get("count"), 64));
        int timeoutTicks = secondsToTicks(args.get("timeout_seconds"), 15);
        Item targetItem = item;
        return findItem(npc.get(), targetItem, maxDistance)
                .map(itemEntity -> assign(conversation, new PickupNearbyItemTask(itemEntity, targetItem, count, 0.6, timeoutTicks)))
                .orElseGet(() -> failure("target_not_found", "No matching dropped item was found nearby.", true));
    }

    private static Optional<ItemEntity> findItem(NPCEntity npc, Item item, double maxDistance) {
        return npc.level().getEntitiesOfClass(ItemEntity.class, npc.getBoundingBox().inflate(maxDistance), entity -> {
                    if (!entity.isAlive() || entity.getItem().isEmpty()) {
                        return false;
                    }
                    return item == null || entity.getItem().getItem() == item;
                })
                .stream()
                .sorted(Comparator.comparingDouble(npc::distanceToSqr))
                .findFirst();
    }

    private static double number(Object value, double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }
}
