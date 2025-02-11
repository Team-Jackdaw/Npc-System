package team.jackdaw.npcsystem.api.assistant;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import team.jackdaw.npcsystem.api.Header;
import team.jackdaw.npcsystem.api.Request;
import team.jackdaw.npcsystem.api.assistant.json.MessageList;
import team.jackdaw.npcsystem.api.assistant.json.ThreadsClass;

import java.util.Map;

public class Threads {
    /**
     * Create a new thread and set the thread id to the npc
     * @param url The url to send the request to
     * @param apiKey The API key to use
     * @return The thread id
     * @throws Exception If the thread id is null
     */
    public static @NotNull String createThread(@NotNull String url, @NotNull String apiKey) throws Exception {
        String res = Request.sendRequest("", url + "/threads", Header.buildBeta(apiKey), Request.Action.POST);
        String id = ThreadsClass.fromJson(res).id;
        if (id == null) {
            throw new Exception("Thread id is null");
        }
        return id;
    }

    /**
     * Add a message to the thread
     * @param url The url to send the request to. It should look like "<a href="https://api.openai.com/v1">https://api.openai.com/v1</a>"
     * @param apiKey The API key to use
     * @param threadId The thread id
     * @param message The message
     * @throws Exception If the message is not sent
     */
    public static void addMessage(@NotNull String url, @NotNull String apiKey, String threadId, String message) throws Exception {
        Map<String, String> content = Map.of(
                "role", "user",
                "content", message
        );
        String res = Request.sendRequest(ThreadsClass.toJson(content), url + "/threads/" + threadId + "/messages", Header.buildBeta(apiKey), Request.Action.POST);
        MessageList.Message message1 = new Gson().fromJson(res, MessageList.Message.class);
        if (message1.content == null) {
            throw new Exception("MessageList not sent");
        }
    }

    /**
     * Discard the thread id from the npc
     * @param url The url to send the request to
     * @param apiKey The API key to use
     * @param threadId The thread id
     * @throws Exception If is there any error
     */
    public static void discardThread(@NotNull String url, @NotNull String apiKey, String threadId) throws Exception {
        Request.sendRequest("", url + "/threads/" + threadId, Header.buildBeta(apiKey), Request.Action.DELETE);
    }

    /**
     * Get the last message from the thread
     * @param url The url to send the request to
     * @param apiKey The API key to use
     * @param threadId The thread id
     * @return The last message
     * @throws Exception If the message is not received
     */
    public static String getLastMessage(@NotNull String url, @NotNull String apiKey, String threadId) throws Exception {
        Map<String, String> filter = Map.of("limit", "1", "order", "desc");
        String res = Request.sendRequest(ThreadsClass.toJson(filter), url + "/threads/" + threadId + "/messages", Header.buildBeta(apiKey), Request.Action.GET);
        MessageList messageList = new Gson().fromJson(res, MessageList.class);
        if (messageList.data == null) {
            throw new Exception("MessageList not received");
        }
        return messageList.data.get(0).content.get(0).text.value;
    }
}
