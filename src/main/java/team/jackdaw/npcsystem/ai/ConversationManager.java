package team.jackdaw.npcsystem.ai;

import org.jetbrains.annotations.NotNull;
import team.jackdaw.npcsystem.BaseManager;
import team.jackdaw.npcsystem.SettingManager;

import java.util.UUID;

public class ConversationManager extends BaseManager<UUID, ConversationWindow> {
    private static final ConversationManager INSTANCE = new ConversationManager();
    private static final long outOfTime = SettingManager.outOfTime;
    private ConversationManager() {}

    public static ConversationManager getInstance() {
        return INSTANCE;
    }

    public void register(@NotNull Agent agent) {
        if (isRegistered(agent.getUUID())) return;
        ConversationWindow conversationWindow = agent.getConversationWindows();
        register(agent.getUUID(), conversationWindow);
    }

    @Override
    protected boolean discard(UUID uuid) {
        get(uuid).discard();
        return true;
    }

    public void removeTimeout() {
        map.forEach((uuid, ConversationWindow) -> {
            if (ConversationWindow.getUpdateTime() + outOfTime < System.currentTimeMillis()) {
                remove(uuid);
            }
        });
    }
}
