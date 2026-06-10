package team.jackdaw.npcsystem.entity.task;

import team.jackdaw.npcsystem.entity.NPCEntity;

public class NpcTaskController {
    private NpcTask currentTask;
    private String lastResult = "idle";

    public boolean assign(NPCEntity npc, NpcTask task) {
        if (task == null || !task.canStart(npc)) {
            lastResult = "failed task rejected";
            return false;
        }
        cancel(npc);
        currentTask = task;
        currentTask.start(npc);
        lastResult = "started " + task.name();
        return true;
    }

    public void tick(NPCEntity npc) {
        if (currentTask == null) {
            return;
        }
        currentTask.tick(npc);
        if (currentTask.isFinished(npc)) {
            lastResult = "finished " + currentTask.name();
            currentTask.stop(npc);
            currentTask = null;
        }
    }

    public void cancel(NPCEntity npc) {
        if (currentTask != null) {
            currentTask.stop(npc);
            lastResult = "cancelled " + currentTask.name();
            currentTask = null;
        }
        npc.getNavigation().stop();
    }

    public boolean isBusy() {
        return currentTask != null;
    }

    public String status() {
        if (currentTask == null) {
            return lastResult;
        }
        return "running " + currentTask.name();
    }
}
