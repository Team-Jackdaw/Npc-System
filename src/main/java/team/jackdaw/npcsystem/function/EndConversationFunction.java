package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.ai.ConversationManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.Map;
import java.util.Objects;

public class EndConversationFunction extends CustomFunction {
    public EndConversationFunction() {
        description = "When you want to end the conversation, call this function. Also call this function when someone say goodbye to you.";
        properties = Map.of();
    }
    @Override
    public Map<String, String> execute(ConversationWindow conversation, Map<String, Object> args) {
        if (conversation.getAgent() instanceof NPC npc) {
            NPC_AI.getNPCEntity(npc).getBrain().remember(NPCRegistration.MEMORY_IS_CHATTING, false);
        }
        if (ConversationManager.getInstance().isRegistered(conversation.getTarget())) {
            ConversationManager.getInstance().get(conversation.getTarget()).resetUpdateTime();
        }
        conversation.resetUpdateTime();
        return SUCCESS;
    }
}
