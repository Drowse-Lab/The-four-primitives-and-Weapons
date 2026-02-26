package minecraftarmorweapon.util;

import minecraftarmorweapon.util.VersionHelper;

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
                // スローネス効果も付与
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 + 10 * baneLevel, 3));
            }
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
        if (weaponName.equals("WitherKatanaItem") || weaponName.equals("MotoWitherKatanaItem")) {
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
            // 呪われた敵への追加効果
            target.hurt(target.damageSources().magic(), damage * 0.3f);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
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
            // 呪われた敵には強化されたウィザー効果
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));
            target.hurt(target.damageSources().wither(), damage * 0.5f);

            // 闇のオーラエフェクト
            if (VersionHelper.getLevel(attacker) instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL,
                    target.getX(), target.getY() + 1, target.getZ(),
                    15, 0.5, 0.5, 0.5, 0.05);
            }
        } else {
            // 通常のウィザー効果
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        }

        // ウィザーサウンド
        attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.5f, 1.0f);
    }

    private static void applyDarknessEffect(LivingEntity attacker, LivingEntity target, float damage) {
        // 暗闇効果
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));

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
}