package team.jackdaw.npcsystem.entity.task;

import java.util.List;

public record NpcTaskBatchResult(
        String batchId,
        String status,
        List<String> tasks,
        String summary,
        long gameTime
) {
}
