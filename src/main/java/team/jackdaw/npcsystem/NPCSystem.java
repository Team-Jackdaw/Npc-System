package team.jackdaw.npcsystem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.jackdaw.npcsystem.ai.ConversationManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.function.NoCallableFunction;
import team.jackdaw.npcsystem.listener.PlayerSendMessageCallback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NPCSystem implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("npc-system");
    public static final Path workingDirectory = Paths.get(System.getProperty("user.dir"), "config", "npc-system");
    public static final boolean debug = true;
    public static MinecraftServer server;

    @Override
    public void onInitialize() {
        // create the working directory
        if (!Files.exists(workingDirectory)) {
            try {
                Files.createDirectories(workingDirectory);
            } catch (IOException e) {
                LOGGER.error("[npc-system] Failed to create the working directory");
                LOGGER.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }
        // sync
        ConfigManager.sync();
        NoCallableFunction.sync();
        // initialize the system
        try {
            Class.forName("team.jackdaw.npcsystem.ai.AgentManager");
            Class.forName("team.jackdaw.npcsystem.ai.ConversationManager");
            Class.forName("team.jackdaw.npcsystem.ai.master.Master");
            Class.forName("team.jackdaw.npcsystem.function.FunctionRegistration");
            Class.forName("team.jackdaw.npcsystem.entity.NPCRegistration");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        // catch the server instance
        ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> NPCSystem.server = server);
        // register commands
        CommandRegistrationCallback.EVENT.register(CommandSet::setupCommand);
        // Register the player chat listener
        PlayerSendMessageCallback.EVENT.register((player, message) -> {
            ConversationWindow conversationWindow =
                    ConversationManager.getInstance().map
                            .values()
                            .stream()
                            .filter(window -> window.getTarget() != null && window.getTarget().equals(player.getUuid()))
                            .findFirst()
                            .orElse(null);
            if (conversationWindow == null || conversationWindow.isOnWait()) return ActionResult.PASS;
            AsyncTask.call(() -> {
                if (!conversationWindow.isOnWait()) {
                    conversationWindow.onWait();
                    conversationWindow.chat(message);
                    conversationWindow.broadcastMessage();
                    conversationWindow.offWait();
                }
                return AsyncTask.nothingToDo();
            });
            return ActionResult.PASS;
        });
        // register events
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            while (!AsyncTask.isTaskQueueEmpty()) {
                AsyncTask.TaskResult result = AsyncTask.pollTaskQueue();
                result.execute();
            }
        });
        // start live cycle manager
        LiveCycleManager.start(Config.updateInterval);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LiveCycleManager.shutdown();
            LiveCycleManager.saveAll();
        });
    }
}
