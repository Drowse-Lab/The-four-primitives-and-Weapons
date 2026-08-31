package the_four_primitives_and_weapons.damage;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ElementDamageKind#MAGIC} の属性ダメージ ( 防具貫通 ) を、
 * 防具の軽減が済んでから足し戻すための受け渡し。
 *
 * <p>LivingHurtEvent ( 軽減前 ) では属性分を一発に乗せずにここへ預け、
 * LivingDamageEvent ( 防具値・防具エンチャ適用後 ) で足す。
 * こうすると被弾は 1 回のままで、属性分だけが防具と保護エンチャを無視する。
 * 貫通させるのは防具まわりだけなので、預けた分には耐性ポーションの軽減を掛け直す。</p>
 */
public final class ElementalMagicPenetration {
    private ElementalMagicPenetration() {}

    private static final class Pending {
        final float amount;
        final long  gameTime;

        Pending(float amount, long gameTime) {
            this.amount   = amount;
            this.gameTime = gameTime;
        }
    }

    // entityUUID → Pending
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    /**
     * 取りこぼし ( LivingHurtEvent がキャンセルされて LivingDamageEvent が来なかった分 ) が
     * 溜まりっぱなしにならないよう、この数を超えたら古い tick の分を捨てる。
     */
    private static final int CLEANUP_THRESHOLD = 256;

    /**
     * 属性が足す分を預ける。
     * 同じ tick に複数回預けられた場合 ( 多段ヒット等 ) は加算する。
     */
    public static void queue(LivingEntity target, float amount) {
        if (target == null || amount <= 0.0F) return;
        long now = target.level().getGameTime();
        if (PENDING.size() > CLEANUP_THRESHOLD) {
            PENDING.values().removeIf(stale -> stale.gameTime != now);
        }
        Pending added = new Pending(amount, now);
        PENDING.merge(target.getUUID(), added,
                (old, incoming) -> old.gameTime == incoming.gameTime
                        ? new Pending(old.amount + incoming.amount, incoming.gameTime)
                        : incoming);
    }

    public static boolean hasPending(LivingEntity target) {
        return PENDING.containsKey(target.getUUID());
    }

    /**
     * 預けてある属性分を、防具軽減後のダメージに足して返す。
     *
     * <p>LivingHurtEvent がキャンセルされて LivingDamageEvent が来なかった場合に
     * 次の被弾へ持ち越さないよう、同じ tick に預けた分だけを有効にする。</p>
     */
    public static float consume(LivingEntity target, DamageSource source, float postDamage) {
        Pending pending = PENDING.remove(target.getUUID());
        if (pending == null) return postDamage;
        if (pending.gameTime != target.level().getGameTime()) return postDamage;
        return postDamage + applyResistanceOnly(target, source, pending.amount);
    }

    /** 対象が持ち越している分を捨てる。 */
    public static void clear(LivingEntity target) {
        if (target != null) PENDING.remove(target.getUUID());
    }

    /**
     * 防具値と防具エンチャは無視しつつ、耐性ポーションだけは効かせる。
     * ( {@code LivingEntity#getDamageAfterMagicAbsorb} の耐性部分と同じ計算 )
     */
    private static float applyResistanceOnly(LivingEntity target, DamageSource source, float amount) {
        if (source != null && (source.is(DamageTypeTags.BYPASSES_EFFECTS)
                || source.is(DamageTypeTags.BYPASSES_RESISTANCE))) {
            return amount;
        }
        MobEffectInstance resistance = target.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (resistance == null) return amount;

        int reduction = (resistance.getAmplifier() + 1) * 5;
        int remaining = 25 - reduction;
        if (remaining <= 0) return 0.0F;
        return Math.max(amount * remaining / 25.0F, 0.0F);
    }
}
