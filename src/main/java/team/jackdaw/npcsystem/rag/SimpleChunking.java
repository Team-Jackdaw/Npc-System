package team.jackdaw.npcsystem.rag;

import java.util.ArrayList;
import java.util.List;

class SimpleChunking {
    static List<String> chunkText(String text, int maxWords) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        int wordCount = 0;

        for (String word : words) {
            if (wordCount >= maxWords) {
                chunks.add(chunk.toString().trim());
                chunk.setLength(0);
                wordCount = 0;
            }
            chunk.append(word).append(" ");
            wordCount++;
        }

        if (!chunk.isEmpty()) {
            chunks.add(chunk.toString().trim());
        }

        return chunks;
    }
}
