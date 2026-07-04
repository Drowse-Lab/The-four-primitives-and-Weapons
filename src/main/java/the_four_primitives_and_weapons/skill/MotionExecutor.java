package the_four_primitives_and_weapons.skill;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import the_four_primitives_and_weapons.api.ISkillAction;
import the_four_primitives_and_weapons.util.DamageCalculator;
import the_four_primitives_and_weapons.events.DashSkillHandler;
import the_four_primitives_and_weapons.procedures.SwordOfNightTpProcedure;
import the_four_primitives_and_weapons.procedures.MagicKatanaSpecialChargeProcedure;

import java.util.List;

/**
 * モーション実行の中央ディスパッチ。
 * motionId に応じて適切な攻撃モーションを実行する。
 */
public class MotionExecutor {

    /**
     * モーションを実行する。
     * @param motionId SkillRegistry に登録されたモーションID
     * @param player 実行するプレイヤー
     * @param chargePercent チャージ率（0.0 = 通常攻撃、0.0～1.0 = チャージ攻撃）
     */
    public static void executeMotion(String motionId, Player player, float chargePercent) {
        if (motionId == null || motionId.isEmpty()) return;

        // 技 ON/OFF トグル: 無効化されていれば何もしない (通常攻撃にフォールバック)
        if (!PlayerSkillData.isMotionEnabled(player, motionId)) {
            return;
        }

        // クロスヘア Attack Cooldown ゲージをキャプチャしてスキル全体に damage scale を適用。
        // ゲージが満タン (1.0) なら 100%、空 (0.0) なら 20% のダメージ。
        // ※ 短時間で連続発動されるとゲージが 0 のままになるため、以前は resetAttackStrengthTicker
        //   していたが、これがスピン斬り (connection.teleport) と競合してたまに回転が止まる事象を
        //   起こすため削除。スキル使用後に通常攻撃のクールダウンが残るのは仕様。
        float cooldownScale = player.getAttackStrengthScale(0.5f);
        DamageCalculator.setCooldownScaleContext(cooldownScale);

        // チャージ技発動時は DamageCalculator に一律ボーナスを適用させる。
        // これで個別にスケールしていない技も、チャージ発動なら攻撃力が上がる。
        boolean chargedContext = chargePercent > 0.0f;
        if (chargedContext) DamageCalculator.setChargeContext(chargePercent);

        // 武器の得意/不得意技かチェック（combatスロット技のみ対象）
        // 得意 → 攻撃力+20% & ゲージ充填+50%
        // 不得意 → 攻撃力-40% & ゲージ充填-50%
        boolean preferredContext = false;
        boolean dislikedContext = false;
        the_four_primitives_and_weapons.skill.WeaponTypeRegistry.WeaponTypeData weaponType =
                the_four_primitives_and_weapons.skill.WeaponTypeRegistry.getTypeForItem(player.getMainHandItem());
        if (weaponType != null) {
            if (weaponType.isPreferredCombatMotion(motionId)) {
                preferredContext = true;
                DamageCalculator.setPreferredContext();
                the_four_primitives_and_weapons.events.WeaponSpecialtyHandler.applyBonus(player);
            } else if (weaponType.isDislikedCombatMotion(motionId)) {
                dislikedContext = true;
                DamageCalculator.setDislikedContext();
                the_four_primitives_and_weapons.events.WeaponSpecialtyHandler.applyPenalty(player);
            } else {
                // 通常技 → ボーナス/ペナルティを解除
                the_four_primitives_and_weapons.events.WeaponSpecialtyHandler.applyNormal(player);
            }
        }

        try {
            // 外部登録されたハンドラーを優先的にチェック
            ISkillAction handler = SkillRegistry.getHandler(motionId);
            if (handler != null) {
                handler.execute(player, chargePercent);
                return;
            }

            Level world = player.level();
            Vec3 lookVec = player.getLookAngle();
            Vec3 playerPos = player.position();

            switch (motionId) {
                case "thrust" -> the_four_primitives_and_weapons.procedures.TyokutouThrustAttackProcedure.execute(world, player.getX(), player.getY(), player.getZ(), player);
                case "upper_left_slash" -> performUpperLeftSlash(player, world, lookVec, playerPos, chargePercent);
                case "upper_right_slash" -> performUpperRightSlash(player, world, lookVec, playerPos, chargePercent);
                case "horizontal_slash" -> performHorizontalSlash(player, world, lookVec, playerPos, chargePercent);
                case "spin_slash" -> performSpinSlash(player, world, playerPos, chargePercent);
                case "slam_down" -> performSlamDown(player, world, lookVec, playerPos, chargePercent);
                // ダッシュ専用スキル
                case "dash_rush" -> DashSkillHandler.activateDashRush(player);
                case "leap_slash" -> DashSkillHandler.activateLeapSlash(player);
                case "shadow_step" -> DashSkillHandler.activateShadowStep(player);
                // 特殊スキル
                case "electric_beam" -> ElectricBeamSkill.fire(player);
                case "electric_slash" -> ElectricSlashSkill.fire(player);
                case "electric_discharge" -> ElectricDischargeBurstSkill.fire(player);
                case "sword_of_night_tp" -> SwordOfNightTpProcedure.execute(world, player.getX(), player.getY(), player.getZ(), player);
                case "magic_katana_special" -> MagicKatanaSpecialChargeProcedure.execute(world, player.getX(), player.getY(), player.getZ(), player, chargePercent);
                default -> performThrust(player, world, lookVec, playerPos, chargePercent);
            }
        } finally {
            // 得意な突き技は後硬直を解除して即連発可能にする（レイピア・直刀・槍など）。
            // spin_slash 等の長尺モーションでリセットすると連結処理と競合するため、
            // 短時間で完結する thrust に限定する。
            if (preferredContext && "thrust".equals(motionId)) {
                player.resetAttackStrengthTicker();
            }
            if (chargedContext) DamageCalculator.clearChargeContext();
            if (preferredContext) DamageCalculator.clearPreferredContext();
            if (dislikedContext) DamageCalculator.clearDislikedContext();
            DamageCalculator.clearCooldownScaleContext();
        }
    }

