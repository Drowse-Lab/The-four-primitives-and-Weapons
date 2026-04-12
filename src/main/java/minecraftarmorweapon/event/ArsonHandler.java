package minecraftarmorweapon.event;

import minecraftarmorweapon.ai.lisp.ProgressionTracker;
import minecraftarmorweapon.command.CustomDifficultyCommand;
import minecraftarmorweapon.command.CustomDifficultyCommand.CustomDifficulty;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * 放火ハンドラー — レベルが上がると敵Mobがプレイヤー周辺の可燃ブロックに火を放つ。
 *
 *  発動条件:
 *    - aiLevel + 進行度ボーナス/20 >= 3
 *    - プレイヤーから8ブロック以内にMonsterが存在し、プレイヤーをターゲットしている
 *    - プレイヤー周囲3ブロック以内に可燃ブロック(葉・板材・原木・羊毛・カーペット等)がある
 *
 *  発動確率はレベルに応じて上昇 (0〜約0.8)。
 */
@Mod.EventBusSubscriber
public class ArsonHandler {

    private static final int CHECK_INTERVAL = 60; // 3秒ごと
    private static final int ARSON_RADIUS = 3;
    private static final int MOB_SEARCH_RADIUS = 8;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long tick = event.getServer().getTickCount();
        if (tick % CHECK_INTERVAL != 0) return;

        CustomDifficulty diff = CustomDifficultyCommand.getCurrentDifficulty();
        int aiLevel = diff.getAiLevel();
        int progBonus = ProgressionTracker.getBaseLevelBonus();
        int effectiveLevel = aiLevel + progBonus / 20;
        if (effectiveLevel < 3) return;

        // 発火確率: レベル3=0.1 → レベル10+=約0.8
        float arsonChance = Math.min(0.8f, 0.1f + (effectiveLevel - 3) * 0.1f);
        Random rand = new Random();

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.dimensionType().natural()) continue;
            // ネザーは常に燃える環境なので発動不要
            if (level.dimensionType().ultraWarm()) continue;

            for (ServerPlayer player : level.players()) {
                if (player.isCreative() || player.isSpectator()) continue;
                if (rand.nextFloat() >= arsonChance) continue;

                // 近くにプレイヤーを狙うMobがいるか
                if (!hasNearbyHostileMob(level, player)) continue;

                // プレイヤー周辺の可燃ブロックに火を放つ
                tryIgniteAround(level, player, rand);
            }
        }
    }

    private static boolean hasNearbyHostileMob(ServerLevel level, ServerPlayer player) {
        net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(MOB_SEARCH_RADIUS);
        for (Monster mob : level.getEntitiesOfClass(Monster.class, box)) {
            if (mob.getTarget() == player) return true;
            if (mob.distanceToSqr(player) <= MOB_SEARCH_RADIUS * MOB_SEARCH_RADIUS && mob.hasLineOfSight(player)) {
                return true;
            }
        }
        return false;
    }

    private static void tryIgniteAround(ServerLevel level, ServerPlayer player, Random rand) {
        BlockPos center = player.blockPosition();
        int attempts = 6;
        for (int i = 0; i < attempts; i++) {
            int dx = rand.nextInt(ARSON_RADIUS * 2 + 1) - ARSON_RADIUS;
            int dy = rand.nextInt(3) - 1;
            int dz = rand.nextInt(ARSON_RADIUS * 2 + 1) - ARSON_RADIUS;
            BlockPos target = center.offset(dx, dy, dz);
            if (tryIgniteAt(level, target)) return;
        }
    }

    private static boolean tryIgniteAt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // 足場そのものが燃える材質ならその上に火を置く
        if (state.ignitedByLava()) {
            BlockPos above = pos.above();
            if (level.isEmptyBlock(above)) {
                level.setBlock(above, BaseFireBlock.getState(level, above), 11);
                return true;
            }
        }
        // 空気ブロックで周囲に燃える材質が隣接するなら火を置く
        if (level.isEmptyBlock(pos)) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);
                BlockState ns = level.getBlockState(neighbor);
                if (ns.ignitedByLava()) {
                    level.setBlock(pos, BaseFireBlock.getState(level, pos), 11);
                    return true;
                }
            }
        }
        return false;
    }
}
