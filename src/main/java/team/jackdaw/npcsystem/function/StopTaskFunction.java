package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;

import java.util.Map;

public class StopTaskFunction extends NpcTaskFunction {
    public StopTaskFunction() {
        description = "Stop this NPC's current Minecraft task.";
        properties = Map.of();
    }

    @Override
    public Map<String, String> execute(ConversationWindow conversation, Map<String, Object> args) {
        return currentNpc(conversation)
                .map(npc -> {
                    npc.getTaskController().cancel(npc);
                    return SUCCESS;
                })
                .orElse(FAILURE);
    }
}
