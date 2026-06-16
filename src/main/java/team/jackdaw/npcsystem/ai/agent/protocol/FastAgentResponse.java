package team.jackdaw.npcsystem.ai.agent.protocol;

import java.util.Map;

public class FastAgentResponse {
    public int v = 1;
    public String rid;
    public String mode = "fast";
    public String a;
    public String kind;
    public String name;
    public Map<String, Object> args;
    public Boolean callback;
    public String speech;
    public String note;
}
