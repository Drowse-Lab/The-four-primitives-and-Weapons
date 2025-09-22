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
     * 直刀の突き攻撃を実行（強化版・チャージ攻撃用）
     * @param world ワールド
     * @param x X座標
     * @param y Y座標
     * @param z Z座標
     * @param entity 実行するエンティティ（プレイヤー）
     */
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        if (entity == null || !(entity instanceof Player player))
            return;

        double range = 8.0;  // 6.0 → 8.0に増加
        double damage = 35.0;  // 25.0 → 35.0に増加

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

        // 前方への強力な突進移動
        player.setDeltaMovement(player.getDeltaMovement().add(lookVec.scale(3.5)));  // 2.5 → 3.5に増加

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

        // チャージ攻撃では貫通（全ての敵にダメージ）
        for (LivingEntity target : targets) {

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

            // 超強力なノックバック（チャージ攻撃）
            target.setDeltaMovement(lookVec.scale(4.0).add(0, 0.6, 0));  // ノックバック強化

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
     * チャージ強化突き攻撃（チャージ率に応じて威力増加）
     * @param world ワールド
     * @param x X座標
     * @param y Y座標
     * @param z Z座標
     * @param entity 実行するエンティティ（プレイヤー）
     * @param chargePercent チャージ率（0.0～1.0）
     */
    public static void executeChargedThrust(LevelAccessor world, double x, double y, double z, Entity entity, float chargePercent) {
        executeChargedThrust(world, x, y, z, entity, chargePercent, false);
    }

    /**
     * チャージ強化突き攻撃（チャージ率に応じて威力増加）
     * @param world ワールド
     * @param x X座標
     * @param y Y座標
     * @param z Z座標
     * @param entity 実行するエンティティ（プレイヤー）
     * @param chargePercent チャージ率（0.0～1.0）
     * @param isCooldown クールダウン中かどうか
     */
    public static void executeChargedThrust(LevelAccessor world, double x, double y, double z, Entity entity, float chargePercent, boolean isCooldown) {
        if (entity == null || !(entity instanceof Player player))
            return;

        // チャージ率に応じてパラメータを強化
        double range = 8.0 + chargePercent * 4.0;  // 8.0～12.0
        double damage = 35.0 + chargePercent * 20.0;  // 35.0～55.0
        double thrustPower = 3.5 + chargePercent * 2.0;  // 3.5～5.5

        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);

        // 超強力な突進エフェクト
        if (world instanceof ServerLevel serverLevel) {
            // メインのエフェクトライン
            for (int i = 0; i < 30; i++) {
                double d = i * 0.4;
                serverLevel.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    startPos.x + lookVec.x * d,
                    startPos.y + lookVec.y * d,
                    startPos.z + lookVec.z * d,
                    2, 0, 0, 0, 0
                );

                // チャージ最大時は追加エフェクト
                if (chargePercent >= 1.0f && i % 2 == 0) {
                    // END_RODを複数生成して確実に表示
                    serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        startPos.x + lookVec.x * d,
                        startPos.y + lookVec.y * d,
                        startPos.z + lookVec.z * d,
                        3, 0.15, 0.15, 0.15, 0.005
                    );

                    // WAX_ONパーティクルも追加（より見やすい）
                    serverLevel.sendParticles(
                        ParticleTypes.WAX_ON,
                        startPos.x + lookVec.x * d,
                        startPos.y + lookVec.y * d,
                        startPos.z + lookVec.z * d,
                        2, 0.1, 0.1, 0.1, 0
                    );

                    if (i % 4 == 0) {
                        serverLevel.sendParticles(
                            ParticleTypes.ELECTRIC_SPARK,
                            startPos.x + lookVec.x * d,
                            startPos.y + lookVec.y * d,
                            startPos.z + lookVec.z * d,
                            5, 0.25, 0.25, 0.25, 0.03
                        );
                    }
                }
            }
        }

        // 超強力な突進移動
        player.setDeltaMovement(player.getDeltaMovement().add(lookVec.scale(thrustPower)));

        // 広めの判定で貫通攻撃
        AABB searchArea = new AABB(
            startPos.x - 1.5, startPos.y - 1.5, startPos.z - 1.5,
            startPos.x + lookVec.x * range + 1.5,
            startPos.y + lookVec.y * range + 1.5,
            startPos.z + lookVec.z * range + 1.5
        ).expandTowards(lookVec.scale(range));

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            target -> {
                if (target == player) return false;

                Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2, 0)
                    .subtract(startPos).normalize();
                double dot = lookVec.dot(toTarget);

                // チャージ率に応じて判定を少し広げる
                double angleThreshold = 0.95 - chargePercent * 0.05;  // 0.95～0.90
                return dot > angleThreshold && target.distanceTo(player) <= range;
            });

        // 全ての敵を貫通
        for (LivingEntity target : targets) {
            float actualDamage = calculateDamage(player, target, (float)damage);
            target.hurt(DamageSource.playerAttack(player), actualDamage);

            applyWeaponEffects(player, target, actualDamage);

            // ターゲット位置にダメージエフェクト
            if (world instanceof ServerLevel serverLevel) {
                Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);

                serverLevel.sendParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    targetPos.x, targetPos.y, targetPos.z,
                    30, 0.5, 0.5, 0.5, 0.1
                );
            }

            // 超強力なノックバック
            double knockbackPower = 4.0 + chargePercent * 2.0;  // 4.0～6.0
            target.setDeltaMovement(lookVec.scale(knockbackPower).add(0, 0.6 + chargePercent * 0.4, 0));

            // チャージ最大時は追加効果
            if (chargePercent >= 1.0f) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));  // スタン効果
                target.setSecondsOnFire(5);  // 燃焼
            }

            // サウンド
            if (world instanceof Level level) {
                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5f + chargePercent * 0.5f, 0.7f);
                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.2f + chargePercent * 0.3f, 0.9f);
            }
        }

        // ビームエフェクト（クールダウン中は直線ビームのみ）
        if (world instanceof ServerLevel serverLevel) {
            if (isCooldown) {
                // クールダウン中は直線ビームのみ
                createStraightBeams(serverLevel, startPos, lookVec, player, chargePercent, range);
            } else {
                // 通常の曲がるビームエフェクト
                if (!targets.isEmpty()) {
                    // 各ターゲットに対してビームを発射
                    for (LivingEntity target : targets) {
                        Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
                        createCurvingBeamsToTarget(serverLevel, targetPos, lookVec, player, chargePercent);
                    }
                } else {
                    // ターゲットがいない場合は視線方向の複数の地点に向けてビームを発射
                    createCurvingBeamsToDirection(serverLevel, lookVec, player, chargePercent, range);
                }
            }
        }

        // 突進音（強化版）
        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.5f + chargePercent * 0.5f, 1.2f);
            if (chargePercent >= 1.0f) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.5f);
            }
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
            if (Math.random() < 1.00) { // 3%の確率で即死
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

    /**
     * クールダウン中の直線ビームエフェクトを作成
     * @param serverLevel サーバーレベル
     * @param startPos 開始位置
     * @param lookVec プレイヤーの視線方向
     * @param player プレイヤー
     * @param chargePercent チャージ率
     * @param range 射程距離
     */
    private static void createStraightBeams(ServerLevel serverLevel, Vec3 startPos, Vec3 lookVec, Player player, float chargePercent, double range) {
        // ビームの数（少なめ）
        int beamCount = (int)(3 + chargePercent * 2); // 3～5本

        Vec3 playerPos = player.position().add(0, player.getEyeHeight() * 0.8, 0);
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
        Vec3 upVec = lookVec.cross(rightVec).normalize();

        for (int i = 0; i < beamCount; i++) {
            // ビームの開始位置（プレイヤーの周囲から）
            double angle = Math.PI * 2 * i / beamCount;
            double radius = 0.5 + Math.random() * 0.3;

            Vec3 beamStart = playerPos
                .add(Math.cos(angle) * radius, Math.sin(angle) * radius * 0.5, Math.sin(angle) * radius);

            // ビームの終点（視線方向の直線上）
            double beamRange = range + Math.random() * 2;
            Vec3 beamEnd = beamStart.add(lookVec.scale(beamRange));

            // 直線に沿ってパーティクルを配置
            int particleCount = 40 + (int)(chargePercent * 20);
            for (int j = 0; j < particleCount; j++) {
                float t = (float)j / (particleCount - 1);
                Vec3 particlePos = beamStart.add(beamEnd.subtract(beamStart).scale(t));

                // シンプルなパーティクル（クールダウン中なので控えめ）
                if (j % 2 == 0) {
                    serverLevel.sendParticles(
                        ParticleTypes.CRIT,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0, 0, 0, 0
                    );
                }

                // 一定間隔で追加エフェクト
                if (j % 10 == 0) {
                    serverLevel.sendParticles(
                        ParticleTypes.ENCHANTED_HIT,
                        particlePos.x, particlePos.y, particlePos.z,
                        2, 0.1, 0.1, 0.1, 0.01
                    );
                }
            }

            // ビームの軌跡に沿って追加ダメージ判定（弱め）
            checkBeamDamage(serverLevel, beamStart, beamEnd, player, chargePercent * 5.0f);
        }

        // 中心から直線的に広がるエフェクト
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            serverLevel.sendParticles(
                ParticleTypes.SWEEP_ATTACK,
                playerPos.x + Math.cos(angle) * 1.5,
                playerPos.y,
                playerPos.z + Math.sin(angle) * 1.5,
                1, 0, 0, 0, 0
            );
        }
    }

    /**
     * 特定のターゲットに向かう曲がるビームエフェクトを作成
     * @param serverLevel サーバーレベル
     * @param targetPos ターゲットの位置
     * @param lookVec プレイヤーの視線方向
     * @param player プレイヤー
     * @param chargePercent チャージ率
     */
    private static void createCurvingBeamsToTarget(ServerLevel serverLevel, Vec3 targetPos, Vec3 lookVec, Player player, float chargePercent) {
        // ビームの数（チャージ率に応じて増加）
        int beamCount = (int)(6 + chargePercent * 8); // 6～14本に調整

        Vec3 playerPos = player.position().add(0, player.getEyeHeight() * 0.8, 0);

        // プレイヤーの右ベクトルと上ベクトルを計算
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
        Vec3 upVec = lookVec.cross(rightVec).normalize();

        for (int i = 0; i < beamCount; i++) {
            // ビームの開始位置（プレイヤーの左右から発生）
            double sideOffset = (Math.random() - 0.5) * 2.0; // -1.0 ~ 1.0 で左右に配置
            double forwardOffset = Math.random() * -0.5; // 少し後ろから
            double heightOffset = (Math.random() - 0.5) * 1.0; // 上下にばらつき

            // 左右交互に、さらにランダムに広がる
            boolean isLeftSide = i % 2 == 0;
            double horizontalSpread = (isLeftSide ? -1 : 1) * (1.5 + Math.random() * 1.5);

            Vec3 beamStart = playerPos
                .add(rightVec.scale(horizontalSpread))
                .add(lookVec.scale(forwardOffset))
                .add(0, heightOffset, 0);

            // ビームの終点（ターゲット位置）
            Vec3 beamEnd = targetPos.add(
                (Math.random() - 0.5) * 0.5,
                (Math.random() - 0.5) * 0.5,
                (Math.random() - 0.5) * 0.5
            );

            // ベジェ曲線のコントロールポイント（プレイヤーから外側に広がってから敵に収束）
            Vec3 midPoint = beamStart.add(beamEnd).scale(0.5);

            // 最初は外側に大きく広がる
            Vec3 controlPoint1 = beamStart.add(
                rightVec.scale(horizontalSpread * 1.5)  // さらに外側に
            ).add(
                lookVec.scale(2.0)  // 少し前方に
            ).add(
                upVec.scale((Math.random() - 0.5) * 2)
            );

            // 敵の少し手前で曲がる
            Vec3 controlPoint2 = beamEnd.add(
                lookVec.scale(-1.0)  // 敵の手前
            ).add(
                rightVec.scale((Math.random() - 0.5) * 1)
            ).add(
                upVec.scale((Math.random() - 0.5) * 0.5)
            );

            // ベジェ曲線に沿ってパーティクルを配置（密度を上げる）
            int particleCount = 50 + (int)(chargePercent * 30);  // 50-80個に増加
            for (int j = 0; j < particleCount; j++) {
                float t = (float)j / (particleCount - 1);

                // 3次ベジェ曲線の計算
                Vec3 particlePos = bezierCubic(beamStart, controlPoint1, controlPoint2, beamEnd, t);

                // パーティクルの種類（チャージ率で変化）
                if (chargePercent >= 1.0f) {
                    // 最大チャージ時は特別なエフェクト
                    // パーティクルを間引いて表示（パケット負荷軽減）
                    if (j % 2 == 0) {  // 半分に間引く
                        serverLevel.sendParticles(
                            ParticleTypes.END_ROD,
                            particlePos.x, particlePos.y, particlePos.z,
                            1, 0.02, 0.02, 0.02, 0
                        );
                    }

                    // 追加の輝きエフェクト（間隔を調整）
                    if (j % 8 == 0) {  // 間隔を広げて重なりを減らす
                        serverLevel.sendParticles(
                            ParticleTypes.ELECTRIC_SPARK,
                            particlePos.x, particlePos.y, particlePos.z,
                            2, 0.1, 0.1, 0.1, 0.01
                        );
                    }
                } else {
                    serverLevel.sendParticles(
                        ParticleTypes.ENCHANTED_HIT,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.02, 0.02, 0.02, 0
                    );
                }
            }

            // ビームの軌跡に沿って追加ダメージ判定
            checkBeamDamage(serverLevel, beamStart, beamEnd, player, chargePercent * 10.0f);
        }

        // プレイヤーの周りから発生するエフェクト
        for (int i = 0; i < 360; i += 30) {
            double rad = Math.toRadians(i);
            serverLevel.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                playerPos.x + Math.cos(rad) * 2,
                playerPos.y,
                playerPos.z + Math.sin(rad) * 2,
                1, 0, 0.1, 0, 0.02
            );
        }

        // ターゲット位置にインパクトエフェクト
        serverLevel.sendParticles(
            ParticleTypes.EXPLOSION,
            targetPos.x, targetPos.y, targetPos.z,
            3, 0.5, 0.5, 0.5, 0
        );
        serverLevel.sendParticles(
            ParticleTypes.FLASH,
            targetPos.x, targetPos.y, targetPos.z,
            1, 0, 0, 0, 0
        );
    }

    /**
     * 視線方向に向かう曲がるビームエフェクトを作成（敵がいない場合）
     * @param serverLevel サーバーレベル
     * @param lookVec プレイヤーの視線方向
     * @param player プレイヤー
     * @param chargePercent チャージ率
     * @param range 射程距離
     */
    private static void createCurvingBeamsToDirection(ServerLevel serverLevel, Vec3 lookVec, Player player, float chargePercent, double range) {
        // ビームの数（チャージ率に応じて増加）
        int beamCount = (int)(8 + chargePercent * 10); // 8～18本に調整

        Vec3 playerPos = player.position().add(0, player.getEyeHeight() * 0.8, 0);

        // プレイヤーの右ベクトルと上ベクトルを計算
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
        Vec3 upVec = lookVec.cross(rightVec).normalize();

        for (int i = 0; i < beamCount; i++) {
            // ビームの開始位置（プレイヤーの左右から発生）
            boolean isLeftSide = i % 2 == 0;
            double horizontalSpread = (isLeftSide ? -1 : 1) * (1.5 + Math.random() * 2.0);
            double heightOffset = (Math.random() - 0.5) * 1.5;

            Vec3 beamStart = playerPos
                .add(rightVec.scale(horizontalSpread))
                .add(lookVec.scale(Math.random() * -0.5))
                .add(0, heightOffset, 0);

            // ビームの最終目標地点（視線方向にランダムに散らばる）
            double targetDistance = range + Math.random() * 5;
            double spreadAngle = Math.toRadians(15 + Math.random() * 10); // 15-25度の広がり
            double rotationAngle = Math.random() * Math.PI * 2;

            // 円錐状に広がるように終点を計算
            Vec3 spreadOffset = rightVec.scale(Math.sin(spreadAngle) * Math.cos(rotationAngle) * targetDistance * 0.3)
                .add(upVec.scale(Math.sin(spreadAngle) * Math.sin(rotationAngle) * targetDistance * 0.3));

            Vec3 beamEnd = playerPos
                .add(lookVec.scale(targetDistance))
                .add(spreadOffset);

            // 屈折しながら飛ぶベジェ曲線のコントロールポイント
            double refractCount = 2 + Math.random() * 2; // 2-4回屈折

            // 第1コントロールポイント（最初の屈折）
            Vec3 controlPoint1 = beamStart
                .add(rightVec.scale(horizontalSpread * 2.0))
                .add(lookVec.scale(targetDistance * 0.3))
                .add(upVec.scale((Math.random() - 0.5) * 3));

            // 第2コントロールポイント（2回目の屈折）
            Vec3 controlPoint2 = playerPos
                .add(lookVec.scale(targetDistance * 0.7))
                .add(rightVec.scale((Math.random() - 0.5) * targetDistance * 0.4))
                .add(upVec.scale((Math.random() - 0.5) * 2));

            // ベジェ曲線に沿ってパーティクルを配置（密度を上げる）
            int particleCount = 60 + (int)(chargePercent * 40);  // 60-100個に増加
            for (int j = 0; j < particleCount; j++) {
                float t = (float)j / (particleCount - 1);

                // 3次ベジェ曲線の計算
                Vec3 particlePos = bezierCubic(beamStart, controlPoint1, controlPoint2, beamEnd, t);

                // パーティクルの種類（進行に応じて変化）
                if (t < 0.3) {
                    // 開始部分
                    serverLevel.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0, 0, 0, 0
                    );
                } else if (t < 0.7) {
                    // 中間部分
                    if (chargePercent >= 1.0f) {
                        // END_RODを間引いて表示（パケット負荷軽減）
                        if (j % 3 == 0) {  // 3分の1に間引く
                            serverLevel.sendParticles(
                                ParticleTypes.END_ROD,
                                particlePos.x, particlePos.y, particlePos.z,
                                1, 0.02, 0.02, 0.02, 0
                            );
                        }
                    } else {
                        serverLevel.sendParticles(
                            ParticleTypes.ENCHANTED_HIT,
                            particlePos.x, particlePos.y, particlePos.z,
                            1, 0.02, 0.02, 0.02, 0
                        );
                    }
                } else {
                    // 終端部分（散らばる）
                    serverLevel.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        particlePos.x, particlePos.y, particlePos.z,
                        2, 0.2, 0.2, 0.2, 0.02
                    );
                }

                // 屈折ポイントで追加エフェクト
                if (Math.abs(t - 0.33) < 0.02 || Math.abs(t - 0.66) < 0.02) {
                    serverLevel.sendParticles(
                        ParticleTypes.FLASH,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0, 0, 0, 0
                    );
                }
            }

            // ビームの軌跡に沿って追加ダメージ判定
            checkBeamDamage(serverLevel, beamStart, beamEnd, player, chargePercent * 8.0f);
        }

        // プレイヤーの周りから発生する円形エフェクト
        for (int i = 0; i < 360; i += 20) {
            double rad = Math.toRadians(i);
            serverLevel.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                playerPos.x + Math.cos(rad) * 2.5,
                playerPos.y,
                playerPos.z + Math.sin(rad) * 2.5,
                2, 0, 0.1, 0, 0.05
            );
        }
    }

    /**
     * 3次ベジェ曲線の計算
     */
    private static Vec3 bezierCubic(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        float mt = 1 - t;
        float mt2 = mt * mt;
        float mt3 = mt2 * mt;

        return p0.scale(mt3)
            .add(p1.scale(3 * mt2 * t))
            .add(p2.scale(3 * mt * t2))
            .add(p3.scale(t3));
    }

    /**
     * ビームの軌跡上にいる敵に追加ダメージ
     */
    private static void checkBeamDamage(ServerLevel world, Vec3 start, Vec3 end, Player player, float damage) {
        Vec3 direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);

        // ビームの経路上にいるエンティティを検索
        AABB searchArea = new AABB(
            Math.min(start.x, end.x) - 1,
            Math.min(start.y, end.y) - 1,
            Math.min(start.z, end.z) - 1,
            Math.max(start.x, end.x) + 1,
            Math.max(start.y, end.y) + 1,
            Math.max(start.z, end.z) + 1
        );

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;

                // ビームからの距離を計算
                Vec3 toEntity = entity.position().subtract(start);
                double projection = toEntity.dot(direction);

                if (projection < 0 || projection > distance) return false;

                Vec3 closestPoint = start.add(direction.scale(projection));
                double distanceToBeam = entity.position().distanceTo(closestPoint);

                return distanceToBeam <= 1.0; // ビームから1ブロック以内
            });

        for (LivingEntity target : targets) {
            // 追加の魔法ダメージ
            target.hurt(DamageSource.MAGIC, damage);

            // 小さなノックバック
            Vec3 knockback = target.position().subtract(start).normalize().scale(0.3);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback));
        }
    }
}
