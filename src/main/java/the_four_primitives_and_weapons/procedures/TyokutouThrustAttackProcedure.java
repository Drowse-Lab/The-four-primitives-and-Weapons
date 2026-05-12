package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import the_four_primitives_and_weapons.util.DamageCalculator;

import java.util.List;
import java.util.Arrays;

public class TyokutouThrustAttackProcedure {

    // 直刀として扱うアイテムのリスト
    // 注意: 曲線ビームを使えるのはLunaItemのみ、他の直刀は高速突き攻撃のみ
    private static final List<String> STRAIGHT_SWORD_ITEMS = Arrays.asList(
        "LunaItem",           // 曲線ビーム使用可能
        "BluepurgeTyokutouItem",
        "KaminariKurikarakenTyokutouItem",
        "KurikarakenutigatanaItem",
        "KurikarakenswordItem",
        "KurikarakenItem",
        "IronTyokutoItem",
        "GoldTyokutoItem",
        "StoneTyokutoItem",
        "WoodenTyokutoItem",
        "DiamondTyokutoItem",
        "NetheriteTyokutoItem"
        // ここに他の直刀アイテムを追加
        // TyokutouSayaItemは鞘なので除外
    );

    /**
     * アイテムが直刀かどうかを判定
     */
    public static boolean isStraightSword(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemName = stack.getItem().getClass().getSimpleName();

        // 刀（曲刀）は直刀ではない - 明示的に除外
        if (itemName.contains("Katana") || itemName.contains("katana")) {
            return false;
        }

        // BluepurgeItemの場合、custom_model_data=2の時は直刀
        if (itemName.equals("BluepurgeItem")) {
            if (stack.hasTag() && stack.getTag().contains("CustomModelData")) {
                int customModelData = stack.getTag().getInt("CustomModelData");
                return customModelData == 2;
            }
            return false;
        }

        return STRAIGHT_SWORD_ITEMS.contains(itemName);
    }

    /**
     * アイテムがLunaかどうかを判定（曲線ビームを使えるのはLunaのみ）
     */
    public static boolean isLunaItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.equals("LunaItem");
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

        double range = 7.0;  // 他の刀と同じ範囲
        double damage = 18.0;  // 他の刀と同じダメージ

        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position().add(0, player.getEyeHeight(), 0);

        // 突進エフェクト
        if (world instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 10; i++) {
                double d = i * 0.6;
                serverLevel.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    startPos.x + lookVec.x * d,
                    startPos.y + lookVec.y * d,
                    startPos.z + lookVec.z * d,
                    2, 0, 0, 0, 0
                );

