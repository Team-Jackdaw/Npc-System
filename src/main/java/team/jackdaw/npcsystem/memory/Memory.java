package team.jackdaw.npcsystem.memory;

import team.jackdaw.npcsystem.api.Ollama;

import java.util.List;

public interface Memory {

    /**
     * The size of the chunk for the text.
     */
    int CHUNK_SIZE = 150;

    /**
     * Get the directory path used by the local text memory.
     * @return local memory storage path
     */
    static String storagePath() {
        return LocalTextMemory.getStoragePath();
    }

    /**
     * Initialize the local memory with the class name.
     * @param className class name of local memory
     */
    static void initialize(String className) {
        try {
            new LocalTextMemory(className).initialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Terminate the local memory with the class name.
     * @param className class name of local memory
     */
    static void terminate(String className) {
        try {
            new LocalTextMemory(className).terminate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Record the text into local memory. The text will be chunked into smaller pieces based on the CHUNK_SIZE.
     * @param text text to be recorded
     * @param className class name of local memory
     * @throws Exception if the text cannot be recorded
     */
    static void record(String text, String className) throws Exception {
        List<String> chunks = SimpleChunking.chunkText(text, CHUNK_SIZE);
        new LocalTextMemory(className).record(text, chunks);
    }

    /**
     * Query for related text chunks by local keyword scoring.
     * @param text text to be queried
     * @param topK number of top results to be returned
     * @param className class name of local memory
     * @return a list of text chunks that have the highest local keyword score for the input text
     * @throws Exception if the text cannot be queried
     */
    static List<String> query(String text, int topK, String className) throws Exception {
        return new LocalTextMemory(className).query(text, topK);
    }

    /**
     * Generate a completion based on the input text and the highest scoring local memory chunks.
     * @param input Prompt message
     * @param topK number of top results to be returned
     * @param className class name of local memory
     * @return the completion message
     * @throws Exception if the completion cannot be generated
     */
    static String completion(String input, int topK, String className) throws Exception {
        List<String> texts = query(input, topK, className);
        String context = String.join("\n", texts);
        String prompt = "Base on the following context to reply the message:\n" + context + "\nMessage: " + input;
        return Ollama.completion(prompt).outputText();
    }
}
