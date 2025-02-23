package team.jackdaw.npcsystem.ai;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public interface AgentManager {
    ConcurrentHashMap<UUID, Agent> AgentMap = new ConcurrentHashMap<>();

    /**
     * Check if an Agent is registered
     *
     * @param uuid The UUID of the Agent
     * @return True if the Agent is registered, false otherwise
     */
    static boolean isRegistered(UUID uuid) {
        return AgentMap.containsKey(uuid);
    }

    /**
     * Get an Agent entity by its UUID.
     *
     * @param uuid The UUID of the Agent entity
     * @return The Agent entity
     */
    static @Nullable Agent getAgentEntity(UUID uuid) {
        return AgentMap.get(uuid);
    }

    /**
     * Initialize an Agent entity if the Agent is not registered.
     *
     * @param agent The Agent to initialize
     */
    static void registerAgent(Agent agent) {
        if (isRegistered(agent.getUUID())) return;
        AgentMap.put(agent.getUUID(), agent);
    }

    /**
     * Remove an Agent from the map.
     *
     * @param uuid The UUID of the Agent entity to remove
     */
    static void removeAgentEntity(UUID uuid) {
        if (!AgentMap.containsKey(uuid)) return;
        if (AgentMap.get(uuid).discard()) {
            AgentMap.remove(uuid);
        }
    }

    /**
     * End all Agent.
     */
    static void endAllAgentEntity() {
        if (AgentMap.isEmpty()) return;
        AgentMap.forEach((uuid, Agent) -> removeAgentEntity(uuid));
    }
}
