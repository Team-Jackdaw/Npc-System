package team.jackdaw.npcsystem.rag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RAGTest {
    private final String memoryName = "TestMemory_" + UUID.randomUUID();

    @AfterEach
    void afterEach() {
        RAG.terminate(memoryName);
    }

    @Test
    public void testRecordAndQueryLocalMemory() throws Exception {
        RAG.initialize(memoryName);
        RAG.record("Aaron is the current king of the empire. Ribo is the escaped crown prince.", memoryName);
        RAG.record("Tony is a teacher and a member of the Brotherhood.", memoryName);

        List<String> res = RAG.query("Who is the current king?", 2, memoryName);

        assertFalse(res.isEmpty());
        assertTrue(res.get(0).contains("Aaron"));
    }

    @Test
    public void testQueryReturnsAtMostTopK() throws Exception {
        RAG.initialize(memoryName);
        RAG.record("Alpha knows the northern gate.", memoryName);
        RAG.record("Alpha knows the western bridge.", memoryName);

        List<String> res = RAG.query("Alpha knows", 1, memoryName);

        assertEquals(1, res.size());
    }

    @Test
    public void testQueryMissingMemoryReturnsEmptyList() throws Exception {
        List<String> res = RAG.query("anything", 3, memoryName);

        assertTrue(res.isEmpty());
    }
}
