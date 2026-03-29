package minecraftarmorweapon.damage;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * エラー属性ダメージハンドラー
 * - 防御値（アーマー・エンチャント・耐性・他mod含む全て）を貫通する
 * - レベル1ごとに10%防御貫通
 * - レベル10以上で100%貫通 + 追加攻撃力ボーナス
 *
 * 仕組み:
 *   LivingHurtEvent（防御軽減前）で元ダメージとレベルを記録
 *   → LivingDamageEvent（全防御軽減後）で実際に軽減された分 × 貫通率 を戻す
 *   これによりバニラ・他mod問わず全ての防御軽減を正確に貫通できる
 */
public class ErrorElementDamageHandler {

    // レベルあたりの防御貫通率（10%）
    private static final float PENETRATION_PER_LEVEL = 0.1f;
    // レベル10超過時の追加ダメージ倍率（レベルごと）
    private static final float BONUS_DAMAGE_PER_LEVEL = 0.15f;

    // LivingHurtEvent → LivingDamageEvent 間のデータ受け渡し用
    private static final Map<UUID, PendingErrorDamage> pendingDamage = new ConcurrentHashMap<>();
    // LivingDamageEvent HIGH → LOWEST 間のデータ受け渡し用
    private static final Map<UUID, PostArmorPenetration> postArmorData = new ConcurrentHashMap<>();

    /**
     * HurtEvent時点で記録するデータ
     */
    public static class PendingErrorDamage {
        public final float preDamage;  // 防御軽減前のダメージ（error適用後）
        public final int elementLevel;
        public final long gameTick;

        public PendingErrorDamage(float preDamage, int elementLevel, long gameTick) {
            this.preDamage = preDamage;
            this.elementLevel = elementLevel;
            this.gameTick = gameTick;
        }
    }

    /**
     * DamageEvent HIGH → LOWEST 間で受け渡すデータ
     * アーマー貫通適用後のダメージを記録し、他modエフェクト貫通に使う
     */
    public static class PostArmorPenetration {
        public final float damageAfterArmorPenetration;  // アーマー貫通適用後のダメージ
        public final float penetrationPercent;
        public final long gameTick;

        public PostArmorPenetration(float damageAfterArmorPenetration, float penetrationPercent, long gameTick) {
            this.damageAfterArmorPenetration = damageAfterArmorPenetration;
            this.penetrationPercent = penetrationPercent;
            this.gameTick = gameTick;
        }
    }

    /**
     * LivingHurtEvent で呼ばれる: レベル10+の攻撃力ボーナスを適用し、貫通用データを記録
     * ここでは防御貫通は行わない（まだ防御軽減が起きていないため）
     */
    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        float totalDamage = originalDamage;

        // レベル10以上: 追加攻撃力ボーナス
        if (elementLevel >= 10) {
            int bonusLevels = elementLevel - 9;
            totalDamage += originalDamage * (bonusLevels * BONUS_DAMAGE_PER_LEVEL);
        }

        // 相手のバフ（有益なエフェクト）を解除する
        removeBuffs(target, elementLevel);

        // LivingDamageEvent用にデータを保存
        pendingDamage.put(target.getUUID(),
            new PendingErrorDamage(totalDamage, elementLevel, target.level().getGameTime()));

