package team.jackdaw.npcsystem.function;

public class FunctionRegistration {
    static {
        FunctionManager.getInstance().register("end_conversation", new EndConversationFunction());
        FunctionManager.getInstance().register("master_reply", new MasterReplyFunction());
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
        FunctionManager.getInstance().register("inspect_inventory", new InspectInventoryFunction());
        FunctionManager.getInstance().register("pickup_nearby_item", new PickupNearbyItemFunction());
        FunctionManager.getInstance().register("drop_item", new DropItemFunction());
        FunctionManager.getInstance().register("give_item", new GiveItemFunction());
        FunctionManager.getInstance().register("observe_functional_blocks", new ObserveFunctionalBlocksFunction());
        FunctionManager.getInstance().register("walk_to_block", new WalkToBlockFunction());
        FunctionManager.getInstance().register("walk_to_functional_block", new WalkToFunctionalBlockFunction());
        FunctionManager.getInstance().register("walk_relative", new WalkRelativeFunction());
    }
}
