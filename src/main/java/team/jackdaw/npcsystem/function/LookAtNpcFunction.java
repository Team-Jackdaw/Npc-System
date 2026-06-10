package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.task.LookAtEntityTask;

import java.util.Map;

public class LookAtNpcFunction extends NpcTaskFunction {
    public LookAtNpcFunction() {
        description = "Make this NPC look at another NPC by UUID or visible name.";
        properties = Map.of(
                "npc", Map.of("description", "The target NPC UUID or visible name.", "type", "string"),
                "seconds", Map.of("description", "How long to keep looking at the NPC.", "type", "integer")
        );
        required = new String[]{"npc"};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        return findNpc((String) args.get("npc"))
                .map(npc -> assign(conversation, new LookAtEntityTask(npc, secondsToTicks(args.get("seconds"), 3))))
                .orElseGet(() -> failure("target_not_found", "NPC not found: " + args.get("npc"), true));
    }
}
