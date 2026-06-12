package team.jackdaw.npcsystem.function;

public class FunctionRegistration {
    static {
        FunctionManager.getInstance().register("end_conversation", new EndConversationFunction());
        FunctionManager.getInstance().register("call_command", new CallCommandFunction());
        FunctionManager.getInstance().register("say", new SayFunction());
        FunctionManager.getInstance().register("look_at_player", new LookAtPlayerFunction());
        FunctionManager.getInstance().register("look_at_npc", new LookAtNpcFunction());
        FunctionManager.getInstance().register("walk_to_player", new WalkToPlayerFunction());
        FunctionManager.getInstance().register("walk_to_npc", new WalkToNpcFunction());
        FunctionManager.getInstance().register("follow_player", new FollowPlayerFunction());
        FunctionManager.getInstance().register("wait", new WaitFunction());
        FunctionManager.getInstance().register("stop_task", new StopTaskFunction());
        FunctionManager.getInstance().register("resume_default_behavior", new ResumeDefaultBehaviorFunction());
    }
}
