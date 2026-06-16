package team.jackdaw.npcsystem.function;

import net.minecraft.core.BlockPos;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.task.WalkToBlockTask;

import java.util.Map;

public class WalkToBlockFunction extends NpcTaskFunction {
    public WalkToBlockFunction() {
        description = "Make this NPC walk near a block position.";
        properties = Map.of(
                "x", Map.of("description", "Block X coordinate.", "type", "integer"),
                "y", Map.of("description", "Block Y coordinate.", "type", "integer"),
                "z", Map.of("description", "Block Z coordinate.", "type", "integer"),
                "stop_distance", Map.of("description", "Distance in blocks to stop from the block.", "type", "number"),
                "timeout_seconds", Map.of("description", "Maximum seconds to try walking.", "type", "integer")
        );
        required = new String[]{"x", "y", "z"};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        Integer x = integerObject(args.get("x"));
        Integer y = integerObject(args.get("y"));
        Integer z = integerObject(args.get("z"));
        if (x == null || y == null || z == null) {
            return failure("invalid_position", "x, y, and z are required.", false);
        }
        double stopDistance = number(args.get("stop_distance"), 2.0);
        int timeoutTicks = secondsToTicks(args.get("timeout_seconds"), 20);
        return assign(conversation, new WalkToBlockTask(new BlockPos(x, y, z), 0.6, stopDistance, timeoutTicks));
    }

    private static Integer integerObject(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static double number(Object value, double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }
}
