package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.task.WalkToEntityTask;

import java.util.Map;

public class WalkToPlayerFunction extends NpcTaskFunction {
    public WalkToPlayerFunction() {
        description = "Make this NPC walk near an online player.";
        properties = Map.of(
                "player", Map.of("description", "The target player's name.", "type", "string"),
                "stop_distance", Map.of("description", "Distance in blocks to stop from the player.", "type", "number"),
                "timeout_seconds", Map.of("description", "Maximum seconds to try walking.", "type", "integer")
        );
        required = new String[]{"player"};
    }

    @Override
    public Map<String, String> execute(ConversationWindow conversation, Map<String, Object> args) {
        double stopDistance = number(args.get("stop_distance"), 2.0);
        int timeoutTicks = secondsToTicks(args.get("timeout_seconds"), 15);
        return findPlayer((String) args.get("player"))
                .map(player -> assign(conversation, new WalkToEntityTask(player, 0.6, stopDistance, timeoutTicks)))
                .orElseGet(() -> failure("Player not found: " + args.get("player")));
    }

    private static double number(Object value, double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }
}
