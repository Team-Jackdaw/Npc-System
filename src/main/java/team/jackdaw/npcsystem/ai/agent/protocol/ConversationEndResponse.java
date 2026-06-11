package team.jackdaw.npcsystem.ai.agent.protocol;

public class ConversationEndResponse {
    public int version = 1;
    public String request_id;
    public String status;
    public boolean memory_updated;
}
