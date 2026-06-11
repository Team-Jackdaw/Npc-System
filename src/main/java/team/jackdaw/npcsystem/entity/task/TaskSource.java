package team.jackdaw.npcsystem.entity.task;

public enum TaskSource {
    PLAYER(100),
    AGENT(80),
    SYSTEM(50),
    DEFAULT(10);

    private final int priority;

    TaskSource(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
