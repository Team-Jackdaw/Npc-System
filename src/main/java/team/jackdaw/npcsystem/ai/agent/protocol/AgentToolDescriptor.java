package team.jackdaw.npcsystem.ai.agent.protocol;

import java.util.Map;

public class AgentToolDescriptor {
    public String name;
    public String kind;
    public String description;
    public Map<String, Map<String, Object>> parameters;
    public String[] required;
}
