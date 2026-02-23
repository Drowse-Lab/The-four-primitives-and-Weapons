package minecraftarmorweapon.block;

import minecraftarmorweapon.damage.ElectricElementDamageHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * ELECTRIC 通電処理
 */
public final class ElectricConductBlock {

    private ElectricConductBlock() {}

    private static final int MAX_RADIUS = 10;
    private static final int MAX_RADIUS_SQR = MAX_RADIUS * MAX_RADIUS;
    private static final int NEIGHBOR_SEARCH = 2; // 起点から導体を探す範囲

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
        if (level.isClientSide()) return;

        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        // 起点が導体ならそのまま開始
        if (CONDUCTIVE_BLOCKS.contains(level.getBlockState(origin).getBlock())) {
            queue.add(origin);
            visited.add(origin);
        } else {
            // 起点が導体でない場合、周囲 NEIGHBOR_SEARCH ブロック内から導体を探す
            visited.add(origin);
            for (int dx = -NEIGHBOR_SEARCH; dx <= NEIGHBOR_SEARCH; dx++) {
                for (int dy = -NEIGHBOR_SEARCH; dy <= NEIGHBOR_SEARCH; dy++) {
                    for (int dz = -NEIGHBOR_SEARCH; dz <= NEIGHBOR_SEARCH; dz++) {
                        BlockPos check = origin.offset(dx, dy, dz);
                        if (CONDUCTIVE_BLOCKS.contains(level.getBlockState(check).getBlock())) {
                            queue.add(check);
                            visited.add(check);
                        }
                    }
                }
            }
        }

        if (queue.isEmpty()) return;

        boolean playedSound = false;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            // 半径制限
            if (current.distSqr(origin) > MAX_RADIUS_SQR) continue;

            BlockState state = level.getBlockState(current);
            if (!CONDUCTIVE_BLOCKS.contains(state.getBlock())) continue;

            // 通電エフェクト
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        current.getX() + 0.5, current.getY() + 1.0, current.getZ() + 0.5,
                        5, 0.3, 0.3, 0.3, 0.02);
            }

            // 通電音（1回だけ）
            if (!playedSound) {
                level.playSound(null,
                        current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5,
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.5f, 1.8f);
                playedSound = true;
            }

            // ブロック上+周囲のエンティティにダメージ
            shockEntities(level, current, damage);

            // 隣接する導体ブロックへ伝播
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
        // ブロックの上と周囲1ブロックのエンティティを感電
        AABB box = new AABB(pos).inflate(0.5, 1.0, 0.5);

        for (Entity entity : level.getEntities(null, box)) {
            if (entity instanceof LivingEntity living && living.isAlive()) {
                ElectricElementDamageHandler.applyElectricDamage(
                        living,
                        (float) damage,
                        null,
                        1
                );
            }
        }
    }
}
