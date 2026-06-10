package team.jackdaw.npcsystem.function;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import team.jackdaw.npcsystem.NPCSystem;
import team.jackdaw.npcsystem.Config;
import team.jackdaw.npcsystem.NPC_AI;
import team.jackdaw.npcsystem.ai.Agent;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.ai.npc.NPC;
import team.jackdaw.npcsystem.api.json.Tool;
import team.jackdaw.npcsystem.entity.NPCEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class NoCallableFunction extends CustomFunction {
    private static final Logger logger = NPCSystem.LOGGER;
    private static final Path folder = NPCSystem.workingDirectory.resolve("functions");
    private final String name;
    private String call;

    private NoCallableFunction(String name, String description, Map<String, Map<String, Object>> properties) {
        this.name = name;
        this.description = description;
        this.properties = properties;
    }

    /**
     * Register a function from a JSON string. It will be called by the OpenAI Assistant and then call the function in Minecraft Level data package.
     * <p>
     * The JSON string should be in the following format:
     * <pre>
     *     {
     *         "type": "function",
     *         "function": {
     *             "name": "functionName",
     *             "description": "This function does something",
     *             "parameters": {
     *                 "type": "object",
     *                 "properties": {
     *                     "param1": {
     *                         "type": "integer",
     *                         "description": "This is the first parameter",
     *                         "enum": [1, 2, 3]
     *                     },
     *                     "param2": {
     *                         "type": "integer",
     *                         "description": "This is the second parameter"
     *                     }
     *                 },
     *                 "required": ["param1", "param2"]
     *             }
     *         }
     *         "call": "NameSpace:function"
     *     }
     * </pre>
     *
     * <b>Note: The parameters must be integer as it will be stored in scoreboard.</b>
     * <p>
     * The parameters are stored in the nearby players' scoreboard with format "npc_UUID_paramName". The UUID is the UUID of the NPC.
     * <p>
     * The "call" field is optional, it is the function stored in Minecraft Level data package with format "NameSpace:function". If it is not null, the function will be called as the Server in NPC's Level with its position and with permission level 2.
     * <p>
     * This function will not be executed if "call" is null. It will only add the integer parameters to the scoreboard.
     *
     * @param json The JSON string
     */
    private static void registerFromJson(String json) {
        Tool tool = Tool.fromJson(json);
        NoCallableFunction function = new NoCallableFunction(tool.function.name, tool.function.description, tool.function.parameters.properties);
        if (tool.call != null) function.call = tool.call;
        function.required = tool.function.parameters.required;
        FunctionManager.getInstance().register(tool.function.name, function);
    }

    /**
     * Get the list of the functions, and register them.
     */
    public static void sync() {
        if (!Files.exists(folder)) {
            try {
                Files.createDirectories(folder);
            } catch (IOException e) {
                logger.error("[npc-system] Failed to create the functions directory");
                logger.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }
        File workingDirectory = folder.toFile();
        File[] files = workingDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.endsWith(".json")) {
                    String json;
                    try {
                        json = Files.readString(file.toPath());
                    } catch (IOException e) {
                        logger.error("[npc-system] Failed to read the function file: " + name);
                        logger.error(e.getMessage());
                        continue;
                    }
                    registerFromJson(json);
                }
            }
        }
    }

    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        Agent agent = conversation.getAgent();
        if (!(agent instanceof NPC npc)) {
            return ToolResult.failure("npc_not_found", "No NPC is associated with this conversation.", false);
        }
        NPCEntity entity = NPC_AI.getNPCEntity(npc);
        if (entity == null) {
            return ToolResult.failure("npc_not_found", "No NPC entity is associated with this conversation.", false);
        }
        AABB rangeBox = entity.getBoundingBox().inflate(Config.range);
        Player player = entity.level()
                .getEntitiesOfClass(Player.class, rangeBox, Player::isAlive)
                .stream()
                .min((a, b) -> Double.compare(entity.distanceToSqr(a), entity.distanceToSqr(b)))
                .orElse(null);
        MinecraftServer server = entity.level().getServer();
        if (server == null) {
            return ToolResult.failure("server_unavailable", "Minecraft server is unavailable.", true);
        }
        // add the args to the closest players' scoreboard
        String playerName;
        if (player != null) {
            playerName = player.getName().getString();
            args.forEach((key, value) -> {
                String scoreName = "npc_" + name + "_" + key;
                Scoreboard scoreboard = server.getScoreboard();
                Objective objective = scoreboard.getObjective(scoreName);
                if (objective == null) {
                    scoreboard.addObjective(scoreName, ObjectiveCriteria.DUMMY, Component.literal(scoreName), ObjectiveCriteria.RenderType.INTEGER, false, null);
                    objective = scoreboard.getObjective(scoreName);
                }
                int intValue;
                if (value instanceof Integer) {
                    intValue = (int) value;
                } else {
                    try {
                        double doubleValue = Double.parseDouble(value.toString());
                        intValue = (int) doubleValue;
                    } catch (NumberFormatException e) {
                        NPCSystem.LOGGER.error("[npc-system] Failed to parse the value of " + key + " in function " + name);
                        return;
                    }
                }
                scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(playerName), objective).set(intValue);
            });
        } else {
            playerName = "";
        }
        // call the function in the Minecraft Level
        boolean success = true;
        if (call != null) {
            Vec3 pos = entity.position();
            CommandSourceStack source = server.createCommandSourceStack();
            CommandSourceStack newSource = source
                    .withPosition(pos)
                    .withLevel((ServerLevel) entity.level())
                    .withSuppressedOutput();
            try {
                server.getCommands().performPrefixedCommand(newSource, "function " + call);
            } catch (Exception e) {
                NPCSystem.LOGGER.error("[npc-system] Failed to execute no-callable function " + name, e);
                success = false;
            }
            if (success && !playerName.isEmpty()) {
                String scoreName = "npc_" + name + "_result";
                Scoreboard scoreboard = server.getScoreboard();
                Objective objective = scoreboard.getObjective(scoreName);
                if (objective != null) {
                    ScoreAccess score = scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(playerName), objective);
                    if (score.get() == 0) success = false;
                }
            }
        }
        if (success) {
            return ToolResult.success("datapack_function_executed", "Datapack function executed.", Map.of("function", name));
        }
        return ToolResult.failure("datapack_function_failed", "Datapack function failed.", true);
    }
}
