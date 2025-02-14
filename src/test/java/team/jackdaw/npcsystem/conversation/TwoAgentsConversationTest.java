package team.jackdaw.npcsystem.conversation;

import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.api.Ollama;
import team.jackdaw.npcsystem.api.chatcompletion.json.ChatResponse;
import team.jackdaw.npcsystem.api.chatcompletion.json.Message;
import team.jackdaw.npcsystem.api.chatcompletion.json.Role;
import team.jackdaw.npcsystem.api.json.Tool;
import team.jackdaw.npcsystem.rag.RAG;

import java.util.List;

import static team.jackdaw.npcsystem.Config.db;
import static team.jackdaw.npcsystem.Config.setOllamaConfig;

public class TwoAgentsConversationTest {

    static {
        setOllamaConfig();
    }

    @Test
    public void testConversation() {
        // 设置
        String className = "ScottEmpire";
        String language = "Chinese";
        int wordLimit = 100;
        int maxTurns = 10;

        try {
            // 根据“Document”数据库中的信息，用LLM选出两个角色（Ribo Lee和Sensei Tony）可以交谈的一个话题。
            String topic = RAG.completion(db, """
                            Choose a topic that Ribo Lee and Sensei Tony can talk about. Just reply with one sentence, without other instructions.
                            """,
                    5, className);
            System.out.println("The topic is: " + topic + "\n");

            // 根据"Document"数据库中的背景信息，用LLM创建两个角色（Ribo Lee和Sensei Tony）的基本信息。
            String contextRibo = RAG.completion(db, """
                            Tell me the ground truth, background, information that Ribo Lee will know. Just reply with one paragraph, without other instructions.
                            """,
                    5, className);
            String promptRibo = String.format("""
                    You are Ribo Lee. You are a spirited teenager. You speak confidently. You are willing to share anything you know and enthusiastically ask things. You know the follow thing:
                    %s
                    You are now having a conversation with Tony. He's an old friend of yours, and your conversations should be in the same tone as they would be with an old friend. And you want to talk about %s.
                    You don't leave easily until you get the intel.
                    Limit each of your responses to no more than %s words. Please speak %s.
                    The conversation is start by you.
                    """, contextRibo, topic, wordLimit, language);
            System.out.println("Ribo prompt: " + promptRibo + "\n");
            String contextTony = RAG.completion(db, """
                            Tell me the ground truth, background, information that Sensei Tony will know. Just reply with one paragraph, without other instructions.
                            """,
                    5, className);
            String promptTony = String.format("""
                    You are Sensei Tony. You're a calm person. Speak succinctly and forcefully without babbling. You are willing to share anything you know and enthusiastically ask things. You know the follow thing:
                    %s
                    You are now having a conversation with Ribo. He's an old friend of yours, and your conversations should be in the same tone as they would be with an old friend.
                    You don't leave easily until you get the intel.
                    Limit each of your responses to no more than %s words. Please speak %s.
                    """, contextTony, wordLimit, language);
            System.out.println("Tony prompt: " + promptTony + "\n");

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
                            "name": "say_good_bye",
                            "description": "Call this function when you want to say good bye to the other. When you find the conversation is repeating something, you should also call this function."
                        }
                    }
                      """;

            Tool stopTool = Tool.fromJson(stopJson);

            // 下面开始模拟聊天
            String newMessageToRibo;
            String newMessageToTony;
            ChatResponse response;

            for (int i = 0; i < maxTurns; i++) {
                // Ribo回复Tony
                response = Ollama.chat(messagesRibo, List.of(stopTool));
                if (response.message.tool_calls != null && response.message.tool_calls.get(0).function.name.equals("say_good_bye")) {
                    newMessageToTony = "See you later.";
                    i = maxTurns;
                } else {
                    newMessageToTony = response.message.content;
                }
                System.out.println("Ribo: " + newMessageToTony + "\n");
                messagesRibo = Ollama.messageBuilder(messagesRibo).addMessage(Role.ASSISTANT, newMessageToTony).build();
                messagesTony = Ollama.messageBuilder(messagesTony).addMessage(Role.USER, newMessageToTony).build();
                // Tony回复Ribo
                response = Ollama.chat(messagesTony, List.of(stopTool));
                if (response.message.tool_calls != null && response.message.tool_calls.get(0).function.name.equals("say_good_bye")) {
                    newMessageToRibo = "See you later.";
                    i = maxTurns;
                } else {
                    newMessageToRibo = response.message.content;
                }
                System.out.println("Tony: " + newMessageToRibo + "\n");
                messagesTony = Ollama.messageBuilder(messagesTony).addMessage(Role.ASSISTANT, newMessageToRibo).build();
                messagesRibo = Ollama.messageBuilder(messagesRibo).addMessage(Role.USER, newMessageToRibo).build();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
