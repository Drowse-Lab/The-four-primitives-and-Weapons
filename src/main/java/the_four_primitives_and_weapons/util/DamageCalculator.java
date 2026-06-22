package the_four_primitives_and_weapons.util;

import the_four_primitives_and_weapons.util.VersionHelper;
import the_four_primitives_and_weapons.damage.SpecialDebuffHandler;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.google.common.collect.Multimap;
import java.util.Collection;

/**
 * 統一的なダメージ計算とエフェクト適用を行うユーティリティクラス
 */
public class DamageCalculator {

    // チャージ技のダメージブースト用スレッドローカル
    // MotionExecutor がチャージ技を発動する前にセットし、終了時にクリアする。
    // セットされている間、calculateDamage は全ての技に一律のチャージ倍率を適用する。
    private static final ThreadLocal<Float> CHARGE_CONTEXT = new ThreadLocal<>();

    // 武器の得意技: 攻撃力+20%
    private static final ThreadLocal<Boolean> PREFERRED_CONTEXT = new ThreadLocal<>();
    // 武器の不得意技: 攻撃力-40%
    private static final ThreadLocal<Boolean> DISLIKED_CONTEXT = new ThreadLocal<>();

    // Attack cooldown (クロスヘアの白いゲージ) によるダメージスケール用スレッドローカル
    // スキル発動時に MotionExecutor が getAttackStrengthScale でキャプチャしてセット、
    // 終了時にクリアする。多段ヒットでもセッション中は同じ scale が使われる。
    private static final ThreadLocal<Float> COOLDOWN_SCALE_CONTEXT = new ThreadLocal<>();

    /**
     * チャージ技コンテキストをセット。発動前に MotionExecutor から呼ばれる。
     * @param chargePercent 0.0～1.0
     */
    public static void setChargeContext(float chargePercent) {
        if (chargePercent > 0.0f) CHARGE_CONTEXT.set(chargePercent);
    }

    /**
     * チャージ技コンテキストをクリア。発動後に MotionExecutor から呼ばれる。
     */
    public static void clearChargeContext() {
        CHARGE_CONTEXT.remove();
    }

    /**
     * 得意技コンテキストをセット（攻撃力+20%）。MotionExecutor から発動前に呼ばれる。
     */
    public static void setPreferredContext() {
        PREFERRED_CONTEXT.set(Boolean.TRUE);
    }

    public static void clearPreferredContext() {
        PREFERRED_CONTEXT.remove();
    }

    /**
     * 不得意技コンテキストをセット（攻撃力-40%）。MotionExecutor から発動前に呼ばれる。
     */
    public static void setDislikedContext() {
        DISLIKED_CONTEXT.set(Boolean.TRUE);
    }

    public static void clearDislikedContext() {
        DISLIKED_CONTEXT.remove();
    }

    /**
     * Attack cooldown スケールをセット（スキル発動時に呼ばれる）。
     * @param scale 0.0 (0%) ～ 1.0 (100%)
     */
    public static void setCooldownScaleContext(float scale) {
        COOLDOWN_SCALE_CONTEXT.set(scale);
    }

    public static void clearCooldownScaleContext() {
        COOLDOWN_SCALE_CONTEXT.remove();
    }