                if (i % 2 == 0) {
                    serverLevel.sendParticles(
                        ParticleTypes.CLOUD,
                        startPos.x + lookVec.x * d,
                        startPos.y + lookVec.y * d,
                        startPos.z + lookVec.z * d,
                        3, 0.3, 0, 0.3, 0.01
                    );
                }
            }
        }

        // 前方への突進移動（他の刀と同じ）
        player.setDeltaMovement(player.getDeltaMovement().add(lookVec.scale(1.8)));

        // 前方の敵を検索（他の刀と同じ判定）
        Vec3 playerPos = player.position();
        Vec3 endPos = playerPos.add(lookVec.scale(range));
        AABB searchArea = new AABB(playerPos.add(-2, -1, -2), endPos.add(2, 2, 2));

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            target -> {
                if (target == player) return false;
                // 前方180度の広い範囲で判定（他の刀と同じ）
                Vec3 toEntity = target.position().subtract(playerPos).normalize();
                double dot = lookVec.dot(toEntity);
                return dot > -0.2 && target.distanceTo(player) <= range;
            });

        // 敵にダメージ
        for (LivingEntity target : targets) {
            // ダメージ計算＋エンチャント＋武器効果を統一適用
            ItemStack weapon = player.getMainHandItem();
            DamageCalculator.dealDamage(player, target, (float)damage, weapon);

            // ノックバック（他の刀と同じ）
            target.setDeltaMovement(lookVec.scale(1.5).add(0, 0.4, 0));

            // ヒットエフェクト
            if (world instanceof ServerLevel serverLevel) {
                Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
                serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    targetPos.x, targetPos.y, targetPos.z,
                    8, 0.3, 0.3, 0.3, 0.05
                );
            }
        }

        // サウンド
        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 1.0f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
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
        double range = 16.0 + chargePercent * 8.0;  // 16.0～24.0（倍増）
        double damage = 35.0 + chargePercent * 20.0;  // 35.0～55.0
        double thrustPower = 0.8 + chargePercent * 0.4;  // 0.8～1.2（他の刀と同程度）

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

                // チャージ率に応じた追加エフェクト
                if (chargePercent >= 0.5f && i % 2 == 0) {
                    // チャージ率に応じてパーティクル数を調整
                    int particleCount = (int)(chargePercent * 3); // 最大3個

                    // ENCHANTED_HIT
                    serverLevel.sendParticles(
                        ParticleTypes.ENCHANTED_HIT,
                        startPos.x + lookVec.x * d,
                        startPos.y + lookVec.y * d,
                        startPos.z + lookVec.z * d,
                        particleCount, 0.15, 0.15, 0.15, 0.005
                    );

                    // 最大チャージ時はEND_RODを追加
                    if (chargePercent >= 1.0f) {
                        serverLevel.sendParticles(
                            ParticleTypes.END_ROD,
                            startPos.x + lookVec.x * d,
                            startPos.y + lookVec.y * d,
                            startPos.z + lookVec.z * d,
                            2, 0.1, 0.1, 0.1, 0.02
                        );
                    }

                    if (i % 4 == 0) {
                        serverLevel.sendParticles(
                            ParticleTypes.ELECTRIC_SPARK,
                            startPos.x + lookVec.x * d,
                            startPos.y + lookVec.y * d,
                            startPos.z + lookVec.z * d,
                            (int)(3 * chargePercent), 0.25, 0.25, 0.25, 0.03
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
        ItemStack weapon = player.getMainHandItem();
        for (LivingEntity target : targets) {
            // ダメージ計算＋エンチャント＋武器効果を統一適用
            float actualDamage = DamageCalculator.dealDamage(player, target, (float)damage, weapon);

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

        // ビームエフェクト（Lunaのみ曲線ビームを表示、他の直刀は突き攻撃のみ）
        if (world instanceof ServerLevel serverLevel) {
            // プレイヤーが持っているアイテムを取得
            ItemStack heldItem = (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY);

            // Lunaのみ曲線ビームを表示
            if (isLunaItem(heldItem)) {
                // 曲がるビームエフェクトを表示（クールダウン無視）
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

                // クールダウン中は追加で直線ビームも表示（オプション）
                if (isCooldown && chargePercent < 0.5f) {
                    // 弱いチャージの時のみ直線ビーム追加
                    createStraightBeams(serverLevel, startPos, lookVec, player, chargePercent * 0.5f, range * 0.7);
                }
            }
            // 他の直刀は曲線ビーム無し（高速突き攻撃のみ）
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

        // エフェクト（小さいDustパーティクル）
        if (world instanceof ServerLevel serverLevel) {
            // 白っぽい小さなDustパーティクル
            DustParticleOptions dustOptions = new DustParticleOptions(new Vector3f(0.9f, 0.95f, 1.0f), 0.4f);

            for (int i = 0; i < 12; i++) {
                double d = i * 0.35;
                serverLevel.sendParticles(
                    dustOptions,
                    startPos.x + lookVec.x * d,
                    startPos.y + lookVec.y * d,
                    startPos.z + lookVec.z * d,
                    2, 0.05, 0.05, 0.05, 0
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

            // ダメージ計算＋エンチャント＋武器効果を統一適用
            ItemStack weapon = player.getMainHandItem();
            DamageCalculator.dealDamage(player, target, (float)damage, weapon);

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
        // 最小3本、最大15本
        int beamCount = Math.max(3, (int)(3 + chargePercent * 12));

        Vec3 playerPos = player.position().add(0, player.getEyeHeight() * 0.8, 0);

        // プレイヤーの右ベクトルと上ベクトルを計算（垂直視線対策）
        Vec3 rightVec;
        Vec3 upVec;

        // 視線が垂直に近いかチェック
        if (Math.abs(lookVec.y) > 0.99) {
            // ほぼ真上または真下を向いている場合
            // 右ベクトルはプレイヤーの向きに基づいて計算
            float yaw = player.getYRot() * 0.017453292F;
            rightVec = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));

            if (lookVec.y > 0) {
                // 真上を向いている場合
                upVec = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
            } else {
                // 真下を向いている場合
                upVec = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
            }
        } else {
            // 通常の計算
            rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
            upVec = lookVec.cross(rightVec).normalize();
        }

        for (int i = 0; i < beamCount; i++) {
            // ビームの開始位置（プレイヤーの左右から発生）
            double sideOffset = (Math.random() - 0.5) * 2.0; // -1.0 ~ 1.0 で左右に配置
            double forwardOffset = Math.random() * -0.5; // 少し後ろから
            double heightOffset = Math.random() * 3.0 + 0.5; // 上方向のみに広がる（0.5～3.5）

            // 左右交互に、さらにランダムに広がる
            boolean isLeftSide = i % 2 == 0;
            double horizontalSpread = (isLeftSide ? -1 : 1) * (2.5 + Math.random() * 2.5);  // より広く

            Vec3 beamStart;
            if (Math.abs(lookVec.y) > 0.99) {
                // 垂直視線の場合、円形に広がる
                double angle = Math.PI * 2 * i / beamCount;
                double radius = 2.5 + Math.random() * 2.5;
                beamStart = playerPos.add(
                    Math.cos(angle) * radius,
                    lookVec.y > 0 ? -1 : 1,  // 上向きなら下から、下向きなら上から
                    Math.sin(angle) * radius
                );
            } else {
                beamStart = playerPos
                    .add(rightVec.scale(horizontalSpread))
                    .add(lookVec.scale(forwardOffset))
                    .add(0, heightOffset, 0);
            }

            // ビームの終点（ターゲット位置）
            Vec3 beamEnd = targetPos.add(
                (Math.random() - 0.5) * 0.5,
                (Math.random() - 0.5) * 0.5,
                (Math.random() - 0.5) * 0.5
            );

            // ベジェ曲線のコントロールポイント（プレイヤーから外側に広がってから敵に収束）
            Vec3 midPoint = beamStart.add(beamEnd).scale(0.5);

            // 最初は外側に大きく広がる
            Vec3 controlPoint1;
            if (Math.abs(lookVec.y) > 0.99) {
                // 垂直視線の場合、放射状に広がる
                double spreadFactor = 3.0 + Math.random() * 2.0;
                controlPoint1 = beamStart.add(
                    (beamStart.x - playerPos.x) * spreadFactor,
                    lookVec.y * 4.0,
                    (beamStart.z - playerPos.z) * spreadFactor
                );
            } else {
                controlPoint1 = beamStart.add(
                    rightVec.scale(horizontalSpread * 2.0)  // さらに外側に
                ).add(
                    lookVec.scale(4.0)  // より前方に
                ).add(
                    upVec.scale(Math.random() * 4 + 1)  // 上方向のみ（1～5）
                );
            }

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

                // パーティクルの種類（ENCHANTED_HITで統一）
                serverLevel.sendParticles(
                    ParticleTypes.ENCHANTED_HIT,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.02, 0.02, 0.02, 0
                );

                // チャージ率に応じた追加エフェクト
                if (chargePercent >= 0.5f && j % 8 == 0) {
                    serverLevel.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        particlePos.x, particlePos.y, particlePos.z,
                        (int)(2 * chargePercent), 0.1, 0.1, 0.1, 0.01
                    );
                }

                // 最大チャージ時はEND_RODを追加
                if (chargePercent >= 1.0f && j % 5 == 0) {
                    serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.05, 0.05, 0.05, 0.01
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
        // 最小4本、最大18本
        int beamCount = Math.max(4, (int)(4 + chargePercent * 14));

        Vec3 playerPos = player.position().add(0, player.getEyeHeight() * 0.8, 0);

        // プレイヤーの右ベクトルと上ベクトルを計算（垂直視線対策）
        Vec3 rightVec;
        Vec3 upVec;

        // 視線が垂直に近いかチェック
        if (Math.abs(lookVec.y) > 0.99) {
            // ほぼ真上または真下を向いている場合
            // 右ベクトルはプレイヤーの向きに基づいて計算
            float yaw = player.getYRot() * 0.017453292F;
            rightVec = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));

            if (lookVec.y > 0) {
                // 真上を向いている場合
                upVec = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
            } else {
                // 真下を向いている場合
                upVec = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
            }
        } else {
            // 通常の計算
            rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
            upVec = lookVec.cross(rightVec).normalize();
        }

        for (int i = 0; i < beamCount; i++) {
            // ビームの開始位置（プレイヤーの左右から発生）
            boolean isLeftSide = i % 2 == 0;
            double horizontalSpread = (isLeftSide ? -1 : 1) * (1.5 + Math.random() * 2.0);
            double heightOffset = Math.random() * 3.5 + 0.5;  // 上方向のみ（0.5～4.0）

            Vec3 beamStart;
            if (Math.abs(lookVec.y) > 0.99) {
                // 垂直視線の場合、円形に広がる
                double angle = Math.PI * 2 * i / beamCount;
                double radius = 2.0 + Math.random() * 2.5;
                beamStart = playerPos.add(
                    Math.cos(angle) * radius,
                    lookVec.y > 0 ? -0.5 : 0.5,  // 上向きなら少し下から、下向きなら少し上から
                    Math.sin(angle) * radius
                );
            } else {
                beamStart = playerPos
                    .add(rightVec.scale(horizontalSpread))
                    .add(lookVec.scale(Math.random() * -0.5))
                    .add(0, heightOffset, 0);
            }

            // ビームの最終目標地点（視線方向にランダムに散らばる）
            double targetDistance = range + Math.random() * 10;  // さらに長距離まで延長
            double spreadAngle = Math.toRadians(15 + Math.random() * 10); // 15-25度の広がり
            double rotationAngle = Math.random() * Math.PI * 2;

            // 円錐状に広がるように終点を計算（上方向に偏重）
            Vec3 spreadOffset;
            Vec3 beamEnd;

            if (Math.abs(lookVec.y) > 0.99) {
                // 垂直視線の場合、円錐状に広がる
                double endRadius = targetDistance * Math.tan(spreadAngle);
                spreadOffset = new Vec3(
                    Math.cos(rotationAngle) * endRadius,
                    0,
                    Math.sin(rotationAngle) * endRadius
                );
                beamEnd = playerPos
                    .add(0, lookVec.y * targetDistance, 0)
                    .add(spreadOffset);
            } else {
                spreadOffset = rightVec.scale(Math.sin(spreadAngle) * Math.cos(rotationAngle) * targetDistance * 0.3)
                    .add(upVec.scale(Math.sin(spreadAngle) * Math.abs(Math.sin(rotationAngle)) * targetDistance * 0.5));
                beamEnd = playerPos
                    .add(lookVec.scale(targetDistance))
                    .add(spreadOffset);
            }

            // 屈折しながら飛ぶベジェ曲線のコントロールポイント
            double refractCount = 2 + Math.random() * 2; // 2-4回屈折

            // 第1コントロールポイント（最初の屈折）
            Vec3 controlPoint1;
            Vec3 controlPoint2;

            if (Math.abs(lookVec.y) > 0.99) {
                // 垂直視線の場合、螺旋状に曲がる
                double cp1Angle = rotationAngle + Math.PI * 0.5;
                double cp1Radius = targetDistance * 0.4;
                controlPoint1 = playerPos.add(
                    Math.cos(cp1Angle) * cp1Radius,
                    lookVec.y * targetDistance * 0.3,
                    Math.sin(cp1Angle) * cp1Radius
                );

                double cp2Angle = rotationAngle - Math.PI * 0.5;
                double cp2Radius = targetDistance * 0.3;
                controlPoint2 = playerPos.add(
                    Math.cos(cp2Angle) * cp2Radius,
                    lookVec.y * targetDistance * 0.7,
                    Math.sin(cp2Angle) * cp2Radius
                );
            } else {
                controlPoint1 = beamStart
                    .add(rightVec.scale(horizontalSpread * 2.0))
                    .add(lookVec.scale(targetDistance * 0.3))
                    .add(upVec.scale(Math.random() * 5 + 2));  // 上方向のみ（2～7）

                controlPoint2 = playerPos
                    .add(lookVec.scale(targetDistance * 0.7))
                    .add(rightVec.scale((Math.random() - 0.5) * targetDistance * 0.4))
                    .add(upVec.scale(Math.random() * 3 + 1));  // 上方向のみ（1～4）
            }

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
                    // 中間部分（ENCHANTED_HITで統一）
                    serverLevel.sendParticles(
                        ParticleTypes.ENCHANTED_HIT,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.02, 0.02, 0.02, 0
                    );
                } else {
                    // 終端部分（散らばる）
                    serverLevel.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        particlePos.x, particlePos.y, particlePos.z,
                        (int)(2 * chargePercent), 0.2, 0.2, 0.2, 0.02
                    );
                }

                // 最大チャージ時はEND_RODを追加
                if (chargePercent >= 1.0f && j % 6 == 0) {
                    serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.05, 0.05, 0.05, 0.01
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

        ItemStack weapon = player.getMainHandItem();
        for (LivingEntity target : targets) {
            // DamageCalculatorを基準にダメージ計算＋武器効果適用
            DamageCalculator.dealDamage(player, target, damage, weapon);

            // 小さなノックバック
            Vec3 knockback = target.position().subtract(start).normalize().scale(0.3);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback));
        }
    }
}