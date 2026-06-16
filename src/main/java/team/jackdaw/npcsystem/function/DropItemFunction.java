package team.jackdaw.npcsystem.function;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.NPCEntity;

import java.util.Map;
import java.util.Optional;

public class DropItemFunction extends NpcTaskFunction {
    public DropItemFunction() {
        description = "Drop an item from this NPC inventory onto the ground.";
        properties = Map.of(
                "item", Map.of("description", "Item id, for example minecraft:apple or apple.", "type", "string"),
                "count", Map.of("description", "Count to drop.", "type", "integer")
        );
        required = new String[]{"item"};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        Optional<NPCEntity> npc = currentNpc(conversation);
        if (npc.isEmpty()) {
            return failure("npc_not_found", "No NPC is associated with this conversation.", false);
        }
        String itemId = stringArg(args, "item", "item_id");
        Optional<Item> item = NpcItemUtil.item(itemId);
        if (item.isEmpty()) {
            return failure("invalid_item", "Unknown item: " + itemId, false);
        }
        int count = Math.max(1, integer(args.get("count"), 1));
        ItemStack removed = NpcItemUtil.remove(npc.get().getInventory(), item.get(), count);
        if (removed.isEmpty()) {
            return failure("item_not_found", "NPC does not have item: " + itemId, true);
        }
        drop(npc.get(), removed);
        return success("dropped", "Item dropped.", Map.of("item", itemId, "count", removed.getCount()));
    }

    static void drop(NPCEntity npc, ItemStack stack) {
        if (!(npc.level() instanceof ServerLevel level) || stack.isEmpty()) {
            return;
        }
        Vec3 look = npc.getLookAngle().normalize().scale(0.35);
        npc.spawnAtLocation(level, stack, new Vec3(look.x, 0.25, look.z));
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }
}
