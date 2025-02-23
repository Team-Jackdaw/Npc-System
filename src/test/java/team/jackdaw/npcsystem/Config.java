package team.jackdaw.npcsystem;

import team.jackdaw.npcsystem.rag.WeaviateDB;

public class Config {
    public static WeaviateDB db = new WeaviateDB("http", "jackdaw-v3:8080");

    public static void setOllamaConfig() {
        SettingManager.apiURL = "http://192.168.122.74:11434";
        SettingManager.embedding_model = "nomic-embed-text";
        SettingManager.chat_model = "qwen2.5:7b";
    }
}
