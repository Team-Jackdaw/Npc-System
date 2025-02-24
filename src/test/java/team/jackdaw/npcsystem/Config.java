package team.jackdaw.npcsystem;

public class Config {
    public static void setOllamaConfig() {
        SettingManager.dbURL = "http://jackdaw-v3:8080";
        SettingManager.apiURL = "http://192.168.122.74:11434";
        SettingManager.embedding_model = "nomic-embed-text";
        SettingManager.chat_model = "qwen2.5:7b";
    }
}
