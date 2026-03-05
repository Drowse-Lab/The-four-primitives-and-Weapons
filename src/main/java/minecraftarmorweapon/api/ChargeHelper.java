package minecraftarmorweapon.api;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import minecraftarmorweapon.util.DamageCalculator;

import java.util.List;

/**
 * チャージ技を簡単に作るためのユーティリティ。
 *
 * <pre>
 * // 使用例: チャージ率に応じてダメージをスケールする
 * float damage = ChargeHelper.scaleDamage(10.0f, chargePercent, 2.0f);
 * // chargePercent=0.0 → 10.0, chargePercent=1.0 → 30.0
 *
 * // 前方の敵を取得してダメージを与える
 * ChargeHelper.damageEntitiesInFront(player, 5.0, 3.0, damage);
 *
 * // 周囲の敵を取得してダメージを与える
 * ChargeHelper.damageEntitiesAround(player, 4.0, damage);
 * </pre>
 */
public final class ChargeHelper {

    private ChargeHelper() {}

    // ==================== ダメージ計算 ====================

    /**
     * チャージ率に応じてダメージをスケールする。
     *
     * @param baseDamage     基礎ダメージ
     * @param chargePercent  チャージ率 (0.0~1.0)
     * @param chargeMultiplier チャージ倍率 (例: 2.0 → フルチャージで3倍)
     * @return スケール済みダメージ
     */
    public static float scaleDamage(float baseDamage, float chargePercent, float chargeMultiplier) {
        if (chargePercent <= 0.0f) return baseDamage;
        return baseDamage * (1.0f + chargePercent * chargeMultiplier);
    }

    /**
     * チャージ率に応じて値をスケールする (距離、範囲など汎用)。
     */
    public static double scaleValue(double base, float chargePercent, double maxBonus) {
        return base + chargePercent * maxBonus;
    }

    /**
     * チャージ判定。
     */
    public static boolean isCharged(float chargePercent) {
        return chargePercent > 0.0f;
    }

    /**
     * フルチャージ判定。
     */
    public static boolean isFullyCharged(float chargePercent) {
        return chargePercent >= 1.0f;
    }

    // ==================== ターゲット検索 ====================

    /**
     * 前方の扇状範囲にいる敵を取得する。
     *
     * @param player    プレイヤー
     * @param range     前方距離
     * @param width     横幅
     * @param minDot    視線との内積の最小値 (0.0=180度, 0.5=120度, 0.8=狭い)
     * @return ターゲットリスト
     */
    public static List<LivingEntity> getEntitiesInFront(Player player, double range, double width, double minDot) {
        Vec3 pos = player.position();
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

        Vec3 min = pos.add(look.scale(-0.5)).add(right.scale(-width)).add(0, -0.5, 0);
        Vec3 max = pos.add(look.scale(range)).add(right.scale(width)).add(0, 2.5, 0);
        AABB area = new AABB(min, max);

        return player.level().getEntitiesOfClass(LivingEntity.class, area, entity -> {
            if (entity == player) return false;
            Vec3 toEntity = entity.position().subtract(pos).normalize();
            return look.dot(toEntity) > minDot && entity.distanceTo(player) <= range + width;
        });
    }

    /**
     * 前方の扇状範囲にいる敵を取得する (デフォルト: minDot=-0.3)。
     */
    public static List<LivingEntity> getEntitiesInFront(Player player, double range, double width) {
        return getEntitiesInFront(player, range, width, -0.3);
    }

    /**
     * 周囲の敵を取得する。
     */
    public static List<LivingEntity> getEntitiesAround(Player player, double range) {
        Vec3 pos = player.position();
        AABB area = new AABB(
                pos.x - range, pos.y - 1, pos.z - range,
                pos.x + range, pos.y + 3, pos.z + range);
        return player.level().getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.distanceTo(player) <= range);
    }

    // ==================== ダメージ適用 ====================

    /**
     * 前方の敵にダメージを与える。
     */
    public static int damageEntitiesInFront(Player player, double range, double width, float damage) {
        List<LivingEntity> targets = getEntitiesInFront(player, range, width);
        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        for (LivingEntity target : targets) {
            DamageCalculator.dealDamage(player, target, damage, weapon);
        }
        return targets.size();
    }

    /**
     * 周囲の敵にダメージを与える。
     */
    public static int damageEntitiesAround(Player player, double range, float damage) {
        List<LivingEntity> targets = getEntitiesAround(player, range);
        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        for (LivingEntity target : targets) {
            DamageCalculator.dealDamage(player, target, damage, weapon);
        }
        return targets.size();
    }

    /**
     * ターゲットリストに対してダメージ + ノックバックを適用する。
     */
    public static void damageAndKnockback(Player player, List<LivingEntity> targets,
                                           float damage, double knockbackStrength) {
        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        Vec3 playerPos = player.position();
        for (LivingEntity target : targets) {
            DamageCalculator.dealDamage(player, target, damage, weapon);
            Vec3 kb = target.position().subtract(playerPos).normalize().scale(knockbackStrength);
            target.setDeltaMovement(target.getDeltaMovement().add(kb.x, 0.1, kb.z));
        }
    }

    // ==================== エフェクト ====================

    /**
     * 前方にパーティクルのラインを表示する。
     */
    public static void spawnParticleLine(Player player, ParticleOptions particle,
                                          double range, double step, int count) {
        Level world = player.level();
        if (world.isClientSide) return;
        ServerLevel serverWorld = (ServerLevel) world;
        Vec3 pos = player.position();
        Vec3 look = player.getLookAngle();
        for (double d = 0; d <= range; d += step) {
            serverWorld.sendParticles(particle,
                    pos.x + look.x * d, pos.y + 1, pos.z + look.z * d,
                    count, 0.1, 0.1, 0.1, 0.02);
        }
    }

    /**
     * 周囲にパーティクルのリングを表示する。
     */
    public static void spawnParticleRing(Player player, ParticleOptions particle,
                                          double radius, int points, int countPerPoint) {
        Level world = player.level();
        if (world.isClientSide) return;
        ServerLevel serverWorld = (ServerLevel) world;
        Vec3 pos = player.position();
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            serverWorld.sendParticles(particle,
                    pos.x + Math.cos(angle) * radius, pos.y + 1,
                    pos.z + Math.sin(angle) * radius,
                    countPerPoint, 0.1, 0.1, 0.1, 0.02);
        }
    }

    /**
     * サウンドを再生する。
     */
    public static void playSound(Player player, SoundEvent sound, float volume, float pitch) {
        Vec3 pos = player.position();
        player.level().playSound(null, pos.x, pos.y, pos.z,
                sound, SoundSource.PLAYERS, volume, pitch);
    }
}
