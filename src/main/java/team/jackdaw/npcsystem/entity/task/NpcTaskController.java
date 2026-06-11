package team.jackdaw.npcsystem.entity.task;

import team.jackdaw.npcsystem.entity.NPCEntity;

import java.util.ArrayDeque;
import java.util.Deque;

public class NpcTaskController {
    private NpcTaskAssignment currentTask;
    private final Deque<NpcTaskAssignment> queue = new ArrayDeque<>();
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
            currentTask.task().stop(npc);
            currentTask = null;
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
}
