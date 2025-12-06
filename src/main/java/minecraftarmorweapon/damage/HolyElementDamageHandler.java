package minecraftarmorweapon.damage;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;

/**
 * 聖属性ダメージハンドラー
 * - アンデットに多くダメージが入る
 */
public class HolyElementDamageHandler {

    // 基礎ダメージ倍率
    private static final float BASE_DAMAGE_MULTIPLIER = 1.1f;
    // アンデットへのダメージ倍率
    private static final float UNDEAD_DAMAGE_MULTIPLIER = 2.5f;
    // レベルごとの追加ダメージ倍率
    private static final float LEVEL_DAMAGE_MULTIPLIER = 0.3f;

    /**
     * エンティティがアンデットかどうかを判定
     * @param entity 判定するエンティティ
     * @return アンデットの場合true
     */
    private static boolean isUndead(LivingEntity entity) {
        // 個別のエンティティタイプで判定
        return entity instanceof Zombie ||
               entity instanceof Skeleton ||
               entity instanceof WitherSkeleton ||
               entity instanceof Stray ||
               entity instanceof Husk ||
               entity instanceof Phantom ||
               entity instanceof Drowned ||
               entity instanceof ZombieVillager ||
               entity instanceof ZombifiedPiglin ||
               entity instanceof WitherBoss;
    }

    /**
     * 聖属性ダメージを計算
     * @param target ターゲットエンティティ
     * @param originalDamage 元のダメージ
     * @param elementLevel 属性レベル
     * @return 計算後のダメージ
     */
    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        float damageMultiplier = BASE_DAMAGE_MULTIPLIER;

        // アンデットの場合、大幅にダメージ増加
        if (isUndead(target)) {
            damageMultiplier = UNDEAD_DAMAGE_MULTIPLIER;

            // レベルによる追加ダメージ
            damageMultiplier += (elementLevel * LEVEL_DAMAGE_MULTIPLIER);

            // 炎上効果を付与（聖なる炎）
            if (elementLevel >= 2) {
                target.setSecondsOnFire(5);
            }

            // 発光効果を付与
            target.setGlowingTag(true);

            // 聖なる光のパーティクル（アンデッド専用の強化版）
            if (target.level instanceof ServerLevel serverLevel) {
                // エンチャントの輝き（金色）
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    30, 0.4, 0.6, 0.4, 0.15);

                // 光の粒子
                serverLevel.sendParticles(ParticleTypes.GLOW,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    20, 0.3, 0.5, 0.3, 0.1);

                // 聖なる炎
                if (elementLevel >= 2) {
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        15, 0.2, 0.3, 0.2, 0.05);
                }
            }
        } else {
            // 通常の聖属性エフェクト
            if (target.level instanceof ServerLevel serverLevel) {
                // 軽い光のエフェクト
                serverLevel.sendParticles(ParticleTypes.WAX_ON,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.3, 0.4, 0.3, 0.05);
            }
        }

        return originalDamage * damageMultiplier;
    }

    /**
     * エンティティに聖属性ダメージを与える
     * @param target ターゲットエンティティ
     * @param damage ダメージ量
     * @param source ダメージソース元
     * @param level 属性レベル
     */
    public static void applyHolyDamage(LivingEntity target, float damage, LivingEntity source, int level) {
        // カスタムダメージソースを作成
        IElementalDamageSource elementalSource = (IElementalDamageSource) new HolyDamageSource("holy");
        elementalSource.setElementType(ElementType.HOLY);
        elementalSource.setElementLevel(level);

        // ダメージを適用
        target.hurt((DamageSource) elementalSource, damage);
    }
}
