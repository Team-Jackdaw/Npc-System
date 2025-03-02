package team.jackdaw.npcsystem.ai.assistant;

import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.ConfigTest;
import team.jackdaw.npcsystem.api.Ollama;
import team.jackdaw.npcsystem.api.json.Role;

public class AssistantTest {
    static {
        ConfigTest.setOllamaConfig();
    }

    @Test
    public void testSummariesConversation() {
        String res = Summarise.summariesConversation(
                "Summarise the conversation. USER is the player, ASSISTANT is the control system of Minecraft powered by AI.",
                Ollama.messageBuilder()
                        .addMessage(Role.SYSTEM, "You are a Assistant, you should provide any information that is useful to the user.")
                        .addMessage(Role.USER, "Hello!")
                        .addMessage(Role.ASSISTANT, "Hi!")
                        .addMessage(Role.USER, "Good bye!")
                        .addMessage(Role.ASSISTANT, "Bye!")
                        .build()
        );
        System.out.println(res);
    }

    @Test
    public void testMarkInteger() {
        String res = Summarise.summariesConversation(
                "Summarise the conversation. USER is the player, ASSISTANT is the control system of Minecraft powered by AI.",
                Ollama.messageBuilder()
                        .addMessage(Role.SYSTEM, "You are a Assistant, you should provide any information that is useful to the user.")
                        .addMessage(Role.USER, "Hello!")
                        .addMessage(Role.ASSISTANT, "Hi!")
                        .addMessage(Role.USER, "Good bye!")
                        .addMessage(Role.ASSISTANT, "Bye!")
                        .build()
        );
        System.out.println(res);
        int importance = Mark.markInteger(res, "You are a mark assistant, you should mark the prompt from 0 to 10 based on how importance it is as a conversation.");
        System.out.println(importance);
    }
}
