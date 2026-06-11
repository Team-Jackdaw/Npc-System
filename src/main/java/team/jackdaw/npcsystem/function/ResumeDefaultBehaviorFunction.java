package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.task.TaskSource;

import java.util.Map;
import java.util.Optional;

public class ResumeDefaultBehaviorFunction extends CustomFunction {
    public ResumeDefaultBehaviorFunction() {
        description = "Let this NPC stop waiting for agent instructions and resume default behavior.";
        properties = Map.of();
        required = new String[]{};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        return currentNpc(conversation)
                .map(npc -> {
                    TaskSource source = npc.getTaskController().currentSource();
                    if (source == TaskSource.SYSTEM || source == TaskSource.DEFAULT || source == null) {
                        npc.getTaskController().cancel(npc);
                    }
                    return ToolResult.success("default_behavior_resumed", "Default behavior resumed.");
                })
                .orElseGet(() -> ToolResult.failure("npc_not_found", "No NPC is associated with this conversation.", false));
    }

    private Optional<NPCEntity> currentNpc(ConversationWindow conversation) {
        if (conversation == null) {
            return Optional.empty();
        }
        Agent agent = conversation.getAgent();
        if (!(agent instanceof NPC npc)) {
            return Optional.empty();
        }
        return Optional.ofNullable(NPC_AI.getNPCEntity(npc));
    }
}
