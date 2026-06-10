package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.master.MasterCW;
import team.jackdaw.npcsystem.rag.RAG;

import java.util.List;
import java.util.Map;

public class RAGQueryFunction extends CustomFunction {
    public RAGQueryFunction() {
        description = "Query local memory then return up to 3 related chunks. Call this function when you want to search something you don't know.";
        properties = Map.of(
                "context", Map.of(
                        "description", "The context you want to know.",
                        "type", "string"
                )
        );
        required = new String[]{"context"};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
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
            return ToolResult.success(res.isEmpty() ? "memory_empty" : "memory_found", "Memory query completed.", Map.of("chunks", res));
        } catch (Exception e) {
            return ToolResult.failure("memory_query_failed", "Memory query failed.", true);
        }
    }
}
