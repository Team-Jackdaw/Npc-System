package team.jackdaw.npcsystem.ai;

import team.jackdaw.npcsystem.ai.npc.Action;
import team.jackdaw.npcsystem.ai.npc.Status;
import team.jackdaw.npcsystem.npcentity.NPCEntity;

public class NPC implements Agent {

    private Status status;
    private NPCEntity entity;

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

    /**
     * Get the real entity of the NPC in game.
     * @return the entity of the NPC
     */
    public NPCEntity getEntity() {
        return entity;
    }

    /**
     * Get the status of the NPC.
     * @return the status of the NPC
     */
    public Status getStatus() {
        return status;
    }

    @Override
    public ConversationWindow getConversationWindows() {
        return null;
    }
}