    /**
     * 通常攻撃と同じノックバックを target に適用。
     * vanilla Player.attack と同じ強度 (0.4 + Knockback enchantment * 0.5)。
     * 武器が saya (鞘) の場合は追加 +1.5 で強いノックバック。
     */
    public static void applyNormalKnockback(LivingEntity attacker, LivingEntity target, ItemStack weapon) {
        int kbLevel = (weapon != null && !weapon.isEmpty())
            ? EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, weapon) : 0;
        double strength = 0.4 + kbLevel * 0.5;
        if (weapon != null && !weapon.isEmpty()
                && the_four_primitives_and_weapons.events.DodgeAndBattouHandler.isSaya(weapon)) {
            strength += 1.5;
        }
        float yawRad = attacker.getYRot() * ((float) Math.PI / 180F);
        target.knockback(strength,
            (double) net.minecraft.util.Mth.sin(yawRad),
            (double) (-net.minecraft.util.Mth.cos(yawRad)));
    }

    /**
     * 現在の Attack cooldown スケール context を取得（複数tick skill が damage に焼き込むのに使う）。
     * @return context 値、未セットなら null
     */
    public static Float getCooldownScaleContext() {
        return COOLDOWN_SCALE_CONTEXT.get();
    }

    /**
     * 武器のダメージを計算する（エンチャント、ポーション効果、属性を含む）
     * @param attacker 攻撃者
     * @param target ターゲット
     * @param baseDamage 基本ダメージ
     * @param weapon 使用武器（nullの場合は手持ちアイテムを使用）
     * @return 最終ダメージ値
     */
    public static float calculateDamage(LivingEntity attacker, LivingEntity target, float baseDamage, ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) {
            weapon = attacker.getItemInHand(InteractionHand.MAIN_HAND);
        }

        float damage = baseDamage;

        // 武器の攻撃力をアイテムの属性修飾子から取得（SwordItem以外のカスタム武器にも対応）
        Multimap<Attribute, AttributeModifier> modifiers = weapon.getAttributeModifiers(EquipmentSlot.MAINHAND);
        Collection<AttributeModifier> attackModifiers = modifiers.get(Attributes.ATTACK_DAMAGE);
        double weaponDamage = 0;
        for (AttributeModifier modifier : attackModifiers) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                weaponDamage += modifier.getAmount();
            }
        }
        damage += (float) weaponDamage;

        // 攻撃力上昇エフェクト
        if (attacker.hasEffect(MobEffects.DAMAGE_BOOST)) {
            int amplifier = attacker.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier();
            damage += damage * (0.3f * (amplifier + 1));
        }

        // 弱体化エフェクト
        if (attacker.hasEffect(MobEffects.WEAKNESS)) {
            int amplifier = attacker.getEffect(MobEffects.WEAKNESS).getAmplifier();
            damage -= damage * (0.2f * (amplifier + 1));
        }

        // シャープネスエンチャント
        int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, weapon);
        if (sharpnessLevel > 0) {
            damage += 0.5f * sharpnessLevel + 0.5f;
        }

        // アンデッド特攻（Smite）
        if (target.getMobType() == MobType.UNDEAD) {
            int smiteLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, weapon);
            if (smiteLevel > 0) {
                damage += 2.5f * smiteLevel;
            }
        }

        // 虫特攻（Bane of Arthropods）
        if (target.getMobType() == MobType.ARTHROPOD) {
            int baneLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS, weapon);
            if (baneLevel > 0) {
                damage += 2.5f * baneLevel;
                // スローネス → attribute modifier ベース (牛乳で消えない / モヤ無し)
                SpecialDebuffHandler.applySlowness(target, 20 + 10 * baneLevel, 3);
            }
        }

        // チャージ技ボーナス（どんな技でもチャージ発動なら攻撃力UP）
        Float chargePct = CHARGE_CONTEXT.get();
        if (chargePct != null && chargePct > 0.0f) {
            damage *= 1.0f + chargePct * 0.5f;
        }

        // 武器の得意技は攻撃ゲージのみ速くなり、ダメージは通常と変わらない（処理なし）

        // 武器の不得意技ペナルティ（攻撃力 -40%）
        if (Boolean.TRUE.equals(DISLIKED_CONTEXT.get())) {
            damage *= 0.6f;
        }

        // Attack cooldown (クロスヘアゲージ) によるダメージスケール
        // バニラ準拠の式: damage * (0.2 + scale^2 * 0.8)
        //   scale 0.0 →  20% (空ゲージ)
        //   scale 0.5 →  40%
        //   scale 1.0 → 100% (満タン)
        // ※ context がセットされている時のみ適用 (= MotionExecutor 経由のスキル発動時)。
        //   バニラの通常攻撃は Player.attack() 自体が cooldown を適用するため、ここでは何もしない。
        //   複数tick skill は session 開始時にscaleを焼き込むので、tick中は context = null で実行される。
        Float ctxScale = COOLDOWN_SCALE_CONTEXT.get();
        if (ctxScale != null) {
            damage *= 0.2f + ctxScale * ctxScale * 0.8f;
        }

        // クリティカル判定（攻撃者がプレイヤーで落下中の場合）
        if (attacker instanceof Player player) {
            if (player.fallDistance > 0.0F && !player.onGround() && !player.onClimbable() &&
                !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger()) {
                damage *= 1.5f;

                // クリティカルエフェクト
                if (VersionHelper.getLevel(player) instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                        15, 0.2, 0.2, 0.2, 0.1);
                }

                player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }

        return damage;
    }

    /**
     * 武器の特殊効果を適用する
     * @param attacker 攻撃者
     * @param target ターゲット
     * @param damage 与えたダメージ
     * @param weapon 使用武器（nullの場合は手持ちアイテムを使用）
     */
    public static void applyWeaponEffects(LivingEntity attacker, LivingEntity target, float damage, ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) {
            weapon = attacker.getItemInHand(InteractionHand.MAIN_HAND);
        }

        // 火属性エンチャント（Fire Aspect）
        int fireAspect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, weapon);
        if (fireAspect > 0) {
            target.setSecondsOnFire(fireAspect * 4);
        }

        // ノックバックエンチャント
        int knockback = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, weapon);
        if (knockback > 0) {
            Vec3 knockbackVec = new Vec3(
                target.getX() - attacker.getX(),
                0,
                target.getZ() - attacker.getZ()
            ).normalize().scale(knockback * 0.5);

            target.setDeltaMovement(
                target.getDeltaMovement().add(knockbackVec.x, 0.1, knockbackVec.z)
            );
        }

        // Killエンチャントの処理はKillEnchantmentHandlerに一元化（重複防止）

        // 武器固有の特殊効果（アイテム名で判定）
        String weaponName = weapon.getItem().getClass().getSimpleName();

        // RiversOfBloodの吸血効果
        if (weaponName.equals("RiversOfBloodItem")) {
            applyRiversOfBloodEffect(attacker, target, damage);
        }

        // WitherKatanaのウィザー効果
        if (weaponName.equals("WitherKatanaItem")) {
            applyWitherKatanaEffect(attacker, target, damage);
        }

        // DarknessKatanaの暗黒効果
        if (weaponName.equals("DarknessKatanaItem") || weaponName.equals("DarknessItem")) {
            applyDarknessEffect(attacker, target, damage);
        }

        // MagicalKatanaの魔法効果
        if (weaponName.equals("MagicalKatanaItem") || weaponName.equals("MagischesFeenKatanaItem")) {
            applyMagicEffect(attacker, target, damage);
        }

        // 霊刀の怨念効果
        if (weaponName.equals("ReitouItem")) {
            applyReitouEffect(attacker, target, damage);
        }
    }

    /**
     * ダメージを与えてエフェクトを適用する統合メソッド
     * @param attacker 攻撃者
     * @param target ターゲット
     * @param baseDamage 基本ダメージ
     * @param weapon 使用武器（nullの場合は手持ちアイテムを使用）
     * @return 実際に与えたダメージ
     */
    public static float dealDamage(LivingEntity attacker, LivingEntity target, float baseDamage, ItemStack weapon) {
        // ダメージ計算
        float actualDamage = calculateDamage(attacker, target, baseDamage, weapon);

        // ダメージを与える
        DamageSource source = attacker instanceof Player player ?
            player.damageSources().playerAttack(player) : attacker.damageSources().mobAttack(attacker);
        target.hurt(source, actualDamage);

        // 武器エフェクトを適用
        applyWeaponEffects(attacker, target, actualDamage, weapon);

        return actualDamage;
    }

    // 特殊武器効果の個別実装
    private static void applyRiversOfBloodEffect(LivingEntity attacker, LivingEntity target, float damage) {
        // ターゲットが呪われているかチェック
        boolean isCursed = target.hasEffect(MobEffects.WITHER) ||
                           (target.getPersistentData().contains("Feyn") &&
                            "cursed".equals(target.getPersistentData().getString("Feyn")));

        float healAmount = isCursed ? damage * 0.5f : damage * 0.2f;
        attacker.heal(healAmount);

        if (isCursed) {
            // 呪われた敵への追加効果 — Wither は DoT (カスタムダメージ) に、 Weakness は attribute modifier に
            target.hurt(target.damageSources().magic(), damage * 0.3f);
            SpecialDebuffHandler.applyWither(target, 100, 0.5f);
            SpecialDebuffHandler.applyWeakness(target, 200, 1);
        }

        // 血のエフェクト
        if (VersionHelper.getLevel(attacker) instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                10, 0.3, 0.3, 0.3, 0.1);
        }

        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
            SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.2f);
    }

    private static void applyWitherKatanaEffect(LivingEntity attacker, LivingEntity target, float damage) {
        // ターゲットが呪われているかチェック
        boolean isCursed = target.getPersistentData().contains("Feyn") &&
                           "cursed".equals(target.getPersistentData().getString("Feyn"));

        if (isCursed) {
            // 呪われた敵には強化されたウィザー効果 — DoT (カスタムダメージ) で実装
            SpecialDebuffHandler.applyWither(target, 200, 1.0f);
            target.hurt(target.damageSources().wither(), damage * 0.5f);

            // 闇のオーラエフェクト
            if (VersionHelper.getLevel(attacker) instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL,
                    target.getX(), target.getY() + 1, target.getZ(),
                    15, 0.5, 0.5, 0.5, 0.05);
            }
        } else {
            // 通常のウィザー効果 — DoT (カスタムダメージ) で実装
            SpecialDebuffHandler.applyWither(target, 100, 0.5f);
        }

        // ウィザーサウンド
        attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.5f, 1.0f);
    }

    private static void applyDarknessEffect(LivingEntity attacker, LivingEntity target, float damage) {
        // 暗闇効果 (ambient=true / visible=false で swirl 抑制)
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false));

        // 追加の闇ダメージ
        target.hurt(target.damageSources().magic(), damage * 0.2f);

        // 暗黒エフェクト
        if (VersionHelper.getLevel(attacker) instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                target.getX(), target.getY() + 1, target.getZ(),
                20, 0.5, 0.5, 0.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                10, 0.3, 0.3, 0.3, 0.05);
        }

        attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.5f, 0.5f);
    }

    private static void applyMagicEffect(LivingEntity attacker, LivingEntity target, float damage) {
        // 魔法ダメージのみ（ランダム効果を削除）
        target.hurt(target.damageSources().magic(), damage * 0.3f);

        // 魔法エフェクト
        if (VersionHelper.getLevel(attacker) instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                20, 0.5, 0.5, 0.5, 0.1);
            serverLevel.sendParticles(ParticleTypes.WITCH,
                target.getX(), target.getY() + 1, target.getZ(),
                10, 0.3, 0.3, 0.3, 0.05);
        }

        attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 1.2f);
    }

    private static void applyReitouEffect(LivingEntity attacker, LivingEntity target, float damage) {
        // 怨念の呪いチェック
        boolean isCursed = target.getPersistentData().contains("Feyn") &&
                           "cursed".equals(target.getPersistentData().getString("Feyn"));

        // 怨念ダメージ（呪われた敵には強化）
        float spiritDamage = isCursed ? damage * 0.4f : damage * 0.15f;
        target.hurt(target.damageSources().magic(), spiritDamage);

        // 呪い付与
        target.getPersistentData().putString("Feyn", "cursed");

        // 黒いモヤエフェクト
        if (VersionHelper.getLevel(attacker) instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                15, 0.4, 0.4, 0.4, 0.05);
            serverLevel.sendParticles(ParticleTypes.SOUL,
                target.getX(), target.getY() + 1, target.getZ(),
                5, 0.3, 0.3, 0.3, 0.02);
        }

        attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.WITHER_SKELETON_HURT, SoundSource.PLAYERS, 0.5f, 0.5f);
    }
}