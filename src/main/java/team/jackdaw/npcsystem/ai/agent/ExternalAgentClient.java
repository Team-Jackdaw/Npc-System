package team.jackdaw.npcsystem.ai.agent;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.NPCSystem;
import team.jackdaw.npcsystem.ai.agent.protocol.DeliberateAgentRequest;
import team.jackdaw.npcsystem.ai.agent.protocol.DeliberateAgentResponse;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentRequest;
import team.jackdaw.npcsystem.ai.agent.protocol.FastAgentResponse;
import team.jackdaw.npcsystem.api.Header;
import team.jackdaw.npcsystem.api.Request;

import java.util.Map;

public class ExternalAgentClient {
    private static final Gson GSON = new Gson();
    private final Sender sender;

    public ExternalAgentClient() {
        this(Request::sendRequest);
    }

    ExternalAgentClient(Sender sender) {
        this.sender = sender;
    }

    public FastAgentResponse fast(@NotNull FastAgentRequest request) throws Exception {
        String requestJson = GSON.toJson(request);
        NPCSystem.debugLog("[npc-system] Agent fast request: {}", requestJson);
        String json = sender.send(requestJson, endpoint("/agent/fast"), headers(), Request.Action.POST);
        NPCSystem.debugLog("[npc-system] Agent fast response: {}", json);
        return GSON.fromJson(json, FastAgentResponse.class);
    }

    public DeliberateAgentResponse deliberate(@NotNull DeliberateAgentRequest request) throws Exception {
        String requestJson = GSON.toJson(request);
        NPCSystem.debugLog("[npc-system] Agent deliberate request: {}", requestJson);
        String json = sender.send(requestJson, endpoint("/agent/deliberate"), headers(), Request.Action.POST);
        NPCSystem.debugLog("[npc-system] Agent deliberate response: {}", json);
        return GSON.fromJson(json, DeliberateAgentResponse.class);
    }

    private static String endpoint(String path) {
        String base = Config.agentBaseUrl == null || Config.agentBaseUrl.isBlank()
                ? "http://127.0.0.1:8765"
                : Config.agentBaseUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    private static Map<String, String> headers() {
        Header header = Header.builder()
                .add(Header.Type.CONTENT_TYPE, null);
        if (Config.agentAuthToken != null && !Config.agentAuthToken.isBlank()) {
            header.add(Header.Type.AUTHORIZATION, Config.agentAuthToken);
        }
        return header.build();
    }

    @FunctionalInterface
    interface Sender {
        String send(String requestJson, String url, Map<String, String> headers, Request.Action action) throws Exception;
    }
}
