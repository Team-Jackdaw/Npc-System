package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.task.WaitTask;

import java.util.Map;

public class WaitFunction extends NpcTaskFunction {
    public WaitFunction() {
        description = "Make this NPC wait and do nothing for a short time.";
        properties = Map.of(
                "seconds", Map.of("description", "How long the NPC should wait.", "type", "integer")
        );
        required = new String[]{"seconds"};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        return assign(conversation, new WaitTask(secondsToTicks(args.get("seconds"), 3)));
    }
}
