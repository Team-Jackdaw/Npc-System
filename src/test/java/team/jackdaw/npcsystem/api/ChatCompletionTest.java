package team.jackdaw.npcsystem.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.api.chatcompletion.ChatCompletion;
import team.jackdaw.npcsystem.api.chatcompletion.json.*;
import team.jackdaw.npcsystem.api.json.Tool;

import java.util.ArrayList;
import java.util.List;

public class ChatCompletionTest {

    public static final String API_URL = "http://192.168.122.74:11434";
    public static final String MODEL = "mistral";

    @Test
    public void testChatRequest() {
        // Test case 1 (no function)
        try {
            ChatResponse res = ChatCompletion.chatRequest(API_URL, MODEL,
                    ChatCompletion.messageBuilder()
                            .addMessage(Role.USER, "Hello")
                            .build(),
                    null
            );
            System.out.println(res.message.content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Contract(pure = true)
    private static @NotNull String getWeather(String location, String format) {
        return "The weather in " + location + " is 20 degrees " + format;
    }

    @Test
    public void testChatRequestFunction() {
        // Test case 2 (with function)
        String functionJson = """
                {
                  "type": "function",
                  "function": {
                    "name": "get_current_weather",
                    "description": "Get the current weather for a location",
                    "parameters": {
                      "type": "object",
                      "properties": {
                        "location": {
                          "type": "string",
                          "description": "The location to get the weather for, e.g. San Francisco, CA"
                        },
                        "format": {
                          "type": "string",
                          "description": "The format to return the weather in, e.g. 'celsius' or 'fahrenheit'",
                          "enum": ["celsius", "fahrenheit"]
                        }
                      },
                      "required": ["location", "format"]
                    }
                  }
                }""";
        ArrayList<Tool> tools = new ArrayList<>();
        tools.add(Tool.fromJson(functionJson));
        List<Message> messages = ChatCompletion.messageBuilder()
                .addMessage(Role.USER, "What is the weather today in Paris?")
                .build();
        try {
            ChatResponse res = ChatCompletion.chatRequest(API_URL, MODEL, messages, tools);
            if (res.message.tool_calls != null) {
                for (ChatResponse.Message.ToolCall toolCall : res.message.tool_calls) {
                    // Test case 3 (with function response)
                    String functionResult = getWeather((String) toolCall.function.arguments.get("location"), (String) toolCall.function.arguments.get("format"));
                    List<Message> messages2 = ChatCompletion.messageBuilder(messages)
                            .addToolMessage(toolCall.function.name, functionResult)
                            .build();
                    ChatResponse res2 = ChatCompletion.chatRequest(API_URL, MODEL, messages2, null);
                    System.out.println(res2.message.content);
                }
            } else {
                System.out.println(res.message.content);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
