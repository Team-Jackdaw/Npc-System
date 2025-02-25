package team.jackdaw.npcsystem;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import team.jackdaw.npcsystem.entity.NPCRegistration;

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
        );
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
