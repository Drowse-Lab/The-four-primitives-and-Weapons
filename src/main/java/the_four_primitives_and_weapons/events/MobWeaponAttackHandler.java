package the_four_primitives_and_weapons.events;

import the_four_primitives_and_weapons.util.VersionHelper;
import the_four_primitives_and_weapons.entity.CommonSoldierEntity;
import the_four_primitives_and_weapons.entity.EliteSoldierEntity;
import the_four_primitives_and_weapons.entity.HeroicTierEntity;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

import the_four_primitives_and_weapons.util.DamageCalculator;

import java.util.List;

/**
 * Mobが武器を振った時の攻撃処理ハンドラー
 *
 * プレイヤーだけでなく、Mobも武器を使って通常攻撃ができるようにします
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class MobWeaponAttackHandler {

    // 再帰防止フラグ（範囲攻撃のhurt→LivingAttackEvent→再び範囲攻撃の無限ループを防ぐ）
    private static boolean isProcessingAttack = false;

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        // 再帰防止：既に範囲攻撃処理中なら何もしない
        if (isProcessingAttack) {
            return;
        }

        try {
            // 攻撃を受けた側のエンティティ
            LivingEntity target = event.getEntity();

            // ダメージソースを取得
            DamageSource source = event.getSource();

            // 攻撃者を取得
            if (source.getEntity() instanceof LivingEntity attacker) {
                // プレイヤーの場合はChargedAttackHandlerで処理されるのでスキップ
                if (attacker instanceof Player) {
                    return;
                }

                // Mobが武器を持っているかチェック
                ItemStack weapon = attacker.getMainHandItem();
                if (isWeapon(weapon)) {
                    // 再帰防止フラグをセット
                    isProcessingAttack = true;
                    try {
                        // 武器攻撃エフェクトを追加
                        enhanceMobWeaponAttack(attacker, target, weapon, event);
                    } finally {
                        isProcessingAttack = false;
                    }
                }
            }
        } catch (Throwable e) {
            System.err.println("[MobWeaponAttack] Error in onLivingAttack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Mobの武器攻撃を強化
     */
    private static void enhanceMobWeaponAttack(LivingEntity attacker, LivingEntity target,
                                               ItemStack weapon, LivingAttackEvent event) {
        Level world = VersionHelper.getLevel(attacker);
        if (world.isClientSide()) return;

        Vec3 attackerPos = attacker.position();
        Vec3 lookVec = attacker.getLookAngle();

        // 武器の種類を判定
        String weaponName = weapon.getItem().getClass().getSimpleName();

        // 刀の場合（Katanaを含むもの） - 先にチェック
        if (weaponName.contains("Katana") || weaponName.contains("katana")) {
            performMobSlashAttack(attacker, target, world, lookVec, attackerPos);
        }
        // 直刀の場合（TyokutouやUtigatanaなど）
        else if (isStraightSword(weapon)) {
            performMobThrustAttack(attacker, target, world, lookVec, attackerPos);
        }
    }

    /**
     * Mobの突き攻撃
     */
    private static void performMobThrustAttack(LivingEntity attacker, LivingEntity target,
                                               Level world, Vec3 lookVec, Vec3 attackerPos) {
        try {
            ServerLevel serverWorld = (ServerLevel) world;

            // 攻撃方向をターゲット位置から計算
            Vec3 attackDirection = target.position().subtract(attackerPos).normalize();

            // 小さいDustパーティクルエフェクト（直刀用）
            DustParticleOptions dustOptions = new DustParticleOptions(
                new Vector3f(0.9f, 0.95f, 1.0f), 0.4f
            );

            for (int i = 0; i < 8; i++) {
                double d = i * 0.4;
                serverWorld.sendParticles(
                    dustOptions,
                    attackerPos.x + attackDirection.x * d,
                    attackerPos.y + attacker.getBbHeight() * 0.6,
                    attackerPos.z + attackDirection.z * d,
                    2, 0.05, 0.05, 0.05, 0
                );
            }

            // 突き音
            world.playSound(null, attackerPos.x, attackerPos.y, attackerPos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8f, 1.5f);

            // 前方の追加ターゲットにダメージ（貫通効果）
            double range = 4.0;
            AABB searchArea = new AABB(attackerPos, attackerPos.add(attackDirection.scale(range))).inflate(1.0);

            List<LivingEntity> additionalTargets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != attacker && entity != target && entity.isAlive() &&
                          !entity.isAlliedTo(attacker) &&
                          !(isSoldierEntity(attacker) && isSoldierEntity(entity)));

            for (LivingEntity additionalTarget : additionalTargets) {
                Vec3 toEntity = additionalTarget.position().subtract(attackerPos);
                double dot = attackDirection.dot(toEntity.normalize());

                if (dot > 0.8 && toEntity.length() <= range) {
                    // 追加ダメージ（エンチャント込み）
                    ItemStack weapon = attacker.getMainHandItem();
                    DamageCalculator.dealDamage(attacker, additionalTarget, 3.0f, weapon);

                    // ノックバック
                    additionalTarget.setDeltaMovement(attackDirection.scale(0.5).add(0, 0.1, 0));
                }
            }
        } catch (Throwable e) {
            System.err.println("[MobWeaponAttack] Error in performMobThrustAttack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Mobのスラッシュ攻撃
     */
    private static void performMobSlashAttack(LivingEntity attacker, LivingEntity target,
                                              Level world, Vec3 lookVec, Vec3 attackerPos) {
        try {
            ServerLevel serverWorld = (ServerLevel) world;

            // 攻撃方向をターゲット位置から計算
            Vec3 attackDirection = target.position().subtract(attackerPos).normalize();

            // スラッシュエフェクト
            for (int i = 0; i < 5; i++) {
                double d = i * 0.5 + 1;
                serverWorld.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    attackerPos.x + attackDirection.x * d,
                    attackerPos.y + attacker.getBbHeight() * 0.6,
                    attackerPos.z + attackDirection.z * d,
                    1, 0.2, 0.2, 0.2, 0
                );
            }

            // スラッシュ音
            world.playSound(null, attackerPos.x, attackerPos.y, attackerPos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0f, 1.0f);

            // 横に広い範囲攻撃
            double range = 3.5;
            double horizontalWidth = 2.0;
            Vec3 rightVec = new Vec3(-attackDirection.z, 0, attackDirection.x).normalize();

            Vec3 minPoint = attackerPos.add(attackDirection.scale(-0.5))
                .add(rightVec.scale(-horizontalWidth))
                .add(0, -0.5, 0);
            Vec3 maxPoint = attackerPos.add(attackDirection.scale(range))
                .add(rightVec.scale(horizontalWidth))
                .add(0, 2.0, 0);

            AABB searchArea = new AABB(minPoint, maxPoint);

            List<LivingEntity> additionalTargets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != attacker && entity != target && entity.isAlive() &&
                          !entity.isAlliedTo(attacker) &&
                          !(isSoldierEntity(attacker) && isSoldierEntity(entity)));

            for (LivingEntity additionalTarget : additionalTargets) {
                Vec3 toEntity = additionalTarget.position().subtract(attackerPos).normalize();
                double dot = attackDirection.dot(toEntity);

                if (dot > -0.2 && additionalTarget.distanceTo(attacker) <= range) {
                    // 追加ダメージ（エンチャント込み）
                    ItemStack weapon = attacker.getMainHandItem();
                    DamageCalculator.dealDamage(attacker, additionalTarget, 4.0f, weapon);

                    // ノックバック
                    Vec3 knockback = toEntity.scale(0.4);
                    additionalTarget.setDeltaMovement(
                        additionalTarget.getDeltaMovement().add(knockback.x, 0.1, knockback.z)
                    );
                }
            }
        } catch (Throwable e) {
            System.err.println("[MobWeaponAttack] Error in performMobSlashAttack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Mobのデフォルト攻撃
     */
    private static void performMobDefaultAttack(LivingEntity attacker, LivingEntity target,
                                                Level world, Vec3 lookVec, Vec3 attackerPos) {
        try {
            ServerLevel serverWorld = (ServerLevel) world;

            // 攻撃方向をターゲット位置から計算
            Vec3 attackDirection = target.position().subtract(attackerPos).normalize();

            // 基本エフェクト
            for (int i = 0; i < 3; i++) {
                serverWorld.sendParticles(
                    ParticleTypes.CRIT,
                    attackerPos.x + attackDirection.x * (i + 1),
                    attackerPos.y + attacker.getBbHeight() * 0.6,
                    attackerPos.z + attackDirection.z * (i + 1),
                    2, 0.1, 0.1, 0.1, 0
                );
            }

            // 攻撃音
            world.playSound(null, attackerPos.x, attackerPos.y, attackerPos.z,
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.8f, 1.0f);

            // 前方範囲の追加ダメージ
            double range = 3.0;
            AABB searchArea = new AABB(
                attackerPos.x - range, attackerPos.y - 1, attackerPos.z - range,
                attackerPos.x + range, attackerPos.y + 2, attackerPos.z + range
            );

            List<LivingEntity> additionalTargets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != attacker && entity != target && entity.isAlive() &&
                          !entity.isAlliedTo(attacker) &&
                          !(isSoldierEntity(attacker) && isSoldierEntity(entity)));

            for (LivingEntity additionalTarget : additionalTargets) {
                Vec3 toEntity = additionalTarget.position().subtract(attackerPos).normalize();
                double dot = attackDirection.dot(toEntity);

                if (dot > 0.5 && additionalTarget.distanceTo(attacker) <= range) {
                    // 追加ダメージ（エンチャント込み）
                    ItemStack weapon = attacker.getMainHandItem();
                    DamageCalculator.dealDamage(attacker, additionalTarget, 3.0f, weapon);

                    // ノックバック
                    Vec3 knockback = toEntity.scale(0.3);
                    additionalTarget.setDeltaMovement(
                        additionalTarget.getDeltaMovement().add(knockback.x, 0.1, knockback.z)
                    );
                }
            }
        } catch (Throwable e) {
            System.err.println("[MobWeaponAttack] Error in performMobDefaultAttack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 兵士エンティティかどうかをチェック（仲間割れ防止用）
     */
    private static boolean isSoldierEntity(LivingEntity entity) {
        return entity instanceof CommonSoldierEntity ||
               entity instanceof EliteSoldierEntity ||
               entity instanceof HeroicTierEntity;
    }

    /**
     * 武器かどうかをチェック
     */
    private static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (stack.getItem() instanceof SwordItem) return true;

        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.contains("Katana") || itemName.contains("Sword") ||
               itemName.contains("Blade") || itemName.contains("katana");
    }

    /**
     * 直刀かどうかをチェック
     */
    private static boolean isStraightSword(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return the_four_primitives_and_weapons.procedures.TyokutouThrustAttackProcedure.isStraightSword(stack);
    }
}
