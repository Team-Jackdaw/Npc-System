package team.jackdaw.npcsystem.function;

import net.minecraft.entity.player.PlayerEntity;
import team.jackdaw.npcsystem.NPCSystem;
import team.jackdaw.npcsystem.ai.ConversationWindow;

import java.util.Map;

// Master only
public class CallCommandFunction extends CustomFunction{
    public CallCommandFunction() {
        permissionLevel = 3;
        description = "Ask the server to execute a command as the player who you are talking to, or else as the server.";
        properties = Map.of(
                "command", Map.of(
                        "description", "This is the command you want the player to call (without the slash).",
                        "type", "string"
                )
        );
                required = new String[] { "command" };
    }
    @Override
    public Map<String, String> execute(ConversationWindow conversation, Map<String, Object> args) {
        String command = (String) args.get("command");
        PlayerEntity player = NPCSystem.server.getPlayerManager().getPlayer(conversation.getTarget());
        int var;
        if (player != null) var = NPCSystem.server.getCommandManager().executeWithPrefix(player.getCommandSource(), command);
        else var = NPCSystem.server.getCommandManager().executeWithPrefix(NPCSystem.server.getCommandSource(), command);
        if (var == 0) return FAILURE;
        return SUCCESS;
    }
}
