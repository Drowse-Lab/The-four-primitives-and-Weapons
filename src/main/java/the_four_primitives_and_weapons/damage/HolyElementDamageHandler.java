package the_four_primitives_and_weapons.damage;

import the_four_primitives_and_weapons.util.VersionHelper;
import net.minecraft.world.item.ItemStack;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聖属性ダメージハンドラー
 * - アンデットに多くダメージが入る
 */
public class HolyElementDamageHandler {

    // 聖属性ダメージを受けたエンティティを追跡（トーテム貫通用）
    private static final Map<UUID, Long> holyDamageTargets = new ConcurrentHashMap<>();

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

            // 発光効果を付与 (旧: setGlowingTag(true) はタイマー無しで永続化していたバグ)
            // duration 100 tick = 5 秒で自然消滅。 amplifier 0、 ambient false、 visible false、 icon false。
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false, false));

            // 聖なる光のパーティクル（アンデッド専用の強化版）
            if (VersionHelper.getLevel(target) instanceof ServerLevel serverLevel) {
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
            if (VersionHelper.getLevel(target) instanceof ServerLevel serverLevel) {
                // 軽い光のエフェクト
                serverLevel.sendParticles(ParticleTypes.WAX_ON,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.3, 0.4, 0.3, 0.05);
            }
        }

        // トーテム貫通用: このエンティティが聖属性ダメージを受けたことを記録
        holyDamageTargets.put(target.getUUID(), target.level().getGameTime());

        return originalDamage * damageMultiplier;
    }

    /**
     * 聖属性ダメージを受けたエンティティかチェック（トーテム貫通用）
     * Mixinから呼ばれる
     */
    public static boolean shouldBypassTotem(UUID targetUUID, long currentGameTime) {
        Long hitTime = holyDamageTargets.remove(targetUUID);
        if (hitTime == null) return false;
        // 2tick以内のデータのみ有効
        return currentGameTime - hitTime <= 1;
    }

    /**
     * エンティティに���属性ダメージを与え��
     * @param target ターゲットエンティティ
     * @param damage ダメージ量
     * @param source ダメージソース元
     * @param level 属性レベル
     */
    public static void applyHolyDamage(LivingEntity target, float damage, LivingEntity source, int level) {
        // カスタム DamageType: the_four_primitives_and_weapons:holy
        DamageSource ds = ModDamageSources.ofElement(target.level(), ElementType.HOLY, source);
        IElementalDamageSource elementalSource = (IElementalDamageSource) ds;
        elementalSource.setElementType(ElementType.HOLY);
        elementalSource.setElementLevel(level);

        target.hurt(ds, damage);
    }

    public static float handleHolyDamage(LivingEntity attacker, LivingEntity target, ItemStack weapon, float baseDmg) {
        int level = ElementalDamageUtils.getElementLevel(weapon);
        float damage = calculateDamage(target, baseDmg, level);
        applyHolyDamage(target, damage - baseDmg, attacker, level);
        return damage;
    }
}
