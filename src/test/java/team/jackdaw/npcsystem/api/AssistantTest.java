package team.jackdaw.npcsystem.api;

import org.junit.jupiter.api.Test;

public class AssistantTest {
    static {
        OpenAI.API_KEY = "sk-1234567890abcdef1234567890abcdef";
    }
    @Test
    public void test() {
        String name = "Assistant_test";
        String assistantId;
        String threadId;
        try {
            assistantId = OpenAI.assistantRequest(name, "You are just a test assistant.", null, null);
            threadId = OpenAI.threadsRequest(OpenAI.ThreadsRequestAction.CREATE, null, null);
            OpenAI.threadsRequest(OpenAI.ThreadsRequestAction.ADD_MESSAGE, threadId, "Hello, " + name);
            OpenAI.run(threadId, assistantId);
            String content = OpenAI.threadsRequest(OpenAI.ThreadsRequestAction.GET_LAST_MESSAGE, threadId, null);
            System.out.println(content);
            OpenAI.threadsRequest(OpenAI.ThreadsRequestAction.DISCARD, threadId, null);
            OpenAI.deleteAssistant(assistantId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
