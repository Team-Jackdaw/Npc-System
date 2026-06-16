package team.jackdaw.npcsystem.entity.task;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import team.jackdaw.npcsystem.entity.NPCEntity;

public class PickupNearbyItemTask implements NpcTask {
    private final ItemEntity target;
    private final Item itemFilter;
    private final int wantedCount;
    private final double speed;
    private final int timeoutTicks;
    private int ticks;
    private boolean finished;
    private String result = "running";

    public PickupNearbyItemTask(ItemEntity target, Item itemFilter, int wantedCount, double speed, int timeoutTicks) {
        this.target = target;
        this.itemFilter = itemFilter;
        this.wantedCount = Math.max(1, wantedCount);
        this.speed = speed;
        this.timeoutTicks = Math.max(1, timeoutTicks);
    }

    @Override
    public String name() {
        return "pickup_item";
    }

    @Override
    public boolean canStart(NPCEntity npc) {
        return NpcTask.super.canStart(npc)
                && target != null
                && target.isAlive()
                && target.level().equals(npc.level())
                && matches(target.getItem());
    }

    @Override
    public void start(NPCEntity npc) {
        npc.getNavigation().moveTo(target, speed);
    }

    @Override
    public void tick(NPCEntity npc) {
        ticks++;
        if (target == null || !target.isAlive() || !target.level().equals(npc.level()) || !matches(target.getItem())) {
            result = "target_lost";
            finished = true;
            return;
        }
        npc.getLookControl().setLookAt(target, 30.0f, 30.0f);
        if (npc.distanceToSqr(target) <= 2.25) {
            pickUp(npc);
            return;
        }
        if (ticks % 20 == 0) {
            npc.getNavigation().moveTo(target, speed);
        }
        if (ticks >= timeoutTicks) {
            result = "timeout";
            finished = true;
        }
    }

    @Override
    public boolean isFinished(NPCEntity npc) {
        return finished;
    }

    @Override
    public void stop(NPCEntity npc) {
        npc.getNavigation().stop();
    }

    public String result() {
        return result;
    }

    private void pickUp(NPCEntity npc) {
        SimpleContainer inventory = npc.getInventory();
        ItemStack source = target.getItem();
        int count = Math.min(wantedCount, source.getCount());
        ItemStack moving = source.copyWithCount(count);
        ItemStack leftover = inventory.addItem(moving);
        int accepted = count - leftover.getCount();
        if (accepted <= 0) {
            result = "inventory_full";
            finished = true;
            return;
        }
        source.shrink(accepted);
        if (source.isEmpty()) {
            target.discard();
        } else {
            target.setItem(source);
        }
        result = "picked_up";
        finished = true;
    }

    private boolean matches(ItemStack stack) {
        return stack != null && !stack.isEmpty() && (itemFilter == null || stack.getItem() == itemFilter);
    }
}