        // パーティクルエフェクト
        if (VersionHelper.getLevel(target) instanceof ServerLevel serverLevel) {
            double x = target.getX();
            double y = target.getY() + target.getBbHeight() / 2;
            double z = target.getZ();

            serverLevel.sendParticles(ParticleTypes.CRIT,
                x, y, z, 15, 0.4, 0.5, 0.4, 0.2);

            serverLevel.sendParticles(ParticleTypes.PORTAL,
                x, y, z, 10, 0.3, 0.4, 0.3, 0.1);

            if (elementLevel >= 10) {
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    x, y, z, 5, 0.3, 0.3, 0.3, 0.1);
            }
        }

        return totalDamage;
    }

    /**
     * [Phase 1] LivingDamageEvent HIGH で呼ばれる:
     * バニラのアーマー・エンチャント・耐性による軽減分を貫通率に応じて戻す
     * その後、他modエフェクト貫通用にデータを記録する
     *
     * @param target     ターゲット
     * @param postDamage 防御軽減後のダメージ
     * @return アーマー貫通適用後のダメージ
     */
    public static float applyArmorPenetration(LivingEntity target, float postDamage) {
        PendingErrorDamage pending = pendingDamage.remove(target.getUUID());
        if (pending == null) {
            return postDamage;
        }

        // 古いデータ（2tick以上前）は無視
        if (target.level().getGameTime() - pending.gameTick > 1) {
            return postDamage;
        }

        float penetrationPercent = Math.min(pending.elementLevel * PENETRATION_PER_LEVEL, 1.0f);

        // 防御で軽減された量 = 軽減前ダメージ - 軽減後ダメージ
        float totalReduction = pending.preDamage - postDamage;
        float result = postDamage;
        if (totalReduction > 0) {
            result = postDamage + totalReduction * penetrationPercent;
        }

        // Phase 2用にデータを記録（他modエフェクト貫通用）
        postArmorData.put(target.getUUID(),
            new PostArmorPenetration(result, penetrationPercent, pending.gameTick));

        return result;
    }

    /**
     * [Phase 2] LivingDamageEvent LOWEST で呼ばれる:
     * Heart Stop等の他modエフェクトがダメージを軽減/0にした場合、
     * その軽減分も貫通率に応じて戻す
     *
     * @param target     ターゲット
     * @param postDamage 他modエフェクト適用後のダメージ
     * @return エフェクト貫通適用後の最終ダメージ
     */
    public static float applyEffectPenetration(LivingEntity target, float postDamage) {
        PostArmorPenetration data = postArmorData.remove(target.getUUID());
        if (data == null) {
            return postDamage;
        }

        // 古いデータは無視
        if (target.level().getGameTime() - data.gameTick > 1) {
            return postDamage;
        }

        // Phase1後に他modが更にダメージを軽減した量
        float effectReduction = data.damageAfterArmorPenetration - postDamage;
        if (effectReduction <= 0) {
            // 他modによる追加軽減なし
            return postDamage;
        }

        // 他modエフェクトの軽減分も貫通率に応じて戻す
        return postDamage + effectReduction * data.penetrationPercent;
    }

    /**
     * 指定UUIDのpendingデータ(Phase1用)があるかチェック
     */
    public static boolean hasPendingHurt(UUID uuid) {
        return pendingDamage.containsKey(uuid);
    }

    /**
     * 指定UUIDのpostArmorデータ(Phase2用)があるかチェック
     */
    public static boolean hasPendingEffect(UUID uuid) {
        return postArmorData.containsKey(uuid);
    }

    /**
     * ターゲットのバフ（有益なエフェクト）を解除する。
     * レベルに応じて解除数が増加する:
     *   Lv1-4: 最大2個、Lv5-9: 最大4個、Lv10+: 全て解除
     *
     * @param target       攻撃対象
     * @param elementLevel 属性レベル
     */
    private static void removeBuffs(LivingEntity target, int elementLevel) {
        // 有益なエフェクトを収集
        List<MobEffectInstance> buffs = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (effect.getEffect().getCategory() == MobEffectCategory.BENEFICIAL) {
                buffs.add(effect);
            }
        }

        if (buffs.isEmpty()) return;

        // レベルに応じた最大解除数
        int maxRemove;
        if (elementLevel >= 10) {
            maxRemove = buffs.size(); // 全解除
        } else if (elementLevel >= 5) {
            maxRemove = 4;
        } else {
            maxRemove = 2;
        }

        int removed = 0;
        for (MobEffectInstance buff : buffs) {
            if (removed >= maxRemove) break;
            target.removeEffect(buff.getEffect());
            removed++;
        }
    }
}
