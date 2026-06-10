package team.jackdaw.npcsystem;

public class ConfigTest {
    public static void setOllamaConfig() {
        Config.dbURL = setting("npc.test.dbUrl", "NPC_TEST_DB_URL", "http://localhost:8080");
        Config.apiURL = setting("npc.test.apiUrl", "NPC_TEST_API_URL", "http://localhost:11434");
        Config.chat_model = setting("npc.test.chatModel", "NPC_TEST_CHAT_MODEL", "qwen3.5:latest");
    }

    private static String setting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv(environment);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return fallback;
    }
}
