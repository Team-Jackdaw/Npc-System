package team.jackdaw.npcsystem.ai.agent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.ai.agent.protocol.DeliberateAgentResponse;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentResponse;
import team.jackdaw.npcsystem.ai.agent.protocol.AgentAction;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.master.Master;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.function.FunctionManager;
import team.jackdaw.npcsystem.function.MasterPermissionFunction;
import team.jackdaw.npcsystem.function.TestFunction;

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
        response.note = "no_action";

        AgentActionExecutor.AgentExecutionResult result = new AgentActionExecutor().execute(null, response);

        assertTrue(result.success());
        assertEquals("no_action", result.toolResult().get("code"));
        assertEquals("", result.responseText());
        assertTrue(result.isNoAction());
    }

    @Test
    void deliberateNoneActionDoesNotExposeReasoningAsResponseText() {
        DeliberateAgentResponse response = new DeliberateAgentResponse();
        response.request_id = "r1";
        response.reasoning_summary = "No useful action is available.";

        AgentActionExecutor.AgentExecutionResult result = new AgentActionExecutor().execute(null, response);

        assertTrue(result.success());
        assertEquals("no_action", result.toolResult().get("code"));
        assertEquals("", result.responseText());
        assertTrue(result.isNoAction());
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
        assertEquals("action_executed", result.toolResult().get("code"));
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

    @Test
    void npcCannotExecuteMasterOnlyAction() {
        NPC npc = new NPC(java.util.UUID.randomUUID());
        AgentManager.getInstance().register(npc);
        ConversationWindow conversation = new ConversationWindow(npc.getUUID());
        FastAgentResponse response = new FastAgentResponse();
        response.rid = "r1";
        response.a = "call";
        response.kind = "tool";
        response.name = "agent_test_master_only";
        response.args = Map.of();

        AgentActionExecutor.AgentExecutionResult result = new AgentActionExecutor().execute(conversation, response);

        assertFalse(result.success());
        assertEquals("permission_denied", result.toolResult().get("code"));
    }

    @Test
    void masterCanExecuteMasterOnlyAction() {
        ConversationWindow conversation = Master.getMaster().getConversationWindows();
        FastAgentResponse response = new FastAgentResponse();
        response.rid = "r1";
        response.a = "call";
        response.kind = "tool";
        response.name = "agent_test_master_only";
        response.args = Map.of();

        AgentActionExecutor.AgentExecutionResult result = new AgentActionExecutor().execute(conversation, response);

        assertTrue(result.success());
        assertEquals("action_executed", result.toolResult().get("code"));
    }

    @Test
    void speechOnlyResponseDoesNotRequireAction() {
        FastAgentResponse response = new FastAgentResponse();
        response.rid = "r1";
        response.a = "none";
        response.speech = "我来了。";

        AgentActionExecutor.AgentExecutionResult result = new AgentActionExecutor().execute(null, response);

        assertTrue(result.success());
        assertEquals("no_action", result.toolResult().get("code"));
        assertEquals("我来了。", result.responseText());
    }

    @Test
    void speechResponseCanAccompanySingleAction() {
        FastAgentResponse response = new FastAgentResponse();
        response.rid = "r1";
        response.a = "call";
        response.kind = "tool";
        response.name = "agent_test_weather";
        response.args = Map.of("location", "Paris", "format", "celsius");
        response.speech = "我先看看天气。";

        AgentActionExecutor.AgentExecutionResult result = new AgentActionExecutor().execute(null, response);

        assertTrue(result.success());
        assertEquals("action_executed", result.toolResult().get("code"));
        assertEquals("我先看看天气。", result.responseText());
    }

    @Test
    void sayActionDoesNotRequestFollowUpByDefault() {
        AgentAction say = action("say", Map.of("message", "你好。"));
        say.kind = "task";

        assertFalse(AgentActionExecutor.shouldCallback(say));
    }

    @Test
    void nonSayTaskRequestsFollowUpByDefault() {
        AgentAction follow = action("follow_player", Map.of("player", "Steve"));
        follow.kind = "task";

        assertTrue(AgentActionExecutor.shouldCallback(follow));
    }

    @Test
    void explicitCallbackIsRespectedForToolKindActions() {
        AgentAction walk = action("walk_to_player", Map.of("player_name", "Steve"));
        walk.kind = "tool";
        walk.callback = true;

        assertTrue(AgentActionExecutor.shouldCallback(walk));
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
