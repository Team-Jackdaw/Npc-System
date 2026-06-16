package team.jackdaw.npcsystem.ai;

import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.AsyncTask;
import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.NPCSystem;
import team.jackdaw.npcsystem.ai.agent.AgentActionExecutor;
import team.jackdaw.npcsystem.ai.agent.AgentRequestBuilder;
import team.jackdaw.npcsystem.ai.agent.ExternalAgentClient;
import team.jackdaw.npcsystem.ai.agent.protocol.ConversationEndRequest;
import team.jackdaw.npcsystem.ai.agent.protocol.DeliberateAgentResponse;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentResponse;
import team.jackdaw.npcsystem.ai.master.Master;
import team.jackdaw.npcsystem.entity.task.NpcTask;
import team.jackdaw.npcsystem.entity.task.NpcTaskAssignment;
import team.jackdaw.npcsystem.entity.task.NpcTaskBatchResult;
import team.jackdaw.npcsystem.entity.task.TaskSource;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.ai.npc.NPC;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

public class ConversationWindow {
    private static final AgentRequestBuilder AGENT_REQUEST_BUILDER = new AgentRequestBuilder();
    private static final AgentActionExecutor AGENT_ACTION_EXECUTOR = new AgentActionExecutor();
    private static final ExternalAgentClient EXTERNAL_AGENT_CLIENT = new ExternalAgentClient();
    private static final List<String> OPENING_LINES = List.of(
            "你好，有什么需要我帮忙的吗？",
            "你好，想聊些什么？",
            "我在这儿，需要我做什么？"
    );
    private static final String AGENT_UNAVAILABLE_REPLY = "我现在有点走神，稍后再说。";
    protected final UUID uuid;
    protected long updateTime = 0L;
    protected UUID target;
    private boolean onWait = false;
    private String activeAgentBatchId;
    private boolean activeAgentBatchCallback;
    private boolean externalAgentEndNotified = false;
    private String lastUserMessage = "";
    private String lastAssistantMessage = "";
    private String lastAgentResultSummary = "";
    private String syntheticSpeakerName = "";

    public ConversationWindow(UUID uuid) {
        this.uuid = uuid;
    }

    public Agent getAgent() {
        return AgentManager.getInstance().get(uuid);
    }

    public UUID getTarget() {
        return target;
    }

    public void setTarget(UUID target) {
        this.target = target;
    }

    public String getSyntheticSpeakerName() {
        return syntheticSpeakerName;
    }

    public void setSyntheticSpeakerName(String syntheticSpeakerName) {
        this.syntheticSpeakerName = syntheticSpeakerName == null ? "" : syntheticSpeakerName;
    }

    public String getLastMessage() {
        return lastAssistantMessage;
    }

    public boolean isOnWait() {
        return onWait;
    }

    public void onWait() {
        onWait = true;
    }

    public void offWait() {
        onWait = false;
    }

    /**
     * Say a message to the agent in this conversation
     *
     * @return The response
     */
    public String chat(String message) {
        updateTime = System.currentTimeMillis();
        lastUserMessage = message == null ? "" : message;
        NPCSystem.debugLog("[npc-system] Conversation {} received player message: {}", uuid, message);
        if (Config.agentEnabled) {
            return chatWithExternalAgent(message, getAgent());
        }
        lastAssistantMessage = AGENT_UNAVAILABLE_REPLY;
        return lastAssistantMessage;
    }

