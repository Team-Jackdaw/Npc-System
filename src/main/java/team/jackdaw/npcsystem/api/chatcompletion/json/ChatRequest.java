package team.jackdaw.npcsystem.api.chatcompletion.json;

import com.google.gson.Gson;
import team.jackdaw.npcsystem.api.json.Tool;

import java.util.List;

public class ChatRequest {
    public String model;
    public List<Message> messages;
    public boolean stream;
    public List<Tool> tools;

    public String toJson() {
        return new Gson().toJson(this);
    }
}
