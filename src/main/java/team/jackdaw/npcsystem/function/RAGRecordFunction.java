package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.master.MasterCW;
import team.jackdaw.npcsystem.rag.RAG;

import java.util.Map;

public class RAGRecordFunction extends CustomFunction{
    public RAGRecordFunction() {
        description = "Record something to the RAG database (your memory). Call this function when you want to save some knowledge for other conversation. For example, when user correct your response, you can record the correct response to the RAG database.";
        properties = Map.of(
                "context", Map.of(
                        "description", "The context you want to record.",
                        "type", "string"
                )
        );
        required = new String[]{"context"};
    }
    @Override
    public Map<String, String> execute(ConversationWindow conversation, Map<String, Object> args) {
        String context = (String) args.get("context");
        String className;
        if (conversation instanceof MasterCW) {
            className = "Master";
        }
        else {
            className = conversation.getAgent().getUUID().toString().toUpperCase();
        }
        try {
            RAG.record(context, className);
            return SUCCESS;
        } catch (Exception e) {
            return FAILURE;
        }
    }
}
