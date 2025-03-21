package team.jackdaw.npcsystem.function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.api.Ollama;
import team.jackdaw.npcsystem.api.json.*;

import java.util.ArrayList;
import java.util.List;

import static team.jackdaw.npcsystem.ConfigTest.setOllamaConfig;

public class CustomFunctionTest {
    @BeforeAll
    static void beforeAll() {
        setOllamaConfig();
        FunctionManager.getInstance().register("get_current_weather", new TestFunction());
    }

    @Test
    public void testFunction() {
        ArrayList<Tool> tools = new ArrayList<>();
        tools.add(FunctionManager.getInstance().getTools("get_current_weather"));
        List<Message> messages = Ollama.messageBuilder()
                .addMessage(Role.USER, "What is the weather today in Paris?")
                .build();
        try {
            ChatResponse res = Ollama.chat(messages, tools);
            if (res.message.tool_calls != null) {
                 MessageBuilder builder = Ollama.messageBuilder(messages);
                for (ChatResponse.Message.ToolCall toolCall : res.message.tool_calls) {
                    String functionResult = FunctionManager.getInstance()
                            .callFunction(null, toolCall.function.name, toolCall.function.arguments)
                            .toString();
                    builder.addToolMessage(toolCall.function.name, functionResult);
                }
                List<Message> messages2 = builder.build();
                ChatResponse res2 = Ollama.chat(messages2, null);
                System.out.println(res2.message.content);
            } else {
                System.out.println(res.message.content);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
