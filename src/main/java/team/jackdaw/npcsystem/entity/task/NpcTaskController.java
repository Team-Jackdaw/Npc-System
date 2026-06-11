package team.jackdaw.npcsystem.entity.task;

import team.jackdaw.npcsystem.entity.NPCEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NpcTaskController {
    private NpcTaskAssignment currentTask;
    private final Deque<NpcTaskAssignment> queue = new ArrayDeque<>();
    private final Deque<NpcTaskBatchResult> completedBatches = new ArrayDeque<>();
    private final Map<String, BatchTracker> batches = new HashMap<>();
    private String lastResult = "idle";

    public boolean assign(NPCEntity npc, NpcTask task) {
        return assign(npc, NpcTaskAssignment.of(task, TaskSource.AGENT));
    }

    public boolean assign(NPCEntity npc, NpcTask task, TaskSource source) {
        return assign(npc, NpcTaskAssignment.of(task, source));
    }

    public boolean assign(NPCEntity npc, NpcTaskAssignment assignment) {
        if (!canAccept(npc, assignment)) {
            lastResult = "failed task rejected";
            return false;
        }
        trackBatch(assignment);
        if (currentTask == null) {
            start(npc, assignment);
            return true;
        }
        if (assignment.canInterrupt(currentTask)) {
            interruptCurrent(npc, assignment.source());
            start(npc, assignment);
            return true;
        }
        if (assignment.source() == TaskSource.DEFAULT) {
            lastResult = "rejected default " + assignment.name();
            return false;
        }
        queue.addLast(assignment);
        lastResult = "queued " + assignment.source().name().toLowerCase() + " " + assignment.name();
        return true;
    }

    public void tick(NPCEntity npc) {
        if (currentTask == null) {
            startNext(npc);
            return;
        }
        currentTask.task().tick(npc);
        if (currentTask.task().isFinished(npc)) {
            lastResult = "finished " + currentTask.source().name().toLowerCase() + " " + currentTask.name();
            NpcTaskAssignment finished = currentTask;
            finished.task().stop(npc);
            currentTask = null;
            finishBatchTask(npc, finished);
            startNext(npc);
        }
    }

    public void cancel(NPCEntity npc) {
        if (currentTask != null) {
            currentTask.task().stop(npc);
            lastResult = "cancelled " + currentTask.source().name().toLowerCase() + " " + currentTask.name();
            currentTask = null;
        }
        queue.clear();
        batches.clear();
        completedBatches.clear();
        if (npc != null) {
            npc.getNavigation().stop();
        }
    }

    public boolean isBusy() {
        return currentTask != null;
    }

    public boolean hasQueuedTasks() {
        return !queue.isEmpty();
    }

    public boolean canRunDefaultTask() {
        return currentTask == null && queue.isEmpty();
    }

    public boolean isRunningDefaultTask() {
        return currentTask != null && currentTask.source() == TaskSource.DEFAULT;
    }

    public int queuedTaskCount() {
        return queue.size();
    }

    public TaskSource currentSource() {
        return currentTask == null ? null : currentTask.source();
    }

    public NpcTaskBatchResult pollCompletedBatch() {
        return completedBatches.pollFirst();
    }

    public String status() {
        if (currentTask == null) {
            return lastResult + " queue=" + queue.size();
        }
        return "running " + currentTask.source().name().toLowerCase() + " " + currentTask.name() + " queue=" + queue.size();
    }

    private boolean canAccept(NPCEntity npc, NpcTaskAssignment assignment) {
        return assignment != null
                && assignment.task() != null
                && (npc == null || assignment.task().canStart(npc));
    }

    private void start(NPCEntity npc, NpcTaskAssignment assignment) {
        currentTask = assignment;
        currentTask.task().start(npc);
        lastResult = "started " + assignment.source().name().toLowerCase() + " " + assignment.name();
    }

    private void interruptCurrent(NPCEntity npc, TaskSource interrupter) {
        NpcTaskAssignment interrupted = currentTask;
        interrupted.task().stop(npc);
        if (interrupter == TaskSource.PLAYER && interrupted.resumeAfterInterrupt() && !interrupted.isDefault()) {
            queue.addFirst(interrupted);
        }
        currentTask = null;
    }

    private void startNext(NPCEntity npc) {
        while (!queue.isEmpty()) {
            NpcTaskAssignment next = queue.removeFirst();
            if (canAccept(npc, next)) {
                start(npc, next);
                return;
            }
            lastResult = "failed queued " + next.name();
        }
    }

    private void trackBatch(NpcTaskAssignment assignment) {
        if (assignment.batchId() == null || assignment.batchId().isBlank()) {
            return;
        }
        batches.computeIfAbsent(assignment.batchId(), BatchTracker::new)
                .add(assignment.name(), assignment.callbackOnBatchComplete());
    }

    private void finishBatchTask(NPCEntity npc, NpcTaskAssignment assignment) {
        if (assignment.batchId() == null || assignment.batchId().isBlank()) {
            return;
        }
        BatchTracker tracker = batches.get(assignment.batchId());
        if (tracker == null) {
            return;
        }
        tracker.finish(assignment.name());
        if (hasBatchTask(assignment.batchId())) {
            return;
        }
        batches.remove(assignment.batchId());
        if (!tracker.callbackOnComplete) {
            return;
        }
        long gameTime = npc == null ? 0L : npc.level().getGameTime();
        completedBatches.addLast(tracker.toResult(gameTime));
    }

    private boolean hasBatchTask(String batchId) {
        if (currentTask != null && batchId.equals(currentTask.batchId())) {
            return true;
        }
        return queue.stream().anyMatch(task -> batchId.equals(task.batchId()));
    }

    private static final class BatchTracker {
        private final String batchId;
        private final List<String> tasks = new ArrayList<>();
        private boolean callbackOnComplete;

        private BatchTracker(String batchId) {
            this.batchId = batchId;
        }

        private void add(String taskName, boolean callbackOnBatchComplete) {
            tasks.add(taskName);
            callbackOnComplete = callbackOnComplete || callbackOnBatchComplete;
        }

        private void finish(String taskName) {
            int index = tasks.indexOf(taskName);
            if (index >= 0) {
                tasks.set(index, taskName + ":finished");
            }
        }

        private NpcTaskBatchResult toResult(long gameTime) {
            String summary = "Agent task batch " + batchId + " finished: " + String.join(", ", tasks);
            return new NpcTaskBatchResult(batchId, "finished", List.copyOf(tasks), summary, gameTime);
        }
    }
}
