package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.task.LookAtEntityTask;

import java.util.Map;

public class LookAtPlayerFunction extends NpcTaskFunction {
    public LookAtPlayerFunction() {
        description = "Make this NPC look at a nearby online player.";
        properties = Map.of(
                "player", Map.of("description", "The target player's name.", "type", "string"),
                "seconds", Map.of("description", "How long to keep looking at the player.", "type", "integer")
        );
        required = new String[]{"player"};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        String playerName = stringArg(args, "player", "player_name", "target_player", "target");
        return findPlayer(playerName)
                .map(player -> assign(conversation, new LookAtEntityTask(player, secondsToTicks(args.get("seconds"), 8))))
                .orElseGet(() -> failure("target_not_found", "Player not found: " + playerName, true));
    }
}
