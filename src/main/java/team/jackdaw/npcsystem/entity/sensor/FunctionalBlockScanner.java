package team.jackdaw.npcsystem.entity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import team.jackdaw.npcsystem.entity.NPCEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FunctionalBlockScanner {
    public static final int DEFAULT_RADIUS = 8;
    public static final int MAX_RESULTS = 12;

    private static final Set<Block> FUNCTIONAL_BLOCKS = Set.of(
            Blocks.WHITE_BED, Blocks.ORANGE_BED, Blocks.MAGENTA_BED, Blocks.LIGHT_BLUE_BED,
            Blocks.YELLOW_BED, Blocks.LIME_BED, Blocks.PINK_BED, Blocks.GRAY_BED,
            Blocks.LIGHT_GRAY_BED, Blocks.CYAN_BED, Blocks.PURPLE_BED, Blocks.BLUE_BED,
            Blocks.BROWN_BED, Blocks.GREEN_BED, Blocks.RED_BED, Blocks.BLACK_BED,
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.BARREL,
            Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
            Blocks.COMPOSTER, Blocks.LECTERN, Blocks.ANVIL, Blocks.CHIPPED_ANVIL,
            Blocks.DAMAGED_ANVIL, Blocks.GRINDSTONE, Blocks.BREWING_STAND,
            Blocks.ENCHANTING_TABLE, Blocks.STONECUTTER, Blocks.LOOM, Blocks.SMITHING_TABLE,
            Blocks.CARTOGRAPHY_TABLE, Blocks.FLETCHING_TABLE, Blocks.CAULDRON,
            Blocks.WATER_CAULDRON, Blocks.LAVA_CAULDRON, Blocks.POWDER_SNOW_CAULDRON,
            Blocks.BELL
    );

    private static final Map<Block, String> CATEGORIES = Map.ofEntries(
            Map.entry(Blocks.CHEST, "storage"),
            Map.entry(Blocks.TRAPPED_CHEST, "storage"),
            Map.entry(Blocks.ENDER_CHEST, "storage"),
            Map.entry(Blocks.BARREL, "storage"),
            Map.entry(Blocks.CRAFTING_TABLE, "crafting"),
            Map.entry(Blocks.FURNACE, "smelting"),
            Map.entry(Blocks.BLAST_FURNACE, "smelting"),
            Map.entry(Blocks.SMOKER, "smelting"),
            Map.entry(Blocks.COMPOSTER, "workstation"),
            Map.entry(Blocks.LECTERN, "workstation"),
            Map.entry(Blocks.GRINDSTONE, "workstation"),
            Map.entry(Blocks.STONECUTTER, "workstation"),
            Map.entry(Blocks.LOOM, "workstation"),
            Map.entry(Blocks.SMITHING_TABLE, "workstation"),
            Map.entry(Blocks.CARTOGRAPHY_TABLE, "workstation"),
            Map.entry(Blocks.FLETCHING_TABLE, "workstation"),
            Map.entry(Blocks.ANVIL, "utility"),
            Map.entry(Blocks.CHIPPED_ANVIL, "utility"),
            Map.entry(Blocks.DAMAGED_ANVIL, "utility"),
            Map.entry(Blocks.BREWING_STAND, "brewing"),
            Map.entry(Blocks.ENCHANTING_TABLE, "enchanting"),
            Map.entry(Blocks.CAULDRON, "utility"),
            Map.entry(Blocks.WATER_CAULDRON, "utility"),
            Map.entry(Blocks.LAVA_CAULDRON, "utility"),
            Map.entry(Blocks.POWDER_SNOW_CAULDRON, "utility"),
            Map.entry(Blocks.BELL, "village")
    );

    private FunctionalBlockScanner() {
    }

    public static List<FunctionalBlockSummary> scan(NPCEntity npc, int radius, int maxResults) {
        int safeRadius = Math.max(1, Math.min(16, radius));
        int safeMax = Math.max(1, Math.min(32, maxResults));
        Level level = npc.level();
        BlockPos center = npc.blockPosition();
        BlockPos min = center.offset(-safeRadius, -safeRadius, -safeRadius);
        BlockPos max = center.offset(safeRadius, safeRadius, safeRadius);
        return BlockPos.betweenClosedStream(min, max)
                .map(BlockPos::immutable)
                .map(pos -> summary(level, center, pos))
                .filter(summary -> summary != null)
                .sorted(Comparator.comparingDouble(FunctionalBlockSummary::distance))
                .limit(safeMax)
                .toList();
    }

    public static boolean matchesType(FunctionalBlockSummary summary, String type) {
        if (summary == null || type == null || type.isBlank()) {
            return false;
        }
        String normalized = type.trim().toLowerCase(java.util.Locale.ROOT);
        return summary.blockId().equals(normalized)
                || summary.blockId().equals("minecraft:" + normalized)
                || summary.blockId().endsWith("_" + normalized)
                || summary.category().equals(normalized);
    }

    private static FunctionalBlockSummary summary(Level level, BlockPos center, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!FUNCTIONAL_BLOCKS.contains(block)) {
            return null;
        }
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        String category = CATEGORIES.getOrDefault(block, blockId.endsWith("_bed") ? "bed" : "functional");
        double distance = Math.sqrt(center.distSqr(pos));
        return new FunctionalBlockSummary(blockId, category, pos, distance);
    }

    public record FunctionalBlockSummary(String blockId, String category, BlockPos pos, double distance) {
        public String compactText() {
            return blockId + "@" + pos.toShortString() + "(" + String.format(java.util.Locale.ROOT, "%.1f", distance) + "格," + category + ")";
        }
    }
}
