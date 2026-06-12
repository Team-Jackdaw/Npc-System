package team.jackdaw.npcsystem.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public interface Request {
    /**
     * Send a request to a URL
     *
     * @param requestJson The request to send
     * @param url     The URL of the request, should include the protocol
     * @return The response from the API in Json format
     * @throws Exception If the request fails
     */
    static @NotNull String sendRequest(@Nullable String requestJson, @NotNull String url, @NotNull Map<String, String> headers, @NotNull Action action) throws Exception {
        Duration timeout = Duration.ofMillis(timeoutMillis());
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpRequest request = buildRequest(requestJson, url, headers, action, timeout);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP request failed with status " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    @NotNull
    private static HttpRequest buildRequest(@Nullable String requestJson, @NotNull String url, @NotNull Map<String, String> headers, @NotNull Action action, @NotNull Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .version(HttpClient.Version.HTTP_1_1);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        if (action == Action.GET) {
            builder.GET();
        } else if (action == Action.POST) {
            builder.POST(HttpRequest.BodyPublishers.ofString(requestJson == null ? "" : requestJson));
        } else if (action == Action.DELETE) {
            if (requestJson == null || requestJson.isBlank()) {
                builder.DELETE();
            } else {
                builder.method("DELETE", HttpRequest.BodyPublishers.ofString(requestJson));
            }
        }
        return builder.build();
    }

    private static int timeoutMillis() {
        String configured = System.getProperty("npc.api.timeoutMillis");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("NPC_API_TIMEOUT_MILLIS");
        }
        if (configured == null || configured.isBlank()) {
            return 30 * 1000;
        }
        return Integer.parseInt(configured);
    }

    /**
     * The action of the request
     */
    enum Action {
        /**
         * Get request
         */
        GET,
        /**
         * Post request
         */
        POST,
        /**
         * Delete request
         */
        DELETE
    }
}
