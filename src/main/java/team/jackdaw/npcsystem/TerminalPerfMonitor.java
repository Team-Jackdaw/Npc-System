package team.jackdaw.npcsystem;

import java.util.Locale;

public final class TerminalPerfMonitor {
    private static boolean running;
    private static int targetTicks;
    private static int ticks;
    private static int npcCount;
    private static long startedAtNanos;
    private static long lastTickNanos;
    private static long totalTickIntervalNanos;
    private static long maxTickIntervalNanos;
    private static Result lastResult = Result.empty();

    private TerminalPerfMonitor() {
    }

    public static void start(int npcCount, int seconds) {
        TerminalPerfMonitor.npcCount = Math.max(0, npcCount);
        targetTicks = Math.max(1, seconds) * 20;
        ticks = 0;
        startedAtNanos = System.nanoTime();
        lastTickNanos = 0L;
        totalTickIntervalNanos = 0L;
        maxTickIntervalNanos = 0L;
        running = true;
        lastResult = Result.running(TerminalPerfMonitor.npcCount, seconds);
        NPCSystem.LOGGER.info("[npc-system] Terminal perf sampling started: npcCount={}, seconds={}", TerminalPerfMonitor.npcCount, seconds);
    }

    public static void tick() {
        if (!running) {
            return;
        }
        long now = System.nanoTime();
        if (lastTickNanos != 0L) {
            long interval = now - lastTickNanos;
            totalTickIntervalNanos += interval;
            maxTickIntervalNanos = Math.max(maxTickIntervalNanos, interval);
        }
        lastTickNanos = now;
        ticks++;
        if (ticks >= targetTicks) {
            finish(now);
        }
    }

    public static Result status() {
        return lastResult;
    }

    public static boolean isRunning() {
        return running;
    }

    private static void finish(long finishedAtNanos) {
        running = false;
        double elapsedSeconds = (finishedAtNanos - startedAtNanos) / 1_000_000_000.0;
        int intervals = Math.max(1, ticks - 1);
        double averageTickMillis = totalTickIntervalNanos / 1_000_000.0 / intervals;
        double maxTickMillis = maxTickIntervalNanos / 1_000_000.0;
        double averageTps = averageTickMillis <= 0.0 ? 20.0 : Math.min(20.0, 1000.0 / averageTickMillis);
        lastResult = new Result(false, npcCount, ticks, elapsedSeconds, averageTickMillis, maxTickMillis, averageTps);
        NPCSystem.LOGGER.info("[npc-system] Terminal perf sampling finished: {}", lastResult.summary());
    }

    public record Result(boolean running, int npcCount, int ticks, double elapsedSeconds, double averageTickMillis, double maxTickMillis, double averageTps) {
        private static Result empty() {
            return new Result(false, 0, 0, 0.0, 0.0, 0.0, 0.0);
        }

        private static Result running(int npcCount, int seconds) {
            return new Result(true, npcCount, seconds * 20, 0.0, 0.0, 0.0, 0.0);
        }

        public String summary() {
            if (running) {
                return "running npcCount=" + npcCount + " targetTicks=" + ticks;
            }
            if (ticks == 0) {
                return "no perf sample has completed";
            }
            return String.format(Locale.ROOT,
                    "npcCount=%d ticks=%d elapsed=%.2fs avgTick=%.2fms maxTick=%.2fms avgTPS=%.2f",
                    npcCount,
                    ticks,
                    elapsedSeconds,
                    averageTickMillis,
                    maxTickMillis,
                    averageTps);
        }
    }
}
