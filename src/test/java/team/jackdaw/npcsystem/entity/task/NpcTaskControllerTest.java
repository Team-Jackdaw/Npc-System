package team.jackdaw.npcsystem.entity.task;

import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.entity.NPCEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcTaskControllerTest {
    @Test
    void agentTasksRunInFifoOrder() {
        NpcTaskController controller = new NpcTaskController();
        FakeTask first = new FakeTask("first", 1);
        FakeTask second = new FakeTask("second", 1);

        assertTrue(controller.assign(null, first, TaskSource.AGENT));
        assertTrue(controller.assign(null, second, TaskSource.AGENT));
        assertEquals(1, controller.queuedTaskCount());

        controller.tick(null);
        assertEquals(1, first.stopCount);
        assertEquals(TaskSource.AGENT, controller.currentSource());
        assertTrue(controller.status().contains("second"));
    }

    @Test
    void playerTaskInterruptsAgentAndAgentReturnsToQueueHead() {
        NpcTaskController controller = new NpcTaskController();
        FakeTask agent = new FakeTask("agent", 10);
        FakeTask player = new FakeTask("player", 1);

        controller.assign(null, agent, TaskSource.AGENT);
        controller.assign(null, player, TaskSource.PLAYER);

        assertEquals(1, agent.stopCount);
        assertEquals(TaskSource.PLAYER, controller.currentSource());
        assertEquals(1, controller.queuedTaskCount());

        controller.tick(null);
        assertEquals(TaskSource.AGENT, controller.currentSource());
        assertTrue(controller.status().contains("agent"));
    }

    @Test
    void agentInterruptsDefaultAndDefaultDoesNotResume() {
        NpcTaskController controller = new NpcTaskController();
        FakeTask defaultTask = new FakeTask("default", 10);
        FakeTask agent = new FakeTask("agent", 1);

        controller.assign(null, defaultTask, TaskSource.DEFAULT);
        controller.assign(null, agent, TaskSource.AGENT);

        assertEquals(1, defaultTask.stopCount);
        assertEquals(TaskSource.AGENT, controller.currentSource());
        assertEquals(0, controller.queuedTaskCount());
    }

    @Test
    void agentQueuedWhilePlayerTaskRuns() {
        NpcTaskController controller = new NpcTaskController();
        FakeTask player = new FakeTask("player", 2);
        FakeTask agent = new FakeTask("agent", 1);

        controller.assign(null, player, TaskSource.PLAYER);
        controller.assign(null, agent, TaskSource.AGENT);

        assertEquals(TaskSource.PLAYER, controller.currentSource());
        assertEquals(1, controller.queuedTaskCount());
        controller.tick(null);
        assertEquals(TaskSource.PLAYER, controller.currentSource());
        controller.tick(null);
        assertEquals(TaskSource.AGENT, controller.currentSource());
    }

    @Test
    void defaultTaskRejectedWhenQueueOrTaskExists() {
        NpcTaskController controller = new NpcTaskController();
        FakeTask player = new FakeTask("player", 2);
        FakeTask defaultTask = new FakeTask("default", 1);

        controller.assign(null, player, TaskSource.PLAYER);

        assertFalse(controller.assign(null, defaultTask, TaskSource.DEFAULT));
        assertEquals(TaskSource.PLAYER, controller.currentSource());
        assertEquals(0, controller.queuedTaskCount());
    }

    @Test
    void cancelClearsCurrentAndQueue() {
        NpcTaskController controller = new NpcTaskController();
        FakeTask first = new FakeTask("first", 10);
        FakeTask second = new FakeTask("second", 10);

        controller.assign(null, first, TaskSource.AGENT);
        controller.assign(null, second, TaskSource.AGENT);
        controller.cancel(null);

        assertNull(controller.currentSource());
        assertEquals(0, controller.queuedTaskCount());
        assertTrue(controller.canRunDefaultTask());
    }

    @Test
    void batchCallbackEmitsAfterLastTaskFinishes() {
        NpcTaskController controller = new NpcTaskController();
        FakeTask first = new FakeTask("first", 1);
        FakeTask second = new FakeTask("second", 1);

        controller.assign(null, NpcTaskAssignment.of(first, TaskSource.AGENT).withBatch("batch-1", true));
        controller.assign(null, NpcTaskAssignment.of(second, TaskSource.AGENT).withBatch("batch-1", true));

        controller.tick(null);
        assertNull(controller.pollCompletedBatch());

        controller.tick(null);
        NpcTaskBatchResult result = controller.pollCompletedBatch();
        assertEquals("batch-1", result.batchId());
        assertEquals("finished", result.status());
        assertEquals(2, result.tasks().size());
        assertTrue(result.summary().contains("batch-1"));
    }

    @Test
    void batchWithoutCallbackDoesNotEmitResult() {
        NpcTaskController controller = new NpcTaskController();
        FakeTask task = new FakeTask("silent", 1);

        controller.assign(null, NpcTaskAssignment.of(task, TaskSource.AGENT).withBatch("batch-2", false));
        controller.tick(null);

        assertNull(controller.pollCompletedBatch());
    }

    private static final class FakeTask implements NpcTask {
        private final String name;
        private final int finishAfterTicks;
        private int ticks;
        private int stopCount;

        private FakeTask(String name, int finishAfterTicks) {
            this.name = name;
            this.finishAfterTicks = finishAfterTicks;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean canStart(NPCEntity npc) {
            return true;
        }

        @Override
        public void tick(NPCEntity npc) {
            ticks++;
        }

        @Override
        public boolean isFinished(NPCEntity npc) {
            return ticks >= finishAfterTicks;
        }

        @Override
        public void stop(NPCEntity npc) {
            stopCount++;
        }
    }
}
