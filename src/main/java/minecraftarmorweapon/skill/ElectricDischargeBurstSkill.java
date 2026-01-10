package minecraftarmorweapon.block;

import minecraftarmorweapon.damage.ElementType;
import minecraftarmorweapon.damage.ElectricElementDamageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.Set;

/**
 * 現実的な「通電」処理
 * ・再帰なし
 * ・接しているブロックのみ対象
 */
public final class ElectricConductBlock {

    private ElectricConductBlock() {}

    /* ===============================
     * 現実的に電気が通るブロック
     * =============================== */
    private static final Set<Block> CONDUCTIVE_BLOCKS = EnumSet.of(
            // 金属
            net.minecraft.world.level.block.Blocks.IRON_BLOCK,
            net.minecraft.world.level.block.Blocks.GOLD_BLOCK,
            net.minecraft.world.level.block.Blocks.COPPER_BLOCK,
            net.minecraft.world.level.block.Blocks.CUT_COPPER,
            net.minecraft.world.level.block.Blocks.EXPOSED_COPPER,
            net.minecraft.world.level.block.Blocks.WEATHERED_COPPER,
            net.minecraft.world.level.block.Blocks.OXIDIZED_COPPER,
            net.minecraft.world.level.block.Blocks.RAW_IRON_BLOCK,
            net.minecraft.world.level.block.Blocks.RAW_COPPER_BLOCK,
            net.minecraft.world.level.block.Blocks.ANVIL,
            net.minecraft.world.level.block.Blocks.CHIPPED_ANVIL,
            net.minecraft.world.level.block.Blocks.DAMAGED_ANVIL,
            net.minecraft.world.level.block.Blocks.LIGHTNING_ROD,
            net.minecraft.world.level.block.Blocks.CHAIN,

            // 水・湿潤
            net.minecraft.world.level.block.Blocks.WATER,
            net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN,
            net.minecraft.world.level.block.Blocks.KELP,
            net.minecraft.world.level.block.Blocks.KELP_PLANT,
            net.minecraft.world.level.block.Blocks.SEAGRASS,
            net.minecraft.world.level.block.Blocks.TALL_SEAGRASS
    );

    /** 電気が通るブロックか */
    public static boolean isConductive(Block block) {
        return CONDUCTIVE_BLOCKS.contains(block);
    }

    /**
     * 通電処理（接触ブロックのみ）
     *
     * @param level  ワールド
     * @param origin 命中ブロック
     * @param damage ダメージ
     */
    public static void conduct(Level level, BlockPos origin, double damage) {

        // 命中ブロック自身
        applyShock(level, origin, damage);

        // 接している6方向のみ
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = origin.relative(dir);
            applyShock(level, neighbor, damage);
        }
    }

    /* ===============================
     * 内部処理
     * =============================== */

    private static void applyShock(Level level, BlockPos pos, double damage) {
        BlockState state = level.getBlockState(pos);
        if (!isConductive(state.getBlock())) return;

        AABB box = new AABB(pos).inflate(0.1);

        for (Entity entity : level.getEntities(null, box)) {
            if (entity instanceof LivingEntity living && living.isAlive()) {
                ElectricElementDamageHandler.apply(
                        living,
                        damage,
                        ElementType.ELECTRIC
                );
            }
        }
    }
}
