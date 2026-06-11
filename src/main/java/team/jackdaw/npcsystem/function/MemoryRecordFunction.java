package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.master.MasterCW;
import team.jackdaw.npcsystem.memory.Memory;

import java.util.Map;

public class MemoryRecordFunction extends CustomFunction{
    public MemoryRecordFunction() {
        description = "Record something to local memory. Call this function when you want to save some knowledge for other conversation. For example, when user correct your response, you can record the correct response to local memory.";
        properties = Map.of(
                "context", Map.of(
                        "description", "The context you want to record.",
                        "type", "string"
                )
        );
        required = new String[]{"context"};
    }
    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        String context = (String) args.get("context");
        String className;
        if (conversation instanceof MasterCW) {
            className = "Master";
        }
        else {
            className = conversation.getAgent().getUUID().toString().toUpperCase();
        }
        try {
            Memory.record(context, className);
            return ToolResult.success("memory_recorded", "Memory recorded.");
        } catch (Exception e) {
            return ToolResult.failure("memory_record_failed", "Memory record failed.", true);
        }
    }
}
