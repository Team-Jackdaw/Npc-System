package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.master.MasterCW;
import team.jackdaw.npcsystem.rag.RAG;

import java.util.List;
import java.util.Map;

public class RAGQueryFunction extends CustomFunction {
    public RAGQueryFunction() {
        description = "Query the RAG database then return 3 top similar chunks. Call this function when user ask you something but you don't know.";
        properties = Map.of(
                "context", Map.of(
                        "description", "The context you want to know.",
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
            List<String> res = RAG.query(context, 3, className);
            return Map.of(
                    "chunk_1", res.get(0),
                    "chunk_2", res.get(1),
                    "chunk_3", res.get(2)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
