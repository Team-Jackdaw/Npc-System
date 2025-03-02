package team.jackdaw.npcsystem.ai.assistant;

import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.api.Ollama;
import team.jackdaw.npcsystem.api.json.CompletionRequest;
import team.jackdaw.npcsystem.api.json.CompletionResponse;
import team.jackdaw.npcsystem.api.json.Message;

import java.util.List;

public interface Summarise {
    static String summariesConversation(String instruction, List<Message> messages) {
        StringBuilder prompt = new StringBuilder();
        for (Message message : messages) {
            prompt.append("Role: ").append(message.role).append("\n");
            prompt.append("Message: ").append(message.content).append("\n");
        }
        try {
            CompletionRequest req = new CompletionRequest();
            req.model = Config.chat_model;
            req.stream = false;
            req.system = "You are a summarise assistant, you should summarise the conversation based on the instruction.";
            req.prompt = String.format("""
                    This is a conversation summarisation task. Please summarise the following conversation based on the instruction below:
                    Instruction: %s
                    Conversation:
                    %s
                    """, instruction, prompt);
            CompletionResponse res = Ollama.completion(req);
            return res.response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
