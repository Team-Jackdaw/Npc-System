package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.sensor.FunctionalBlockScanner;

import java.util.Map;

public class ObserveFunctionalBlocksFunction extends NpcTaskFunction {
    public ObserveFunctionalBlocksFunction() {
        description = "Observe nearby functional blocks such as beds, chests, crafting tables, furnaces, and workstations.";
        properties = Map.of(
                "radius", Map.of("description", "Search radius in blocks.", "type", "integer")
        );
        required = new String[]{};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        return currentNpc(conversation)
                .map(npc -> observe(npc, integer(args.get("radius"), FunctionalBlockScanner.DEFAULT_RADIUS)))
                .orElseGet(() -> failure("npc_not_found", "No NPC is associated with this conversation.", false));
    }

    private Map<String, Object> observe(NPCEntity npc, int radius) {
        var blocks = FunctionalBlockScanner.scan(npc, radius, FunctionalBlockScanner.MAX_RESULTS);
        var data = blocks.stream()
                .map(block -> Map.<String, Object>of(
                        "block_id", block.blockId(),
                        "category", block.category(),
                        "x", block.pos().getX(),
                        "y", block.pos().getY(),
                        "z", block.pos().getZ(),
                        "distance", block.distance()
                ))
                .toList();
        return success(blocks.isEmpty() ? "none_found" : "functional_blocks", "Functional blocks observed.", Map.of("blocks", data));
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }
}
