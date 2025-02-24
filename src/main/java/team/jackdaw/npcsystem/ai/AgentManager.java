package team.jackdaw.npcsystem.ai;

import org.jetbrains.annotations.NotNull;
import team.jackdaw.npcsystem.BaseManager;

import java.util.UUID;

public class AgentManager extends BaseManager<UUID, Agent> {
    private static final AgentManager INSTANCE = new AgentManager();

    private AgentManager() {}

    public void register(@NotNull Agent agent) {
        if (isRegistered(agent.getUUID())) return;
        register(agent.getUUID(), agent);
    }

    public static AgentManager getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean discard(UUID uuid){
        return get(uuid).discard();
    }
}
