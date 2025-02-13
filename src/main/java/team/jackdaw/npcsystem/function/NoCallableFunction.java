package team.jackdaw.npcsystem.function;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import team.jackdaw.npcsystem.NPCSystem;
import team.jackdaw.npcsystem.SettingManager;
import team.jackdaw.npcsystem.conversation.ConversationHandler;
import team.jackdaw.npcsystem.npc.NPCEntity;

import java.util.Map;

class NoCallableFunction extends CustomFunction {
    String name;
    String call;

    NoCallableFunction(String name, String description, Map<String, Map<String, Object>> properties) {
        this.name = name;
        this.description = description;
        this.properties = properties;
    }

    public Map<String, String> execute(ConversationHandler conversation, Map<String, Object> args) {
        Map<String, String> failed = Map.of("status", "failed");
        Map<String, String> ok = Map.of("status", "success");
        NPCEntity npc = conversation.getNpc();
        Entity entity = npc.getEntity();
        PlayerEntity player = entity.world.getClosestPlayer(entity, SettingManager.range);
        MinecraftServer server = entity.getServer();
        if (server == null) return failed;
        // add the args to the closest players' scoreboard
        String playerName;
        if (player != null) {
            playerName = player.getName().getString();
            args.forEach((key, value) -> {
                String scoreName = "npc_" + name + "_" + key;
                Scoreboard scoreboard = server.getScoreboard();
                ScoreboardObjective objective = scoreboard.getObjective(scoreName);
                if (objective == null) {
                    scoreboard.addObjective(scoreName, ScoreboardCriterion.DUMMY, Text.of(scoreName), ScoreboardCriterion.RenderType.INTEGER);
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
                scoreboard.getPlayerScore(playerName, objective).setScore(intValue);
            });
        } else {
            playerName = "";
        }
        // call the function in the Minecraft World
        int var = 1;
        if (call != null) {
            Vec3d pos = entity.getPos();
            ServerCommandSource source = server.getCommandSource();
            ServerCommandSource newSource = source
                    .withPosition(pos)
                    .withWorld((ServerWorld) entity.world)
                    .withSilent()
                    .withLevel(2);
            var = server.getCommandManager().executeWithPrefix(newSource, "function " + call);
            if (var != 0 && !playerName.isEmpty()) {
                String scoreName = "npc_" + name + "_result";
                Scoreboard scoreboard = server.getScoreboard();
                ScoreboardObjective objective = scoreboard.getObjective(scoreName);
                if (objective != null && scoreboard.playerHasObjective(playerName, objective)) {
                    ScoreboardPlayerScore score = scoreboard.getPlayerScore(playerName, objective);
                    if (score.getScore() == 0) var = 0;
                }
            }
        }
        return var != 0 ? ok : failed;
    }
}