    private String chatWithExternalAgent(String message, Agent agent) {
        try {
            AgentActionExecutor.AgentExecutionResult result;
            if (shouldUseDeliberateMode(agent)) {
                DeliberateAgentResponse response = EXTERNAL_AGENT_CLIENT.deliberate(AGENT_REQUEST_BUILDER.deliberate(this, message, agent));
                result = AGENT_ACTION_EXECUTOR.execute(this, response);
            } else {
                FastAgentResponse response = EXTERNAL_AGENT_CLIENT.fast(AGENT_REQUEST_BUILDER.fast(this, message, agent));
                result = AGENT_ACTION_EXECUTOR.execute(this, response);
            }
            NPCSystem.debugLog("[npc-system] External agent chat result success={} toolResult={}", result.success(), result.toolResult());
            lastAgentResultSummary = result.toolResult() == null ? "" : result.toolResult().toString();
            lastAssistantMessage = result.responseText() == null ? "" : result.responseText();
            if (!result.success() && lastAssistantMessage.isBlank()) {
                lastAssistantMessage = AGENT_UNAVAILABLE_REPLY;
            }
            return lastAssistantMessage;
        } catch (Exception e) {
            NPCSystem.LOGGER.error("[npc-system] External agent request failed", e);
            resumeDefaultAfterAgentFailure("external agent request failed");
            lastAssistantMessage = AGENT_UNAVAILABLE_REPLY;
            return lastAssistantMessage;
        }
    }

    private static boolean shouldUseDeliberateMode(Agent agent) {
        return agent instanceof Master || "deliberate".equalsIgnoreCase(Config.agentMode);
    }

    public AsyncTask.TaskResult requestAgentFollowUp(NpcTaskBatchResult taskResult) {
        updateTime = System.currentTimeMillis();
        if (!Config.agentEnabled || !(getAgent() instanceof NPC npc)) {
            return AsyncTask.nothingToDo();
        }
        try {
            if ("deliberate".equalsIgnoreCase(Config.agentMode)) {
                DeliberateAgentResponse response = EXTERNAL_AGENT_CLIENT.deliberate(AGENT_REQUEST_BUILDER.deliberate(this, taskResult, npc));
                return new AgentFollowUpResult(taskResult.summary(), response);
            }
            FastAgentResponse response = EXTERNAL_AGENT_CLIENT.fast(AGENT_REQUEST_BUILDER.fast(this, taskResult, npc));
            return new AgentFollowUpResult(taskResult.summary(), response);
        } catch (Exception e) {
            NPCSystem.LOGGER.error("[npc-system] External agent follow-up failed", e);
            return new AgentFollowUpFailureResult("external agent follow-up failed");
        }
    }

    public void beginAgentBatch(String batchId, boolean callbackOnBatchComplete) {
        activeAgentBatchId = batchId;
        activeAgentBatchCallback = callbackOnBatchComplete;
    }

    public void endAgentBatch() {
        activeAgentBatchId = null;
        activeAgentBatchCallback = false;
    }

    public NpcTaskAssignment createAgentTaskAssignment(NpcTask task) {
        return NpcTaskAssignment.of(task, TaskSource.AGENT)
                .withBatch(activeAgentBatchId, activeAgentBatchCallback);
    }

    private final class AgentFollowUpResult implements AsyncTask.TaskResult {
        private final String message;
        private final FastAgentResponse fastResponse;
        private final DeliberateAgentResponse deliberateResponse;

        private AgentFollowUpResult(String message, FastAgentResponse fastResponse) {
            this.message = message;
            this.fastResponse = fastResponse;
            this.deliberateResponse = null;
        }

        private AgentFollowUpResult(String message, DeliberateAgentResponse deliberateResponse) {
            this.message = message;
            this.fastResponse = null;
            this.deliberateResponse = deliberateResponse;
        }

        @Override
        public void execute() {
            AgentActionExecutor.AgentExecutionResult result = fastResponse != null
                    ? AGENT_ACTION_EXECUTOR.execute(ConversationWindow.this, fastResponse)
                    : AGENT_ACTION_EXECUTOR.execute(ConversationWindow.this, deliberateResponse);
            NPCSystem.debugLog("[npc-system] Agent follow-up result success={} toolResult={}", result.success(), result.toolResult());
            lastAgentResultSummary = result.toolResult() == null ? "" : result.toolResult().toString();
            String responseText = result.responseText();
            if (responseText != null && !responseText.isBlank()) {
                lastUserMessage = message;
                lastAssistantMessage = responseText;
                NPC_AI.broadcastMessage(ConversationWindow.this);
            }
            if (result.isNoAction()) {
                resumeDefaultAfterAgentFailure("agent follow-up requested no action");
                return;
            }
            if (!result.success()) {
                NPCSystem.LOGGER.warn("[npc-system] Agent follow-up failed: {}", result.toolResult());
                resumeDefaultAfterAgentFailure("agent follow-up action failed");
                return;
            }
        }

