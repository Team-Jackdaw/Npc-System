package team.jackdaw.npcsystem.entity.task;

public record NpcTaskAssignment(
        NpcTask task,
        TaskSource source,
        int priority,
        boolean interruptible,
        boolean resumeAfterInterrupt,
        String batchId,
        boolean callbackOnBatchComplete
) {
    public static NpcTaskAssignment of(NpcTask task, TaskSource source) {
        return new NpcTaskAssignment(
                task,
                source,
                source.priority(),
                true,
                source == TaskSource.AGENT || source == TaskSource.SYSTEM,
                null,
                source == TaskSource.AGENT
        );
    }

    public NpcTaskAssignment withBatch(String batchId, boolean callbackOnBatchComplete) {
        return new NpcTaskAssignment(
                task,
                source,
                priority,
                interruptible,
                resumeAfterInterrupt,
                batchId,
                callbackOnBatchComplete
        );
    }

    public boolean isDefault() {
        return source == TaskSource.DEFAULT;
    }

    public boolean canInterrupt(NpcTaskAssignment current) {
        return current == null || (priority > current.priority && current.interruptible);
    }

    public String name() {
        return task == null ? "none" : task.name();
    }
}
