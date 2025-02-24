package team.jackdaw.npcsystem.ai.npc;

import net.minecraft.util.annotation.Debug;
import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.npc.Action;
import team.jackdaw.npcsystem.ai.npc.Status;
import team.jackdaw.npcsystem.entity.NPCEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NPC implements Agent {

    private Status status;
    private NPCEntity entity;
    private List<String> tools;

    public NPC(NPCEntity npcEntity) {
        entity = npcEntity;
    }

    @Deprecated
    @Debug
    public NPC() {
        entity = null;
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

    /**
     * Get the entity of the NPC.
     * @return the entity of the NPC
     */
    public NPCEntity getEntity() {
        return entity;
    }

    /**
     * Use for only update the entity (with a same UUID) in case the previous one has removed (unloaded).
     * @param entity new entity (should have same UUID)
     */
    public void setEntity(NPCEntity entity) {
        this.entity = entity;
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

    @Override
    public boolean discard() {
        return false;
    }
}
