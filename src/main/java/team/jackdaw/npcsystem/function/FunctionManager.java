package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.BaseManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.agent.protocol.AgentToolDescriptor;
import team.jackdaw.npcsystem.api.json.Function;
import team.jackdaw.npcsystem.api.json.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FunctionManager extends BaseManager<String, CustomFunction> {
    private static final FunctionManager INSTANCE = new FunctionManager();
    private FunctionManager() {}

    public static FunctionManager getInstance() {
        return INSTANCE;
    }

    public ArrayList<String> getRegistryList() {
        return new ArrayList<>(map.keySet());
    }

    /**
     * Call a function by its name. It will be executed by Ollama LLM and work on the conversation.
     *
     * @param functionName The name of the function
     * @param conversation The conversation handler
     * @param args         The arguments
     */
    public Map<String, Object> callFunction(ConversationWindow conversation, String functionName, Map<String, Object> args) {
        CustomFunction function = get(functionName);
        if (function == null) {
            throw new IllegalArgumentException("Function not found: " + functionName);
        }
        if (conversation != null && conversation.getAgent().getPermissionLevel() < function.permissionLevel) {
            return ToolResult.failure("permission_denied", "The current agent cannot call this function.", false);
        }
        return function.execute(conversation, args);
    }

    /**
     * Get the JSON string of a function by its functionName.
     *
     * @param functionName The functionName of the function
     * @return The JSON string
     */
    public Tool getTools(String functionName) {
        CustomFunction function = get(functionName);
        Tool tool = new Tool();
        tool.type = "function";
        tool.function = new Function();
        tool.function.name = functionName;
        tool.function.description = function.description;
        tool.function.parameters = new Function.Parameters();
        tool.function.parameters.type = "object";
        tool.function.parameters.properties = function.properties;
        tool.function.parameters.required = Objects.requireNonNullElseGet(function.required, () -> function.properties.keySet().stream().map(Object::toString).toArray(String[]::new));
        return tool;
    }

    public AgentToolDescriptor getAgentToolDescriptor(String functionName) {
        CustomFunction function = get(functionName);
        AgentToolDescriptor descriptor = new AgentToolDescriptor();
        descriptor.name = functionName;
        descriptor.kind = isTaskFunction(function) ? "task" : "tool";
        descriptor.description = function.description;
        descriptor.parameters = function.properties == null ? Map.of() : function.properties;
        String[] required = Objects.requireNonNullElseGet(function.required, () -> descriptor.parameters.keySet().stream().map(Object::toString).toArray(String[]::new));
        descriptor.required = required;
        return descriptor;
    }

    public List<AgentToolDescriptor> getAgentToolDescriptors(List<String> functionNames) {
        return functionNames.stream()
                .filter(this::isRegistered)
                .map(this::getAgentToolDescriptor)
                .toList();
    }

    private static boolean isTaskFunction(CustomFunction function) {
        return function instanceof NpcTaskFunction;
    }
}
