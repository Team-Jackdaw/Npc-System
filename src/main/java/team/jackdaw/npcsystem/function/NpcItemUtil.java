package team.jackdaw.npcsystem.function;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

final class NpcItemUtil {
    private NpcItemUtil() {
    }

    static Optional<Item> item(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String trimmed = id.trim();
        Identifier identifier = trimmed.contains(":") ? Identifier.tryParse(trimmed) : Identifier.tryParse("minecraft:" + trimmed);
        if (identifier == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(identifier);
    }

    static ItemStack remove(SimpleContainer inventory, Item item, int count) {
        if (inventory == null || item == null || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = inventory.removeItemType(item, count);
        inventory.setChanged();
        return removed;
    }

    static int count(SimpleContainer inventory, Item item) {
        if (inventory == null || item == null) {
            return 0;
        }
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
