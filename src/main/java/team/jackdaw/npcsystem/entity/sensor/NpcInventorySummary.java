package team.jackdaw.npcsystem.entity.sensor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record NpcInventorySummary(int occupiedSlots, int totalSlots, int totalItems, List<ItemEntry> items) {
    public static NpcInventorySummary from(SimpleContainer inventory) {
        if (inventory == null) {
            return new NpcInventorySummary(0, 0, 0, List.of());
        }
        Map<String, Integer> counts = new TreeMap<>();
        int occupied = 0;
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            occupied++;
            total += stack.getCount();
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            counts.merge(id, stack.getCount(), Integer::sum);
        }
        List<ItemEntry> entries = new ArrayList<>();
        counts.forEach((id, count) -> entries.add(new ItemEntry(id, count)));
        return new NpcInventorySummary(occupied, inventory.getContainerSize(), total, List.copyOf(entries));
    }

    public boolean isFull() {
        return totalSlots > 0 && occupiedSlots >= totalSlots;
    }

    public String compactText() {
        if (items.isEmpty()) {
            return "空";
        }
        StringBuilder builder = new StringBuilder();
        items.forEach(item -> builder.append(item.id()).append("x").append(item.count()).append("; "));
        return builder.toString().trim();
    }

    public record ItemEntry(String id, int count) {
    }
}
