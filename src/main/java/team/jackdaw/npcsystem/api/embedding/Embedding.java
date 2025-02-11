package team.jackdaw.npcsystem.api.embedding;

import org.jetbrains.annotations.NotNull;
import team.jackdaw.npcsystem.api.Header;
import team.jackdaw.npcsystem.api.Request;
import team.jackdaw.npcsystem.api.embedding.json.EmbeddingRequest;
import team.jackdaw.npcsystem.api.embedding.json.EmbeddingResponse;

import java.util.List;

public class Embedding {
    /**
     * Send an embedding request to the server
     * @param url The url to send the request to. It should look like "<a href="http://localhost:11434">http://localhost:11434</a>"
     * @param model The model to use
     * @param input The input to use
     * @return The embedding response
     * @throws Exception If the embedding is not successful
     */
    public static EmbeddingResponse embedRequest(@NotNull String url, @NotNull String model, @NotNull List<String> input) throws Exception {
        EmbeddingRequest request = new EmbeddingRequest();
        request.model = model;
        request.input = input;
        String res = Request.sendRequest(request.toJson(), url + "/api/embed", Header.buildDefault(), Request.Action.POST);
        return EmbeddingResponse.fromJson(res);
    }

    public static EmbeddingResponse embedRequest(@NotNull String url, @NotNull EmbeddingRequest request) throws Exception {
        String res = Request.sendRequest(request.toJson(), url + "/api/embed", Header.buildDefault(), Request.Action.POST);
        return EmbeddingResponse.fromJson(res);
    }
}
