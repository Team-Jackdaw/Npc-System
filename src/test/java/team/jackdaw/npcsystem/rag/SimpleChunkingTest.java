package team.jackdaw.npcsystem.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static team.jackdaw.npcsystem.rag.SimpleChunking.chunkText;

public class SimpleChunkingTest {
    @Test
    public void testChunkText() {
        String text = "This is an example paragraph that needs to be split into chunks. " +
                      "This is an example paragraph that needs to be split into chunks. " +
                      "This is an example paragraph that needs to be split into chunks. " +
                      "This is an example paragraph that needs to be split into chunks. " +
                      "This is an example paragraph that needs to be split into chunks.";
        List<String> chunks = chunkText(text, 10);

        for (String chunk : chunks) {
            System.out.println("Chunk: " + chunk);
        }
    }
}
