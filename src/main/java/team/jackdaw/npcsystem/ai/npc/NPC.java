package team.jackdaw.npcsystem.ai.npc;

import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.entity.NPCEntity;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NPC extends Agent {
    private static final int MAX_OBSERVATIONS = 12;
    private final ArrayDeque<TimedObservation> observations = new ArrayDeque<>();
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
        if (observation == null || observation.isBlank()) {
            return;
        }
        String latest = observations.peekLast() == null ? null : observations.peekLast().observation();
        if (observation.equals(latest)) {
            return;
        }
        observations.addLast(new TimedObservation(time, observation));
        while (observations.size() > MAX_OBSERVATIONS) {
            observations.removeFirst();
        }
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
            builder.append("当前观察:\n").append(entity.getLastObservation()).append("\n");
        }
        if (!observations.isEmpty()) {
            builder.append("最近观察:\n");
            observations.forEach(observation -> builder.append("- [")
                    .append(observation.gameTime())
                    .append("] ")
                    .append(observation.observation())
                    .append("\n"));
        }
        return builder.toString().trim();
    }

    private record TimedObservation(long gameTime, String observation) {
    }
}
