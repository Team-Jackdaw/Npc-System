package team.jackdaw.npcsystem.ai.master;

import team.jackdaw.npcsystem.AsyncTask;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.assistant.Mark;
import team.jackdaw.npcsystem.ai.assistant.Summarise;
import team.jackdaw.npcsystem.rag.RAG;

public class MasterCW extends ConversationWindow {
    MasterCW() {
        super(Master.getMaster().getUUID());
    }

    @Override
    public boolean discard() {
        AsyncTask.call(() -> {
            String instruction = """
                    This is a conversation between the Master and a server manager player (OP). Summarise the conversation.
                    """;
            try {
                String res = Summarise.summariesConversation(instruction, messages);
                int importance = Mark.markInteger(res, "You are a mark assistant, you should mark the prompt from 0 to 10 based on how importance it is as a conversation.");
                if (importance > 7) RAG.record(res, "Master");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return AsyncTask.nothingToDo();
        });
        return true;
    }
}
