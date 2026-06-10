package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;

import java.util.Map;

public class StopTaskFunction extends NpcTaskFunction {
    public StopTaskFunction() {
        description = "Stop this NPC's current Minecraft task.";
        properties = Map.of();
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        return currentNpc(conversation)
                .map(npc -> {
                    npc.getTaskController().cancel(npc);
                    return ToolResult.success("task_stopped", "Current task stopped.");
                })
                .orElseGet(() -> failure("npc_not_found", "No NPC is associated with this conversation.", false));
    }
}
