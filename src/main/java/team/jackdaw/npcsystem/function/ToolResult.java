package team.jackdaw.npcsystem.function;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolResult {
    private ToolResult() {
    }

    public static Map<String, Object> success(String code, String message) {
        return success(code, message, Map.of());
    }

    public static Map<String, Object> success(String code, String message, Map<String, ?> data) {
        return result("success", code, message, data, false);
    }

    public static Map<String, Object> failure(String code, String message, boolean retryable) {
        return failure(code, message, Map.of(), retryable);
    }

    public static Map<String, Object> failure(String code, String message, Map<String, ?> data, boolean retryable) {
        return result("failure", code, message, data, retryable);
    }

    private static Map<String, Object> result(String status, String code, String message, Map<String, ?> data, boolean retryable) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", status);
        result.put("code", code);
        result.put("message", message);
        result.put("data", data == null ? Map.of() : data);
        result.put("retryable", retryable);
        return result;
    }
}
