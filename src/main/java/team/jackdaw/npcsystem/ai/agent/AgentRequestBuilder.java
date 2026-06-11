package team.jackdaw.npcsystem.ai.agent;

import net.minecraft.server.level.ServerPlayer;
import team.jackdaw.npcsystem.NPCSystem;
import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.agent.protocol.AgentLimits;
import team.jackdaw.npcsystem.ai.agent.protocol.DeliberateAgentRequest;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentRequest;
import team.jackdaw.npcsystem.ai.master.Master;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.sensor.NpcSensorState;
import team.jackdaw.npcsystem.entity.sensor.ObservationEvent;
import team.jackdaw.npcsystem.entity.task.NpcTaskBatchResult;
import team.jackdaw.npcsystem.function.FunctionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AgentRequestBuilder {
    public FastAgentRequest fast(ConversationWindow conversation, String message, Agent agent) {
        FastAgentRequest request = new FastAgentRequest();
        request.rid = UUID.randomUUID().toString();
        request.mode = "fast";
        request.npc = fastAgent(agent);
        request.evt = recentFastEvents(agent);
        if (message != null && !message.isBlank()) {
            request.evt.add(List.of("CHAT_HEARD", 7, message, 0L));
        }
        request.near = fastNear(agent);
        request.tools = availableToolNames(conversation);
        request.limits = limits(2, 60);
        return request;
    }

    public FastAgentRequest fast(ConversationWindow conversation, NpcTaskBatchResult taskResult, NPC npc) {
        FastAgentRequest request = fast(conversation, "", npc);
        request.evt.add(List.of("TASK_BATCH_FINISHED", 8, taskResult.summary(), taskResult.gameTime()));
        return request;
    }

    public DeliberateAgentRequest deliberate(ConversationWindow conversation, String message, Agent agent) {
        DeliberateAgentRequest request = new DeliberateAgentRequest();
        request.request_id = UUID.randomUUID().toString();
        request.mode = "deliberate";
        request.npc = deliberateAgent(agent);
        request.observations = observations(agent);
        request.conversation = conversation(conversation, message);
        request.memory = memory();
        request.available_tools = FunctionManager.getInstance().getAgentToolDescriptors(availableToolNames(conversation));
        request.limits = limits(2, 200);
        return request;
    }

    public DeliberateAgentRequest deliberate(ConversationWindow conversation, NpcTaskBatchResult taskResult, NPC npc) {
        DeliberateAgentRequest request = deliberate(conversation, taskResult.summary(), npc);
        request.observations.recent_events = new ArrayList<>(request.observations.recent_events);
        request.observations.recent_events.add(Map.of(
                "type", "TASK_BATCH_FINISHED",
                "importance", 8,
                "text", taskResult.summary(),
                "game_time", taskResult.gameTime(),
                "facts", Map.of("batch_id", taskResult.batchId(), "status", taskResult.status(), "tasks", taskResult.tasks())
        ));
        return request;
    }

    private static FastAgentRequest.Npc fastAgent(Agent agent) {
        FastAgentRequest.Npc dto = new FastAgentRequest.Npc();
        dto.id = agent.getUUID().toString();
        dto.name = agentName(agent);
        dto.kind = agentKind(agent);
        dto.permission = agent.getPermissionLevel();
        NPCEntity entity = agent instanceof NPC npc ? NPC_AI.getNPCEntity(npc) : null;
        if (entity != null) {
            dto.task = entity.getTaskController().status();
            dto.hp = entity.getHealth();
            dto.pos = List.of((double) entity.blockPosition().getX(), (double) entity.blockPosition().getY(), (double) entity.blockPosition().getZ());
            dto.dim = entity.level().dimension().identifier().toString();
        } else {
            dto.task = "idle";
            dto.hp = 0.0;
            dto.pos = List.of();
            dto.dim = "unknown";
        }
        return dto;
    }

    private static FastAgentRequest.Near fastNear(Agent agent) {
        FastAgentRequest.Near near = new FastAgentRequest.Near();
        NPCEntity entity = agent instanceof NPC npc ? NPC_AI.getNPCEntity(npc) : null;
        if (entity == null) {
            near.p = List.of();
            near.n = List.of();
            near.e = List.of();
            return near;
        }
        NpcSensorState.Snapshot snapshot = entity.getSensorState().snapshot();
        near.p = compactEntities(snapshot.nearbyPlayers());
        near.n = compactEntities(snapshot.nearbyNpcs());
        near.e = compactEntities(snapshot.nearbyEntities());
        return near;
    }

    private static DeliberateAgentRequest.Npc deliberateAgent(Agent agent) {
        DeliberateAgentRequest.Npc dto = new DeliberateAgentRequest.Npc();
        dto.uuid = agent.getUUID().toString();
        dto.name = agentName(agent);
        dto.kind = agentKind(agent);
        dto.permission = agent.getPermissionLevel();
        dto.instruction = agent.getInstruction();
        NPCEntity entity = agent instanceof NPC npc ? NPC_AI.getNPCEntity(npc) : null;
        if (entity == null) {
            dto.status = Map.of("entity", "none", "task", "idle");
        } else {
            NpcSensorState.Snapshot snapshot = entity.getSensorState().snapshot();
            dto.status = Map.of(
                    "task", entity.getTaskController().status(),
                    "health", snapshot.health(),
                    "max_health", snapshot.maxHealth(),
                    "position", Map.of("x", snapshot.position().getX(), "y", snapshot.position().getY(), "z", snapshot.position().getZ()),
                    "dimension", snapshot.dimension(),
                    "biome", snapshot.biome(),
                    "weather", snapshot.weather()
            );
        }
        return dto;
    }

    private static DeliberateAgentRequest.Observations observations(Agent agent) {
        DeliberateAgentRequest.Observations observations = new DeliberateAgentRequest.Observations();
        NPCEntity entity = agent instanceof NPC npc ? NPC_AI.getNPCEntity(npc) : null;
        observations.summary = entity == null ? agent.getInstruction() : entity.getSensorState().snapshot().summary();
        if (agent instanceof NPC npc) {
            observations.recent_events = npc.recentEvents().stream().map(AgentRequestBuilder::event).toList();
            observations.important_events = npc.importantEvents().stream().map(AgentRequestBuilder::event).toList();
        } else {
            observations.recent_events = List.of();
            observations.important_events = List.of();
        }
        return observations;
    }

    private static DeliberateAgentRequest.Conversation conversation(ConversationWindow window, String message) {
        DeliberateAgentRequest.Conversation dto = new DeliberateAgentRequest.Conversation();
        dto.speaker = speakerName(window);
        dto.message = message == null ? "" : message;
        dto.history = List.of();
        return dto;
    }

    private static String speakerName(ConversationWindow window) {
        if (window.getTarget() == null) {
            return "";
        }
        if (NPCSystem.server != null) {
            ServerPlayer player = NPCSystem.server.getPlayerList().getPlayer(window.getTarget());
            if (player != null) {
                return player.getName().getString();
            }
        }
        return window.getTarget().toString();
    }

    private static DeliberateAgentRequest.Memory memory() {
        DeliberateAgentRequest.Memory memory = new DeliberateAgentRequest.Memory();
        memory.recent = List.of();
        memory.relevant = List.of();
        return memory;
    }

    private static AgentLimits limits(int maxActions, int maxReplyChars) {
        AgentLimits limits = new AgentLimits();
        limits.max_actions = maxActions;
        limits.max_reply_chars = maxReplyChars;
        return limits;
    }

    private static List<String> availableToolNames(ConversationWindow conversation) {
        return conversation.getAgent().getTools().stream()
                .filter(FunctionManager.getInstance()::isRegistered)
                .toList();
    }

    private static List<List<Object>> recentFastEvents(Agent agent) {
        List<List<Object>> events = new ArrayList<>();
        if (!(agent instanceof NPC npc)) {
            return events;
        }
        npc.recentEvents().stream().skip(Math.max(0, npc.recentEvents().size() - 8)).forEach(event -> events.add(List.of(
                event.type().name(),
                event.importance(),
                event.text(),
                event.gameTime()
        )));
        return events;
    }

    private static Map<String, Object> event(ObservationEvent event) {
        return Map.of(
                "type", event.type().name(),
                "importance", event.importance(),
                "text", event.text(),
                "game_time", event.gameTime(),
                "facts", event.facts()
        );
    }

    private static List<String> compactEntities(List<NpcSensorState.EntitySummary> entities) {
        return entities.stream()
                .map(entity -> entity.name() + "@" + String.format(Locale.ROOT, "%.1f", entity.distance()))
                .toList();
    }

    private static String agentName(Agent agent) {
        if (agent instanceof Master) {
            return "Master";
        }
        if (agent instanceof NPC npc) {
            NPCEntity entity = NPC_AI.getNPCEntity(npc);
            return entity == null ? npc.getUUID().toString() : entity.getName().getString();
        }
        return agent.getUUID().toString();
    }

    private static String agentKind(Agent agent) {
        return agent instanceof Master ? "master" : "npc";
    }
}
