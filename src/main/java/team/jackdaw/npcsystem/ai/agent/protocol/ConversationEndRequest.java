package team.jackdaw.npcsystem.ai.agent.protocol;

import java.util.Map;

public class ConversationEndRequest {
    public int version = 1;
    public String request_id;
    public Npc npc;
    public String reason;
    public Map<String, Object> snapshot;

    public static class Npc {
        public String uuid;
        public String id;
        public String name;
        public String kind;
        public int permission;
    }
}
