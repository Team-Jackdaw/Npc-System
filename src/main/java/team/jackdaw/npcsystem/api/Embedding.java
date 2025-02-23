package team.jackdaw.npcsystem.api;

import org.jetbrains.annotations.NotNull;
import team.jackdaw.npcsystem.api.json.EmbeddingRequest;
import team.jackdaw.npcsystem.api.json.EmbeddingResponse;

import java.util.List;

class Embedding {
    static EmbeddingResponse embedRequest(@NotNull String url, @NotNull String model, @NotNull List<String> input) throws Exception {
        EmbeddingRequest request = new EmbeddingRequest();
        request.model = model;
        request.input = input;
        String res = Request.sendRequest(request.toJson(), url + "/api/embed", Header.buildDefault(), Request.Action.POST);
        return EmbeddingResponse.fromJson(res);
    }

    static EmbeddingResponse embedRequest(@NotNull String url, @NotNull EmbeddingRequest request) throws Exception {
        String res = Request.sendRequest(request.toJson(), url + "/api/embed", Header.buildDefault(), Request.Action.POST);
        return EmbeddingResponse.fromJson(res);
    }
}
