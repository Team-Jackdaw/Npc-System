package team.jackdaw.npcsystem.ai.assistant;

import com.google.gson.Gson;
import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.api.Ollama;
import team.jackdaw.npcsystem.api.json.CompletionRequest;
import team.jackdaw.npcsystem.api.json.CompletionResponse;

import java.util.Map;

public interface Mark {
    static int markInteger(String input, String systemPrompt) {
        try {
            CompletionRequest req = new CompletionRequest();
            req.model = Config.chat_model;
            req.system = systemPrompt;
            req.prompt = input;
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
            return gradeValue.intValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
