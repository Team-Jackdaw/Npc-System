package team.jackdaw.npcsystem.rag;

import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import org.jetbrains.annotations.NotNull;
import team.jackdaw.npcsystem.api.Ollama;
import team.jackdaw.npcsystem.api.completion.json.CompletionResponse;

import java.util.List;
import java.util.Objects;

public class RAG {
    public static int CHUNK_SIZE = 150;
    public static void record(@NotNull WeaviateDB db, String text) throws Exception {
        List<String> chunks = SimpleChunking.chunkText(text, CHUNK_SIZE);
        List<Float[]> vectors = Objects.requireNonNull(Ollama.embed(chunks)).embeddings.stream().map(f -> f.toArray(Float[]::new)).toList();
        db.insertData(chunks, vectors);
    }
    public static String completion(@NotNull WeaviateDB db, String input, int topK) throws Exception {
        List<String> chunks = SimpleChunking.chunkText(input, CHUNK_SIZE);
        List<Float[]> vectors = Objects.requireNonNull(Ollama.embed(chunks)).embeddings.stream().map(f -> f.toArray(Float[]::new)).toList();
        GraphQLResponse res = db.query(vectors, topK);
        List<String> texts = WeaviateDB.queryGetText(res);
        String context = String.join("\n", texts);
        String prompt = "Base on the following context to reply the message:\n" + context + "\nMessage: " + input;
        CompletionResponse res1 = Ollama.completion(prompt);
        return res1.response;
    }
}
