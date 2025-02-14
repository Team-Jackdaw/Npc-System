package team.jackdaw.npcsystem;

import team.jackdaw.npcsystem.api.Ollama;
import team.jackdaw.npcsystem.rag.WeaviateDB;

public class Config {
    public static WeaviateDB db = new WeaviateDB("http", "jackdaw-v3:8080");

    public static void setOllamaConfig() {
        Ollama.API_URL = "http://192.168.122.74:11434";
        Ollama.EMBEDDING_MODEL = "nomic-embed-text";
        Ollama.CHAT_MODEL = "qwen2.5:7b";
    }
}
