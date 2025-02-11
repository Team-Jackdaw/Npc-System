package team.jackdaw.npcsystem.api;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.api.completion.Completion;
import team.jackdaw.npcsystem.api.completion.json.CompletionRequest;
import team.jackdaw.npcsystem.api.completion.json.CompletionResponse;

import java.util.Map;

public class CompletionTest {
    public static final String API_URL = "http://192.168.122.74:11434";
    public static final String MODEL = "mistral";

    @Test
    public void testCompletionRequest() {
        try {
            CompletionResponse res = Completion.completionRequest(API_URL, MODEL, "Hello");
            System.out.println(res.response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMarkRequest() {
        try {
            CompletionRequest req = new CompletionRequest();
            req.model = MODEL;
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
            CompletionResponse res = Completion.completionRequest(API_URL, req);
            Map grade = new Gson().fromJson(res.response, Map.class);
            Double gradeValue = (Double) grade.get("grade");
            System.out.println(gradeValue.intValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
