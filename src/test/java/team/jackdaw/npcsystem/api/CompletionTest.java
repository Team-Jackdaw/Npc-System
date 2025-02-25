package team.jackdaw.npcsystem.api;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.api.json.CompletionRequest;
import team.jackdaw.npcsystem.api.json.CompletionResponse;

import java.util.Map;

import static team.jackdaw.npcsystem.ConfigTest.setOllamaConfig;

public class CompletionTest {
static {
    setOllamaConfig();
}

    @Test
    public void testCompletionRequest() {
        try {
            CompletionResponse res = Ollama.completion("Hello");
            System.out.println(res.response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMarkRequest() {
        try {
            CompletionRequest req = new CompletionRequest();
            req.model = Config.chat_model;
            req.system = "You are a mark assistant, you should mark the prompt from 0 to 10 based on how importance it is as a daily live memory.";
            req.prompt = "I went to the park with my friends today.";
            req.stream = false;
            String formatJson = """
                    {
                        "type": "object",
                        "properties": {
                          "grade": {
                            "type": "integer"
                          }
                        },
                        "required": [
                          "grade"
                        ]
                      }
                    """;
            req.format = new Gson().fromJson(formatJson, Map.class);
            CompletionResponse res = Ollama.completion(req);
            Map grade = new Gson().fromJson(res.response, Map.class);
            Double gradeValue = (Double) grade.get("grade");
            System.out.println(gradeValue.intValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
