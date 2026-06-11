package team.jackdaw.npcsystem.ai.agent;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.agent.protocol.AgentAction;
import team.jackdaw.npcsystem.ai.agent.protocol.DeliberateAgentResponse;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentResponse;
import team.jackdaw.npcsystem.function.FunctionManager;
import team.jackdaw.npcsystem.function.ToolResult;

import java.util.Map;

public class AgentActionExecutor {
    public AgentExecutionResult execute(ConversationWindow conversation, FastAgentResponse response) {
        if (response == null || !"call".equals(response.a)) {
            return AgentExecutionResult.success(responseText(response), ToolResult.success("no_action", "No action requested."));
        }
        return execute(conversation, response.name, response.args, responseText(response));
    }

    public AgentExecutionResult execute(ConversationWindow conversation, DeliberateAgentResponse response) {
        if (response == null || response.action == null || !"call".equals(response.action.type)) {
            return AgentExecutionResult.success(responseText(response), ToolResult.success("no_action", "No action requested."));
        }
        return execute(conversation, response.action.name, response.action.arguments, responseText(response));
    }

    private AgentExecutionResult execute(ConversationWindow conversation, String name, Map<String, Object> args, String responseText) {
        if (name == null || name.isBlank()) {
            return AgentExecutionResult.failure(responseText, ToolResult.failure("invalid_action", "Action name is missing.", false));
        }
        try {
            Map<String, Object> result = FunctionManager.getInstance().callFunction(conversation, name, args == null ? Map.of() : args);
            if ("success".equals(result.get("status"))) {
                return AgentExecutionResult.success(responseText, result);
            }
            return AgentExecutionResult.failure(responseText, result);
        } catch (IllegalArgumentException e) {
            return AgentExecutionResult.failure(responseText, ToolResult.failure("function_not_found", e.getMessage(), false));
        }
    }

    private static String responseText(FastAgentResponse response) {
        if (response == null) {
            return "";
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
