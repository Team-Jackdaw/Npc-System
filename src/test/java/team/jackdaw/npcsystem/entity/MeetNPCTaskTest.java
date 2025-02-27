package team.jackdaw.npcsystem.entity;

import org.junit.jupiter.api.Test;

public class MeetNPCTaskTest {

    @Test
    public void testShouldRun() {
        double pTick = 0.0001;
        int totalTicks = 6000;
        int trials = 100000;
        int successCount = 0;

        for (int i = 0; i < trials; i++) {
            boolean triggered = false;
            for (int j = 0; j < totalTicks; j++) {
                if (Math.random() < pTick) {
                    triggered = true;
                    break;
                }
            }
            if (triggered) {
                successCount++;
            }
        }

        double empiricalProbability = (double) successCount / trials;
        System.out.println("实验得到的概率：" + empiricalProbability);
    }
}
