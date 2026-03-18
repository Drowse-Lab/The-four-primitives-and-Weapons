package minecraftarmorweapon.damage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * エラー属性ダメージハンドラー
 * - ランダムなデバフを付与する（混沌属性）
 * - ダメージ倍率が不規則（0.5x〜3.0x）
 * - ターゲットをランダムにテレポートさせることがある
 * - ポータル・エンチャントパーティクルを放出
 */
public class ErrorElementDamageHandler {

    private static final float BASE_DAMAGE_MULTIPLIER = 1.0f;
    private static final float CHAOS_MIN = 0.5f;
    private static final float CHAOS_MAX = 3.0f;

    // 同一tickでの二重適用防止
    private static final Map<UUID, Long> lastAppliedTick = new ConcurrentHashMap<>();

    private static final Random rng = new Random();

    /** ランダムデバフ一覧（レベルに応じて選択数が増える） */
    private static final MobEffectInstance[] CHAOS_EFFECTS = {
        new MobEffectInstance(MobEffects.CONFUSION,    120, 0, false, true),
        new MobEffectInstance(MobEffects.BLINDNESS,     80, 0, false, true),
        new MobEffectInstance(MobEffects.WEAKNESS,     100, 1, false, true),
        new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true),
        new MobEffectInstance(MobEffects.WITHER,        60, 0, false, true),
        new MobEffectInstance(MobEffects.LEVITATION,    40, 0, false, true),
        new MobEffectInstance(MobEffects.HUNGER,       100, 2, false, true),
    };

    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        long currentTick = target.level().getGameTime();
        UUID targetId = target.getUUID();
        Long lastTick = lastAppliedTick.get(targetId);
        if (lastTick != null && currentTick - lastTick < 2) {
            return originalDamage;
        }
        lastAppliedTick.put(targetId, currentTick);

        // 混沌倍率：レベルが高いほど振れ幅が大きい
        float chaosRange = (CHAOS_MAX - CHAOS_MIN) * (1.0f + elementLevel * 0.1f);
        float multiplier = BASE_DAMAGE_MULTIPLIER + CHAOS_MIN + rng.nextFloat() * chaosRange;

        // ランダムデバフをレベル個付与
        int effectCount = Math.min(1 + elementLevel / 3, CHAOS_EFFECTS.length);
        for (int i = 0; i < effectCount; i++) {
            MobEffectInstance source = CHAOS_EFFECTS[rng.nextInt(CHAOS_EFFECTS.length)];
            target.addEffect(new MobEffectInstance(
                source.getEffect(),
                source.getDuration(),
                source.getAmplifier(),
                false, true
            ));
        }

        // Lv5以上: 小範囲テレポート
        if (elementLevel >= 5 && rng.nextFloat() < 0.3f) {
            applyRandomTeleport(target);
        }

        // パーティクル
        if (target.level() instanceof ServerLevel serverLevel) {
            spawnParticles(serverLevel, target);
        }

        return originalDamage * multiplier;
    }

    private static void applyRandomTeleport(LivingEntity target) {
        double range = 5.0;
        for (int attempt = 0; attempt < 8; attempt++) {
            double tx = target.getX() + (rng.nextDouble() - 0.5) * range * 2;
            double ty = target.getY() + (rng.nextDouble() - 0.5) * range;
            double tz = target.getZ() + (rng.nextDouble() - 0.5) * range * 2;
            BlockPos pos = new BlockPos((int) tx, (int) ty, (int) tz);
            if (target.level().getBlockState(pos).isAir()
                    && target.level().getBlockState(pos.above()).isAir()) {
                target.teleportTo(tx, ty, tz);
                break;
            }
        }
    }

    private static void spawnParticles(ServerLevel level, LivingEntity target) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2;
        double z = target.getZ();
        // ポータル粒子（混沌感）
        level.sendParticles(ParticleTypes.PORTAL, x, y, z, 12,
                0.3, 0.5, 0.3, 0.1);
        // エンチャントヒット（エラーグリッチ感）
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 8,
                0.2, 0.4, 0.2, 0.05);
        // 白煙
        level.sendParticles(ParticleTypes.POOF, x, y + 0.5, z, 5,
                0.2, 0.2, 0.2, 0.05);
    }
}