    // === 突き ===
    private static void performThrust(Player player, Level world, Vec3 lookVec, Vec3 playerPos, float chargePercent) {
        boolean isCharged = chargePercent > 0.0f;
        float baseDamage = isCharged ? 15.0f * (1.0f + chargePercent) : 7.0f;
        double range = isCharged ? 6.0 + chargePercent * 2.0 : 5.0;

        // 竹破壊
        breakBambooInPath(world, playerPos, lookVec, range);

        // エフェクト
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;

            if (isCharged) {
                // チャージ版: 貫通突き
                for (double d = 0; d <= range; d += 0.3) {
                    serverWorld.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        playerPos.x + lookVec.x * d, playerPos.y + 1, playerPos.z + lookVec.z * d,
                        5, 0.2, 0.2, 0.2, 0.05);
                    if (chargePercent >= 1.0f) {
                        serverWorld.sendParticles(ParticleTypes.END_ROD,
                            playerPos.x + lookVec.x * d, playerPos.y + 1, playerPos.z + lookVec.z * d,
                            2, 0.1, 0.1, 0.1, 0);
                    }
                }
            } else {
                // 通常版
                for (int i = 0; i < 5; i++) {
                    double d = i * 0.5 + 1;
                    Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
                    serverWorld.sendParticles(ParticleTypes.CRIT,
                        playerPos.x + lookVec.x * d, playerPos.y + 1, playerPos.z + lookVec.z * d,
                        2, 0.05, 0.05, 0.05, 0);
                    for (double side = -1.5; side <= 1.5; side += 0.5) {
                        if (side != 0) {
                            serverWorld.sendParticles(ParticleTypes.ENCHANTED_HIT,
                                playerPos.x + lookVec.x * d + rightVec.x * side,
                                playerPos.y + 1,
                                playerPos.z + lookVec.z * d + rightVec.z * side,
                                1, 0.02, 0.02, 0.02, 0);
                        }
                    }
                }
            }
        }

        // ターゲット検索
        double horizontalWidth = isCharged ? 1.0 : 2.5;
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();

        if (isCharged) {
            // チャージ版: 直線貫通
            Vec3 endPos = playerPos.add(lookVec.scale(range));
            AABB searchArea = new AABB(playerPos, endPos).inflate(1.0);
            List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> {
                    if (entity == player) return false;
                    Vec3 toEntity = entity.position().subtract(playerPos);
                    double dot = lookVec.dot(toEntity.normalize());
                    return dot > 0.8 && toEntity.length() <= range;
                });
            for (LivingEntity target : targets) {
                ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
                DamageCalculator.dealDamage(player, target, baseDamage, weapon);
                DamageCalculator.applyNormalKnockback(player, target, weapon);
                if (chargePercent >= 1.0f) {
                    target.setSecondsOnFire(5);
                }
            }
        } else {
            // 通常版: 横広範囲
            Vec3 minPoint = playerPos.add(lookVec.scale(0.5))
                .add(rightVec.scale(-horizontalWidth)).add(0, -0.5, 0);
            Vec3 maxPoint = playerPos.add(lookVec.scale(range))
                .add(rightVec.scale(horizontalWidth)).add(0, 1.5, 0);
            AABB attackBox = new AABB(minPoint, maxPoint);
            List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, attackBox,
                entity -> entity != player);
            for (LivingEntity target : targets) {
                ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
                DamageCalculator.dealDamage(player, target, baseDamage, weapon);
                DamageCalculator.applyNormalKnockback(player, target, weapon);
            }
        }

        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            isCharged ? SoundEvents.TRIDENT_THUNDER : SoundEvents.PLAYER_ATTACK_KNOCKBACK,
            SoundSource.PLAYERS, isCharged ? 1.0f : 1.0f, isCharged ? 1.0f : 1.2f);
    }

    // === 左上斬り ===
    private static void performUpperLeftSlash(Player player, Level world, Vec3 lookVec, Vec3 playerPos, float chargePercent) {
        boolean isCharged = chargePercent > 0.0f;
        float baseDamage = isCharged ? 10.0f * (1.0f + chargePercent) : 9.0f;

        breakBambooInPath(world, playerPos, lookVec, 5.0);

        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            // 左上から右下への斜め斬り ( 向きに対して横へ広げ、 緩やかに降下 )
            double px = -lookVec.z, pz = lookVec.x;            // 水平の右向きベクトル
            double plen = Math.sqrt(px * px + pz * pz);
            if (plen > 1e-4) { px /= plen; pz /= plen; }
            double cx = playerPos.x + lookVec.x * 2;
            double cz = playerPos.z + lookVec.z * 2;
            for (int i = -4; i <= 4; i++) {
                double t = i * 0.28;                            // 横の広がり ( 左→右 )
                serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    cx + px * t,
                    playerPos.y + 1.5 - i * 0.10,               // 降下を緩やかに ( 0.2→0.10 )
                    cz + pz * t,
                    1, 0, 0, 0, 0);
            }
            if (isCharged) {
                serverWorld.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    playerPos.x + lookVec.x * 2, playerPos.y + 1.2, playerPos.z + lookVec.z * 2,
                    8, 0.5, 0.5, 0.5, 0.1);
            }
        }

        performSlashDamage(player, world, lookVec, playerPos, baseDamage, 4.5, 3.0, chargePercent);

        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.9f);
    }

    // === 右上斬り ===
    private static void performUpperRightSlash(Player player, Level world, Vec3 lookVec, Vec3 playerPos, float chargePercent) {
        boolean isCharged = chargePercent > 0.0f;
        float baseDamage = isCharged ? 10.0f * (1.0f + chargePercent) : 9.0f;

        breakBambooInPath(world, playerPos, lookVec, 5.0);

        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            // 右上から左下への斜め斬り ( 横は反転、 緩やかに降下 )
            double px = -lookVec.z, pz = lookVec.x;            // 水平の右向きベクトル
            double plen = Math.sqrt(px * px + pz * pz);
            if (plen > 1e-4) { px /= plen; pz /= plen; }
            double cx = playerPos.x + lookVec.x * 2;
            double cz = playerPos.z + lookVec.z * 2;
            for (int i = -4; i <= 4; i++) {
                double t = i * 0.28;                            // 横の広がり ( 右→左 )
                serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    cx - px * t,
                    playerPos.y + 1.5 - i * 0.10,               // 降下を緩やかに ( 0.2→0.10 )
                    cz - pz * t,
                    1, 0, 0, 0, 0);
            }
            if (isCharged) {
                serverWorld.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    playerPos.x + lookVec.x * 2, playerPos.y + 1.2, playerPos.z + lookVec.z * 2,
                    8, 0.5, 0.5, 0.5, 0.1);
            }
        }

        performSlashDamage(player, world, lookVec, playerPos, baseDamage, 4.5, 3.0, chargePercent);

        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    // === 横一文字 ===
    private static void performHorizontalSlash(Player player, Level world, Vec3 lookVec, Vec3 playerPos, float chargePercent) {
        boolean isCharged = chargePercent > 0.0f;
        float baseDamage = isCharged ? 14.0f * (1.0f + chargePercent) : 12.0f;

        breakBambooInPath(world, playerPos, lookVec, 5.0);

        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            // 横一文字エフェクト
            Vec3 right = lookVec.cross(new Vec3(0, 1, 0)).normalize();
            for (int i = -3; i <= 3; i++) {
                serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    playerPos.x + lookVec.x * 2 + right.x * i * 0.3,
                    playerPos.y + 1,
                    playerPos.z + lookVec.z * 2 + right.z * i * 0.3,
                    1, 0, 0, 0, 0);
            }
            if (isCharged) {
                for (int i = -4; i <= 4; i++) {
                    serverWorld.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        playerPos.x + lookVec.x * 2 + right.x * i * 0.4,
                        playerPos.y + 1,
                        playerPos.z + lookVec.z * 2 + right.z * i * 0.4,
                        2, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }

        performSlashDamage(player, world, lookVec, playerPos, baseDamage, 4.5, 3.0, chargePercent);

        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.1f);
    }

    // === 回転斬り ===
    /**
     * 旧版は一瞬で全周ダメージ + setYRot(+720) だった.
     * 新版は {@link SpinSlashTickHandler} に session 開始を委譲し、multi-tick で
     *   - player を 720° 滑らかに回転
     *   - 開始角 → 現在角 のアーク内の敵にだけダメージ
     *   - electric_book / thunder_book 装備時は回転速度倍速
     * を実現する.
     */
    private static void performSpinSlash(Player player, Level world, Vec3 playerPos, float chargePercent) {
        boolean isCharged = chargePercent > 0.0f;
        float baseDamage = isCharged ? 12.0f * (1.0f + chargePercent * 1.5f) : 10.0f;
        double baseRange = isCharged ? 4.0 + chargePercent * 2.0 : 3.5;
        // 武器タイプによる範囲倍率 (大剣/槍は広く、短剣は狭く)
        double rangeScale = WeaponTypeRegistry.getSpinRangeScale(player.getMainHandItem());
        double range = baseRange * rangeScale;

        // 開始時の小さな視覚フラッシュ (足元のリング)
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            for (int i = 0; i < 360; i += 30) {
                double rad = Math.toRadians(i);
                serverWorld.sendParticles(ParticleTypes.CRIT,
                    playerPos.x + Math.cos(rad) * range * 0.5,
                    playerPos.y + 0.2,
                    playerPos.z + Math.sin(rad) * range * 0.5,
                    1, 0, 0, 0, 0);
            }
            if (isCharged && chargePercent >= 1.0f) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * 2 * i / 8;
                    serverWorld.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        playerPos.x + Math.cos(angle) * range,
                        playerPos.y + 1,
                        playerPos.z + Math.sin(angle) * range,
                        10, 0.2, 0.5, 0.2, 0.1);
                }
            }
        }

        // multi-tick session 開始 — 以降の回転 + アークダメージは SpinSlashTickHandler が担当
        SpinSlashTickHandler.start(player, baseDamage, range, isCharged);

        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    // === 叩きつける ===
    private static void performSlamDown(Player player, Level world, Vec3 lookVec, Vec3 playerPos, float chargePercent) {
        boolean isCharged = chargePercent > 0.0f;
        float baseDamage = isCharged ? 16.0f * (1.0f + chargePercent * 1.2f) : 13.0f;
        double forwardRange = isCharged ? 4.5 + chargePercent : 3.5;
        double width = isCharged ? 2.5 : 1.8;

        // 視点を上→下にアニメーション (振り下ろしの動き)
        SlamDownPitchHandler.start(player);

        // 竹破壊
        breakBambooInPath(world, playerPos, lookVec, forwardRange);

        // エフェクト
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();

            // 振り下ろし軌跡（上から下へ）
            for (int i = 0; i <= 6; i++) {
                double t = i / 6.0;
                serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    playerPos.x + lookVec.x * (1.5 + t * 0.5),
                    playerPos.y + 2.2 - t * 2.0,
                    playerPos.z + lookVec.z * (1.5 + t * 0.5),
                    1, 0, 0, 0, 0);
            }
            // 着弾点の地面爆発エフェクト
            double impactX = playerPos.x + lookVec.x * (forwardRange * 0.6);
            double impactZ = playerPos.z + lookVec.z * (forwardRange * 0.6);
            for (int i = 0; i < (isCharged ? 30 : 15); i++) {
                double ang = Math.random() * Math.PI * 2;
                double r = Math.random() * width;
                serverWorld.sendParticles(ParticleTypes.EXPLOSION,
                    impactX + Math.cos(ang) * r * 0.3,
                    playerPos.y + 0.1,
                    impactZ + Math.sin(ang) * r * 0.3,
                    1, 0, 0, 0, 0);
                serverWorld.sendParticles(net.minecraft.core.particles.BlockParticleOption.class.cast(
                        new net.minecraft.core.particles.BlockParticleOption(
                                ParticleTypes.BLOCK,
                                world.getBlockState(new BlockPos((int) impactX, (int) playerPos.y - 1, (int) impactZ)))),
                    impactX + Math.cos(ang) * r,
                    playerPos.y + 0.2,
                    impactZ + Math.sin(ang) * r,
                    1, 0.1, 0.2, 0.1, 0.1);
            }
            if (isCharged) {
                // フルチャージ時の衝撃波
                for (int i = 0; i < 16; i++) {
                    double ang = Math.PI * 2 * i / 16;
                    serverWorld.sendParticles(ParticleTypes.CLOUD,
                        impactX + Math.cos(ang) * width,
                        playerPos.y + 0.3,
                        impactZ + Math.sin(ang) * width,
                        2, 0.1, 0.05, 0.1, 0.05);
                }
            }
        }

        // 前方の敵にダメージ（上下広めのAABB）
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
        Vec3 minPoint = playerPos.add(lookVec.scale(-0.3))
            .add(rightVec.scale(-width)).add(0, -1.0, 0);
        Vec3 maxPoint = playerPos.add(lookVec.scale(forwardRange))
            .add(rightVec.scale(width)).add(0, 2.5, 0);
        AABB searchArea = new AABB(minPoint, maxPoint);

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().subtract(playerPos).normalize();
                double dot = lookVec.dot(toEntity);
                return dot > 0.0 && entity.distanceTo(player) <= forwardRange + width;
            });

        for (LivingEntity target : targets) {
            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            DamageCalculator.dealDamage(player, target, baseDamage, weapon);
            DamageCalculator.applyNormalKnockback(player, target, weapon);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                isCharged ? 40 : 20,
                isCharged ? 2 : 1));
        }

        // サウンド
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6f, 1.4f);
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    // === 共通の斬撃ダメージ処理 ===
    private static void performSlashDamage(Player player, Level world, Vec3 lookVec, Vec3 playerPos,
                                            float baseDamage, double forwardRange, double horizontalRange,
                                            float chargePercent) {
        // 武器ごとの攻撃範囲: weapon_stats.json の attack_range を斬撃の奥行き(と横幅)に反映。
        the_four_primitives_and_weapons.skill.WeaponStatsRegistry.WeaponStats st =
                the_four_primitives_and_weapons.skill.WeaponStatsRegistry.getStats(
                        player.getItemInHand(InteractionHand.MAIN_HAND));
        if (st != null && !Float.isNaN(st.attackRange)) {
            forwardRange = Math.max(1.0, forwardRange + st.attackRange);
            horizontalRange = Math.max(0.75, horizontalRange + st.attackRange * 0.5);
        }
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();

        Vec3 minPoint = playerPos.add(lookVec.scale(-0.5))
            .add(rightVec.scale(-horizontalRange)).add(0, -0.5, 0);
        Vec3 maxPoint = playerPos.add(lookVec.scale(forwardRange))
            .add(rightVec.scale(horizontalRange)).add(0, 2.5, 0);

        AABB searchArea = new AABB(minPoint, maxPoint);

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().subtract(playerPos).normalize();
                double dot = lookVec.dot(toEntity);
                return dot > -0.3 && entity.distanceTo(player) <= forwardRange + horizontalRange;
            });

        for (LivingEntity target : targets) {
            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            DamageCalculator.dealDamage(player, target, baseDamage, weapon);
            DamageCalculator.applyNormalKnockback(player, target, weapon);
        }
    }

    // === 竹破壊ユーティリティ ===
    public static void breakBambooInPath(Level world, Vec3 startPos, Vec3 direction, double range) {
        if (world.isClientSide) return;

        for (double d = 0; d <= range; d += 0.5) {
            Vec3 checkPos = startPos.add(direction.scale(d));
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos pos = new BlockPos(
                            (int)(checkPos.x + dx),
                            (int)(checkPos.y + dy),
                            (int)(checkPos.z + dz));
                        BlockState state = world.getBlockState(pos);
                        if (state.getBlock() == Blocks.BAMBOO ||
                            state.getBlock() == Blocks.BAMBOO_SAPLING) {
                            world.destroyBlock(pos, true);
                        }
                    }
                }
            }
        }
    }
}
