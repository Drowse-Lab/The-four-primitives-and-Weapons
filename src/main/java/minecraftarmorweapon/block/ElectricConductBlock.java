package minecraftarmorweapon.block;

import minecraftarmorweapon.damage.ElementType;
import minecraftarmorweapon.damage.ElectricElementDamageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * ELECTRIC 通電処理（最終版）
 */
public final class ElectricConductBlock {

    private ElectricConductBlock() {}

    private static final int MAX_RADIUS = 10;
    private static final int MAX_RADIUS_SQR = MAX_RADIUS * MAX_RADIUS;

    private static final Set<Block> CONDUCTIVE_BLOCKS = Set.of(
            // 金属
            Blocks.IRON_BLOCK,
            Blocks.GOLD_BLOCK,
            Blocks.COPPER_BLOCK,
            Blocks.CUT_COPPER,
            Blocks.EXPOSED_COPPER,
            Blocks.WEATHERED_COPPER,
            Blocks.OXIDIZED_COPPER,
            Blocks.RAW_IRON_BLOCK,
            Blocks.RAW_COPPER_BLOCK,
            Blocks.ANVIL,
            Blocks.CHIPPED_ANVIL,
            Blocks.DAMAGED_ANVIL,
            Blocks.LIGHTNING_ROD,
            Blocks.CHAIN,

            // 水・湿潤
            Blocks.WATER,
            Blocks.BUBBLE_COLUMN,
            Blocks.KELP,
            Blocks.KELP_PLANT,
            Blocks.SEAGRASS,
            Blocks.TALL_SEAGRASS
    );

    public static void conduct(Level level, BlockPos origin, double damage) {

        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            // 半径制限（絶対条件）
            if (current.distSqr(origin) > MAX_RADIUS_SQR) continue;

            BlockState state = level.getBlockState(current);
            if (!CONDUCTIVE_BLOCKS.contains(state.getBlock())) continue;

            shockEntities(level, current, damage);

            // 接しているブロックのみ
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (visited.contains(next)) continue;
                if (next.distSqr(origin) > MAX_RADIUS_SQR) continue;

                visited.add(next);
                queue.add(next);
            }
        }
    }

    private static void shockEntities(Level level, BlockPos pos, double damage) {
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
