package minecraftarmorweapon.ai;

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

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.events.ChargedAttackHandler;

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
        Level world = entity.level;
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
     * チャージ攻撃を実行（プレイヤーの左クリック長押し動作）
     */
    private void executeChargeAttack(ALifeAIBridge.AIAction action) {
        // 盲目効果時：チャージ攻撃が80%の確率で失敗
        if (entity.hasEffect(MobEffects.BLINDNESS)) {
            if (random.nextDouble() < 0.8) {
                System.out.println("[PlayerLikeAI] " + entity.getName().getString() + " は盲目のためチャージ攻撃に失敗しました");
                return;
            }
        }

        // 暗闇効果時：チャージ攻撃の範囲が50%減少
        double rangeMultiplier = 1.0;
        if (entity.hasEffect(MobEffects.DARKNESS)) {
            rangeMultiplier = 0.5;
            System.out.println("[PlayerLikeAI] " + entity.getName().getString() + " は暗闇のためチャージ攻撃の範囲が減少しました");
        }

        Level world = entity.level;
        Vec3 entityPos = entity.position();

        // ターゲットを見る
        if (action.target != null) {
            entity.getLookControl().setLookAt(action.target.x, action.target.y, action.target.z);
        }

        // 攻撃方向をターゲット位置から計算
        Vec3 lookVec;
        if (action.target != null) {
            lookVec = action.target.subtract(entityPos).normalize();
        } else {
            lookVec = entity.getLookAngle();
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
            System.out.println("[PlayerLikeAI] " + entity.getName().getString() + " は混乱のため攻撃方向がずれました");
        }

        // プレイヤーのChargedAttackHandlerと同じロジック
        // ティアに応じたチャージ率を使用
        float chargePercent = action.chargePercent;
        float baseDamage = 15.0f * (1.0f + chargePercent);
        double range = (6.0f + chargePercent * 2.0f) * rangeMultiplier;

        // ラムダ式で使用するためfinalにコピー
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

        for (LivingEntity target : targets) {
            // ダメージを与える
            target.hurt(DamageSource.mobAttack(entity), baseDamage);

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
            if (MinecraftArmorWeaponModMobEffects.GUARD.get() != null) {
                entity.addEffect(new MobEffectInstance(
                    MinecraftArmorWeaponModMobEffects.GUARD.get(),
                    duration,
                    0
                ));
            }

            // LONG_RANGE_WEAPON_CUTエフェクトを付与（ReplicaSwordOfLightの効果）
            if (MinecraftArmorWeaponModMobEffects.LONG_RANGE_WEAPON_CUT.get() != null) {
                entity.addEffect(new MobEffectInstance(
                    MinecraftArmorWeaponModMobEffects.LONG_RANGE_WEAPON_CUT.get(),
                    2,
                    1
                ));
            }

            // エフェクト
            Level world = entity.level;
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
     * 通常攻撃を実行
     */
    private void executeNormalAttack(ALifeAIBridge.AIAction action) {
        LivingEntity target = entity.getTarget();
        if (target != null && entity.distanceTo(target) <= 3.0) {
            // デバッグログ
            System.out.println("[PlayerLikeAI] executeNormalAttack:");
            System.out.println("  - Entity: " + entity.getName().getString());
            System.out.println("  - Attack Type: " + action.attackType);
            System.out.println("  - Combo: " + action.combo);

            entity.doHurtTarget(target);
        }
    }

    /**
     * ターゲットに向かって移動
     */
    private void moveToTarget(ALifeAIBridge.AIAction action) {
        if (action.target != null) {
            entity.getNavigation().moveTo(action.target.x, action.target.y, action.target.z, action.speed);
            entity.getLookControl().setLookAt(action.target.x, action.target.y, action.target.z);
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
        Level world = entity.level;
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

        // 撤退方向に移動（通常より速く）
        Vec3 retreatVec = action.direction.scale(action.speed);
        entity.setDeltaMovement(retreatVec.x, entity.getDeltaMovement().y, retreatVec.z);

        // 煙エフェクト
        Level world = entity.level;
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
        if (MinecraftArmorWeaponModMobEffects.GUARD.get() != null) {
            entity.addEffect(new MobEffectInstance(
                MinecraftArmorWeaponModMobEffects.GUARD.get(),
                60, // 3秒
                0
            ));
        }

        // ガードエフェクト
        Level world = entity.level;
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
}
