package team.jackdaw.npcsystem.api.json;

import com.google.gson.Gson;

public class CompletionResponse {
    public String model;
    public String created_at;
    public String response;
    public String thinking;
    public String done_reason;
    public boolean done;

    public String outputText() {
        if (response != null && !response.isBlank()) {
            return response;
        }
        return thinking;
    }

    public static CompletionResponse fromJson(String json) {
        return new Gson().fromJson(json, CompletionResponse.class);
    }
}
