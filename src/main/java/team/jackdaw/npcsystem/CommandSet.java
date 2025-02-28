package team.jackdaw.npcsystem;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.ConversationManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.master.Master;
import team.jackdaw.npcsystem.entity.NPCRegistration;

import java.util.Objects;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandSet {
    private static final Text yes = Text.literal("Yes").formatted(Formatting.GREEN);
    private static final Text no = Text.literal("No").formatted(Formatting.RED);

    private static boolean hasOPPermission(ServerCommandSource source) {
        return source.hasPermissionLevel(2);
    }

    public static void setupCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("npc")
                .executes(CommandSet::status)
                .then(literal("spawn")
                        .requires(CommandSet::hasOPPermission)
                        .executes(CommandSet::spawn)
                )
                .then(literal("saveAll")
                        .requires(CommandSet::hasOPPermission)
                        .executes(CommandSet::saveAll)
                )
                .then(literal("debug")
                        .requires(CommandSet::hasOPPermission)
                        .executes(CommandSet::debug)
                )
                .then(literal("master")
                        .requires(CommandSet::hasOPPermission)
                        .then(argument("message", StringArgumentType.greedyString())
                                .executes(CommandSet::master)
                        )
                )

        );
    }

    private static int master(CommandContext<ServerCommandSource> context) {
        String message = context.getArgument("message", String.class);
        ConversationWindow window = Master.getMaster().getConversationWindows();
        if (window.isOnWait()) {
            return 0;
        }
        AsyncTask.call(() -> {
            if (!window.isOnWait()) {
                window.onWait();
                window.chat(message);
                Objects.requireNonNull(context.getSource().getPlayer()).sendMessage(Text.of("<Master> " + window.getLastMessage()));
                window.offWait();
            }
            return AsyncTask.nothingToDo();
        });
        return 1;
    }

    private static int debug(CommandContext<ServerCommandSource> context) {
        PlayerEntity player = context.getSource().getPlayer();
        if (player != null) {
            player.sendMessage(Text.literal("NPC Entity Registry: " + NPC_AI.NPC_ENTITY_MANAGER.map.keySet()));
            player.sendMessage(Text.literal("NPC AI Registry: " + AgentManager.getInstance().map.keySet()));
            player.sendMessage(Text.literal("Conversation: " + ConversationManager.getInstance().map.keySet()));
        }
        return 1;
    }

    private static int saveAll(CommandContext<ServerCommandSource> context) {
        LiveCycleManager.asyncSaveAll();
        return 1;
    }

    private static int spawn(CommandContext<ServerCommandSource> context) {
        PlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;
        NPCRegistration.ENTITY_NPC.spawn((ServerWorld) player.getWorld(), player.getBlockPos(), SpawnReason.COMMAND);
        return 1;
    }

    private static int status(CommandContext<ServerCommandSource> context) {
        Text helpText = Text.literal("")
                .append(Text.literal("[npc-system] NPC System:").formatted(Formatting.UNDERLINE))
                .append("").formatted(Formatting.RESET)
                .append("\nEnabled: ").append(yes)
                .append(Text.literal("\nYou can spawn a new NPC by /npc spawn. ").formatted(Formatting.UNDERLINE));
        context.getSource().sendFeedback(helpText, false);
        return 1;
    }
}
