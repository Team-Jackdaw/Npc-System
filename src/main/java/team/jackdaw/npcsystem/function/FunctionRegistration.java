package team.jackdaw.npcsystem.function;

public class FunctionRegistration {
    static {
        FunctionManager.getInstance().register("rag_query", new RAGQueryFunction());
        FunctionManager.getInstance().register("rag_record", new RAGRecordFunction());
        FunctionManager.getInstance().register("end_conversation", new EndConversationFunction());
        FunctionManager.getInstance().register("call_command", new CallCommandFunction());
    }
}
