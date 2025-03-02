package team.jackdaw.npcsystem;

import team.jackdaw.npcsystem.ai.ConversationManager;
import team.jackdaw.npcsystem.function.NoCallableFunction;
import team.jackdaw.npcsystem.group.GroupManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class LiveCycleManager {

    private static ScheduledExecutorService executorService;

    static void start(long updateInterval) {
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(LiveCycleManager::update, 0, updateInterval, TimeUnit.MILLISECONDS);
    }

    static void update() {
        ConversationManager.getInstance().removeTimeout();
        GroupManager.getInstance().removeTimeout();
    }

    public static void asyncSaveAll() {
        AsyncTask.call(() -> {
            saveAll();
            ConfigManager.sync();
            NoCallableFunction.sync();
            return AsyncTask.nothingToDo();
        });
    }

    static void saveAll() {
        ConversationManager.getInstance().clear();
        GroupManager.getInstance().clear();
    }

    static void shutdown() {
        executorService.shutdown();
    }
}
