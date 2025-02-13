package team.jackdaw.npcsystem.conversation;

import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.api.Ollama;
import team.jackdaw.npcsystem.api.chatcompletion.json.ChatResponse;
import team.jackdaw.npcsystem.api.chatcompletion.json.Message;
import team.jackdaw.npcsystem.api.chatcompletion.json.Role;
import team.jackdaw.npcsystem.api.json.Tool;
import team.jackdaw.npcsystem.rag.RAG;
import team.jackdaw.npcsystem.rag.WeaviateDB;

import java.util.List;

public class TwoAgentsConversationTest {

    @Test
    public void testConversation() {
        // 连接到Ollama模型和Weaviate数据库
        Ollama.API_URL = "http://192.168.122.74:11434";
        Ollama.CHAT_MODEL = "qwen2.5:14b";
        WeaviateDB db = new WeaviateDB("http", "jackdaw-v3:8080");

        // 根据"Document"数据库中的背景信息，用LLM创建两个角色（Ribo Lee和Sensei Tony）的基本信息。
        try {
            String contextRibo = RAG.completion(db, """
                            Tell me the ground truth, background, information that Ribo Lee will know. Just reply with one paragraph, without other instructions.
                            """,
                    5, "Document");
            String promptRibo = "You are Ribo Lee. You are a spirited teenager. You speak confidently. You know the follow thing:\n" + contextRibo + """
                    You are now having a conversation with Tony. And you want to ask him about the current state of the Empire's foreign wars.
                    Limit each of your responses to no more than 30 words.
                    The conversation is start by you.
                    """;
            System.out.println("Prompt of Ribo: " + promptRibo);
            String contextTony = RAG.completion(db, """
                            Tell me the ground truth, background, information that Sensei Tony will know. Just reply with one paragraph, without other instructions.
                            """,
                    5, "Document");
            String promptTony = "You are Sensei Tony. You're a calm person. Speak succinctly and forcefully without babbling. You know the follow thing: " + contextTony + """
                    You are now having a conversation with Ribo. And you want to ask him about his background and his plan.
                    Limit each of your responses to no more than 30 words.
                    """;
            System.out.println("Prompt of Tony: " + promptTony);

            // 构建他们的消息记录
            List<Message> messagesRibo = Ollama.messageBuilder()
                    .addMessage(Role.SYSTEM, promptRibo)
                    .build();
            List<Message> messagesTony = Ollama.messageBuilder()
                    .addMessage(Role.SYSTEM, promptTony)
                    .build();

            // 添加停止聊天函数
            String stopJson = """
                    {
                        "type": "function",
                        "function": {
                            "name": "stop_conversation",
                            "description": "This function stops the conversation. Call this function when you want to stop the conversation."
                        }
                    }
                      """;

            Tool stopTool = Tool.fromJson(stopJson);

            // 下面开始模拟聊天
            String newMessageToRibo;
            String newMessageToTony;
            ChatResponse response;

            newMessageToTony = Ollama.chat(messagesRibo, null).message.content;
            System.out.println("Ribo: " + newMessageToTony + "\n");
            messagesRibo = Ollama.messageBuilder(messagesRibo).addMessage(Role.ASSISTANT, newMessageToTony).build();
            messagesTony = Ollama.messageBuilder(messagesTony).addMessage(Role.USER, newMessageToTony).build();

            for (int i = 0; i < 100; i++) {
                // Tony回复Ribo
                response = Ollama.chat(messagesTony, List.of(stopTool));
                if (response.message.tool_calls != null && response.message.tool_calls.get(0).function.name.equals("stop_conversation")) {
                    newMessageToRibo = "See you later.";
                    i = 100;
                } else {
                    newMessageToRibo = response.message.content;
                }
                System.out.println("Tony: " + newMessageToRibo + "\n");
                messagesTony = Ollama.messageBuilder(messagesTony).addMessage(Role.ASSISTANT, newMessageToRibo).build();
                messagesRibo = Ollama.messageBuilder(messagesRibo).addMessage(Role.USER, newMessageToRibo).build();
                // Ribo回复Tony
                response = Ollama.chat(messagesRibo, List.of(stopTool));
                if (response.message.tool_calls != null && response.message.tool_calls.get(0).function.name.equals("stop_conversation")) {
                    newMessageToTony = "See you later.";
                    i = 100;
                } else {
                    newMessageToTony = response.message.content;
                }
                System.out.println("Ribo: " + newMessageToTony + "\n");
                messagesRibo = Ollama.messageBuilder(messagesRibo).addMessage(Role.ASSISTANT, newMessageToTony).build();
                messagesTony = Ollama.messageBuilder(messagesTony).addMessage(Role.USER, newMessageToTony).build();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
