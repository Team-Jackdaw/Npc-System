package team.jackdaw.npcsystem.ai.agent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentResponse;
import team.jackdaw.npcsystem.ai.agent.protocol.AgentAction;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.master.Master;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.function.FunctionManager;
import team.jackdaw.npcsystem.function.MasterPermissionFunction;
import team.jackdaw.npcsystem.function.TestFunction;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentActionExecutorTest {
    @BeforeAll
    static void registerFunction() {
        FunctionManager.getInstance().register("agent_test_weather", new TestFunction());
        FunctionManager.getInstance().register("agent_test_master_only", new MasterPermissionFunction());
    }

    @Test
    void noneActionDoesNotCallFunction() {
        FastAgentResponse response = new FastAgentResponse();
        response.rid = "r1";
        response.a = "none";

        AgentActionExecutor.AgentExecutionResult result = new AgentActionExecutor().execute(null, response);

        assertTrue(result.success());
        assertEquals("no_action", result.toolResult().get("code"));
    }

    @Test
    void callActionExecutesFunction() {
        FastAgentResponse response = new FastAgentResponse();
        response.rid = "r1";
        response.a = "call";
        response.kind = "tool";
        response.name = "agent_test_weather";
        response.args = Map.of("location", "Paris", "format", "celsius");

        AgentActionExecutor.AgentExecutionResult result = new AgentActionExecutor().execute(null, response);

        assertTrue(result.success());
        assertEquals("success", result.toolResult().get("status"));
        assertEquals("actions_executed", result.toolResult().get("code"));
    }

    @Test
    void missingFunctionReturnsFailure() {
        FastAgentResponse response = new FastAgentResponse();
        response.rid = "r1";
        response.a = "call";
        response.kind = "tool";
        response.name = "missing_function";
        response.args = Map.of();

        AgentActionExecutor.AgentExecutionResult result = new AgentActionExecutor().execute(null, response);

        assertFalse(result.success());
        assertEquals("actions_failed", result.toolResult().get("code"));
    }

    @Test
    void actionsListExecutesMultipleActions() {
        FastAgentResponse response = new FastAgentResponse();
        response.rid = "r1";
        response.actions = List.of(
                action("agent_test_weather", Map.of("location", "Paris", "format", "celsius")),
                action("agent_test_weather", Map.of("location", "Berlin", "format", "fahrenheit"))
        );

        AgentActionExecutor.AgentExecutionResult result = new AgentActionExecutor().execute(null, response);

        assertTrue(result.success());
        assertEquals("actions_executed", result.toolResult().get("code"));
    }

    @Test
    void actionsListIsLimitedToTwoActions() {
        FastAgentResponse response = new FastAgentResponse();
        response.rid = "r1";
        response.actions = List.of(
                action("agent_test_weather", Map.of("location", "Paris", "format", "celsius")),
                action("agent_test_weather", Map.of("location", "Berlin", "format", "fahrenheit")),
                action("agent_test_weather", Map.of("location", "Rome", "format", "celsius"))
        );

        AgentActionExecutor.AgentExecutionResult result = new AgentActionExecutor().execute(null, response);

        assertTrue(result.success());
        assertEquals("partial_actions_executed", result.toolResult().get("code"));
    }

    @Test
    void npcCannotExecuteMasterOnlyAction() {
        NPC npc = new NPC(java.util.UUID.randomUUID());
        AgentManager.getInstance().register(npc);
        ConversationWindow conversation = new ConversationWindow(npc.getUUID());
        FastAgentResponse response = new FastAgentResponse();
        response.rid = "r1";
        response.actions = List.of(action("agent_test_master_only", Map.of()));

        AgentActionExecutor.AgentExecutionResult result = new AgentActionExecutor().execute(conversation, response);

        assertFalse(result.success());
        assertEquals("actions_failed", result.toolResult().get("code"));
    }

    @Test
    void masterCanExecuteMasterOnlyAction() {
        ConversationWindow conversation = Master.getMaster().getConversationWindows();
        FastAgentResponse response = new FastAgentResponse();
        response.rid = "r1";
        response.actions = List.of(action("agent_test_master_only", Map.of()));

        AgentActionExecutor.AgentExecutionResult result = new AgentActionExecutor().execute(conversation, response);

        assertTrue(result.success());
        assertEquals("actions_executed", result.toolResult().get("code"));
    }

    private static AgentAction action(String name, Map<String, Object> arguments) {
        AgentAction action = new AgentAction();
        action.type = "call";
        action.kind = "tool";
        action.name = name;
        action.arguments = arguments;
        return action;
    }
}
