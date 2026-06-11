package team.jackdaw.npcsystem.ai.agent.protocol;

import java.util.List;
import java.util.Map;

public class DeliberateAgentRequest {
    public int version = 1;
    public String request_id;
    public String mode = "deliberate";
    public Npc npc;
    public Observations observations;
    public Conversation conversation;
    public Memory memory;
    public List<AgentToolDescriptor> available_tools;
    public AgentLimits limits;

    public static class Npc {
        public String uuid;
        public String name;
        public String kind;
        public int permission;
        public String instruction;
        public Map<String, Object> status;
    }

    public static class Observations {
        public String summary;
        public List<Map<String, Object>> recent_events;
        public List<Map<String, Object>> important_events;
    }

    public static class Conversation {
        public String speaker;
        public String message;
        public List<Map<String, String>> history;
    }

    public static class Memory {
        public List<String> recent;
        public List<String> relevant;
    }
}
