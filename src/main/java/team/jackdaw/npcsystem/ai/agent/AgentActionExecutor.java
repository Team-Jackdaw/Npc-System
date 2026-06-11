package team.jackdaw.npcsystem.ai.agent;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.agent.protocol.AgentAction;
import team.jackdaw.npcsystem.ai.agent.protocol.DeliberateAgentResponse;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentResponse;
import team.jackdaw.npcsystem.NPCSystem;
import team.jackdaw.npcsystem.function.FunctionManager;
import team.jackdaw.npcsystem.function.ToolResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AgentActionExecutor {
    private static final int MAX_ACTIONS = 2;

    public AgentExecutionResult execute(ConversationWindow conversation, FastAgentResponse response) {
        List<AgentAction> actions = fastActions(response);
        if (actions.isEmpty()) {
            return AgentExecutionResult.success(responseText(response), ToolResult.success("no_action", "No action requested."));
        }
        return execute(conversation, actions, responseText(response));
    }

    public AgentExecutionResult execute(ConversationWindow conversation, DeliberateAgentResponse response) {
        List<AgentAction> actions = deliberateActions(response);
        if (actions.isEmpty()) {
            return AgentExecutionResult.success(responseText(response), ToolResult.success("no_action", "No action requested."));
        }
        return execute(conversation, actions, responseText(response));
    }

    private AgentExecutionResult execute(ConversationWindow conversation, List<AgentAction> actions, String responseText) {
        String batchId = UUID.randomUUID().toString();
        boolean callbackOnBatchComplete = actions.stream().anyMatch(AgentActionExecutor::shouldCallback);
        List<Map<String, Object>> results = new ArrayList<>();
        boolean anySuccess = false;
        boolean anyFailure = false;

        if (conversation != null) {
            conversation.beginAgentBatch(batchId, callbackOnBatchComplete);
        }
        try {
            for (AgentAction action : actions.stream().limit(MAX_ACTIONS).toList()) {
                AgentExecutionResult result = executeOne(conversation, action.name, action.arguments, responseText);
                results.add(result.toolResult());
                anySuccess = anySuccess || result.success();
                anyFailure = anyFailure || !result.success();
            }
        } finally {
            if (conversation != null) {
                conversation.endAgentBatch();
            }
        }

        if (actions.size() > MAX_ACTIONS) {
            anyFailure = true;
            results.add(ToolResult.failure("too_many_actions", "Only the first 2 actions were accepted.", false));
        }
        Map<String, Object> toolResult = anyFailure
                ? ToolResult.failure(anySuccess ? "partial_actions_executed" : "actions_failed", "Agent actions completed with failures.", Map.of("results", results), false)
                : ToolResult.success("actions_executed", "Agent actions executed.", Map.of("results", results));
        return anyFailure && !anySuccess
                ? AgentExecutionResult.failure(responseText, toolResult)
                : AgentExecutionResult.success(responseText, toolResult);
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

    private static List<AgentAction> fastActions(FastAgentResponse response) {
        if (response == null) {
            return List.of();
        }
        if (response.actions != null && !response.actions.isEmpty()) {
            return response.actions.stream().filter(action -> "call".equals(action.type)).toList();
        }
        if (!"call".equals(response.a)) {
            return List.of();
        }
        AgentAction action = new AgentAction();
        action.type = "call";
        action.kind = response.kind;
        action.name = response.name;
        action.arguments = response.args;
        return List.of(action);
    }

    private static List<AgentAction> deliberateActions(DeliberateAgentResponse response) {
        if (response == null) {
            return List.of();
        }
        if (response.actions != null && !response.actions.isEmpty()) {
            return response.actions.stream().filter(action -> "call".equals(action.type)).toList();
        }
        if (response.action == null || !"call".equals(response.action.type)) {
            return List.of();
        }
        return List.of(response.action);
    }

    private static boolean shouldCallback(AgentAction action) {
        return "task".equals(action.kind) && !Boolean.FALSE.equals(action.callback);
    }

    private static String responseText(FastAgentResponse response) {
        if (response == null) {
            return "";
        }
        if (response.actions != null) {
            for (AgentAction action : response.actions) {
                if ("say".equals(action.name) && action.arguments != null && action.arguments.get("message") != null) {
                    return action.arguments.get("message").toString();
                }
            }
        }
        if ("say".equals(response.name) && response.args != null && response.args.get("message") != null) {
            return response.args.get("message").toString();
        }
        return response.note == null ? "" : response.note;
    }

    private static String responseText(DeliberateAgentResponse response) {
        if (response == null) {
            return "";
        }
        if (response.speech != null && !response.speech.isBlank()) {
            return response.speech;
        }
        if (response.actions != null) {
            for (AgentAction action : response.actions) {
                if ("say".equals(action.name) && action.arguments != null && action.arguments.get("message") != null) {
                    return action.arguments.get("message").toString();
                }
            }
        }
        if (response.action != null && "say".equals(response.action.name) && response.action.arguments != null && response.action.arguments.get("message") != null) {
            return response.action.arguments.get("message").toString();
        }
        return response.reasoning_summary == null ? "" : response.reasoning_summary;
    }

    public record AgentExecutionResult(boolean success, String responseText, Map<String, Object> toolResult) {
        public static AgentExecutionResult success(String responseText, Map<String, Object> toolResult) {
            return new AgentExecutionResult(true, responseText, toolResult);
        }

        public static AgentExecutionResult failure(String responseText, Map<String, Object> toolResult) {
            return new AgentExecutionResult(false, responseText, toolResult);
        }
    }
}
