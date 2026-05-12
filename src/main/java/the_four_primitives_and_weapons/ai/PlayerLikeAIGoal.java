package the_four_primitives_and_weapons.ai;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.util.DamageCalculator;

import java.util.List;
import java.util.EnumSet;
import java.util.Random;

/**
 * プレイヤーのような動作をするAI Goal
 *
 * このGoalは、ALifeAIBridgeからのアクションを受け取り、
 * プレイヤーと同じような動作（回避、チャージ攻撃など）を実行します
 */
public class PlayerLikeAIGoal extends Goal {

    private final Mob entity;
    private final ALifeAIBridge aiBridge;
    private final int tier;
    private final Random random = new Random();

    // 状態管理
    private ALifeAIBridge.AIAction currentAction = null;
    private int actionTicks = 0;
    private boolean isExecutingAction = false;

    // 落下ダメージ無効時間
    private int fallDamageImmunityTicks = 0;

    // コンボカウンター
    private int comboCounter = 0;
    private long lastAttackTime = 0;

    public PlayerLikeAIGoal(Mob entity, int tier) {
        this.entity = entity;
        this.tier = tier;
        this.aiBridge = new ALifeAIBridge(entity, tier);

        // このGoalは他のGoalと並行して実行可能
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        // 常に実行可能
        return entity.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        // エンティティの生存確認
        if (!entity.isAlive() || entity.isRemoved()) {
            return;
        }

        try {
            // AIを更新してアクションを取得
            currentAction = aiBridge.update();

            // アクションを実行
            if (currentAction != null) {
                executeAction(currentAction);
            }

            // 落下ダメージ無効時間のカウントダウン
            if (fallDamageImmunityTicks > 0) {
                fallDamageImmunityTicks--;
            }

            actionTicks++;
        } catch (Throwable e) {
            System.err.println("[PlayerLikeAI] Critical error in tick for " + entity.getName().getString() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * アクションを実行
     */
    private void executeAction(ALifeAIBridge.AIAction action) {
        try {
            switch (action.action) {
                case "dodge":
                    executeDodge(action);
                    break;
                case "dash_attack":
                    executeDashAttack(action);
                    break;
                case "charge_attack":
                    executeChargeAttack(action);
                    break;
                case "use_weapon_skill":
                    executeWeaponSkill(action);
                    break;
                case "attack":
                    executeNormalAttack(action);
                    break;
                case "move_to_target":
                    moveToTarget(action);
                    break;
                case "move_away":
                    moveAway(action);
                    break;
                case "strafe":
                    strafe(action);
                    break;
                case "heal":
                    executeHeal(action);
                    break;
                case "retreat":
                    executeRetreat(action);
                    break;
                case "guard":
                    executeGuard(action);
                    break;
                case "idle":
                default:
                    // 何もしない
                    break;
            }
        } catch (Throwable e) {
            System.err.println("[PlayerLikeAI] Error executing action " + action.action + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ダッシュ攻撃を実行（DodgeAndBattouHandlerのperformDashAttackと同じ）
     */
    private void executeDashAttack(ALifeAIBridge.AIAction action) {
        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        Level world = VersionHelper.getLevel(entity);
        Vec3 lookVec = target.position().subtract(entity.position()).normalize();
        Vec3 entityPos = entity.position();

        // 竹を破壊する
        breakBambooInPath(entityPos, lookVec, 7.0);

        // 前方への高速移動
        double dashStrength = 1.8;
        entity.setDeltaMovement(entity.getDeltaMovement().add(lookVec.scale(dashStrength)));

        // エフェクト
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;

            // ダッシュ攻撃のエフェクト
            for (int i = 0; i < 10; i++) {
                double d = i * 0.6;
                serverWorld.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    entityPos.x + lookVec.x * d,
                    entityPos.y + 1,
                    entityPos.z + lookVec.z * d,
                    2, 0, 0, 0, 0
                );

                serverWorld.sendParticles(
                    ParticleTypes.CLOUD,
                    entityPos.x + lookVec.x * d,
                    entityPos.y + 0.1,
                    entityPos.z + lookVec.z * d,
                    3, 0.3, 0, 0.3, 0.01
                );
            }
        }

        // 前方の敵に大ダメージ
        double range = 7.0;
        Vec3 endPos = entityPos.add(lookVec.scale(range));
        AABB searchArea = new AABB(entityPos.add(-2, -1, -2), endPos.add(2, 2, 2));

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            targetEntity -> {
                if (targetEntity == entity) return false;
                if (!entity.canAttack(targetEntity)) return false;
                Vec3 toEntity = targetEntity.position().subtract(entityPos).normalize();
                double dot = lookVec.dot(toEntity);
                return dot > -0.2 && entity.distanceTo(targetEntity) <= range;
            });

        ItemStack weapon = entity.getItemInHand(InteractionHand.MAIN_HAND);

        for (LivingEntity targetEntity : targets) {
            float baseDamage = 18.0f;
            DamageCalculator.dealDamage(entity, targetEntity, baseDamage, weapon);

            targetEntity.setDeltaMovement(lookVec.scale(1.5).add(0, 0.4, 0));
        }

        // サウンド
        world.playSound(null, entityPos.x, entityPos.y, entityPos.z,
            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.2f, 1.0f);
        world.playSound(null, entityPos.x, entityPos.y, entityPos.z,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0f, 1.2f);
    }

    /**
     * 回避を実行（プレイヤーの右クリック動作）
     */
    private void executeDodge(ALifeAIBridge.AIAction action) {
        if (action.direction == null) {
            return;
        }

        // 盲目効果時：回避が70%の確率で失敗
        if (entity.hasEffect(MobEffects.BLINDNESS)) {
            if (random.nextDouble() < 0.7) {
                System.out.println("[PlayerLikeAI] " + entity.getName().getString() + " は盲目のため回避に失敗しました");
                return;
            }
        }

        // 混乱効果時：回避方向がランダムになる
        Vec3 dodgeDirection = action.direction;
        if (entity.hasEffect(MobEffects.CONFUSION)) {
            double angle = random.nextDouble() * Math.PI * 2;
            dodgeDirection = new Vec3(Math.cos(angle), 0, Math.sin(angle)).normalize();
            System.out.println("[PlayerLikeAI] " + entity.getName().getString() + " は混乱のため回避方向がずれました");
        }

        Vec3 dodgeVec = dodgeDirection.scale(action.speed);

        // 回避移動（上方向の加速も含む）
        entity.setDeltaMovement(
            dodgeVec.x,
            entity.getDeltaMovement().y + action.verticalBoost,
            dodgeVec.z
        );

        // 落下ダメージ無効（1.5秒 = 30 ticks）
        fallDamageImmunityTicks = 30;

        // エフェクト
        Level world = VersionHelper.getLevel(entity);
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            Vec3 pos = entity.position();

            // 煙のエフェクト
            serverWorld.sendParticles(
                ParticleTypes.CLOUD,
                pos.x, pos.y, pos.z,
                20, 0.3, 0.5, 0.3, 0.05
            );
        }

        // サウンド
        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.ENDER_PEARL_THROW, SoundSource.HOSTILE, 0.8f, 1.5f);
    }

    /**
     * チャージ攻撃を実行（ChargedAttackHandlerと同じロジック）
     * 武器タイプにより異なる攻撃を実行：
     * - 刀（Katana）: 周囲回転斬り
     * - その他: 貫通突き（デフォルト）
     */
    private void executeChargeAttack(ALifeAIBridge.AIAction action) {
        // 盲目効果時：チャージ攻撃が80%の確率で失敗
        if (entity.hasEffect(MobEffects.BLINDNESS)) {
            if (random.nextDouble() < 0.8) {
                System.out.println("[PlayerLikeAI] " + entity.getName().getString() + " は盲目のためチャージ攻撃に失敗しました");
                return;
            }
        }

        // 武器取得して攻撃タイプを判定
        ItemStack weapon = entity.getItemInHand(InteractionHand.MAIN_HAND);
        String weaponName = weapon.getItem().getClass().getSimpleName();
        boolean isKatana = weaponName.contains("Katana") || weaponName.contains("katana");

        if (isKatana) {
            // 刀: 周囲回転斬り（ChargedAttackHandler.performSpinSlashと同じ）
            executeSpinSlash(action);
        } else {
            // デフォルト: 貫通突き（ChargedAttackHandler.performChargedThrustと同じ）
            executeChargedThrust(action);
        }
    }

    /**
     * 貫通突きチャージ攻撃（ChargedAttackHandler.performChargedThrustと同じ）
     */
    private void executeChargedThrust(ALifeAIBridge.AIAction action) {
        // 暗闇効果時：チャージ攻撃の範囲が50%減少
        double rangeMultiplier = 1.0;
        if (entity.hasEffect(MobEffects.DARKNESS)) {
            rangeMultiplier = 0.5;
        }

        Level world = VersionHelper.getLevel(entity);
        Vec3 entityPos = entity.position();

        // 攻撃方向を計算
        Vec3 lookVec;
        if (action.target != null) {
            lookVec = action.target.subtract(entityPos).normalize();
        } else {
            LivingEntity target = entity.getTarget();
            if (target != null) {
                lookVec = target.position().subtract(entityPos).normalize();
            } else {
                lookVec = entity.getLookAngle();
            }
        }

        // 混乱効果時：攻撃方向がランダムにずれる
        if (entity.hasEffect(MobEffects.CONFUSION)) {
            double angleOffset = (random.nextDouble() - 0.5) * Math.PI * 0.5; // ±45度のずれ
            double cos = Math.cos(angleOffset);
            double sin = Math.sin(angleOffset);
            lookVec = new Vec3(
                lookVec.x * cos - lookVec.z * sin,
                lookVec.y,
                lookVec.x * sin + lookVec.z * cos
            ).normalize();
        }

        float chargePercent = action.chargePercent;
        float baseDamage = 15.0f * (1.0f + chargePercent);
        double range = (6.0f + chargePercent * 2.0f) * rangeMultiplier;

        // 竹を破壊する
        breakBambooInPath(entityPos, lookVec, range);

        final Vec3 finalLookVec = lookVec;

        // 貫通突きエフェクト
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;

            for (double d = 0; d <= range; d += 0.3) {
                serverWorld.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    entityPos.x + finalLookVec.x * d,
                    entityPos.y + 1,
                    entityPos.z + finalLookVec.z * d,
                    5, 0.2, 0.2, 0.2, 0.05
                );

                if (chargePercent >= 1.0f) {
                    serverWorld.sendParticles(
                        ParticleTypes.END_ROD,
                        entityPos.x + finalLookVec.x * d,
                        entityPos.y + 1,
                        entityPos.z + finalLookVec.z * d,
                        2, 0.1, 0.1, 0.1, 0
                    );
                }
            }
        }

