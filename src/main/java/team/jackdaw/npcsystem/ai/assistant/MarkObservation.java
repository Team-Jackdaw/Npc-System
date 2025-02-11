package team.jackdaw.npcsystem.ai.assistant;

import com.google.gson.Gson;
import team.jackdaw.npcsystem.api.Ollama;
import team.jackdaw.npcsystem.api.completion.Completion;
import team.jackdaw.npcsystem.api.completion.json.CompletionRequest;
import team.jackdaw.npcsystem.api.completion.json.CompletionResponse;

import java.util.Map;

public class MarkObservation {
    public static int markObservation(String observation) {
        try {
            CompletionRequest req = new CompletionRequest();
            req.model = Ollama.CHAT_MODEL;
            req.system = "You are a mark assistant, you should mark the prompt from 0 to 10 based on how importance it is as a daily live memory.";
            req.prompt = observation;
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
            CompletionResponse res = Completion.completionRequest(Ollama.API_URL, req);
            Map grade = new Gson().fromJson(res.response, Map.class);
            Double gradeValue = (Double) grade.get("grade");
            return gradeValue.intValue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
