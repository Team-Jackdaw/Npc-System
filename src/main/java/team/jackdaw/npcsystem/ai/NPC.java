package team.jackdaw.npcsystem.ai;

import lombok.Getter;
import team.jackdaw.npcsystem.ai.npc.Action;
import team.jackdaw.npcsystem.ai.npc.Status;
import team.jackdaw.npcsystem.npcentity.NPCEntity;

import java.util.List;
import java.util.UUID;

public class NPC implements Agent {

    @Getter
    private Status status;
    @Getter
    private NPCEntity entity;
    private List<String> tools;

    /**
     * Input the latest thing the intelligence has observed (in natural language). And store it in Observation Storage.
     *
     * @param time        the time of the observation
     * @param observation the observation
     */
    public void observe(long time, String observation) {

    }

    /**
     * Get the next action to take.
     *
     * @return the next action to take
     */
    public Action nextAction() {
        return null;
    }

    /**
     * Add an event to the event list. NPC should maintain a opinion on every event.
     *
     * @param event the event to add
     */
    public void addEvent(String event) {

    }

    @Override
    public UUID getUUID() {
        return entity.getUuid();
    }

    @Override
    public ConversationWindow getConversationWindows() {
        return null;
    }

    @Override
    public void addTool(String tool) {

    }

    @Override
    public List<String> getTools() {
        return tools;
    }
}
