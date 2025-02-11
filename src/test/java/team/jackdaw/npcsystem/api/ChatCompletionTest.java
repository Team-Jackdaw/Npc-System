package team.jackdaw.npcsystem.api;

import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.api.json.ChatResponse;
import team.jackdaw.npcsystem.api.json.Tool;

import java.util.ArrayList;

public class ChatCompletionTest {

    public static final String API_URL = "http://192.168.122.74:11434/api/chat";
    public static final String MODEL = "qwen2.5:14b";

    @Test
    public void testChatRequest() {
        // Test case 1 (no function)
        try {
            ChatResponse res = ChatCompletion.chatRequest(API_URL, MODEL,
                    ChatCompletion.messageBuilder()
                            .addMessage("user", "Hello")
                            .build(),
                    null
            );
            System.out.println(res.message.content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testChatRequestFunction() {
        // Test case 2 (with function)
        String functionJson = "{\n" +
                "      \"type\": \"function\",\n" +
                "      \"function\": {\n" +
                "        \"name\": \"get_current_weather\",\n" +
                "        \"description\": \"Get the current weather for a location\",\n" +
                "        \"parameters\": {\n" +
                "          \"type\": \"object\",\n" +
                "          \"properties\": {\n" +
                "            \"location\": {\n" +
                "              \"type\": \"string\",\n" +
                "              \"description\": \"The location to get the weather for, e.g. San Francisco, CA\"\n" +
                "            },\n" +
                "            \"format\": {\n" +
                "              \"type\": \"string\",\n" +
                "              \"description\": \"The format to return the weather in, e.g. 'celsius' or 'fahrenheit'\",\n" +
                "              \"enum\": [\"celsius\", \"fahrenheit\"]\n" +
                "            }\n" +
                "          },\n" +
                "          \"required\": [\"location\", \"format\"]\n" +
                "        }\n" +
                "      }\n" +
                "    }";
        ArrayList<Tool> tools = new ArrayList<>();
        tools.add(Tool.fromJson(functionJson));
        try {
            ChatResponse res = ChatCompletion.chatRequest(API_URL, MODEL,
                    ChatCompletion.messageBuilder()
                            .addMessage("user", "What is the weather today in Paris?")
                            .build(),
                    tools
            );
            if (res.message.tool_calls != null) {
                for (ChatResponse.Message.ToolCall toolCall : res.message.tool_calls) {
                    System.out.println(toolCall.function.name);
                    System.out.println(toolCall.function.arguments);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
