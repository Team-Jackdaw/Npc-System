package team.jackdaw.npcsystem.api.assistant.json;

import com.google.gson.Gson;

import java.util.Map;

public class ThreadsClass {
    public String id;

    public static String toJson(Map<String, String> map) {
        return new Gson().toJson(map);
    }

    public static ThreadsClass fromJson(String json) {
        return new Gson().fromJson(json, ThreadsClass.class);
    }
}