        // 貫通攻撃（直線上の全ての敵）
        Vec3 endPos = entityPos.add(finalLookVec.scale(range));
        AABB searchArea = new AABB(entityPos, endPos).inflate(1.0);

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            target -> {
                if (target == entity) return false;
                if (!entity.canAttack(target)) return false;
                Vec3 toEntity = target.position().subtract(entityPos);
                double dot = finalLookVec.dot(toEntity.normalize());
                return dot > 0.8 && toEntity.length() <= range;
            });

        ItemStack weapon = entity.getItemInHand(InteractionHand.MAIN_HAND);

        for (LivingEntity target : targets) {
            DamageCalculator.dealDamage(entity, target, baseDamage, weapon);

            // 貫通による吹き飛ばし
            target.setDeltaMovement(finalLookVec.scale(2.0 * chargePercent).add(0, 0.5, 0));

            if (chargePercent >= 1.0f) {
                // 最大チャージで出血効果
                target.setSecondsOnFire(5);
            }
        }

        world.playSound(null, entityPos.x, entityPos.y, entityPos.z,
            SoundEvents.TRIDENT_THUNDER, SoundSource.HOSTILE, 1.0f, 1.0f);
    }

    /**
     * 回転斬りチャージ攻撃（ChargedAttackHandler.performSpinSlashと同じ）
     */
    private void executeSpinSlash(ALifeAIBridge.AIAction action) {
        Level world = VersionHelper.getLevel(entity);
        Vec3 entityPos = entity.position();

        float chargePercent = action.chargePercent;
        float baseDamage = 12.0f * (1.0f + chargePercent * 1.5f);
        double range = 4.0f + chargePercent * 2.0f;

        // 回転斬りエフェクト
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;

            // 複数の円を描く
            for (int ring = 0; ring < 3; ring++) {
                double r = range * (ring + 1) / 3.0;
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    serverWorld.sendParticles(
                        ring == 0 ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.CRIT,
                        entityPos.x + Math.cos(rad) * r,
                        entityPos.y + 1 + ring * 0.3,
                        entityPos.z + Math.sin(rad) * r,
                        1, 0, 0, 0, 0
                    );
                }
            }

            if (chargePercent >= 1.0f) {
                // 最大チャージで追加エフェクト
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * 2 * i / 8;
                    serverWorld.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        entityPos.x + Math.cos(angle) * range,
                        entityPos.y + 1,
                        entityPos.z + Math.sin(angle) * range,
                        10, 0.2, 0.5, 0.2, 0.1
                    );
                }
            }
        }

        // 周囲の敵全てにダメージ
        AABB searchArea = new AABB(
            entityPos.x - range, entityPos.y - 1, entityPos.z - range,
            entityPos.x + range, entityPos.y + 3, entityPos.z + range
        );

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            target -> target != entity && entity.canAttack(target) && entity.distanceTo(target) <= range);

        ItemStack weapon = entity.getItemInHand(InteractionHand.MAIN_HAND);

        for (LivingEntity target : targets) {
            DamageCalculator.dealDamage(entity, target, baseDamage, weapon);

            // 円形ノックバック
            Vec3 knockback = target.position().subtract(entityPos).normalize().scale(1.0 + chargePercent);
            target.setDeltaMovement(knockback.x, 0.4, knockback.z);

            if (chargePercent >= 1.0f) {
                // スタン効果（仮）
                target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
            }
        }

        world.playSound(null, entityPos.x, entityPos.y, entityPos.z,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5f, 0.7f);
    }

    /**
     * 武器スキルを使用（プレイヤーの右クリック動作）
     */
    private void executeWeaponSkill(ALifeAIBridge.AIAction action) {
        // 盲目効果時：スキルが60%の確率で失敗
        if (entity.hasEffect(MobEffects.BLINDNESS)) {
            if (random.nextDouble() < 0.6) {
                System.out.println("[PlayerLikeAI] " + entity.getName().getString() + " は盲目のためスキル使用に失敗しました");
                return;
            }
        }

        // 混乱効果時：スキルが40%の確率で失敗
        if (entity.hasEffect(MobEffects.CONFUSION)) {
            if (random.nextDouble() < 0.4) {
                System.out.println("[PlayerLikeAI] " + entity.getName().getString() + " は混乱のためスキル使用に失敗しました");
                return;
            }
        }

        // スキルタイプに応じた処理
        if ("guard".equals(action.skillType)) {
            // ガードスキル（ReplicaSwordOfLight相当）
            int duration = (int)(action.duration * 20); // 秒をティックに変換

            // GUARDエフェクトを付与
            if (TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get() != null) {
                entity.addEffect(new MobEffectInstance(
                    TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get(),
                    duration,
                    0
                ));
            }

            // LONG_RANGE_WEAPON_CUTエフェクトを付与（ReplicaSwordOfLightの効果）
            if (TheFourPrimitivesAndWeaponsModMobEffects.LONG_RANGE_WEAPON_CUT.get() != null) {
                entity.addEffect(new MobEffectInstance(
                    TheFourPrimitivesAndWeaponsModMobEffects.LONG_RANGE_WEAPON_CUT.get(),
                    2,
                    1
                ));
            }

            // エフェクト
            Level world = VersionHelper.getLevel(entity);
            if (!world.isClientSide) {
                ServerLevel serverWorld = (ServerLevel) world;
                serverWorld.sendParticles(
                    ParticleTypes.ENCHANT,
                    entity.getX(), entity.getY() + 1, entity.getZ(),
                    10, 0.5, 0.5, 0.5, 0.1
                );
            }

            // サウンド
            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 1.0f, 1.0f);
        }
    }

    /**
     * 通常攻撃を実行（ChargedAttackHandlerと同じロジック）
     * 武器タイプにより異なる攻撃を実行：
     * - 刀（Katana）: 三段コンボ斬り（左上、右上、横一文字）
     * - その他: デフォルト攻撃
     */
    private void executeNormalAttack(ALifeAIBridge.AIAction action) {
        LivingEntity target = entity.getTarget();
        if (target == null || entity.distanceTo(target) > 5.0) {
            return;
        }

        // コンボリセット判定（最後の攻撃から1秒以上経過していたらリセット）
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime > 1000) {
            comboCounter = 0;
        }
        lastAttackTime = currentTime;

        // 武器取得して攻撃タイプを判定
        ItemStack weapon = entity.getItemInHand(InteractionHand.MAIN_HAND);
        String weaponName = weapon.getItem().getClass().getSimpleName();
        boolean isKatana = weaponName.contains("Katana") || weaponName.contains("katana");

        if (isKatana) {
            // 刀: 三段コンボ（ChargedAttackHandler.performKatanaComboと同じ）
            executeKatanaCombo(target);
        } else {
            // デフォルト攻撃
            executeDefaultAttack(target, action);
        }
    }

    /**
     * 刀のコンボ攻撃（ChargedAttackHandler.performKatanaComboと同じ）
     */
    private void executeKatanaCombo(LivingEntity target) {
        Level world = VersionHelper.getLevel(entity);
        Vec3 entityPos = entity.position();
        Vec3 lookVec = target.position().subtract(entityPos).normalize();

        // コンボ段階に応じた攻撃
        int combo = comboCounter % 3;

        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;

            // コンボに応じたエフェクト
            if (combo == 0) {
                // 左上から右下への斬撃
                for (int i = -2; i <= 2; i++) {
                    serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        entityPos.x + lookVec.x * 2 - 0.5 + i * 0.2,
                        entityPos.y + 1.5 - i * 0.2,
                        entityPos.z + lookVec.z * 2,
                        1, 0, 0, 0, 0);
                }
            } else if (combo == 1) {
                // 右上から左下への斬撃
                for (int i = -2; i <= 2; i++) {
                    serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        entityPos.x + lookVec.x * 2 + 0.5 - i * 0.2,
                        entityPos.y + 1.5 - i * 0.2,
                        entityPos.z + lookVec.z * 2,
                        1, 0, 0, 0, 0);
                }
            } else {
                // 横一文字斬り
                Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();
                for (int i = -3; i <= 3; i++) {
                    serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        entityPos.x + lookVec.x * 2 + rightVec.x * i * 0.3,
                        entityPos.y + 1,
                        entityPos.z + lookVec.z * 2 + rightVec.z * i * 0.3,
                        1, 0, 0, 0, 0);
                }
            }
        }

        // 攻撃範囲と処理（横に広い範囲）
        double forwardRange = 4.5;  // 前方リーチ
        double horizontalRange = 3.0;  // 横幅を大幅に拡大
        float baseDamage = combo == 2 ? 12.0f : 9.0f;

        // 右ベクトルを計算
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();

        // 横長の攻撃範囲を構築
        Vec3 minPoint = entityPos.add(lookVec.scale(-0.5))
            .add(rightVec.scale(-horizontalRange))
            .add(0, -0.5, 0);
        Vec3 maxPoint = entityPos.add(lookVec.scale(forwardRange))
            .add(rightVec.scale(horizontalRange))
            .add(0, 2.5, 0);

        AABB searchArea = new AABB(minPoint, maxPoint);

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            e -> {
                if (e == entity) return false;
                if (!entity.canAttack(e)) return false;
                Vec3 toEntity = e.position().subtract(entityPos).normalize();
                double dot = lookVec.dot(toEntity);
                // より広い横方向の判定（180度近い範囲）
                return dot > -0.3 && entity.distanceTo(e) <= forwardRange + horizontalRange;
            });

        ItemStack weapon = entity.getItemInHand(InteractionHand.MAIN_HAND);

        for (LivingEntity t : targets) {
            DamageCalculator.dealDamage(entity, t, baseDamage, weapon);

            Vec3 knockback = t.position().subtract(entityPos).normalize().scale(0.4);
            t.setDeltaMovement(t.getDeltaMovement().add(knockback.x, 0.1, knockback.z));
        }

        world.playSound(null, entityPos.x, entityPos.y, entityPos.z,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE,
            1.0f, 0.9f + combo * 0.1f);

        // コンボカウンターを増やす
        comboCounter++;
    }

    /**
     * デフォルト攻撃（武器タイプに応じたエフェクト付き）
     */
    private void executeDefaultAttack(LivingEntity target, ALifeAIBridge.AIAction action) {
        if (target == null || entity.distanceTo(target) > 3.0) {
            return;
        }

        // 通常のダメージ処理
        boolean hitSuccess = entity.doHurtTarget(target);

        if (hitSuccess) {
            Level world = VersionHelper.getLevel(entity);
            Vec3 targetPos = target.position();

            // 武器タイプに応じたエフェクトとノックバック
            if (!world.isClientSide) {
                ServerLevel serverWorld = (ServerLevel) world;

                // 攻撃タイプに応じたパーティクルエフェクト
                switch (action.attackType) {
                    case "iai_slash":
                    case "quick_slash":
                    case "slash":
                        // 斬撃エフェクト
                        serverWorld.sendParticles(
                            ParticleTypes.SWEEP_ATTACK,
                            targetPos.x, targetPos.y + target.getBbHeight() / 2, targetPos.z,
                            2, 0.3, 0.3, 0.3, 0
                        );
                        break;

                    case "thrust":
                    case "pierce":
                    case "charge_thrust":
                        // 突きエフェクト（火花）
                        serverWorld.sendParticles(
                            ParticleTypes.CRIT,
                            targetPos.x, targetPos.y + target.getBbHeight() / 2, targetPos.z,
                            8, 0.2, 0.2, 0.2, 0.1
                        );
                        // 強めのノックバック
                        Vec3 knockback = target.position().subtract(entity.position()).normalize().scale(0.6);
                        target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.15, knockback.z));
                        break;

                    case "overhead_smash":
                    case "cleave":
                        // 叩きつけエフェクト（爆発パーティクル）
                        serverWorld.sendParticles(
                            ParticleTypes.EXPLOSION,
                            targetPos.x, targetPos.y, targetPos.z,
                            1, 0, 0, 0, 0
                        );
                        // 非常に強いノックバック
                        Vec3 smashKnockback = target.position().subtract(entity.position()).normalize().scale(0.8);
                        target.setDeltaMovement(target.getDeltaMovement().add(smashKnockback.x, 0.3, smashKnockback.z));
                        break;

                    case "spin_slash":
                    case "sweep":
                        // 回転斬りエフェクト（複数回の斬撃）
                        for (int i = 0; i < 3; i++) {
                            serverWorld.sendParticles(
                                ParticleTypes.SWEEP_ATTACK,
                                targetPos.x, targetPos.y + target.getBbHeight() / 2, targetPos.z,
                                1, 0.4, 0.4, 0.4, 0
                            );
                        }
                        break;

                    case "downward_cut":
                        // 下段斬りエフェクト
                        serverWorld.sendParticles(
                            ParticleTypes.SWEEP_ATTACK,
                            targetPos.x, targetPos.y + 0.5, targetPos.z,
                            2, 0.3, 0.2, 0.3, 0
                        );
                        break;

                    default:
                        // デフォルトエフェクト
                        serverWorld.sendParticles(
                            ParticleTypes.SWEEP_ATTACK,
                            targetPos.x, targetPos.y + target.getBbHeight() / 2, targetPos.z,
                            1, 0.2, 0.2, 0.2, 0
                        );
                        break;
                }

                // 攻撃音（攻撃タイプに応じて変更）
                if (action.attackType.contains("smash") || action.attackType.contains("cleave")) {
                    world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.0f, 0.8f);
                } else if (action.attackType.contains("thrust") || action.attackType.contains("pierce")) {
                    world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0f, 1.2f);
                } else {
                    world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0f, 1.0f);
                }
            }
        }
    }

    /**
     * ターゲットに向かって移動
     */
    private void moveToTarget(ALifeAIBridge.AIAction action) {
        if (action.target == null || entity.getNavigation() == null) {
            return;
        }

        try {
            // スプリント状態を設定
            entity.setSprinting(action.isSprinting);

            // 移動速度係数を計算
            // Navigation APIは基本速度に対する係数を期待する
            // 通常移動: 1.0倍、スプリント: 1.3倍
            float speedModifier = action.isSprinting ? 1.3f : 1.0f;

            // Navigation APIを安全に呼び出す
            if (entity.getNavigation() != null) {
                entity.getNavigation().moveTo(action.target.x, action.target.y, action.target.z, speedModifier);
            }

            // LookControlを安全に呼び出す
            if (entity.getLookControl() != null) {
                entity.getLookControl().setLookAt(action.target.x, action.target.y, action.target.z);
            }
        } catch (Exception e) {
            System.err.println("[PlayerLikeAI] Error in moveToTarget: " + e.getMessage());
        }
    }

    /**
     * ターゲットから離れる
     */
    private void moveAway(ALifeAIBridge.AIAction action) {
        if (action.target != null) {
            Vec3 entityPos = entity.position();
            Vec3 targetPos = action.target;
            Vec3 awayVec = entityPos.subtract(targetPos).normalize().scale(5.0);
            Vec3 destination = entityPos.add(awayVec);

            entity.getNavigation().moveTo(destination.x, destination.y, destination.z, action.speed);
        }
    }

    /**
     * ターゲットの周囲を移動
     */
    private void strafe(ALifeAIBridge.AIAction action) {
        if (action.target != null) {
            // 円周上を移動する簡易実装
            Vec3 entityPos = entity.position();
            Vec3 targetPos = action.target;
            Vec3 toTarget = targetPos.subtract(entityPos);

            // 接線方向に移動（反時計回り）
            Vec3 tangent = new Vec3(-toTarget.z, 0, toTarget.x).normalize();
            Vec3 destination = entityPos.add(tangent.scale(2.0));

            entity.getNavigation().moveTo(destination.x, destination.y, destination.z, action.speed);
            entity.getLookControl().setLookAt(targetPos.x, targetPos.y, targetPos.z);
        }
    }

    /**
     * 回復を実行
     */
    private void executeHeal(ALifeAIBridge.AIAction action) {
        float healAmount = action.damageMultiplier; // heal_amountを再利用
        entity.heal(healAmount);

        // 回復エフェクト
        Level world = VersionHelper.getLevel(entity);
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            serverWorld.sendParticles(
                ParticleTypes.HEART,
                entity.getX(), entity.getY() + 1.5, entity.getZ(),
                5, 0.3, 0.3, 0.3, 0.1
            );
        }

        // 回復サウンド
        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.HOSTILE, 0.5f, 1.5f);
    }

    /**
     * 撤退を実行
     */
    private void executeRetreat(ALifeAIBridge.AIAction action) {
        if (action.direction == null) {
            return;
        }

        // スプリント状態を設定（撤退時は常にスプリント）
        entity.setSprinting(true);

        // 撤退方向に移動（通常より速く、スプリント時はさらに速く）
        float speed = action.speed;
        if (action.isSprinting) {
            speed *= 1.3f;
        }
        Vec3 retreatVec = action.direction.scale(speed);
        entity.setDeltaMovement(retreatVec.x, entity.getDeltaMovement().y, retreatVec.z);

        // 煙エフェクト
        Level world = VersionHelper.getLevel(entity);
        if (!world.isClientSide && random.nextInt(5) == 0) {
            ServerLevel serverWorld = (ServerLevel) world;
            serverWorld.sendParticles(
                ParticleTypes.POOF,
                entity.getX(), entity.getY(), entity.getZ(),
                1, 0.1, 0.1, 0.1, 0.02
            );
        }
    }

    /**
     * 防御姿勢を実行
     */
    private void executeGuard(ALifeAIBridge.AIAction action) {
        // ガード効果を付与
        if (TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get() != null) {
            entity.addEffect(new MobEffectInstance(
                TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get(),
                60, // 3秒
                0
            ));
        }

        // ガードエフェクト
        Level world = VersionHelper.getLevel(entity);
        if (!world.isClientSide && actionTicks % 10 == 0) {
            ServerLevel serverWorld = (ServerLevel) world;
            serverWorld.sendParticles(
                ParticleTypes.ENCHANTED_HIT,
                entity.getX(), entity.getY() + 1, entity.getZ(),
                3, 0.3, 0.3, 0.3, 0
            );
        }
    }

    /**
     * 落下ダメージを無効化するかチェック
     */
    public boolean isFallDamageImmune() {
        return fallDamageImmunityTicks > 0;
    }

    /**
     * 攻撃経路上の竹を破壊する（DodgeAndBattouHandler.breakBambooInPathと同じ）
     */
    private void breakBambooInPath(Vec3 startPos, Vec3 direction, double range) {
        Level world = VersionHelper.getLevel(entity);
        if (world.isClientSide) return;

        // 攻撃経路に沿って竹をチェック
        for (double d = 0; d <= range; d += 0.5) {
            Vec3 checkPos = startPos.add(direction.scale(d));

            // 上下左右も含めて範囲をチェック
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos pos = new BlockPos(
                            (int)(checkPos.x + dx),
                            (int)(checkPos.y + dy),
                            (int)(checkPos.z + dz)
                        );

                        BlockState state = world.getBlockState(pos);

                        // 竹または竹の苗をチェック
                        if (state.getBlock() == Blocks.BAMBOO ||
                            state.getBlock() == Blocks.BAMBOO_SAPLING) {
                            // 竹を破壊（ドロップあり）
                            world.destroyBlock(pos, true);
                        }
                    }
                }
            }
        }
    }
}
