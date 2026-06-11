package team.jackdaw.npcsystem.ai.agent;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.ai.agent.protocol.DeliberateAgentRequest;
import team.jackdaw.npcsystem.ai.agent.protocol.DeliberateAgentResponse;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentRequest;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentResponse;
import team.jackdaw.npcsystem.api.Request;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalAgentClientTest {
    private static final Gson GSON = new Gson();

    @AfterEach
    void resetConfig() {
        Config.agentBaseUrl = "http://127.0.0.1:8765";
        Config.agentAuthToken = "";
    }

    @Test
    void sendsFastRequestToFastEndpoint() throws Exception {
        Config.agentBaseUrl = "http://example.test/";
        Config.agentAuthToken = "token";
        ExternalAgentClient client = new ExternalAgentClient((requestJson, url, headers, action) -> {
            assertEquals("http://example.test/agent/fast", url);
            assertEquals(Request.Action.POST, action);
            assertEquals("Bearer token", headers.get("Authorization"));
            assertTrue(requestJson.contains("\"rid\":\"r1\""));
            FastAgentResponse response = new FastAgentResponse();
            response.rid = "r1";
            response.a = "none";
            return GSON.toJson(response);
        });
        FastAgentRequest request = new FastAgentRequest();
        request.rid = "r1";

        FastAgentResponse response = client.fast(request);

        assertEquals("r1", response.rid);
        assertEquals("none", response.a);
    }

    @Test
    void sendsDeliberateRequestToDeliberateEndpoint() throws Exception {
        Config.agentBaseUrl = "http://example.test";
        ExternalAgentClient client = new ExternalAgentClient((requestJson, url, headers, action) -> {
            assertEquals("http://example.test/agent/deliberate", url);
            assertEquals(Request.Action.POST, action);
            assertEquals("application/json", headers.get("Content-Type"));
            DeliberateAgentResponse response = new DeliberateAgentResponse();
            response.request_id = "r2";
            return GSON.toJson(response);
        });
        DeliberateAgentRequest request = new DeliberateAgentRequest();
        request.request_id = "r2";

        DeliberateAgentResponse response = client.deliberate(request);

        assertEquals("r2", response.request_id);
    }
}
