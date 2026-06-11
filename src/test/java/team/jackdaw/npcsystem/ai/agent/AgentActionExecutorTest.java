package team.jackdaw.npcsystem.ai.agent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentResponse;
import team.jackdaw.npcsystem.function.FunctionManager;
import team.jackdaw.npcsystem.function.TestFunction;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentActionExecutorTest {
    @BeforeAll
    static void registerFunction() {
        FunctionManager.getInstance().register("agent_test_weather", new TestFunction());
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
        assertEquals("weather_found", result.toolResult().get("code"));
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
        assertEquals("function_not_found", result.toolResult().get("code"));
    }
}
