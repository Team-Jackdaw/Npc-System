package team.jackdaw.npcsystem.function;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolResultTest {
    @Test
    void successContainsStandardFields() {
        Map<String, Object> result = ToolResult.success("task_started", "Task started.", Map.of("task", "wait"));

        assertEquals("success", result.get("status"));
        assertEquals("task_started", result.get("code"));
        assertEquals("Task started.", result.get("message"));
        assertEquals("wait", ((Map<?, ?>) result.get("data")).get("task"));
        assertFalse((Boolean) result.get("retryable"));
    }

    @Test
    void failureContainsStandardFields() {
        Map<String, Object> result = ToolResult.failure("target_not_found", "Player not found.", true);

        assertEquals("failure", result.get("status"));
        assertEquals("target_not_found", result.get("code"));
        assertEquals("Player not found.", result.get("message"));
        assertTrue(((Map<?, ?>) result.get("data")).isEmpty());
        assertTrue((Boolean) result.get("retryable"));
    }
}
