package team.jackdaw.npcsystem.function;

import team.jackdaw.npcsystem.ai.ConversationWindow;

import java.util.Map;

public class MasterPermissionFunction extends CustomFunction {
    public MasterPermissionFunction() {
        permissionLevel = 3;
        description = "Master-only test function.";
        properties = Map.of();
        required = new String[]{};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        return ToolResult.success("master_permission_ok", "Master permission accepted.");
    }
}
