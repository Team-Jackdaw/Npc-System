package team.jackdaw.npcsystem;

public class ConfigTest {
    public static void setOllamaConfig() {
        Config.dbURL = "http://jackdaw-v3:8080";
        Config.apiURL = "http://192.168.122.74:11434";
        Config.embedding_model = "nomic-embed-text";
        Config.chat_model = "qwen2.5:7b";
    }
}
