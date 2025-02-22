package team.jackdaw.npcsystem.ai;

import team.jackdaw.npcsystem.ai.npc.Action;
import team.jackdaw.npcsystem.ai.npc.Status;
import team.jackdaw.npcsystem.npcentity.NPCEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NPC implements Agent {

    private Status status;
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
     * Get the schedule of this day.
     *
     * @return the schedule
     */
    public Map<Integer, Action> getSchedule() {
        return null;
    }

    /**
     * Add an event to the event list. NPC should maintain a opinion on every event.
     *
     * @param event the event to add
     */
    public void addEvent(String event) {

    }

    /**
     * Get the status of the NPC.
     * @return the status of the NPC
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Get the entity of the NPC.
     * @return the entity of the NPC
     */
    public NPCEntity getEntity() {
        return entity;
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
