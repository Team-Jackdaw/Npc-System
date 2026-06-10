package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.task.FollowEntityTask;

import java.util.Map;

public class FollowPlayerFunction extends NpcTaskFunction {
    public FollowPlayerFunction() {
        description = "Make this NPC follow an online player for a limited time.";
        properties = Map.of(
                "player", Map.of("description", "The target player's name.", "type", "string"),
                "seconds", Map.of("description", "How long to follow the player.", "type", "integer"),
                "stop_distance", Map.of("description", "Distance in blocks to keep from the player.", "type", "number")
        );
        required = new String[]{"player"};
    }

    @Override
    public Map<String, String> execute(ConversationWindow conversation, Map<String, Object> args) {
        int durationTicks = secondsToTicks(args.get("seconds"), 30);
        double stopDistance = number(args.get("stop_distance"), 3.0);
        return findPlayer((String) args.get("player"))
                .map(player -> assign(conversation, new FollowEntityTask(player, 0.6, stopDistance, durationTicks)))
                .orElse(FAILURE);
    }

    private static double number(Object value, double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }
}
