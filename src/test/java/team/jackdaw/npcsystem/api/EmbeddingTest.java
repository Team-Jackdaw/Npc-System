package team.jackdaw.npcsystem.api;

import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.api.embedding.Embedding;
import team.jackdaw.npcsystem.api.embedding.json.EmbeddingResponse;

import java.util.List;

public class EmbeddingTest {
    public static final String API_URL = "http://192.168.122.74:11434";
    public static final String MODEL = "nomic-embed-text";
    @Test
    public void testEmbedding() {
        try {
            EmbeddingResponse res = Embedding.embedRequest(API_URL, MODEL, List.of("Hello world!"));
            System.out.println(res.embeddings.get(0).toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
