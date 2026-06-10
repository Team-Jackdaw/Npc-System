package team.jackdaw.npcsystem.function;

import net.minecraft.server.level.ServerPlayer;
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
        ServerPlayer player = NPCSystem.server.getPlayerList().getPlayer(conversation.getTarget());
        try {
            if (player != null) {
                NPCSystem.server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), command);
            } else {
                NPCSystem.server.getCommands().performPrefixedCommand(NPCSystem.server.createCommandSourceStack(), command);
            }
            return SUCCESS;
        } catch (Exception e) {
            NPCSystem.LOGGER.error("[npc-system] Failed to execute command function", e);
            return FAILURE;
        }
    }
}
