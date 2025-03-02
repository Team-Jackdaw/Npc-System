package team.jackdaw.npcsystem;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.master.Master;
import team.jackdaw.npcsystem.entity.NPCRegistration;
import team.jackdaw.npcsystem.group.Group;
import team.jackdaw.npcsystem.group.GroupManager;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandSet {
    private static final Text yes = Text.literal("Yes").formatted(Formatting.GREEN);
    private static final Text no = Text.literal("No").formatted(Formatting.RED);

    private static final SuggestionProvider<ServerCommandSource> groupSuggestionProvider = (context, builder) -> {
        for (String group : GroupManager.getInstance().getGroupList()) {
            builder.suggest(group);
        }
        return builder.buildFuture();
    };

    private static boolean hasOPPermission(ServerCommandSource source) {
        return source.hasPermissionLevel(2);
    }

    public static void setupCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("npc")
                .executes(CommandSet::status)
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

    private static int help(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(Text.of("[npc-system] Coming soooooon!"), false);
        return 1;
    }

    private static int addGroup(CommandContext<ServerCommandSource> context) {
        String group = context.getArgument("newGroup", String.class);
        GroupManager.getInstance().register(group, true);
        context.getSource().sendFeedback(Text.of("[npc-system] Group added"), true);
        return 1;
    }

    private static int allGroupStatus(CommandContext<ServerCommandSource> context) {
        Text statusText = Text.literal("")
                .append(Text.literal("[npc-system] Group List:").formatted(Formatting.UNDERLINE))
                .append("").formatted(Formatting.RESET)
                .append("\n").append(Text.literal(String.join(", ", GroupManager.getInstance().getGroupList())).formatted(Formatting.GOLD))
                .append("\n").append(Text.literal(GroupManager.getInstance().getGroupTree("Global")).formatted(Formatting.BLUE))
                .append("\nUse ").append(Text.literal("/npc help").formatted(Formatting.GRAY)).append(" for help");
        context.getSource().sendFeedback(statusText, false);
        return 1;
    }

    private static int groupStatus(CommandContext<ServerCommandSource> context) {
        String group = context.getArgument("group", String.class);
        Group g = GroupManager.getInstance().get(group);
        if (g == null) {
            context.getSource().sendFeedback(Text.of("[npc-system] Group not found."), false);
            return 0;
        }
        Text statusText = Text.literal("")
                .append(Text.literal("[npc-system] Group Status:").formatted(Formatting.UNDERLINE))
                .append("").formatted(Formatting.RESET)
                .append("\nName: ").append(Text.literal(g.getName()).formatted(Formatting.GOLD))
                .append("\nParent Groups: ").append(Text.literal(
                        String.join("->", GroupManager.getInstance().getParentGroups(g.getName()).stream().map(Group::getName).toList())
                ).formatted(Formatting.GOLD))
                .append("\nInstruction: ").append(Text.literal(g.getInstruction()).formatted(Formatting.AQUA))
                .append("\nTemp Events: ").append(Text.literal(
                        String.join(", ", g.getEvent())
                ).formatted(Formatting.BLUE))
                .append("\n Member: ").append(Text.literal(String.join(", ", g.getMemberList())).formatted(Formatting.DARK_PURPLE))
                .append("\nLast Load Time: ").append(Text.literal(String.valueOf(g.getLastLoadTimeString())).formatted(Formatting.GRAY))
                .append("\nUse ").append(Text.literal("/npc help").formatted(Formatting.GRAY)).append(" for help");
        context.getSource().sendFeedback(statusText, false);
        return 1;
    }

    private static int popGroupEvent(CommandContext<ServerCommandSource> context) {
        String group = context.getArgument("group", String.class);
        Group g = GroupManager.getInstance().get(group);
        if (g == null) {
            context.getSource().sendFeedback(Text.of("[npc-system] Group not found."), false);
            return 0;
        }
        g.popEvent();
        context.getSource().sendFeedback(Text.of("[npc-system] Event popped"), true);
        return 1;
    }

    private static int addGroupEvent(CommandContext<ServerCommandSource> context) {
        String group = context.getArgument("group", String.class);
        Group g = GroupManager.getInstance().get(group);
        if (g == null) {
            context.getSource().sendFeedback(Text.of("[npc-system] Group not found."), false);
            return 0;
        }
        String event = context.getArgument("event", String.class);
        g.addEvent(event);
        context.getSource().sendFeedback(Text.of("[npc-system] Event added"), true);
        return 1;
    }

    private static int setGroupInstruction(CommandContext<ServerCommandSource> context) {
        String group = context.getArgument("group", String.class);
        Group g = GroupManager.getInstance().get(group);
        if (g == null) {
            context.getSource().sendFeedback(Text.of("[npc-system] Group not found."), false);
            return 0;
        }
        String instruction = context.getArgument("instruction", String.class);
        g.setInstruction(instruction);
        context.getSource().sendFeedback(Text.of("[npc-system] Group Instruction set."), true);
        return 1;
    }

    private static int setGroupParent(CommandContext<ServerCommandSource> context) {
        String group = context.getArgument("group", String.class);
        String parent = context.getArgument("parent", String.class);
        GroupManager.getInstance().setGroupParent(group, parent);
        context.getSource().sendFeedback(Text.of("[npc-system] Group parent set"), true);
        return 1;
    }

    private static int master(CommandContext<ServerCommandSource> context) {
        String message = context.getArgument("message", String.class);
        context.getSource().sendMessage(Text.literal("")
                .append(Text.literal("<->Master> ").formatted(Formatting.GREEN))
                .append(Text.of(message)).formatted(Formatting.RESET)
        );
        ConversationWindow window = Master.getMaster().getConversationWindows();
        if (window.isOnWait()) {
            return 0;
        }
        AsyncTask.call(() -> {
            if (!window.isOnWait()) {
                window.onWait();
                Entity target = context.getSource().getEntity();
                if (target != null) window.setTarget(target.getUuid());
                window.chat(message);
                NPC_AI.broadcastMessage(window);
                window.offWait();
            }
            return AsyncTask.nothingToDo();
        });
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
                .append("\nEnabled: ").append(Config.enabled ? yes : no)
                .append("\nDatabase URL: ").append(Text.of(Config.dbURL))
                .append("\nAPI URL: ").append(Text.of(Config.apiURL))
                .append("\nChat Model: ").append(Text.of(Config.chat_model))
                .append("\nEmbedding Model: ").append(Text.of(Config.embedding_model))
                .append("\nChat Range: ").append(Text.of(String.valueOf(Config.range)))
                .append("\nText Bubble: ").append(Config.isBubble ? yes : no)
                .append("\nChat Bar: ").append(Config.isChatBar ? yes : no)
                .append("\nBubble Color: ").append(Text.of(Config.bubbleColor.toString()))
                .append("\nTime Lasting Per Char: ").append(Text.of(String.valueOf(Config.timeLastingPerChar)))
                .append("\nYou can spawn a new NPC by ").append(Text.literal("/npc spawn").formatted(Formatting.UNDERLINE).formatted(Formatting.AQUA)).append(". ").formatted(Formatting.RESET);
        context.getSource().sendFeedback(helpText, false);
        return 1;
    }
}
