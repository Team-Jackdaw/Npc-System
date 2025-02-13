package team.jackdaw.npcsystem.api.completion.json;

import com.google.gson.Gson;

import java.util.Map;

public class CompletionRequest {
    public String model;
    public String prompt;
    public String system;
    public boolean stream;
    public Map format;

    public String toJson() {
        return new Gson().toJson(this);
    }
}
