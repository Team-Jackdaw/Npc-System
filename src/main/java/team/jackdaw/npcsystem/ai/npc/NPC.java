package team.jackdaw.npcsystem.ai.npc;

import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.ConversationWindow;

import java.util.Map;
import java.util.UUID;

public class NPC extends Agent {
    private Status status;

    public NPC(UUID uuid) {
        this.uuid = uuid;
        addTool("end_conversation");
    }

    @Deprecated
    public NPC() {
        this.uuid = UUID.randomUUID();
    }

    /**
     * Input the latest thing the intelligence has observed (in natural language). And store it in Observation Storage.
     *
     * @param time        the time of the observation
     * @param observation the observation
     */
    public void observe(long time, String observation) {
    }

    /**
     * Get the schedule of this day.
     *
     * @return the schedule map with every entry (start time, action)
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

    @Override
    public String getInstruction() {
        return "Your are a Minecraft NPC. You can talk something that you may know. Please talk in Chinese and words limit to 30.";
    }

    @Override
    public ConversationWindow createConversationWindows() {
        return new ConversationWindow(uuid);
    }
}
