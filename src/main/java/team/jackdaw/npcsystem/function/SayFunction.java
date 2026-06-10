package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.task.SpeakTask;

import java.util.Map;

public class SayFunction extends NpcTaskFunction {
    public SayFunction() {
        description = "Make this NPC say a short message in the Minecraft world.";
        properties = Map.of(
                "message", Map.of(
                        "description", "The message the NPC should say.",
                        "type", "string"
                )
        );
        required = new String[]{"message"};
    }

    @Override
    public Map<String, String> execute(ConversationWindow conversation, Map<String, Object> args) {
        Object message = args.get("message");
        if (message == null || message.toString().isBlank()) {
            return FAILURE;
        }
        return assign(conversation, new SpeakTask(message.toString()));
    }
}
