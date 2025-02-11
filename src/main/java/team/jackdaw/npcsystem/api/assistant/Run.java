package team.jackdaw.npcsystem.api.assistant;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import team.jackdaw.npcsystem.api.Header;
import team.jackdaw.npcsystem.api.Request;
import team.jackdaw.npcsystem.api.assistant.json.RequiredAction;
import team.jackdaw.npcsystem.api.assistant.json.ToolCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Run {
    private String id;
    private String thread_id;
    private String assistant_id;
    private String status;
    private RequiredAction required_action;

    /**
     * Run the assistant for the NPC from the OpenAI API, and it will return the result.
     *
     * @param url The url to send the request to. It should look like "<a href="https://api.openai.com/v1">https://api.openai.com/v1</a>"
     * @param apiKey The API key to use
     * @param threadId The thread id
     * @param assistantId The assistant id
     * @return The result of the run
     * @throws Exception If the run status is null
     */
    public static Run run(@NotNull String url, @NotNull String apiKey, String threadId, String assistantId) throws Exception {
        Map<String, String> assistant = Map.of("assistant_id", assistantId);
        String res = Request.sendRequest(toJson(assistant), url + "/threads/" + threadId + "/runs", Header.buildBeta(apiKey), Request.Action.POST);
        Run run = fromJson(res);
        if (run.status == null) {
            throw new Exception("Run status is null");
        }
        run = checkOrTimeOut(url, apiKey, run);
        return run;
    }

    /**
     * Submit the tool output
     * @param url The url to send the request to. It should look like "<a href="https://api.openai.com/v1">https://api.openai.com/v1</a>"
     * @param apiKey The API key to use
     * @param res The result of the tool
     * @return The result of the run
     * @throws Exception If the run status is null
     */
    public Run submitToolOutput(@NotNull String url, @NotNull String apiKey, List<Map<String, String>> res) throws Exception {
        ArrayList<Map> outputs = new ArrayList<>();
        for (int i = 0; i < required_action.submit_tool_outputs.tool_calls.size(); i++) {
            ToolCall toolCall = required_action.submit_tool_outputs.tool_calls.get(i);
            if (toolCall.type.equals("function")) {
                outputs.add(Map.of(
                        "tool_call_id", toolCall.id,
                        "output", res.get(i)
                ));
            }
        }
        Map result = Map.of("tool_outputs", outputs);
        String response = toJson(result);
        Request.sendRequest(response, url + "/threads/" + thread_id + "/runs/" + id + "/submit_tool_outputs" , Header.buildBeta(apiKey), Request.Action.POST);
        return checkOrTimeOut(url, apiKey, this);
    }

    private static Run checkOrTimeOut(@NotNull String url, @NotNull String apiKey, @NotNull Run run) throws Exception {
        long expire = System.currentTimeMillis() + 10000;
        String newRes;
        newRes = run.updateStatus(url, apiKey);
        while (!run.isCompleted() && !run.isRequiresAction()) {
            if (System.currentTimeMillis() > expire) throw new Exception("Time out");
            Thread.sleep(50);
            newRes = run.updateStatus(url, apiKey);
        }
        return fromJson(newRes);
    }

    private @NotNull String updateStatus(@NotNull String url, @NotNull String apiKey) throws Exception {
        String res = Request.sendRequest(
                null,
                url + "/threads/" + thread_id + "/runs/" + id,
                Header.builder()
                        .add(Header.Type.AUTHORIZATION, apiKey)
                        .add(Header.Type.OPENAI_BETA, null)
                        .build(),
                Request.Action.GET
        );
        Run run = fromJson(res);
        if (run.status == null) {
            throw new Exception("Run status is null");
        }
        this.status = run.status;
        return res;
    }

    private static String toJson(Map map) {
        return new Gson().toJson(map);
    }

    private static Run fromJson(String json) {
        return new Gson().fromJson(json, Run.class);
    }

    /**
     * Check if the run is completed
     * @return True if the run is completed
     */
    public boolean isCompleted() {
        return this.status.equals("completed");
    }

    /**
     * Check if the run requires action
     * @return True if the run requires action
     */
    public boolean isRequiresAction() {
        return this.status.equals("requires_action");
    }
}
