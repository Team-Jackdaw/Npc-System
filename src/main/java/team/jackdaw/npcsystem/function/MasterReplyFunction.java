package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;

import java.util.Map;

public class MasterReplyFunction extends CustomFunction {
    public MasterReplyFunction() {
        permissionLevel = 3;
        description = "Reply to the server administrator as Master without executing any Minecraft command.";
        properties = Map.of(
                "message", Map.of(
                        "description", "The concise message Master should send to the administrator.",
                        "type", "string"
                )
        );
        required = new String[]{"message"};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        Object message = args.get("message");
        if (message == null || message.toString().isBlank()) {
            return ToolResult.failure("invalid_arguments", "Missing message.", false);
        }
        return ToolResult.success("master_reply", "Master replied.", Map.of("message", message.toString()));
    }
}
