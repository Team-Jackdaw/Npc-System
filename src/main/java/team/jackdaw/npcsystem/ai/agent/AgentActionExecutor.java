package team.jackdaw.npcsystem.ai.agent;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.agent.protocol.AgentAction;
import team.jackdaw.npcsystem.ai.agent.protocol.DeliberateAgentResponse;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentResponse;
import team.jackdaw.npcsystem.NPCSystem;
import team.jackdaw.npcsystem.function.FunctionManager;
import team.jackdaw.npcsystem.function.ToolResult;

import java.util.Map;
import java.util.UUID;

public class AgentActionExecutor {
    public AgentExecutionResult execute(ConversationWindow conversation, FastAgentResponse response) {
        AgentAction action = fastAction(response);
        if (action == null) {
            return AgentExecutionResult.success(responseText(response), ToolResult.success("no_action", "No action requested."));
        }
        return execute(conversation, action, responseText(response));
    }

    public AgentExecutionResult execute(ConversationWindow conversation, DeliberateAgentResponse response) {
        AgentAction action = deliberateAction(response);
        if (action == null) {
            return AgentExecutionResult.success(responseText(response), ToolResult.success("no_action", "No action requested."));
        }
        return execute(conversation, action, responseText(response));
    }

    private AgentExecutionResult execute(ConversationWindow conversation, AgentAction action, String responseText) {
        String batchId = UUID.randomUUID().toString();
        boolean callbackOnBatchComplete = shouldCallback(action);

        if (conversation != null) {
            conversation.beginAgentBatch(batchId, callbackOnBatchComplete);
        }
        try {
            AgentExecutionResult result = executeOne(conversation, action.name, action.arguments, responseText);
            Map<String, Object> toolResult = result.success()
                    ? ToolResult.success("action_executed", "Agent action executed.", Map.of("result", result.toolResult()))
                    : result.toolResult();
            return result.success()
                    ? AgentExecutionResult.success(responseText, toolResult)
                    : AgentExecutionResult.failure(responseText, toolResult);
        } finally {
            if (conversation != null) {
                conversation.endAgentBatch();
            }
        }
    }

    private AgentExecutionResult executeOne(ConversationWindow conversation, String name, Map<String, Object> args, String responseText) {
        if (name == null || name.isBlank()) {
            return AgentExecutionResult.failure(responseText, ToolResult.failure("invalid_action", "Action name is missing.", false));
        }
        try {
            Map<String, Object> result = FunctionManager.getInstance().callFunction(conversation, name, args == null ? Map.of() : args);
            NPCSystem.debugLog("[npc-system] Agent action {} args={} result={}", name, args == null ? Map.of() : args, result);
            if ("success".equals(result.get("status"))) {
                return AgentExecutionResult.success(responseText, result);
            }
            NPCSystem.LOGGER.warn("[npc-system] Agent action {} returned failure: {}", name, result);
            return AgentExecutionResult.failure(responseText, result);
        } catch (IllegalArgumentException e) {
            NPCSystem.LOGGER.warn("[npc-system] Agent action failed: {}", e.getMessage());
            return AgentExecutionResult.failure(responseText, ToolResult.failure("function_not_found", e.getMessage(), false));
        }
    }

    private static AgentAction fastAction(FastAgentResponse response) {
        if (response == null) {
            return null;
        }
        if (!"call".equals(response.a) || isReplyAction(response.name)) {
            return null;
        }
        AgentAction action = new AgentAction();
        action.type = "call";
        action.kind = response.kind;
        action.name = response.name;
        action.arguments = response.args;
        action.callback = response.callback;
        return action;
    }

    private static AgentAction deliberateAction(DeliberateAgentResponse response) {
        if (response == null) {
            return null;
        }
        if (response.action == null || !"call".equals(response.action.type) || isReplyAction(response.action.name)) {
            return null;
        }
        return response.action;
    }

    static boolean shouldCallback(AgentAction action) {
        if (action.callback != null) {
            return action.callback;
        }
        if (!"task".equals(action.kind)) {
            return false;
        }
        return !"say".equals(action.name);
    }

    private static String responseText(FastAgentResponse response) {
        if (response == null) {
            return "";
        }
        if (response.speech != null && !response.speech.isBlank()) {
            return response.speech;
        }
        return "";
    }

    private static String responseText(DeliberateAgentResponse response) {
        if (response == null) {
            return "";
        }
        if (response.speech != null && !response.speech.isBlank()) {
            return response.speech;
        }
        return "";
    }

    private static boolean isReplyAction(String name) {
        return "say".equals(name) || "master_reply".equals(name);
    }

    public record AgentExecutionResult(boolean success, String responseText, Map<String, Object> toolResult) {
        public static AgentExecutionResult success(String responseText, Map<String, Object> toolResult) {
            return new AgentExecutionResult(true, responseText, toolResult);
        }

        public static AgentExecutionResult failure(String responseText, Map<String, Object> toolResult) {
            return new AgentExecutionResult(false, responseText, toolResult);
        }

        public boolean isNoAction() {
            return toolResult != null && "no_action".equals(toolResult.get("code"));
        }
    }
}
