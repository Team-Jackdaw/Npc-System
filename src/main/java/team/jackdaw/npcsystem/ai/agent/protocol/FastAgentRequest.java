package team.jackdaw.npcsystem.ai.agent.protocol;

import java.util.List;
import java.util.Map;

public class FastAgentRequest {
    public int v = 1;
    public String rid;
    public String mode = "fast";
    public Npc npc;
    public List<List<Object>> evt;
    public Near near;
    public List<String> tools;
    public AgentLimits limits;

    public static class Npc {
        public String id;
        public String name;
        public String task;
        public double hp;
        public List<Double> pos;
        public String dim;
    }

    public static class Near {
        public List<String> p;
        public List<String> n;
        public List<String> e;
    }
}
