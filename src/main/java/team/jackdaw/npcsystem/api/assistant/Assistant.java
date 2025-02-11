package team.jackdaw.npcsystem.api.assistant;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.jackdaw.npcsystem.api.Header;
import team.jackdaw.npcsystem.api.Request;
import team.jackdaw.npcsystem.api.json.Tool;
import team.jackdaw.npcsystem.api.assistant.json.AssistantClass;

import java.util.List;
import java.util.Map;

public class Assistant {
    /**
     * Create or modify an assistant for the NPC from the OpenAI API
     * @param url The url to send the request to. It should look like "<a href="https://api.openai.com/v1">https://api.openai.com/v1</a>"
     * @param apiKey The API key to use
     * @param model The model to use
     * @param name The name of the assistant
     * @param instruction The instruction for the assistant
     * @param tools The tools to use (optional)
     * @param assistantId The assistant id to modify (optional)
     * @return The assistant id
     * @throws Exception If the assistant is not created or modified successfully
     */
    public static @NotNull String assistantRequest(@NotNull String url, @NotNull String apiKey, @NotNull String model, @NotNull String name, @NotNull String instruction,@Nullable List<Tool> tools, @Nullable String assistantId) throws Exception {
        Map assistantRequest;
        if (tools == null || tools.isEmpty()) {
            assistantRequest = Map.of(
                    "name", name,
                    "model", model,
                    "instructions", instruction
            );
        } else {
            assistantRequest = Map.of(
                    "name", name,
                    "model", model,
                    "instructions", instruction,
                    "tools", tools
            );
        }
        String res;
        if (assistantId == null) {
            res = Request.sendRequest(new Gson().toJson(assistantRequest), url + "/assistants", Header.buildBeta(apiKey), Request.Action.POST);
        } else {
            res = Request.sendRequest(new Gson().toJson(assistantRequest), url + "/assistants/" + assistantId, Header.buildBeta(apiKey), Request.Action.POST);
        }
        String id = AssistantClass.fromJson(res).id;
        if (id == null) {
            throw new Exception("Assistant id is null");
        }
        return id;
    }

    /**
     * Delete an assistant from the OpenAI API
     * @param url The url to send the request to. It should look like "<a href="https://api.openai.com/v1">https://api.openai.com/v1</a>"
     * @param apiKey The API key to use
     * @param assistantId The assistant id to delete
     * @throws Exception If the assistant is not deleted successfully
     */
    public static void deleteAssistant(@NotNull String url, @NotNull String apiKey, @NotNull String assistantId) throws Exception {
        Request.sendRequest(null, url + "/assistants/" + assistantId, Header.buildBeta(apiKey), Request.Action.DELETE);
    }
}
