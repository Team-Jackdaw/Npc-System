package team.jackdaw.npcsystem.ai.agent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.agent.protocol.DeliberateAgentRequest;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentRequest;
import team.jackdaw.npcsystem.ai.master.Master;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.entity.sensor.ObservationEvent;
import team.jackdaw.npcsystem.entity.sensor.ObservationType;
import team.jackdaw.npcsystem.function.FunctionManager;
import team.jackdaw.npcsystem.function.CallCommandFunction;
import team.jackdaw.npcsystem.function.SayFunction;
import team.jackdaw.npcsystem.function.TestFunction;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentRequestBuilderTest {
    @BeforeAll
    static void registerFunctions() {
        FunctionManager.getInstance().register("say", new SayFunction());
        FunctionManager.getInstance().register("call_command", new CallCommandFunction());
        FunctionManager.getInstance().register("agent_test_weather", new TestFunction());
    }

    @Test
    void buildsFastRequestFromNpcContext() {
        NPC npc = npcWithTools();
        npc.observe(List.of(new ObservationEvent(ObservationType.CHAT_HEARD, 7, "Steve: hello", 10L)));
        ConversationWindow conversation = conversation(npc);

        FastAgentRequest request = new AgentRequestBuilder().fast(conversation, "hello", npc);

        assertEquals("fast", request.mode);
        assertEquals(npc.getUUID().toString(), request.npc.id);
        assertEquals("npc", request.npc.kind);
        assertEquals(1, request.npc.permission);
        assertEquals(60, request.limits.max_reply_chars);
        assertFalse(request.rid.isBlank());
        assertEquals("say", request.tools.getFirst());
        assertFalse(request.evt.isEmpty());
    }

    @Test
    void buildsDeliberateRequestWithToolDescriptors() {
        NPC npc = npcWithTools();
        ConversationWindow conversation = conversation(npc);
        UUID speaker = UUID.randomUUID();
        conversation.setTarget(speaker);

        DeliberateAgentRequest request = new AgentRequestBuilder().deliberate(conversation, "please follow me", npc);

        assertEquals("deliberate", request.mode);
        assertEquals(npc.getUUID().toString(), request.npc.uuid);
        assertEquals("npc", request.npc.kind);
        assertEquals(1, request.npc.permission);
        assertEquals(speaker.toString(), request.conversation.speaker);
        assertEquals("please follow me", request.conversation.message);
        assertEquals(200, request.limits.max_reply_chars);
        assertNotNull(request.observations.recent_events);
        assertEquals("say", request.available_tools.getFirst().name);
        assertEquals("task", request.available_tools.getFirst().kind);
    }

    @Test
    void buildsDeliberateRequestForMasterWithAdminTools() {
        Master master = Master.getMaster();
        ConversationWindow conversation = master.getConversationWindows();
        conversation.setTarget(UUID.randomUUID());

        DeliberateAgentRequest request = new AgentRequestBuilder().deliberate(conversation, "/time set day", master);

        assertEquals("master", request.npc.kind);
        assertEquals(3, request.npc.permission);
        assertEquals("Master", request.npc.name);
        assertEquals("/time set day", request.conversation.message);
        assertEquals("call_command", request.available_tools.stream()
                .filter(tool -> tool.name.equals("call_command"))
                .findFirst()
                .orElseThrow()
                .name);
    }

    private static NPC npcWithTools() {
        NPC npc = new NPC(UUID.randomUUID());
        npc.setTools(List.of("say", "agent_test_weather"));
        return npc;
    }

    private static ConversationWindow conversation(NPC npc) {
        AgentManager.getInstance().register(npc);
        return new ConversationWindow(npc.getUUID());
    }
}
