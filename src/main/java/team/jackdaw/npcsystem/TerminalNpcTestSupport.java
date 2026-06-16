package team.jackdaw.npcsystem;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.entity.NPCEntity;

import java.util.Comparator;
import java.util.Optional;

public final class TerminalNpcTestSupport {
    public static final String SPEAKER_NAME = "TerminalTester";
    private static Entity target;
    private static ConversationWindow lastWindow;
    private static NPCEntity lastNpc;

    private TerminalNpcTestSupport() {
    }

    public static Optional<Entity> findTarget(String name) {
        if (!SPEAKER_NAME.equalsIgnoreCase(name) || target == null || target.isRemoved() || !target.isAlive()) {
            return Optional.empty();
        }
        return Optional.of(target);
    }

    public static int chat(CommandSourceStack source, String message) {
        Optional<NPCEntity> npc = nearestNpc(source);
        if (npc.isEmpty()) {
            source.sendSystemMessage(Component.literal("[npc-system] No NPC found for terminal chat test."));
            return 0;
        }
        NPCEntity entity = npc.get();
        ensureTarget((ServerLevel) entity.level(), entity);
        if (target == null || target.isRemoved() || !target.isAlive()) {
            source.sendSystemMessage(Component.literal("[npc-system] Terminal chat test target unavailable."));
            return 0;
        }
        NPC ai = NPC_AI.getAI(entity);
        if (ai == null) {
            NPC_AI.registerNPC(entity);
            ai = NPC_AI.getAI(entity);
        }
        if (ai == null) {
            source.sendSystemMessage(Component.literal("[npc-system] NPC AI registration unavailable."));
            return 0;
        }
        ConversationWindow window = ai.getNewConversationWindows();
        lastWindow = window;
        lastNpc = entity;
        window.setTarget(target.getUUID());
        window.setSyntheticSpeakerName(SPEAKER_NAME);
        if (window.isOnWait()) {
            source.sendSystemMessage(Component.literal("[npc-system] Terminal chat test conversation is busy."));
            return 0;
        }
        AsyncTask.call(() -> {
            window.onWait();
            NPCSystem.debugLog("[npc-system] Terminal test sends message to NPC {}: {}", entity.getUUID(), message);
            window.chat(message);
            NPC_AI.broadcastMessage(window);
            window.offWait();
            return AsyncTask.nothingToDo();
        });
        source.sendSystemMessage(Component.literal("[npc-system] Terminal chat test sent to NPC " + entity.getUUID() + "."));
        return 1;
    }

    public static int status(CommandSourceStack source) {
        refreshForcedChunks();
        Component text = Component.literal("")
                .append(Component.literal("[npc-system] Terminal NPC Test").withStyle(ChatFormatting.UNDERLINE))
                .append("\nSpeaker: ").append(Component.literal(SPEAKER_NAME))
                .append("\nTarget: ").append(Component.literal(target == null ? "none" : target.getUUID().toString()))
                .append("\nNPC: ").append(Component.literal(lastNpc == null ? "none" : lastNpc.getUUID().toString()))
                .append("\nTask: ").append(Component.literal(lastNpc == null ? "none" : lastNpc.getTaskController().status()))
                .append("\nLast User Message: ").append(Component.literal(lastWindow == null ? "" : lastWindow.getLastUserMessage()))
                .append("\nLast NPC Reply: ").append(Component.literal(lastWindow == null ? "" : lastWindow.getLastAssistantMessage()))
                .append("\nLast Agent Result: ").append(Component.literal(lastWindow == null ? "" : lastWindow.getLastAgentResultSummary()));
        source.sendSystemMessage(text);
        return 1;
    }

    private static void refreshForcedChunks() {
        if (lastNpc != null && !lastNpc.isRemoved() && lastNpc.level() instanceof ServerLevel level) {
            forceChunk(level, lastNpc);
        }
        if (target != null && !target.isRemoved() && target.level() instanceof ServerLevel level) {
            forceChunk(level, target);
        }
    }

    private static Optional<NPCEntity> nearestNpc(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 position = source.getPosition();
        return NPC_AI.NPC_ENTITY_MANAGER.map.values().stream()
                .filter(npc -> !npc.isRemoved() && npc.level().equals(level))
                .min(Comparator.comparingDouble(npc -> npc.distanceToSqr(position)));
    }

    private static void ensureTarget(ServerLevel level, NPCEntity npc) {
        forceChunk(level, npc);
        if (target != null && !target.isRemoved() && target.isAlive() && target.level().equals(level)) {
            target.teleportTo(npc.getX() + 2.0, npc.getY(), npc.getZ());
            forceChunk(level, target);
            return;
        }
        ArmorStand stand = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
        if (stand == null) {
            return;
        }
        stand.setCustomName(Component.literal(SPEAKER_NAME));
        stand.setCustomNameVisible(false);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setPos(npc.getX() + 2.0, npc.getY(), npc.getZ());
        level.addFreshEntity(stand);
        target = stand;
        forceChunk(level, stand);
    }

    private static void forceChunk(ServerLevel level, Entity entity) {
        ChunkPos chunk = entity.chunkPosition();
        level.setChunkForced(chunk.x(), chunk.z(), true);
    }
}
