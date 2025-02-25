package team.jackdaw.npcsystem.rag;

import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.api.Ollama;

import java.util.List;

public interface RAG {

    /**
     * The size of the chunk for the text.
     */
    int CHUNK_SIZE = 150;

    /**
     * Record the text into the database. The text will be chunked into smaller pieces based on the CHUNK_SIZE.
     * @param text text to be recorded
     * @param className class name of database (make sure the class is created in the database)
     * @throws Exception if the text cannot be recorded
     */
    static void record(String text, String className) throws Exception {
        String[] url = Config.dbURL.split("://");
        WeaviateDB db = new WeaviateDB(url[0], url[1]);
        List<String> chunks = SimpleChunking.chunkText(text, CHUNK_SIZE);
        List<Float[]> vectors = Ollama.embed(chunks).embeddings.stream().map(f -> f.toArray(Float[]::new)).toList();
        db.insertData(chunks, vectors, className);
    }

    /**
     * Query for the related text chunk that has the highest similarity by comparing the embedded vector.
     * @param text text to be queried
     * @param topK number of top results to be returned
     * @param className class name of database (make sure the class is created in the database)
     * @return a list of text chunks that have the highest similarity with the input text
     * @throws Exception if the text cannot be queried
     */
    static List<String> query(String text, int topK, String className) throws Exception {
        String[] url = Config.dbURL.split("://");
        WeaviateDB db = new WeaviateDB(url[0], url[1]);
        List<String> chunks = SimpleChunking.chunkText(text, CHUNK_SIZE);
        List<Float[]> vectors = Ollama.embed(chunks).embeddings.stream().map(f -> f.toArray(Float[]::new)).toList();
        return WeaviateDB.queryGetText(db.query(vectors, topK, className), className);
    }

    /**
     * Generate a completion based on the input text. The completion will be generated based on the context of the text that has the highest similarity.
     * @param input Prompt message
     * @param topK number of top results to be returned
     * @param className class name of database (make sure the class is created in the database)
     * @return the completion message
     * @throws Exception if the completion cannot be generated
     */
    static String completion(String input, int topK, String className) throws Exception {
        List<String> texts = query(input, topK, className);
        String context = String.join("\n", texts);
        String prompt = "Base on the following context to reply the message:\n" + context + "\nMessage: " + input;
        return Ollama.completion(prompt).response;
    }
}
