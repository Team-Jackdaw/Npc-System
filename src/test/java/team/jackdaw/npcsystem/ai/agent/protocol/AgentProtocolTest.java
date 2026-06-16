package team.jackdaw.npcsystem.ai.agent.protocol;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AgentProtocolTest {
    private static final Gson GSON = new Gson();

    @Test
    void fastRequestAndResponseRoundTrip() {
        FastAgentRequest request = new FastAgentRequest();
        request.rid = "request-1";
        request.npc = new FastAgentRequest.Npc();
        request.npc.id = "npc-1";
        request.npc.name = "npc";
        request.npc.task = "idle";
        request.npc.hp = 20.0;
        request.npc.pos = List.of(1.0, 64.0, 2.0);
        request.npc.dim = "minecraft:overworld";
        request.evt = List.of(List.of("CHAT_HEARD", 7, "Steve: hi", 123L));
        request.near = new FastAgentRequest.Near();
        request.near.p = List.of("Steve@3.0");
        request.tools = List.of("look_at_player");
        request.limits = new AgentLimits();
        request.limits.max_reply_chars = 60;

        FastAgentRequest parsedRequest = GSON.fromJson(GSON.toJson(request), FastAgentRequest.class);
        assertEquals(1, parsedRequest.v);
        assertEquals("fast", parsedRequest.mode);
        assertEquals("request-1", parsedRequest.rid);
        assertEquals("look_at_player", parsedRequest.tools.getFirst());

        FastAgentResponse response = new FastAgentResponse();
        response.rid = parsedRequest.rid;
        response.a = "call";
        response.kind = "task";
        response.name = "look_at_player";
        response.args = Map.of("player", "Steve", "seconds", 3);
        response.callback = false;
        response.speech = "hi";

        FastAgentResponse parsedResponse = GSON.fromJson(GSON.toJson(response), FastAgentResponse.class);
        assertEquals("look_at_player", parsedResponse.name);
        assertEquals("Steve", parsedResponse.args.get("player"));
        assertEquals(false, parsedResponse.callback);
        assertEquals("hi", parsedResponse.speech);
    }

    @Test
    void deliberateRequestAndResponseRoundTrip() {
        DeliberateAgentRequest request = new DeliberateAgentRequest();
        request.request_id = "request-2";
        request.npc = new DeliberateAgentRequest.Npc();
        request.npc.uuid = "npc-2";
        request.npc.name = "npc";
        request.npc.instruction = "act as an npc";
        request.npc.status = Map.of("task", "idle", "health", 20.0);
        request.observations = new DeliberateAgentRequest.Observations();
        request.observations.summary = "summary";
        request.observations.recent_events = List.of(Map.of("type", "CHAT_HEARD"));
        request.observations.important_events = List.of();
        request.conversation = new DeliberateAgentRequest.Conversation();
        request.conversation.speaker = "Steve";
        request.conversation.message = "follow me";
        request.conversation.history = List.of();
        request.memory = new DeliberateAgentRequest.Memory();
        request.memory.recent = List.of();
        request.memory.relevant = List.of("Steve likes mining.");
        AgentToolDescriptor tool = new AgentToolDescriptor();
        tool.name = "follow_player";
        tool.kind = "task";
        tool.description = "Follow a player.";
        tool.parameters = Map.of("player", Map.of("type", "string"));
        tool.required = new String[]{"player"};
        request.available_tools = List.of(tool);
        request.limits = new AgentLimits();
        request.limits.max_reply_chars = 200;

        DeliberateAgentRequest parsedRequest = GSON.fromJson(GSON.toJson(request), DeliberateAgentRequest.class);
        assertEquals(1, parsedRequest.version);
        assertEquals("deliberate", parsedRequest.mode);
        assertEquals("follow_player", parsedRequest.available_tools.getFirst().name);
        assertNotNull(parsedRequest.observations.recent_events);

        DeliberateAgentResponse response = new DeliberateAgentResponse();
        response.request_id = parsedRequest.request_id;
        AgentAction follow = new AgentAction();
        follow.type = "call";
        follow.kind = "task";
        follow.name = "follow_player";
        follow.arguments = Map.of("player", "Steve");
        response.action = follow;
        response.speech = "好。";
        response.memory_updates = List.of();
        response.reasoning_summary = "Player asked for follow.";

        DeliberateAgentResponse parsedResponse = GSON.fromJson(GSON.toJson(response), DeliberateAgentResponse.class);
        assertEquals("follow_player", parsedResponse.action.name);
        assertEquals("好。", parsedResponse.speech);
    }
}
