package team.jackdaw.npcsystem.ai.agent.protocol;

import java.util.List;

public class DeliberateAgentResponse {
    public int version = 1;
    public String request_id;
    public String mode = "deliberate";
    public AgentAction action;
    public String speech;
    public List<String> memory_updates;
    public String reasoning_summary;
}
