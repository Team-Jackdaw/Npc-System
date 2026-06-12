package team.jackdaw.npcsystem;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import team.jackdaw.npcsystem.ai.AgentManager;
import team.jackdaw.npcsystem.ai.ConversationManager;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.master.Master;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.NPCRegistration;
import team.jackdaw.npcsystem.group.Group;
import team.jackdaw.npcsystem.group.GroupManager;
import team.jackdaw.npcsystem.memory.Memory;

import java.util.Comparator;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CommandSet {
    private static final Component yes = Component.literal("Yes").withStyle(ChatFormatting.GREEN);
    private static final Component no = Component.literal("No").withStyle(ChatFormatting.RED);

    private static final SuggestionProvider<CommandSourceStack> groupSuggestionProvider = (context, builder) -> {
        for (String group : GroupManager.getInstance().getGroupList()) {
            builder.suggest(group);
        }
        return builder.buildFuture();
    };

    private static boolean hasOPPermission(CommandSourceStack source) {
        return source.permissions() instanceof LevelBasedPermissionSet permissions
                && permissions.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS);
    }

    public static void setupCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, CommandSelection environment) {
        dispatcher.register(literal("npc")
                .executes(CommandSet::status)
                .then(literal("debug")
                        .requires(CommandSet::hasOPPermission)
                        .then(literal("on").executes(context -> setDebug(context, true)))
                        .then(literal("off").executes(context -> setDebug(context, false)))
                        .executes(CommandSet::debug))
                .then(literal("help")
                        .requires(CommandSet::hasOPPermission)
                        .executes(CommandSet::help))
                .then(literal("spawn")
                        .requires(CommandSet::hasOPPermission)
                        .executes(CommandSet::spawn)
                )
                .then(literal("saveAll")
                        .requires(CommandSet::hasOPPermission)
                        .executes(CommandSet::saveAll)
                )
                .then(literal("master")
                        .requires(CommandSet::hasOPPermission)
                        .then(argument("message", StringArgumentType.greedyString())
                                .executes(CommandSet::master)
                        )
                )
                .then(literal("group")
                        .requires(CommandSet::hasOPPermission)
                        .then(argument("group", StringArgumentType.word())
                                .suggests(groupSuggestionProvider)
                                .then(literal("setParent")
                                        .then(argument("parent", StringArgumentType.word())
                                                .suggests(groupSuggestionProvider)
                                                .executes(CommandSet::setGroupParent)))
                                .then(literal("setInstruction")
                                        .then(argument("instruction", StringArgumentType.greedyString())
                                                .executes(CommandSet::setGroupInstruction)))
                                .then(literal("addEvent")
                                        .then(argument("event", StringArgumentType.greedyString())
                                                .executes(CommandSet::addGroupEvent)))
                                .then(literal("popEvent")
                                        .executes(CommandSet::popGroupEvent))
                                .executes(CommandSet::groupStatus))
                        .executes(CommandSet::allGroupStatus))
                .then(literal("addGroup")
                        .requires(CommandSet::hasOPPermission)
                        .then(argument("newGroup", StringArgumentType.word())
                                .executes(CommandSet::addGroup)))
        );
    }

    private static void sendFeedback(CommandContext<CommandSourceStack> context, Component message, boolean broadcastToOps) {
        context.getSource().sendSuccess(() -> message, broadcastToOps);
    }

    private static int debug(CommandContext<CommandSourceStack> context) {
        Optional<NPCEntity> nearestNpc = nearestNpc(context.getSource());
        if (nearestNpc.isEmpty()) {
            context.getSource().sendSystemMessage(Component.literal("[npc-system] No NPC found in this level."));
            return 0;
        }
        NPCEntity npc = nearestNpc.get();
        Vec3 sourcePos = context.getSource().getPosition();
        Component debugText = Component.literal("")
                .append(Component.literal("[npc-system] Nearest NPC Debug").withStyle(ChatFormatting.UNDERLINE))
                .append("").withStyle(ChatFormatting.RESET)
                .append("\nName: ").append(Component.literal(npc.getName().getString()).withStyle(ChatFormatting.GOLD))
                .append("\nUUID: ").append(Component.literal(npc.getUUID().toString()))
                .append("\nDistance: ").append(Component.literal(String.format(java.util.Locale.ROOT, "%.1f", Math.sqrt(npc.distanceToSqr(sourcePos)))))
                .append("\nPosition: ").append(Component.literal(npc.blockPosition().toShortString()))
                .append("\nTask: ").append(Component.literal(npc.getTaskController().status()).withStyle(ChatFormatting.AQUA))
                .append("\nDebug Logging: ").append(Config.debug ? yes : no)
                .append("\nDebug Log File: ").append(Component.literal(NPCSystem.debugLogFile.toAbsolutePath().toString()).withStyle(ChatFormatting.GRAY))
                .append("\nAgent Registered: ").append(AgentManager.getInstance().isRegistered(npc.getUUID()) ? yes : no)
                .append("\nConversation Registered: ")
                .append(ConversationManager.getInstance().isRegistered(npc.getUUID()) ? yes : no)
                .append("\nNearby Players: ").append(Component.literal(npc.getSensorState().snapshot().nearbyPlayers().toString()).withStyle(ChatFormatting.GRAY))
                .append("\nNearby NPCs: ").append(Component.literal(npc.getSensorState().snapshot().nearbyNpcs().toString()).withStyle(ChatFormatting.GRAY))
                .append("\nHeard Chat: ").append(Component.literal(npc.getSensorState().snapshot().heardChats().toString()).withStyle(ChatFormatting.GRAY))
                .append("\nObservation: ").append(Component.literal(npc.getLastObservation()).withStyle(ChatFormatting.GRAY))
                .append("\nRegistries: NPC=")
                .append(Component.literal(String.valueOf(NPC_AI.NPC_ENTITY_MANAGER.map.size())))
                .append(", Agent=")
                .append(Component.literal(String.valueOf(AgentManager.getInstance().map.size())))
                .append(", Conversation=")
                .append(Component.literal(ConversationManager.getInstance().map.keySet().toString()))
                .append("\nUse ").append(Component.literal("/npc debug on|off").withStyle(ChatFormatting.GRAY)).append(" to toggle detailed logs.");
        context.getSource().sendSystemMessage(debugText);
        return 1;
    }

    private static int setDebug(CommandContext<CommandSourceStack> context, boolean enabled) {
        Config.debug = enabled;
        ConfigManager.save();
        Component message = Component.literal("[npc-system] Debug logging " + (enabled ? "enabled" : "disabled") + ".")
                .append(Component.literal(" Log file: " + NPCSystem.debugLogFile.toAbsolutePath()).withStyle(ChatFormatting.GRAY));
        sendFeedback(context, message, true);
        return 1;
    }

    private static Optional<NPCEntity> nearestNpc(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 position = source.getPosition();
        return NPC_AI.NPC_ENTITY_MANAGER.map.values().stream()
                .filter(npc -> !npc.isRemoved() && npc.level().equals(level))
                .min(Comparator.comparingDouble(npc -> npc.distanceToSqr(position)));
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        sendFeedback(context, Component.literal("[npc-system] Coming soooooon!"), false);
        return 1;
    }

    private static int addGroup(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("newGroup", String.class);
        GroupManager.getInstance().register(group, true);
        sendFeedback(context, Component.literal("[npc-system] Group added"), true);
        return 1;
    }

    private static int allGroupStatus(CommandContext<CommandSourceStack> context) {
        Component statusText = Component.literal("")
                .append(Component.literal("[npc-system] Group List:").withStyle(ChatFormatting.UNDERLINE))
                .append("").withStyle(ChatFormatting.RESET)
                .append("\n").append(Component.literal(String.join(", ", GroupManager.getInstance().getGroupList())).withStyle(ChatFormatting.GOLD))
                .append("\n").append(Component.literal(GroupManager.getInstance().getGroupTree("Global")).withStyle(ChatFormatting.BLUE))
                .append("\nUse ").append(Component.literal("/npc help").withStyle(ChatFormatting.GRAY)).append(" for help");
        sendFeedback(context, statusText, false);
        return 1;
    }

    private static int groupStatus(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);
        Group g = GroupManager.getInstance().get(group);
        if (g == null) {
            sendFeedback(context, Component.literal("[npc-system] Group not found."), false);
            return 0;
        }
        Component statusText = Component.literal("")
                .append(Component.literal("[npc-system] Group Status:").withStyle(ChatFormatting.UNDERLINE))
                .append("").withStyle(ChatFormatting.RESET)
                .append("\nName: ").append(Component.literal(g.getName()).withStyle(ChatFormatting.GOLD))
                .append("\nParent Groups: ").append(Component.literal(
                        String.join("->", GroupManager.getInstance().getParentGroups(g.getName()).stream().map(Group::getName).toList())
                ).withStyle(ChatFormatting.GOLD))
                .append("\nInstruction: ").append(Component.literal(g.getInstruction()).withStyle(ChatFormatting.AQUA))
                .append("\nTemp Events: ").append(Component.literal(
                        String.join(", ", g.getEvent())
                ).withStyle(ChatFormatting.BLUE))
                .append("\n Member: ").append(Component.literal(String.join(", ", g.getMemberList())).withStyle(ChatFormatting.DARK_PURPLE))
                .append("\nLast Load Time: ").append(Component.literal(String.valueOf(g.getLastLoadTimeString())).withStyle(ChatFormatting.GRAY))
                .append("\nUse ").append(Component.literal("/npc help").withStyle(ChatFormatting.GRAY)).append(" for help");
        sendFeedback(context, statusText, false);
        return 1;
    }

    private static int popGroupEvent(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);
        Group g = GroupManager.getInstance().get(group);
        if (g == null) {
            sendFeedback(context, Component.literal("[npc-system] Group not found."), false);
            return 0;
        }
        g.popEvent();
        sendFeedback(context, Component.literal("[npc-system] Event popped"), true);
        return 1;
    }

    private static int addGroupEvent(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);
        Group g = GroupManager.getInstance().get(group);
        if (g == null) {
            sendFeedback(context, Component.literal("[npc-system] Group not found."), false);
            return 0;
        }
        String event = context.getArgument("event", String.class);
        g.addEvent(event);
        sendFeedback(context, Component.literal("[npc-system] Event added"), true);
        return 1;
    }

    private static int setGroupInstruction(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);
        Group g = GroupManager.getInstance().get(group);
        if (g == null) {
            sendFeedback(context, Component.literal("[npc-system] Group not found."), false);
            return 0;
        }
        String instruction = context.getArgument("instruction", String.class);
        g.setInstruction(instruction);
        sendFeedback(context, Component.literal("[npc-system] Group Instruction set."), true);
        return 1;
    }

    private static int setGroupParent(CommandContext<CommandSourceStack> context) {
        String group = context.getArgument("group", String.class);
        String parent = context.getArgument("parent", String.class);
        GroupManager.getInstance().setGroupParent(group, parent);
        sendFeedback(context, Component.literal("[npc-system] Group parent set"), true);
        return 1;
    }

    private static int master(CommandContext<CommandSourceStack> context) {
        String message = context.getArgument("message", String.class);
        context.getSource().sendSystemMessage(Component.literal("")
                .append(Component.literal("<->Master> ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(message)).withStyle(ChatFormatting.RESET)
        );
        ConversationWindow window = Master.getMaster().getConversationWindows();
        if (window.isOnWait()) {
            return 0;
        }
        AsyncTask.call(() -> {
            if (!window.isOnWait()) {
                window.onWait();
                Entity target = context.getSource().getEntity();
                if (target != null) window.setTarget(target.getUUID());
                window.chat(message);
                NPC_AI.broadcastMessage(window);
                window.offWait();
            }
            return AsyncTask.nothingToDo();
        });
        return 1;
    }

    private static int saveAll(CommandContext<CommandSourceStack> context) {
        LiveCycleManager.asyncSaveAll();
        return 1;
    }

    private static int spawn(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;
        NPCRegistration.ENTITY_NPC.spawn((ServerLevel) player.level(), player.blockPosition(), EntitySpawnReason.COMMAND);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        Component helpText = Component.literal("")
                .append(Component.literal("[npc-system] NPC System:").withStyle(ChatFormatting.UNDERLINE))
                .append("").withStyle(ChatFormatting.RESET)
                .append("\nEnabled: ").append(Config.enabled ? yes : no)
                .append("\nDebug Logging: ").append(Config.debug ? yes : no)
                .append("\nMemory Storage: ").append(Component.literal(Memory.storagePath()))
                .append("\nAPI URL: ").append(Component.literal(Config.apiURL))
                .append("\nChat Model: ").append(Component.literal(Config.chat_model))
                .append("\nChat Range: ").append(Component.literal(String.valueOf(Config.range)))
                .append("\nText Bubble: ").append(Config.isBubble ? yes : no)
                .append("\nChat Bar: ").append(Config.isChatBar ? yes : no)
                .append("\nBubble Color: ").append(Component.literal(Config.bubbleColor.toString()))
                .append("\nTime Lasting Per Char: ").append(Component.literal(String.valueOf(Config.timeLastingPerChar)))
                .append("\nYou can spawn a new NPC by ").append(Component.literal("/npc spawn").withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.AQUA)).append(". ").withStyle(ChatFormatting.RESET);
        sendFeedback(context, helpText, false);
        return 1;
    }
}
