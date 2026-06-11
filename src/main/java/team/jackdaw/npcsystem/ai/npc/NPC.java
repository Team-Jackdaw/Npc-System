package team.jackdaw.npcsystem.ai.npc;

import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.sensor.ObservationEvent;
import team.jackdaw.npcsystem.entity.sensor.ObservationType;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NPC extends Agent {
    private static final int MAX_RECENT_EVENTS = 32;
    private static final int MAX_IMPORTANT_EVENTS = 32;
    private final ArrayDeque<ObservationEvent> recentEvents = new ArrayDeque<>();
    private final ArrayDeque<ObservationEvent> importantEvents = new ArrayDeque<>();
    private Status status;

    public NPC(UUID uuid) {
        this.uuid = uuid;
        setTools(List.of(
                "end_conversation",
                "rag_query",
                "rag_record",
                "say",
                "look_at_player",
                "look_at_npc",
                "walk_to_player",
                "walk_to_npc",
                "follow_player",
                "wait",
                "stop_task"
        ));
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
        observe(List.of(new ObservationEvent(ObservationType.STATUS_CHANGED, 3, observation, time)));
    }

    public void observe(List<ObservationEvent> events) {
        for (ObservationEvent event : events) {
            if (event == null || event.text() == null || event.text().isBlank()) {
                continue;
            }
            recentEvents.addLast(event);
            while (recentEvents.size() > MAX_RECENT_EVENTS) {
                recentEvents.removeFirst();
            }
            if (event.importance() >= 7) {
                importantEvents.addLast(event);
                while (importantEvents.size() > MAX_IMPORTANT_EVENTS) {
                    importantEvents.removeFirst();
                }
            }
        }
    }

    public List<ObservationEvent> recentEvents() {
        return List.copyOf(recentEvents);
    }

    public List<ObservationEvent> importantEvents() {
        return List.copyOf(importantEvents);
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
        return "Your are a Minecraft NPC. You can talk in Chinese and keep responses within 30 words. Use tools when you need to move, look at someone, follow, wait, remember, or query memory.";
    }

    @Override
    public ConversationWindow createConversationWindows() {
        return new ConversationWindow(uuid);
    }

    public String getContextPrompt() {
        StringBuilder builder = new StringBuilder();
        NPCEntity entity = NPC_AI.getNPCEntity(this);
        if (entity != null) {
            builder.append("当前任务: ").append(entity.getTaskController().status()).append("\n");
            builder.append("当前状态摘要:\n").append(entity.getLastObservation()).append("\n");
        }
        if (!importantEvents.isEmpty()) {
            builder.append("重要事件:\n");
            importantEvents.forEach(event -> builder.append("- [")
                    .append(event.gameTime())
                    .append("] ")
                    .append(event.type())
                    .append(": ")
                    .append(event.text())
                    .append("\n"));
        }
        if (!recentEvents.isEmpty()) {
            builder.append("最近事件:\n");
            recentEvents.stream().skip(Math.max(0, recentEvents.size() - 8)).forEach(event -> builder.append("- [")
                    .append(event.gameTime())
                    .append("] ")
                    .append(event.type())
                    .append(": ")
                    .append(event.text())
                    .append("\n"));
        }
        return builder.toString().trim();
    }
}
