package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.task.WalkToEntityTask;

import java.util.Map;

public class WalkToNpcFunction extends NpcTaskFunction {
    public WalkToNpcFunction() {
        description = "Make this NPC walk near another NPC by UUID or visible name.";
        properties = Map.of(
                "npc", Map.of("description", "The target NPC UUID or visible name.", "type", "string"),
                "stop_distance", Map.of("description", "Distance in blocks to stop from the NPC.", "type", "number"),
                "timeout_seconds", Map.of("description", "Maximum seconds to try walking.", "type", "integer")
        );
        required = new String[]{"npc"};
    }

    @Override
    public Map<String, String> execute(ConversationWindow conversation, Map<String, Object> args) {
        double stopDistance = number(args.get("stop_distance"), 2.0);
        int timeoutTicks = secondsToTicks(args.get("timeout_seconds"), 15);
        return findNpc((String) args.get("npc"))
                .map(npc -> assign(conversation, new WalkToEntityTask(npc, 0.6, stopDistance, timeoutTicks)))
                .orElseGet(() -> failure("NPC not found: " + args.get("npc")));
    }

    private static double number(Object value, double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }
}
