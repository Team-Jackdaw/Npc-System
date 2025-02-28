package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.Map;
import java.util.Objects;

public class EndConversationFunction extends CustomFunction {
    public EndConversationFunction() {
        description = "When you want to end the conversation, call this function.";
        properties = Map.of();
    }
    @Override
    public Map<String, String> execute(ConversationWindow conversation, Map<String, Object> args) {
        Objects.requireNonNull(NPC_AI.getNPCEntity((NPC) conversation.getAgent()))
                .getBrain()
                .remember(NPCRegistration.MEMORY_IS_CHATTING, false);
        return Map.of(
                "message", "The conversation has ended. You can say goodbye to the other."
        );
    }
}
