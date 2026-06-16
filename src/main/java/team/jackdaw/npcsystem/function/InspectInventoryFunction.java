package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.sensor.NpcInventorySummary;

import java.util.Map;

public class InspectInventoryFunction extends NpcTaskFunction {
    public InspectInventoryFunction() {
        description = "Inspect the NPC inventory.";
        properties = Map.of();
        required = new String[]{};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        return currentNpc(conversation)
                .map(this::inspect)
                .orElseGet(() -> failure("npc_not_found", "No NPC is associated with this conversation.", false));
    }

    private Map<String, Object> inspect(NPCEntity npc) {
        NpcInventorySummary summary = NpcInventorySummary.from(npc.getInventory());
        return success("inventory", "NPC inventory inspected.", Map.of(
                "occupied_slots", summary.occupiedSlots(),
                "total_slots", summary.totalSlots(),
                "total_items", summary.totalItems(),
                "items", summary.items()
        ));
    }
}
