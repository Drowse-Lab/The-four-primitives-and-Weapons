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
        // モーション無し / 技 OFF → 素の通常攻撃にフォールバックする。
        // この場合も「攻撃に載っている属性」は見せたいので、 斬撃の弧ではなく
        // 武器の前方にコンパクトな属性パーティクルだけ出す。
        if (motionId == null || motionId.isEmpty()
                || !PlayerSkillData.isMotionEnabled(player, motionId)) {
            spawnSwingElementParticle(player);
            return;
        }

        // スキル ( モーション ) に武器の属性パーティクルを載せる。 全スキル共通。
        spawnMotionElementParticle(player);

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
            Vec3 lookVec = horizontalLook(player);   // ピッチ無視: 上下向いても yaw 正面に技を出す
            Vec3 playerPos = player.position();

            switch (motionId) {
                case "thrust" -> {
                    // weapon_stats に "thrust" 設定を持つ武器 ( ダガー等 ) は、通常突きも
                    // 短reachのJSON突き ( thrust.range + attack_range ) を使う。
                    // 持たない武器 ( 刀/直刀等 ) は従来の長reach突き。
                    the_four_primitives_and_weapons.skill.WeaponStatsRegistry.WeaponStats st =
                            the_four_primitives_and_weapons.skill.WeaponStatsRegistry.getStats(player.getMainHandItem());
                    if (st != null && st.thrust != null) {
                        the_four_primitives_and_weapons.procedures.JsonThrustProcedure.execute(player, chargePercent, st.thrust);
                    } else {
                        the_four_primitives_and_weapons.procedures.TyokutouThrustAttackProcedure.execute(world, player.getX(), player.getY(), player.getZ(), player);
                    }
                }
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
                // スキル画面で設定した一撃目/二撃目/三撃目を高速で連続発動する。
                case "thrust_combo" -> the_four_primitives_and_weapons.procedures.SkillComboProcedure.execute(player, chargePercent);
                default -> performThrust(player, world, lookVec, playerPos, chargePercent);
            }
        } finally {
            // ※ ここで得意な突きだけ resetAttackStrengthTicker() していたが削除。
            //    このメソッドはゲージを 0 にする ( = 次の一撃の cooldownScale が 0 になる ) ので、
            //    「即連発可能にする」というコメントとは逆に 得意技ほど次撃が弱くなっていた。
            //    連発の速さは ChargedAttackHandler のゲージ判定 + 得意技のゲージ充填 1.5 倍で表現する。
            if (chargedContext) DamageCalculator.clearChargeContext();
            if (preferredContext) DamageCalculator.clearPreferredContext();
            if (dislikedContext) DamageCalculator.clearDislikedContext();
            DamageCalculator.clearCooldownScaleContext();
        }
    }

    /**
     * スキル発動時に、武器に載った属性のパーティクルを前方へ弧状に出す。
     * 通常攻撃だけでなくチャージ/ダッシュ/特殊など全スキルに属性色を乗せる。
     */
    /**
     * モーションが出ない素の通常攻撃 ( 技 OFF / 未設定 ) 用の属性パーティクル。
     * 斬撃の弧が無いので、武器の前方に小さくまとめて出す。
     */
    private static void spawnSwingElementParticle(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        the_four_primitives_and_weapons.damage.ElementType elem =
                the_four_primitives_and_weapons.damage.ElementalDamageUtils.getAttackElementType(player);
        if (elem == the_four_primitives_and_weapons.damage.ElementType.NONE) return;

        Vec3 look = player.getLookAngle();
        Vec3 c = player.getEyePosition().add(look.scale(1.5)).subtract(0.0, 0.35, 0.0);
        the_four_primitives_and_weapons.damage.ElementalParticles.spawn(sl, elem, c.x, c.y, c.z, 8);
    }

    /**
     * 全スキル共通の属性パーティクル。
     * 斬撃の弧を描く技は {@link #slashCloudFan} 側が属性色の弧を出すので、
     * ここは弧を持たない技 ( 回転斬り / 振り下ろし / ダッシュ等 ) でも属性が乗るようにする保険。
     */
    private static void spawnMotionElementParticle(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        the_four_primitives_and_weapons.damage.ElementType elem =
                the_four_primitives_and_weapons.damage.ElementalDamageUtils.getAttackElementType(player);
        if (elem == the_four_primitives_and_weapons.damage.ElementType.NONE) return;

        double delta = fanSpread(player);
        for (Vec3 p : fanPoints(player, player.getLookAngle(), player.position(), 0.0)) {
            // 範囲は灰色 dust と同じ ( 横に広く・上下は広げない )。
            the_four_primitives_and_weapons.damage.ElementalParticles.spawnWide(
                    sl, elem, p.x, p.y, p.z, 10, delta, 0.0);
        }
    }

    // === 突き ===
    private static void performThrust(Player player, Level world, Vec3 lookVec, Vec3 playerPos, float chargePercent) {
        boolean isCharged = chargePercent > 0.0f;
        float baseDamage = isCharged ? 15.0f * (1.0f + chargePercent) : 7.0f;
        double range = Math.max(1.0, (isCharged ? 6.0 + chargePercent * 2.0 : 5.0)
                + the_four_primitives_and_weapons.skill.WeaponStatsRegistry.attackRangeBonus(player.getMainHandItem()));

        // 竹破壊
        breakBambooInPath(world, playerPos, lookVec, range);

        // エフェクト ( 13-mystic-swords katana と同じ 3クラスタ dust 扇に統一 )
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            slashCloudFan(serverWorld, player, lookVec, playerPos, 0.0);
            if (isCharged) {
                serverWorld.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    playerPos.x + lookVec.x * 2, playerPos.y + 1.2, playerPos.z + lookVec.z * 2,
                    8, 0.4, 0.4, 0.4, 0.1);
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
            // 左上から右下への斜め斬り: 横に大きく広がる白い雲のファン ( 左が高い )
            slashCloudFan(serverWorld, player, lookVec, playerPos, -0.5);
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
            // 右上から左下への斜め斬り: 横に大きく広がる白い雲のファン ( 右が高い )
            slashCloudFan(serverWorld, player, lookVec, playerPos, 0.5);
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
            // 横一文字: 横に大きく広がる白い雲のファン ( 水平 )
            slashCloudFan(serverWorld, player, lookVec, playerPos, 0.0);
            if (isCharged) {
                Vec3 right = lookVec.cross(new Vec3(0, 1, 0)).normalize();
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
        double range = Math.max(1.0, baseRange * rangeScale
                + the_four_primitives_and_weapons.skill.WeaponStatsRegistry.attackRangeBonus(player.getMainHandItem()));

        // 開始時の小さな視覚フラッシュ (足元のリング)
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            // 全周に dust のリング ( 属性が載っていれば属性色 )
            net.minecraft.core.particles.ParticleOptions ringDust = slashDust(player);
            for (int i = 0; i < 360; i += 30) {
                double rad = Math.toRadians(i);
                serverWorld.sendParticles(ringDust,
                    playerPos.x + Math.cos(rad) * range * 0.7,
                    playerPos.y + 1.0,
                    playerPos.z + Math.sin(rad) * range * 0.7,
                    3, 0.2, 0.1, 0.2, 0.001);
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
        double rangeBonus = the_four_primitives_and_weapons.skill.WeaponStatsRegistry.attackRangeBonus(player.getMainHandItem());
        double forwardRange = Math.max(1.0, (isCharged ? 4.5 + chargePercent : 3.5) + rangeBonus);
        double width = Math.max(0.75, (isCharged ? 2.5 : 1.8) + rangeBonus * 0.5);

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
        final double fRange = (st != null && !Float.isNaN(st.attackRange))
                ? Math.max(1.0, forwardRange + st.attackRange) : forwardRange;
        final double hRange = (st != null && !Float.isNaN(st.attackRange))
                ? Math.max(0.75, horizontalRange + st.attackRange * 0.5) : horizontalRange;
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();

        Vec3 minPoint = playerPos.add(lookVec.scale(-0.5))
            .add(rightVec.scale(-hRange)).add(0, -0.5, 0);
        Vec3 maxPoint = playerPos.add(lookVec.scale(fRange))
            .add(rightVec.scale(hRange)).add(0, 2.5, 0);

        AABB searchArea = new AABB(minPoint, maxPoint);

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().subtract(playerPos).normalize();
                double dot = lookVec.dot(toEntity);
                return dot > -0.3 && entity.distanceTo(player) <= fRange + hRange;
            });

        for (LivingEntity target : targets) {
            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            DamageCalculator.dealDamage(player, target, baseDamage, weapon);
            DamageCalculator.applyNormalKnockback(player, target, weapon);
        }
    }

    // === 竹破壊ユーティリティ ===
    /** ピッチを無視した水平前方ベクトル ( 上下を向いていても yaw 正面に技を出すための向き )。 */
    public static Vec3 horizontalLook(Player player) {
        return Vec3.directionFromRotation(0.0f, player.getYRot());
    }

    // 13-mystic-swords の katana ( Sword:101 ) と同じ色: 明るい暖色グレー、 サイズ1
    // ( 同パッケージの SpinSlashTickHandler 等からも使うので package-private )
    static final net.minecraft.core.particles.DustParticleOptions DUST_KATANA =
            new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.867f, 0.835f, 0.835f), 1.0f);

    /**
     * 斬撃の粉。 攻撃に属性が載っていれば <b>属性色の dust</b>、 無属性なら既定の灰色 katana dust。
     * 属性は「武器 → 魔導書スロット」の順で解決するので、 本だけでも色が乗る。
     */
    public static net.minecraft.core.particles.ParticleOptions slashDust(Player player) {
        the_four_primitives_and_weapons.damage.ElementType elem =
                the_four_primitives_and_weapons.damage.ElementalDamageUtils.getAttackElementType(player);
        net.minecraft.core.particles.DustParticleOptions colored =
                the_four_primitives_and_weapons.damage.ElementalParticles.dustOf(elem);
        return colored != null ? colored : DUST_KATANA;
    }

    /** 突き系の見た目: 前方に dust のクラスタを3つ ( 手前→奥 ) 並べる ( 属性が載っていれば属性色 )。 */
    private static void katanaDustForward(ServerLevel sw, Player player, Vec3 look, Vec3 playerPos, double range) {
        double y = playerPos.y + player.getEyeHeight() * 0.85;
        double far = Math.max(2.0, range);
        net.minecraft.core.particles.ParticleOptions dust = slashDust(player);
        for (int k = 0; k < 3; k++) {
            double d = 1.5 + k * (far - 1.5) / 2.0;
            sw.sendParticles(dust,
                playerPos.x + look.x * d, y, playerPos.z + look.z * d,
                40, 0.5, 0.3, 0.5, 0.002);
        }
    }

    /**
     * 斬撃の見た目 ( 13-mystic-swords の katana を忠実再現 ):
     * 前方3ブロックに 左/中/右 の3クラスタを斜めに配置し、 各50個の dust を横に広く散布。
     * @param tilt 斜め具合 ( 0=横一文字 / 負=左が高い(\) / 正=右が高い(/) )。 端の高さ差 = tilt。
     */
    /** 斬撃/突き共通の 3クラスタ dust 扇。 全ての突きの見た目統一にも使う ( 突きは tilt=0 )。 */
    public static void slashCloudFan(ServerLevel sw, Player player, Vec3 look, Vec3 playerPos, double tilt) {
        double delta = fanSpread(player);
        // 攻撃に属性が載っているなら、 元の灰色 dust は出さず属性パーティクルだけで弧を描く。
        the_four_primitives_and_weapons.damage.ElementType elem =
                the_four_primitives_and_weapons.damage.ElementalDamageUtils.getAttackElementType(player);
        boolean elemental = elem != the_four_primitives_and_weapons.damage.ElementType.NONE;

        for (Vec3 p : fanPoints(player, look, playerPos, tilt)) {
            if (elemental) {
                // 灰色 dust と全く同じ範囲に撒く ( delta 1 0 1 = 横に広く・上下は広げない )。
                // 個数も emit の 1.5 倍補正込みで灰色 dust の 50 個相当になるよう 32 を渡す。
                the_four_primitives_and_weapons.damage.ElementalParticles.spawnWide(
                        sw, elem, p.x, p.y, p.z, 32, delta, 0.0);
            } else {
                // 本家: delta 1 0 1 ( 横に広く・上下は広げない ), speed 0.001, count 50
                sw.sendParticles(DUST_KATANA, p.x, p.y, p.z, 50, delta, 0.0, delta, 0.001);
            }
        }
    }

    /** 武器の攻撃範囲 ( attack_range ) による扇のスケール。 範囲が広い武器ほど大きく描く。 */
    private static double fanScale(Player player) {
        double bonus = the_four_primitives_and_weapons.skill.WeaponStatsRegistry.attackRangeBonus(player.getMainHandItem());
        return Math.max(0.4, 1.0 + bonus * 0.25);
    }

    /** 扇の各クラスタの散布幅。 */
    private static double fanSpread(Player player) {
        return 1.0 * fanScale(player);
    }

    /**
     * 斬撃の扇 ( 左 / 中央 / 右 の 3 クラスタ ) の中心座標。
     * dust ( slashCloudFan ) と属性粒子 ( spawnMotionElementParticle ) が
     * 同じ弧に重なるよう、形の定義をここに一本化する。
     *
     * @param tilt 斜め斬りの傾き ( 左右クラスタの高さ差 )
     */
    private static Vec3[] fanPoints(Player player, Vec3 look, Vec3 playerPos, double tilt) {
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
        double scale = fanScale(player);
        double fwd = 3.0 * scale;                      // ^3 ( 前方 ) を範囲でスケール
        double sideBase = 2.0 * scale;                 // 横の広がり
        double[] sides   = { -sideBase, 0.0, sideBase };
        double[] heights = { 1.2 - tilt, 1.2, 1.2 + tilt }; // ^0.7 / ^1.2 / ^1.7 ( 斜め )
        Vec3[] pts = new Vec3[3];
        for (int k = 0; k < 3; k++) {
            pts[k] = new Vec3(
                    playerPos.x + look.x * fwd + right.x * sides[k],
                    playerPos.y + heights[k],
                    playerPos.z + look.z * fwd + right.z * sides[k]);
        }
        return pts;
    }

    public static void breakBambooInPath(Level world, Vec3 startPos, Vec3 direction, double range) {
        if (world.isClientSide) return;

        // MutableBlockPos で毎回の BlockPos 生成を無くし、 過走査 ( step 0.5 ) を 1.0 に。
        // ±1 の走査幅があるので step 1.0 でも隙間なくカバーでき、 挙動はほぼ同じ・コストは大幅減。
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (double d = 0; d <= range; d += 1.0) {
            double cx = startPos.x + direction.x * d;
            double cy = startPos.y + direction.y * d;
            double cz = startPos.z + direction.z * d;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        pos.set((int) (cx + dx), (int) (cy + dy), (int) (cz + dz));
                        BlockState state = world.getBlockState(pos);
                        if (state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING)) {
                            world.destroyBlock(pos.immutable(), true);
                        }
                    }
                }
            }
        }
    }
}
