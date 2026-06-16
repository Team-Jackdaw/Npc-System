package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.sensor.FunctionalBlockScanner;
import team.jackdaw.npcsystem.entity.task.WalkToBlockTask;

import java.util.Map;

public class WalkToFunctionalBlockFunction extends NpcTaskFunction {
    public WalkToFunctionalBlockFunction() {
        description = "Make this NPC walk to the nearest matching functional block.";
        properties = Map.of(
                "block_type", Map.of("description", "Block id or category, for example chest, bed, crafting, storage, minecraft:furnace.", "type", "string"),
                "radius", Map.of("description", "Search radius in blocks.", "type", "integer"),
                "stop_distance", Map.of("description", "Distance in blocks to stop from the block.", "type", "number"),
                "timeout_seconds", Map.of("description", "Maximum seconds to try walking.", "type", "integer")
        );
        required = new String[]{"block_type"};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        return currentNpc(conversation)
                .map(npc -> walk(conversation, npc, args))
                .orElseGet(() -> failure("npc_not_found", "No NPC is associated with this conversation.", false));
    }

    private Map<String, Object> walk(ConversationWindow conversation, NPCEntity npc, Map<String, Object> args) {
        String type = stringArg(args, "block_type", "type", "block", "category");
        if (type == null) {
            return failure("invalid_block_type", "block_type is required.", false);
        }
        int radius = integer(args.get("radius"), FunctionalBlockScanner.DEFAULT_RADIUS);
        double stopDistance = number(args.get("stop_distance"), 2.0);
        int timeoutTicks = secondsToTicks(args.get("timeout_seconds"), 20);
        return FunctionalBlockScanner.scan(npc, radius, 32)
                .stream()
                .filter(summary -> FunctionalBlockScanner.matchesType(summary, type))
                .findFirst()
                .map(summary -> assign(conversation, new WalkToBlockTask(summary.pos(), 0.6, stopDistance, timeoutTicks)))
                .orElseGet(() -> failure("target_not_found", "No matching functional block was found nearby: " + type, true));
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static double number(Object value, double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }
}
