package team.jackdaw.npcsystem.api.assistant.json;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Map;

public class AssistantClass {
    public String id;
    public String name;
    public String instructions;
    public String model;
    public ArrayList<Map> tools;

    public static AssistantClass fromJson(String json) {
        return new Gson().fromJson(json, AssistantClass.class);
    }
}
