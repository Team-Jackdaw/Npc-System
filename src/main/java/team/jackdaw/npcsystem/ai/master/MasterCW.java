package team.jackdaw.npcsystem.ai.master;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.assistant.Summarise;
import team.jackdaw.npcsystem.rag.RAG;

public class MasterCW extends ConversationWindow {
    MasterCW() {
        super(Master.getMaster().getUUID());
    }

    @Override
    public boolean discard() {
        String instruction = """
                This is a conversation between the Master and a server manager player (OP). Summarise the conversation.
                """;
        try {
            RAG.record(Summarise.summariesConversation(instruction, messages), "Master");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
