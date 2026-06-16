package team.jackdaw.npcsystem.function;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import team.jackdaw.npcsystem.ai.ConversationWindow;
import team.jackdaw.npcsystem.entity.NPCEntity;
import team.jackdaw.npcsystem.entity.task.WalkToBlockTask;

import java.util.Locale;
import java.util.Map;

public class WalkRelativeFunction extends NpcTaskFunction {
    public WalkRelativeFunction() {
        description = "Make this NPC walk relative to its current facing direction.";
        properties = Map.of(
                "direction", Map.of("description", "One of forward, backward, left, right.", "type", "string"),
                "blocks", Map.of("description", "Distance in blocks.", "type", "integer"),
                "timeout_seconds", Map.of("description", "Maximum seconds to try walking.", "type", "integer")
        );
        required = new String[]{"direction", "blocks"};
    }

    @Override
    public Map<String, Object> execute(ConversationWindow conversation, Map<String, Object> args) {
        return currentNpc(conversation)
                .map(npc -> walk(conversation, npc, args))
                .orElseGet(() -> failure("npc_not_found", "No NPC is associated with this conversation.", false));
    }

    private Map<String, Object> walk(ConversationWindow conversation, NPCEntity npc, Map<String, Object> args) {
        String direction = stringArg(args, "direction", "dir");
        int blocks = integer(args.get("blocks"), 0);
        if (direction == null || blocks <= 0) {
            return failure("invalid_arguments", "direction and positive blocks are required.", false);
        }
        Vec3 forward = npc.getLookAngle();
        forward = new Vec3(forward.x, 0.0, forward.z);
        if (forward.lengthSqr() < 0.0001) {
            forward = Vec3.directionFromRotation(0.0f, npc.getYRot());
            forward = new Vec3(forward.x, 0.0, forward.z);
        }
        forward = forward.normalize();
        Vec3 vector = switch (direction.toLowerCase(Locale.ROOT)) {
            case "forward" -> forward;
            case "backward" -> forward.scale(-1.0);
            case "left" -> new Vec3(forward.z, 0.0, -forward.x);
            case "right" -> new Vec3(-forward.z, 0.0, forward.x);
            default -> null;
        };
        if (vector == null) {
            return failure("invalid_direction", "direction must be forward, backward, left, or right.", false);
        }
        Vec3 target = npc.position().add(vector.scale(Math.min(64, blocks)));
        int timeoutTicks = secondsToTicks(args.get("timeout_seconds"), Math.max(5, blocks * 2));
        return assign(conversation, new WalkToBlockTask(BlockPos.containing(target), 0.6, 1.0, timeoutTicks));
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }
}
