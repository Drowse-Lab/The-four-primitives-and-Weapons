package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.InteractionHand;

import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

import java.util.List;
import java.util.Arrays;

public class TyokutouThrustAttackProcedure {

    // 直刀として扱うアイテムのリスト
    private static final List<String> STRAIGHT_SWORD_ITEMS = Arrays.asList(
        "LunaItem"
        // ここに他の直刀アイテムを追加
        // "OtherStraightSwordItem",
    );

    /**
     * アイテムが直刀かどうかを判定
     */
    public static boolean isStraightSword(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemName = stack.getItem().getClass().getSimpleName();
        return STRAIGHT_SWORD_ITEMS.contains(itemName);
    }

    /**
     * 直刀の突き攻撃を実行
     * @param world ワールド
     * @param x X座標
     * @param y Y座標
     * @param z Z座標
     * @param entity 実行するエンティティ（プレイヤー）
     */
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null || !(entity instanceof Player player))
            return;

        double range = 6.0;
        double damage = 25.0;

        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);

        // 突進エフェクト
        if (world instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double d = i * 0.3;
                serverLevel.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    startPos.x + lookVec.x * d,
                    startPos.y + lookVec.y * d,
                    startPos.z + lookVec.z * d,
                    1, 0, 0, 0, 0
                );

                if (i % 2 == 0) {
                    serverLevel.sendParticles(
                        ParticleTypes.CRIT,
                        startPos.x + lookVec.x * d,
                        startPos.y + lookVec.y * d,
                        startPos.z + lookVec.z * d,
                        3, 0.1, 0.1, 0.1, 0.02
                    );
                }
            }
        }

        // 前方への突進移動
        player.setDeltaMovement(player.getDeltaMovement().add(lookVec.scale(2.5)));

        // 直線上の敵を検索
        AABB searchArea = new AABB(
            startPos.x - 1, startPos.y - 1, startPos.z - 1,
            startPos.x + lookVec.x * range + 1,
            startPos.y + lookVec.y * range + 1,
            startPos.z + lookVec.z * range + 1
        ).expandTowards(lookVec.scale(range));

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            target -> {
                if (target == player) return false;

                // 非常に狭い判定（ほぼ直線）
                Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2, 0)
                    .subtract(startPos).normalize();
                double dot = lookVec.dot(toTarget);

                return dot > 0.95 && target.distanceTo(player) <= range;
            });

        // 最も近い敵にヒット
        if (!targets.isEmpty()) {
            targets.sort((a, b) -> Double.compare(
                a.distanceToSqr(player),
                b.distanceToSqr(player)
            ));

            LivingEntity target = targets.get(0);

            // ダメージ計算（エンチャントや効果を適用）
            float actualDamage = calculateDamage(player, target, (float)damage);
            target.hurt(DamageSource.playerAttack(player), actualDamage);

            // 武器特殊効果を適用
            applyWeaponEffects(player, target, actualDamage);

            // 貫通エフェクト
            if (world instanceof ServerLevel serverLevel) {
                Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);

                serverLevel.sendParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    targetPos.x, targetPos.y, targetPos.z,
                    20, 0.3, 0.3, 0.3, 0.1
                );

                serverLevel.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    targetPos.x, targetPos.y, targetPos.z,
                    5, 0.4, 0.4, 0.4, 0
                );

                // 貫通ラインエフェクト
                for (int i = 0; i < 10; i++) {
                    Vec3 particlePos = targetPos.add(
                        lookVec.x * (i - 5) * 0.3,
                        lookVec.y * (i - 5) * 0.3,
                        lookVec.z * (i - 5) * 0.3
                    );
                    serverLevel.sendParticles(
                        ParticleTypes.ENCHANTED_HIT,
                        particlePos.x, particlePos.y, particlePos.z,
                        2, 0, 0, 0, 0
                    );
                }
            }

            // 強力なノックバック
            target.setDeltaMovement(lookVec.scale(2.5).add(0, 0.4, 0));

            // サウンド
            if (world instanceof Level level) {
                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5f, 0.8f);
                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.2f, 1.0f);
            }
        }

        // 突進音
        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.3f, 1.5f);
        }
    }

    /**
     * 通常の突き攻撃（右クリック用）
     */
    public static void executeNormalThrust(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null || !(entity instanceof Player player))
            return;

        double range = 4.0;
        double damage = 12.0;

        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);

        // エフェクト
        if (world instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 8; i++) {
                double d = i * 0.5;
                serverLevel.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    startPos.x + lookVec.x * d,
                    startPos.y + lookVec.y * d,
                    startPos.z + lookVec.z * d,
                    1, 0, 0, 0, 0
                );
            }
        }

        // 敵を検索
        AABB searchArea = new AABB(
            startPos.x - 0.5, startPos.y - 0.5, startPos.z - 0.5,
            startPos.x + lookVec.x * range + 0.5,
            startPos.y + lookVec.y * range + 0.5,
            startPos.z + lookVec.z * range + 0.5
        );

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            target -> {
                if (target == player) return false;

                Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2, 0)
                    .subtract(startPos).normalize();
                double dot = lookVec.dot(toTarget);

                return dot > 0.9 && target.distanceTo(player) <= range;
            });

        if (!targets.isEmpty()) {
            LivingEntity target = targets.get(0);

            // ダメージ計算（エンチャントや効果を適用）
            float actualDamage = calculateDamage(player, target, (float)damage);
            target.hurt(DamageSource.playerAttack(player), actualDamage);

            // 武器特殊効果を適用
            applyWeaponEffects(player, target, actualDamage);

            // ノックバック
            target.setDeltaMovement(lookVec.scale(0.8).add(0, 0.2, 0));

            // エフェクト
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    8, 0.2, 0.2, 0.2, 0.05
                );
            }

            // サウンド
            if (world instanceof Level level) {
                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.2f);
            }
        }

        // 突き音
        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.8f);
        }
    }

    /**
     * ダメージ計算（エンチャント、ポーション効果、属性を含む）
     */
    private static float calculateDamage(Player player, LivingEntity target, float baseDamage) {
        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (weapon.isEmpty()) {
            weapon = player.getItemInHand(InteractionHand.OFF_HAND);
        }

        float damage = baseDamage;

        // 武器の基本攻撃力を取得
        if (weapon.getItem() instanceof SwordItem swordItem) {
            damage += swordItem.getDamage();
        }

        // プレイヤーの攻撃力属性
        double attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        damage += (float)(attackDamage - 1.0); // 基本値1.0を引く

        // 攻撃力上昇エフェクト
        if (player.hasEffect(MobEffects.DAMAGE_BOOST)) {
            int amplifier = player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier();
            damage += damage * (0.3f * (amplifier + 1));
        }

        // 弱体化エフェクト
        if (player.hasEffect(MobEffects.WEAKNESS)) {
            int amplifier = player.getEffect(MobEffects.WEAKNESS).getAmplifier();
            damage -= damage * (0.2f * (amplifier + 1));
        }

        // シャープネスエンチャント
        int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, weapon);
        if (sharpnessLevel > 0) {
            damage += 0.5f * sharpnessLevel + 0.5f;
        }

        // アンデッド特攻
        if (target.getMobType() == MobType.UNDEAD) {
            int smiteLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, weapon);
            if (smiteLevel > 0) {
                damage += 2.5f * smiteLevel;
            }
        }

        // 虫特攻
        if (target.getMobType() == MobType.ARTHROPOD) {
            int baneLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS, weapon);
            if (baneLevel > 0) {
                damage += 2.5f * baneLevel;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 + 10 * baneLevel, 3));
            }
        }

        // クリティカル判定
        if (player.fallDistance > 0.0F && !player.isOnGround() && !player.onClimbable() &&
            !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger()) {
            damage *= 1.5f;

            // クリティカルエフェクト
            if (player.level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    15, 0.2, 0.2, 0.2, 0.1);
            }

            player.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        return damage;
    }

    /**
     * 武器特殊効果を適用
     */
    private static void applyWeaponEffects(Player player, LivingEntity target, float damage) {
        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (weapon.isEmpty()) {
            weapon = player.getItemInHand(InteractionHand.OFF_HAND);
        }

        // 火属性エンチャント
        int fireAspect = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, weapon);
        if (fireAspect > 0) {
            target.setSecondsOnFire(fireAspect * 4);
        }

        // ノックバックエンチャント
        int knockback = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, weapon);
        if (knockback > 0) {
            Vec3 knockbackVec = new Vec3(
                target.getX() - player.getX(),
                0,
                target.getZ() - player.getZ()
            ).normalize().scale(knockback * 0.5);

            target.setDeltaMovement(
                target.getDeltaMovement().add(knockbackVec.x, 0.1, knockbackVec.z)
            );
        }

        // 略奪エンチャント（経験値増加）
        int looting = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, weapon);
        if (looting > 0 && target.getHealth() <= 0) {
            // ドロップ増加の処理は別途
        }

        // Killエンチャント
        if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), weapon) > 0) {
            if (Math.random() < 0.03) { // 3%の確率で即死
                target.hurt(DamageSource.MAGIC, target.getMaxHealth() * 2);

                if (player.level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                        target.getX(), target.getY() + 1, target.getZ(),
                        20, 0.5, 0.5, 0.5, 0.1);
                }

                player.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 2.0f);
            }
        }
    }
}