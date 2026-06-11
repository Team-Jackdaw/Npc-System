package team.jackdaw.npcsystem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team.jackdaw.npcsystem.ai.ConversationManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.function.NoCallableFunction;
import team.jackdaw.npcsystem.listener.PlayerSendMessageCallback;
import team.jackdaw.npcsystem.listener.SpawnNPCCallback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NPCSystem implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("npc-system");
    public static final Path workingDirectory = Paths.get(System.getProperty("user.dir"), "config", "npc-system");
    public static MinecraftServer server;

    public static void debugLog(String message, Object... args) {
        if (Config.debug) {
            LOGGER.info(message, args);
        }
    }

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
        // sync config
        ConfigManager.sync();
        if (!Config.enabled) return;
        // initialize the system
        try {
            Class.forName("team.jackdaw.npcsystem.ai.AgentManager");
            Class.forName("team.jackdaw.npcsystem.ai.ConversationManager");
            Class.forName("team.jackdaw.npcsystem.NPC_AI");
            Class.forName("team.jackdaw.npcsystem.ai.master.Master");
            Class.forName("team.jackdaw.npcsystem.group.GroupManager");
            Class.forName("team.jackdaw.npcsystem.group.GroupDataManager");
            Class.forName("team.jackdaw.npcsystem.function.FunctionRegistration");
            Class.forName("team.jackdaw.npcsystem.entity.NPCRegistration");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        // sync other files
        NoCallableFunction.sync();
        // catch the server instance
        ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> NPCSystem.server = server);
        // register commands
        CommandRegistrationCallback.EVENT.register(CommandSet::setupCommand);
        // Register for NPCEntity registration
        SpawnNPCCallback.EVENT.register((npc -> {
            NPC_AI.registerNPC(npc);
            npc.setInvulnerable(true);
            return InteractionResult.PASS;
        }));
        // Register the starting conversation by player
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // The entity should be an NPC
            if (!(entity instanceof NPCEntity npc)) return InteractionResult.PASS;
            // The player must be sneaking to start a conversation
            if (!player.isShiftKeyDown()) return InteractionResult.PASS;
            // start a conversation
            NPCSystem.debugLog("[npc-system] Player {} started conversation with NPC {}", player.getName().getString(), npc.getUUID());
            if (!ConversationManager.getInstance().isRegistered(npc.getUUID()))
                NPC_AI.startPlayerConversation(npc, player);
            return InteractionResult.FAIL;
        });
        // Register the player chat listener
        PlayerSendMessageCallback.EVENT.register((player, message) -> {
            NPCSystem.debugLog("[npc-system] Player chat from {}: {}", player.getName().getString(), message);
            NPC_AI.NPC_ENTITY_MANAGER.map.values().forEach(npc -> npc.hearChat(player, message));
            ConversationWindow conversationWindow =
                    ConversationManager.getInstance().map
                            .values()
                            .stream()
                            .filter(window -> window.getTarget() != null && window.getTarget().equals(player.getUUID()))
                            .findFirst()
                            .orElse(null);
            if (conversationWindow != null && !conversationWindow.isOnWait()) {
                AsyncTask.call(() -> {
                    conversationWindow.onWait();
                    NPCSystem.debugLog("[npc-system] Forwarding player chat to NPC conversation target={}", player.getUUID());
                    conversationWindow.chat(message);
                    NPC_AI.broadcastMessage(conversationWindow);
                    conversationWindow.offWait();
                    return AsyncTask.nothingToDo();
                });
            }
            return InteractionResult.PASS;
        });
        // register events
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            while (!AsyncTask.isTaskQueueEmpty()) {
                AsyncTask.TaskResult result = AsyncTask.pollTaskQueue();
                result.execute();
            }
            // check if the NPC entity is removed
            NPC_AI.NPC_ENTITY_MANAGER.map.forEach((uuid, npc) -> {
                if (npc.isRemoved()) {
                    if (ConversationManager.getInstance().isRegistered(npc.getUUID()))
                        ConversationManager.getInstance().remove(npc.getUUID());
                }
            });
        });
        // start live cycle manager
        LiveCycleManager.start(Config.updateInterval);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LiveCycleManager.shutdown();
            LiveCycleManager.saveAll();
        });
    }
}
