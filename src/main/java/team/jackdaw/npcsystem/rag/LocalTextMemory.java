package team.jackdaw.npcsystem.rag;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LocalTextMemory {
    private static final Gson GSON = new Gson();
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{IsHan}]|[\\p{L}\\p{N}]+");
    private static final File DIRECTORY = Paths.get(System.getProperty("user.dir"), "config", "npc-system", "rag").toFile();
    private static final Object FILE_LOCK = new Object();

    private final File file;

    LocalTextMemory(String className) {
        this.file = new File(DIRECTORY, sanitize(className) + ".json");
    }

    static String getStoragePath() {
        return DIRECTORY.getAbsolutePath();
    }

    void initialize() throws IOException {
        synchronized (FILE_LOCK) {
            mkdir();
            if (!file.exists()) {
                save(new MemoryData());
            }
        }
    }

    void terminate() throws IOException {
        synchronized (FILE_LOCK) {
            if (file.exists()) {
                Files.delete(file.toPath());
            }
        }
    }

    void record(String text, List<String> chunks) throws IOException {
        synchronized (FILE_LOCK) {
            initialize();
            MemoryData data = load();
            MemoryRecord record = new MemoryRecord();
            record.id = UUID.randomUUID().toString();
            record.text = text;
            record.chunks = new ArrayList<>(chunks);
            record.createdAt = Instant.now().toString();
            data.records.add(record);
            save(data);
        }
    }

    List<String> query(String text, int topK) throws IOException {
        synchronized (FILE_LOCK) {
            if (topK <= 0 || !file.exists()) {
                return List.of();
            }
            Map<String, Integer> queryTokens = tokenCounts(text);
            if (queryTokens.isEmpty()) {
                return load().records.stream()
                        .sorted(Comparator.comparing((MemoryRecord record) -> record.createdAt).reversed())
                        .flatMap(record -> record.chunks.stream())
                        .limit(topK)
                        .toList();
            }
            List<ScoredChunk> scoredChunks = new ArrayList<>();
            MemoryData data = load();
            for (MemoryRecord record : data.records) {
                for (String chunk : record.chunks) {
                    int score = score(queryTokens, tokenCounts(chunk));
                    if (score > 0) {
                        scoredChunks.add(new ScoredChunk(chunk, record.createdAt, score));
                    }
                }
            }
            return scoredChunks.stream()
                    .sorted(Comparator.comparingInt(ScoredChunk::score)
                            .thenComparing(ScoredChunk::createdAt)
                            .reversed())
                    .map(ScoredChunk::text)
                    .limit(topK)
                    .toList();
        }
    }

    private MemoryData load() throws IOException {
        initialize();
        String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        MemoryData data = GSON.fromJson(json, MemoryData.class);
        if (data == null) {
            return new MemoryData();
        }
        if (data.records == null) {
            data.records = new ArrayList<>();
        }
        return data;
    }

    private void save(MemoryData data) throws IOException {
        mkdir();
        Files.writeString(file.toPath(), GSON.toJson(data), StandardCharsets.UTF_8);
    }

    private static void mkdir() throws IOException {
        if (!DIRECTORY.exists()) {
            Files.createDirectories(DIRECTORY.toPath());
        }
    }

    private static String sanitize(String className) {
        return className.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static Map<String, Integer> tokenCounts(String text) {
        Map<String, Integer> tokens = new HashMap<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            tokens.put(token, tokens.getOrDefault(token, 0) + 1);
        }
        return tokens;
    }

    private static int score(Map<String, Integer> queryTokens, Map<String, Integer> chunkTokens) {
        int score = 0;
        for (Map.Entry<String, Integer> entry : queryTokens.entrySet()) {
            score += Math.min(entry.getValue(), chunkTokens.getOrDefault(entry.getKey(), 0));
        }
        return score;
    }

    private static final class MemoryData {
        private List<MemoryRecord> records = new ArrayList<>();
    }

    private static final class MemoryRecord {
        private String id;
        private String text;
        private List<String> chunks = new ArrayList<>();
        private String createdAt;
    }

    private record ScoredChunk(String text, String createdAt, int score) {
    }
}
