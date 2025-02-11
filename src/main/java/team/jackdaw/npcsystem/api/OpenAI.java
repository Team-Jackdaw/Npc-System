package team.jackdaw.npcsystem.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.jackdaw.npcsystem.api.assistant.Assistant;
import team.jackdaw.npcsystem.api.assistant.Run;
import team.jackdaw.npcsystem.api.assistant.Threads;
import team.jackdaw.npcsystem.api.json.Tool;

import java.util.List;
import java.util.Map;

public class OpenAI {
    public static String API_URL = "https://api.openai.com/v1";
    public static String API_KEY = "sk-1234567890";
    public static String MODEL = "gpt-4o-mini";
    public static String assistantRequest(@NotNull String name, @NotNull String instruction, @Nullable List<Tool> tools, @Nullable String assistantId) throws Exception {
        return Assistant.assistantRequest(API_URL, API_KEY, MODEL, name, instruction, tools, assistantId);
    }
    public static void deleteAssistant(@NotNull String assistantId) throws Exception {
        Assistant.deleteAssistant(API_URL, API_KEY, assistantId);
    }
    public static String threadsRequest(@NotNull ThreadsRequestAction action, @Nullable String threadId, @Nullable String message) throws Exception {
        return switch (action) {
            case CREATE -> Threads.createThread(API_URL, API_KEY);
            case ADD_MESSAGE -> {
                Threads.addMessage(API_URL, API_KEY, threadId, message);
                yield null;
            }
            case GET_LAST_MESSAGE -> Threads.getLastMessage(API_URL, API_KEY, threadId);
            case DISCARD -> {
                Threads.discardThread(API_URL, API_KEY, threadId);
                yield null;
            }
        };
    }
    public enum ThreadsRequestAction {
        CREATE,
        ADD_MESSAGE,
        GET_LAST_MESSAGE,
        DISCARD
    }
    public static Run run(@NotNull String threadId, @NotNull String assistantId) throws Exception {
        return Run.run(API_URL, API_KEY, threadId, assistantId);
    }
    public static Run submitToolOutput(@NotNull Run run, @NotNull List
            <Map<String, String>> res) throws Exception {
        return run.submitToolOutput(API_URL, API_KEY, res);
    }
}
