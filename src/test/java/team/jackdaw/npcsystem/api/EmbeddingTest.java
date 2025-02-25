package team.jackdaw.npcsystem.api;

import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.api.json.EmbeddingResponse;

import java.util.List;

import static team.jackdaw.npcsystem.ConfigTest.setOllamaConfig;

public class EmbeddingTest {
    static {
        setOllamaConfig();
    }
    @Test
    public void testEmbedding() {
        try {
            EmbeddingResponse res = Ollama.embed(List.of("Hello world!"));
            System.out.println(res.embeddings.get(0).toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
