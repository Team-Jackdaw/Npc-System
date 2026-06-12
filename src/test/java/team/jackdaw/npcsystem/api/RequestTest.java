package team.jackdaw.npcsystem.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestTest {
    @Test
    void sendsPostJsonWithJdkHttpClient() throws Exception {
        HttpServer server = startServer(exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("application/json", exchange.getRequestHeaders().getFirst("Content-Type"));
            assertEquals("{\"hello\":\"world\"}", body);
            byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
        });
        try {
            String response = Request.sendRequest(
                    "{\"hello\":\"world\"}",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/agent/fast",
                    Header.buildDefault(),
                    Request.Action.POST
            );

            assertEquals("{\"ok\":true}", response);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void throwsOnErrorStatus() throws Exception {
        HttpServer server = startServer(exchange -> {
            byte[] response = "bad request".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, response.length);
            exchange.getResponseBody().write(response);
        });
        try {
            assertThrows(IllegalStateException.class, () -> Request.sendRequest(
                    "{}",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/agent/fast",
                    Header.buildDefault(),
                    Request.Action.POST
            ));
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startServer(Handler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/agent/fast", exchange -> {
            try (exchange) {
                handler.handle(exchange);
            }
        });
        server.start();
        return server;
    }

    @FunctionalInterface
    private interface Handler {
        void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException;
    }
}