        @Override
        public boolean isCallable() {
            return true;
        }
    }

    private final class AgentFollowUpFailureResult implements AsyncTask.TaskResult {
        private final String reason;

        private AgentFollowUpFailureResult(String reason) {
            this.reason = reason;
        }

        @Override
        public void execute() {
            resumeDefaultAfterAgentFailure(reason);
        }

        @Override
        public boolean isCallable() {
            return true;
        }
    }

    private void resumeDefaultAfterAgentFailure(String reason) {
        if (!(getAgent() instanceof NPC npc)) {
            return;
        }
        NPCEntity entity = NPC_AI.getNPCEntity(npc);
        if (entity == null) {
            return;
        }
        if (entity.getTaskController().currentSource() == TaskSource.SYSTEM) {
            entity.getTaskController().cancel(entity);
            NPCSystem.LOGGER.warn("[npc-system] Resumed default behavior for NPC {} after {}", npc.getUUID(), reason);
        }
    }

    /**
     * Let Agent start the conversation
     * @return The response
     */
    public String chat() {
        updateTime = System.currentTimeMillis();
        lastAssistantMessage = OPENING_LINES.get(ThreadLocalRandom.current().nextInt(OPENING_LINES.size()));
        return lastAssistantMessage;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void resetUpdateTime() {
        updateTime = 0L;
    }

    public String getLastUserMessage() {
        return lastUserMessage;
    }

    public String getLastAssistantMessage() {
        return lastAssistantMessage;
    }

    public String getLastAgentResultSummary() {
        return lastAgentResultSummary;
    }

    public void endExternalAgentConversation(String reason) {
        if (externalAgentEndNotified || !Config.agentEnabled) {
            return;
        }
        externalAgentEndNotified = true;
        try {
            EXTERNAL_AGENT_CLIENT.endConversation(conversationEndRequest(reason));
        } catch (Exception e) {
            if (Config.debug) {
                NPCSystem.LOGGER.warn("[npc-system] External agent conversation end notification failed", e);
            } else {
                NPCSystem.LOGGER.warn("[npc-system] External agent conversation end notification failed: {}", e.toString());
            }
        }
    }

    protected boolean discard() {
        endExternalAgentConversation("conversation_removed");
        return true;
    }

    private ConversationEndRequest conversationEndRequest(String reason) {
        Agent agent = getAgent();
        ConversationEndRequest request = new ConversationEndRequest();
        request.request_id = UUID.randomUUID().toString();
        request.reason = reason == null || reason.isBlank() ? "ended" : reason;
        request.npc = new ConversationEndRequest.Npc();
        request.npc.uuid = agent.getUUID().toString();
        request.npc.id = agent.getUUID().toString();
        request.npc.name = agent instanceof Master ? "Master" : agent.getUUID().toString();
        request.npc.kind = agent instanceof Master ? "master" : "npc";
        request.npc.permission = agent.getPermissionLevel();
        if (agent instanceof NPC npc) {
            NPCEntity entity = NPC_AI.getNPCEntity(npc);
            if (entity != null) {
                request.snapshot = Map.of(
                        "task", entity.getTaskController().status(),
                        "last_observation", entity.getLastObservation() == null ? "" : entity.getLastObservation()
                );
                return request;
            }
        }
        request.snapshot = Map.of("entity", "none");
        return request;
    }
}
