package team.jackdaw.npcsystem.api.completion;

import org.jetbrains.annotations.NotNull;
import team.jackdaw.npcsystem.api.Header;
import team.jackdaw.npcsystem.api.Request;
import team.jackdaw.npcsystem.api.completion.json.CompletionRequest;
import team.jackdaw.npcsystem.api.completion.json.CompletionResponse;

public class Completion {
    /**
     * Send a completion request to the server
     * @param url The url to send the request to. It should look like "<a href="http://localhost:11434">http://localhost:11434</a>"
     * @param model The model to use
     * @param prompt The prompt to use
     * @return The completion response
     * @throws Exception If the completion is not successful
     */
    public static @NotNull CompletionResponse completionRequest(@NotNull String url, @NotNull String model, @NotNull String prompt) throws Exception {
        CompletionRequest completionRequest = new CompletionRequest();
        completionRequest.model = model;
        completionRequest.prompt = prompt;
        completionRequest.stream = false;
        String res = Request.sendRequest(completionRequest.toJson(), url + "/api/generate", Header.buildDefault(), Request.Action.POST);
        return CompletionResponse.fromJson(res);
    }

    public static @NotNull CompletionResponse completionRequest(@NotNull String url, @NotNull CompletionRequest request) throws Exception {
        String res = Request.sendRequest(request.toJson(), url + "/api/generate", Header.buildDefault(), Request.Action.POST);
        return CompletionResponse.fromJson(res);
    }
}
