package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.NPCSystem;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.api.json.Function;
import team.jackdaw.npcsystem.api.json.Tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FunctionManager {
    private static final Map<String, CustomFunction> functionRegistry = new HashMap<>();

    public static ArrayList<String> getRegistryList() {
        return new ArrayList<>(functionRegistry.keySet());
    }

    /**
     * Register a function that can be used by the NPC. It will be called by the OpenAI Assistant.
     * @param name The name of the function
     * @param function The function
     */
    public static void registerFunction(String name, CustomFunction function) {
        functionRegistry.put(name, function);
    }

    /**
     * Call a function by its name. It will be executed by the OpenAI Assistant and work on the conversation.
     * @param name The name of the function
     * @param conversation The conversation handler
     * @param args The arguments
     */
    public static Map<String, String> callFunction(ConversationWindow conversation, String name, Map<String, Object> args) {
        CustomFunction function = functionRegistry.get(name);
        if (function == null) {
            throw new IllegalArgumentException("Function not found: " + name);
        }
        if (NPCSystem.debug) NPCSystem.LOGGER.info("Calling function: " + name + " with args: " + args);
        return function.execute(conversation, args);
    }

    /**
     * Get the JSON string of a function by its name.
     * @param name The name of the function
     * @return The JSON string
     */
    public static Tool getTools(String name) {
        CustomFunction function = functionRegistry.get(name);
        Tool tool = new Tool();
        tool.type = "function";
        tool.function = new Function();
        tool.function.name = name;
        tool.function.description = function.description;
        tool.function.parameters = new Function.Parameters();
        tool.function.parameters.type = "object";
        tool.function.parameters.properties = function.properties;
        tool.function.parameters.required = Objects.requireNonNullElseGet(function.required, () -> function.properties.keySet().stream().map(Object::toString).toArray(String[]::new));
        return tool;
    }
}
