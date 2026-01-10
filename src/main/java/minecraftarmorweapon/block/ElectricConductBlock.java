package minecraftarmorweapon.block;

import minecraftarmorweapon.damage.ElementType;
import minecraftarmorweapon.damage.ElectricElementDamageHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * 現実的な「電気が通る」ブロック処理
 */
public final class ElectricConductBlock {

    private ElectricConductBlock() {}

    /* ===============================
     * 現実的に通電するブロック
     * =============================== */
    private static final Set<Material> CONDUCTIVE_BLOCKS = EnumSet.of(
            // 金属
            Material.IRON_BLOCK,
            Material.GOLD_BLOCK,
            Material.COPPER_BLOCK,
            Material.CUT_COPPER,
            Material.CUT_COPPER_SLAB,
            Material.CUT_COPPER_STAIRS,
            Material.EXPOSED_COPPER,
            Material.WEATHERED_COPPER,
            Material.OXIDIZED_COPPER,
            Material.RAW_IRON_BLOCK,
            Material.RAW_COPPER_BLOCK,
            Material.ANVIL,
            Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL,
            Material.LIGHTNING_ROD,
            Material.CHAIN,

            // 水・湿潤
            Material.WATER,
            Material.BUBBLE_COLUMN,
            Material.KELP,
            Material.KELP_PLANT,
            Material.SEAGRASS,
            Material.TALL_SEAGRASS
    );

    /** 電気が通るか */
    public static boolean isConductive(Block block) {
        return CONDUCTIVE_BLOCKS.contains(block.getType());
    }

    /** 通電伝播 */
    public static void conduct(Block origin, double damage) {
        Set<Block> visited = new HashSet<>();
        propagate(origin, visited, damage);
    }

    /* ===============================
     * 内部処理
     * =============================== */

    private static void propagate(
            Block block,
            Set<Block> visited,
            double damage
    ) {
        if (visited.contains(block)) return;
        if (!isConductive(block)) return;

        visited.add(block);

        shockEntities(block, damage);

        for (BlockFace face : BlockFace.values()) {
            if (!face.isCartesian()) continue;
            propagate(block.getRelative(face), visited, damage);
        }
    }

    /** 接触しているエンティティを感電させる */
    private static void shockEntities(Block block, double damage) {
        World world = block.getWorld();
        Location center = block.getLocation().add(0.5, 0.5, 0.5);

        for (Entity entity : world.getNearbyEntities(center, 1.2, 1.2, 1.2)) {
            if (!(entity instanceof LivingEntity living)) continue;

            ElectricElementDamageHandler.apply(
                    living,
                    damage,
                    ElementType.ELECTRIC
            );
        }
    }
}
