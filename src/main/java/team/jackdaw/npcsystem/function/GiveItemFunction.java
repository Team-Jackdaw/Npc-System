package team.jackdaw.npcsystem.function;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.NPCEntity;

import java.util.Map;
import java.util.Optional;

public class GiveItemFunction extends NpcTaskFunction {
    public GiveItemFunction() {
        description = "Give an item from this NPC inventory to an online player.";
        properties = Map.of(
                "player", Map.of("description", "The target player's name.", "type", "string"),
                "item", Map.of("description", "Item id, for example minecraft:apple or apple.", "type", "string"),
                "count", Map.of("description", "Count to give.", "type", "integer")
        );
        required = new String[]{"player", "item"};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        Optional<NPCEntity> npc = currentNpc(conversation);
        if (npc.isEmpty()) {
            return failure("npc_not_found", "No NPC is associated with this conversation.", false);
        }
        String playerName = stringArg(args, "player", "player_name", "target_player", "target");
        Optional<ServerPlayer> player = findPlayer(playerName);
        if (player.isEmpty()) {
            return failure("target_not_found", "Player not found: " + playerName, true);
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
        ItemStack toGive = removed.copy();
        boolean inserted = player.get().getInventory().add(toGive);
        if (!inserted || !toGive.isEmpty()) {
            DropItemFunction.drop(npc.get(), toGive.isEmpty() ? removed : toGive);
            int delivered = removed.getCount() - toGive.getCount();
            return success("given_partial", "Player inventory was full; leftover item dropped nearby.", Map.of("item", itemId, "count", Math.max(0, delivered)));
        }
        return success("given", "Item given to player.", Map.of("player", playerName, "item", itemId, "count", removed.getCount()));
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }
}
