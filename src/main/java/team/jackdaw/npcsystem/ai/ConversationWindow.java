package team.jackdaw.npcsystem.ai;

import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.AsyncTask;
import team.jackdaw.npcsystem.NPCSystem;
import team.jackdaw.npcsystem.ai.agent.AgentActionExecutor;
import team.jackdaw.npcsystem.ai.agent.AgentRequestBuilder;
import team.jackdaw.npcsystem.ai.agent.ExternalAgentClient;
import team.jackdaw.npcsystem.ai.agent.protocol.DeliberateAgentResponse;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentResponse;
import team.jackdaw.npcsystem.api.Ollama;
import team.jackdaw.npcsystem.api.json.*;
import team.jackdaw.npcsystem.entity.task.NpcTask;
import team.jackdaw.npcsystem.entity.task.NpcTaskAssignment;
import team.jackdaw.npcsystem.entity.task.NpcTaskBatchResult;
import team.jackdaw.npcsystem.entity.task.TaskSource;
import team.jackdaw.npcsystem.function.FunctionManager;
import team.jackdaw.npcsystem.ai.npc.NPC;

import java.util.List;
import java.util.UUID;

public class ConversationWindow {
    private static final AgentRequestBuilder AGENT_REQUEST_BUILDER = new AgentRequestBuilder();
    private static final AgentActionExecutor AGENT_ACTION_EXECUTOR = new AgentActionExecutor();
    private static final ExternalAgentClient EXTERNAL_AGENT_CLIENT = new ExternalAgentClient();
    protected final UUID uuid;
    protected List<Message> messages;
    protected long updateTime = 0L;
    protected UUID target;
    private boolean onWait = false;
    private String lastInjectedContext = "";
    private String activeAgentBatchId;
    private boolean activeAgentBatchCallback;

    public ConversationWindow(UUID uuid) {
        this.uuid = uuid;
        messages = Ollama.messageBuilder()
                .addMessage(Role.SYSTEM, AgentManager.getInstance().get(uuid).getInstruction())
                .build();
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

    public List<Message> getMessages() {
        return messages;
    }

    public String getLastMessage() {
        return messages.get(messages.size() - 1).content;
    }

    public List<Tool> getTools() {
        return getAgent().getTools()
                .stream()
                .map(FunctionManager.getInstance()::getTools)
                .toList();
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
    public ChatResponse chat(String message) {
        updateTime = System.currentTimeMillis();
        if (Config.agentEnabled && getAgent() instanceof NPC npc) {
            ChatResponse externalResponse = chatWithExternalAgent(message, npc);
            if (externalResponse != null || !Config.agentFallbackToOllama) {
                return externalResponse;
            }
        }
        return chatWithOllama(message);
    }

    private ChatResponse chatWithOllama(String message) {
        MessageBuilder builder = Ollama.messageBuilder(messages);
        addCurrentContext(builder);
        messages = builder
                .addMessage(Role.USER, message)
                .build();
        ChatResponse response;
        try {
            response = Ollama.chat(messages, getTools());
            if (response.message.tool_calls != null) {
                MessageBuilder messagedBuilder = Ollama.messageBuilder(messages);
                for (ChatResponse.Message.ToolCall toolCall : response.message.tool_calls) {
                    String functionResult = FunctionManager.getInstance()
                            .callFunction(this, toolCall.function.name, toolCall.function.arguments)
                            .toString();
                    messagedBuilder.addToolMessage(toolCall.function.name, functionResult);
                }
                List<Message> messages2 = messagedBuilder.build();
                response = Ollama.chat(messages2, null);
            }
            messages = Ollama.messageBuilder(messages)
                    .addMessage(Role.ASSISTANT, response.message.content)
                    .build();
        } catch (Exception e) {
            messages = Ollama.messageBuilder(messages)
                    .addMessage(Role.ASSISTANT, "I'm sorry, I can't do that.")
                    .build();
            response = null;
        }

        return response;
    }

    private ChatResponse chatWithExternalAgent(String message, NPC npc) {
        try {
            AgentActionExecutor.AgentExecutionResult result;
            if ("deliberate".equalsIgnoreCase(Config.agentMode)) {
                DeliberateAgentResponse response = EXTERNAL_AGENT_CLIENT.deliberate(AGENT_REQUEST_BUILDER.deliberate(this, message, npc));
                result = AGENT_ACTION_EXECUTOR.execute(this, response);
            } else {
                FastAgentResponse response = EXTERNAL_AGENT_CLIENT.fast(AGENT_REQUEST_BUILDER.fast(this, message, npc));
                result = AGENT_ACTION_EXECUTOR.execute(this, response);
            }
            if (!result.success() && Config.agentFallbackToOllama) {
                return null;
            }
            ChatResponse response = agentChatResponse(result.responseText());
            messages = Ollama.messageBuilder(messages)
                    .addMessage(Role.USER, message)
                    .addMessage(Role.ASSISTANT, response.message.content)
                    .build();
            return response;
        } catch (Exception e) {
            NPCSystem.LOGGER.error("[npc-system] External agent request failed", e);
            return null;
        }
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
            return AsyncTask.nothingToDo();
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
            if (!result.success()) {
                NPCSystem.LOGGER.warn("[npc-system] Agent follow-up failed: {}", result.toolResult());
                return;
            }
            String responseText = result.responseText();
            if (responseText != null && !responseText.isBlank()) {
                messages = Ollama.messageBuilder(messages)
                        .addMessage(Role.USER, message)
                        .addMessage(Role.ASSISTANT, responseText)
                        .build();
            }
        }

        @Override
        public boolean isCallable() {
            return true;
        }
    }

    private static ChatResponse agentChatResponse(String content) {
        ChatResponse response = new ChatResponse();
        response.message = new ChatResponse.Message();
        response.message.role = "assistant";
        response.message.content = content == null ? "" : content;
        response.done = true;
        response.done_reason = "stop";
        return response;
    }

    /**
     * Let Agent start the conversation
     * @return The response
     */
    public ChatResponse chat() {
        updateTime = System.currentTimeMillis();
        ChatResponse response;
        try {
            MessageBuilder builder = Ollama.messageBuilder(messages);
            addCurrentContext(builder);
            messages = builder.build();
            response = Ollama.chat(messages, null);
            messages = Ollama.messageBuilder(messages)
                    .addMessage(Role.ASSISTANT, response.message.content)
                    .build();
        } catch (Exception e) {
            messages = Ollama.messageBuilder(messages)
                    .addMessage(Role.ASSISTANT, "I'm sorry, I can't do that.")
                    .build();
            response = null;
        }
        return response;
    }

    private void addCurrentContext(MessageBuilder builder) {
        if (getAgent() instanceof NPC npc) {
            String context = npc.getContextPrompt();
            if (context != null && !context.isBlank() && !context.equals(lastInjectedContext)) {
                builder.addMessage(Role.SYSTEM, "当前NPC上下文:\n" + context);
                lastInjectedContext = context;
            }
        }
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void resetUpdateTime() {
        updateTime = 0L;
    }


    protected boolean discard() {
        return true;
    }
}
