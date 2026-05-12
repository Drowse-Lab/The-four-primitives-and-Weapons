package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import the_four_primitives_and_weapons.util.DamageCalculator;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.entity.DarkProjectileEntity;
import the_four_primitives_and_weapons.entity.TornadoEntity;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class MagicKatanaSpecialChargeProcedure {

    /**
     * MagischesFeenKatanaまたはMagicalKatanaの特殊チャージ攻撃を実行
     * @param world ワールド
     * @param x X座標
     * @param y Y座標
     * @param z Z座標
     * @param entity プレイヤー
     * @param chargePercent チャージ率
     */
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, float chargePercent) {
        if (!(entity instanceof Player player)) return;

        // プレイヤーのインベントリをチェック
        ItemStack specialItem = findSpecialItem(player);

        if (specialItem.isEmpty()) {
            // 特殊アイテムがない場合は通常の魔法攻撃
            executeDefaultMagicAttack(world, x, y, z, player, chargePercent);
            return;
        }

        String itemName = specialItem.getItem().getClass().getSimpleName();

        // 魔導書の属性レベルを取得（ダメージスケーリング用）
        int elementLevel = the_four_primitives_and_weapons.damage.ElementalDamageUtils.getElementLevel(specialItem);

        // アイテムに応じた特殊攻撃を実行
        switch (itemName) {
            case "StormItem":
                executeStormAttack(world, x, y, z, player, chargePercent);
                break;
            case "WindStepItem":
                executeWindStepAttack(world, x, y, z, player, chargePercent);
                break;
            case "ThunderboltItem":
                executeThunderboltAttack(world, x, y, z, player, chargePercent);
                break;
            case "FireballItem":
                executeFireballAttack(world, x, y, z, player, chargePercent);
                break;
            case "BubbleshotItem":
                executeBubbleshotAttack(world, x, y, z, player, chargePercent);
                break;
            case "DarknessItem":
                executeDarknessAttack(world, x, y, z, player, chargePercent);
                break;
            case "IceBookItem":
                executeIceAttack(world, x, y, z, player, chargePercent, elementLevel);
                break;
            case "ElectricBookItem":
                executeElectricAttack(world, x, y, z, player, chargePercent, elementLevel);
                break;
            case "CorrosionBookItem":
                executeCorrosionAttack(world, x, y, z, player, chargePercent, elementLevel);
                break;
            case "HolyBookItem":
                executeHolyAttack(world, x, y, z, player, chargePercent, elementLevel);
                break;
            case "ErrorBookItem":
                executeErrorAttack(world, x, y, z, player, chargePercent, elementLevel);
                break;
            default:
                executeDefaultMagicAttack(world, x, y, z, player, chargePercent);
                break;
        }
    }

    /**
     * Curios APIのbookスロットから特殊アイテムを検索
     */
    private static ItemStack findSpecialItem(Player player) {
        AtomicReference<ItemStack> specialItem = new AtomicReference<>(ItemStack.EMPTY);

        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            handler.getStacksHandler("book").ifPresent(stacksHandler -> {
                for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                    ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        String itemName = stack.getItem().getClass().getSimpleName();
                        if (itemName.equals("StormItem") || itemName.equals("WindStepItem") ||
                            itemName.equals("ThunderboltItem") || itemName.equals("FireballItem") ||
                            itemName.equals("BubbleshotItem") || itemName.equals("DarknessItem") ||
                            itemName.equals("IceBookItem") || itemName.equals("ElectricBookItem") ||
                            itemName.equals("CorrosionBookItem") || itemName.equals("HolyBookItem") ||
                            itemName.equals("ErrorBookItem")) {
                            specialItem.set(stack);
                            return;
                        }
                    }
                }
            });
        });

        return specialItem.get();
    }

    /**
     * StormItem - 竜巻と感電効果
     */
    private static void executeStormAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent) {
        if (!(world instanceof Level level)) return;

        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position().add(0, 0.5, 0); // 少し上から開始
        Vec3 direction = lookVec.normalize();

        // TornadoEntityを生成（Tempest風に前方に飛んでいく）
        TornadoEntity tornado = new TornadoEntity(level, player, startPos, direction, true, 5.0f, player.getItemInHand(InteractionHand.MAIN_HAND));
        tornado.setSpeed(0.3f); // 遅くして垂直な形を維持
        tornado.setLifespan(400); // 速度を下げたので寿命を延長（20マス進む）
        tornado.setRadius(3.0f); // 効果範囲3ブロック
        tornado.setMaxHeight(5.0f); // 高さ5ブロック（Tempestと同じ）

        level.addFreshEntity(tornado);

        // サウンド
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 2.0f, 0.8f);
    }

    /**
     * WindStepItem - 竜巻効果（感電なし、Tempest風に前方に飛んでいく）
     */
    private static void executeWindStepAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent) {
        if (!(world instanceof Level level)) return;

        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position().add(0, 0.5, 0); // 少し上から開始
        Vec3 direction = lookVec.normalize();

        // TornadoEntityを生成（Tempest風に前方に飛んでいく、感電なし）
        TornadoEntity tornado = new TornadoEntity(level, player, startPos, direction, false, 4.0f, player.getItemInHand(InteractionHand.MAIN_HAND));
        tornado.setSpeed(0.3f); // 遅くして垂直な形を維持
        tornado.setLifespan(400); // 速度を下げたので寿命を延長（20マス進む）
        tornado.setRadius(3.0f); // 効果範囲3ブロック
        tornado.setMaxHeight(5.0f); // 高さ5ブロック（Tempestと同じ）

        level.addFreshEntity(tornado);

        // サウンド
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 2.0f, 1.2f);
    }

    /**
     * ThunderboltItem - 横斬撃と雷撃（5秒後に雷が落ちる）
     */
    private static void executeThunderboltAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();

        // 横に広い斬撃範囲
        double width = 8.0;
        double range = 6.0;

        // 横斬撃エフェクト
        if (world instanceof ServerLevel serverLevel) {
            for (double w = -width/2; w <= width/2; w += 0.5) {
                Vec3 slashPos = playerPos.add(rightVec.scale(w)).add(lookVec.scale(range/2));
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    slashPos.x, slashPos.y + 1, slashPos.z,
                    1, 0, 0, 0, 0);
            }
        }

        // 攻撃範囲
        AABB searchArea = new AABB(
            playerPos.x - width/2, playerPos.y - 1, playerPos.z - width/2,
            playerPos.x + width/2, playerPos.y + 3, playerPos.z + width/2
        ).expandTowards(lookVec.scale(range));

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().subtract(playerPos).normalize();
                double dot = lookVec.dot(toEntity);
                return dot > 0 && entity.distanceTo(player) <= range + width/2;
            });

        for (LivingEntity target : targets) {
            // ダメージ（武器で実際に攻撃 + ボーナス2）
            player.attack(target);

            // 無敵時間をリセットしてボーナスダメージを追加
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), 2.0f);

            // プレイヤーの向いている方向に少しノックバック
            Vec3 knockbackVec = lookVec.normalize().multiply(0.5, 0.2, 0.5); // 水平方向に0.5、上に0.2
            target.setDeltaMovement(target.getDeltaMovement().add(knockbackVec));

            // 5秒間（100tick）のThunderboltエフェクトを付与
            target.addEffect(new MobEffectInstance(
                the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects.TUNDERBOLTEFFRCT.get(),
                100, // 100tick = 5秒
                0,
                false,
                false
            ));

            // 5秒間動けないように（移動速度低下 レベル10で完全停止）
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 10, false, false));

            // 初期のFLASHパーティクル
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLASH,
                    target.getX(), target.getY() + 1, target.getZ(),
                    3, 0.3, 0.5, 0.3, 0);
            }
        }

        // サウンド
        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }

    /**
     * FireballItem - 火炎弾攻撃
     */
    private static void executeFireballAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent) {
        Vec3 lookVec = player.getLookAngle();

        // gameruleで地形破壊のオンオフを確認
        boolean mobGriefing = world.getLevelData().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);

        // 火炎弾を作成
        if (world instanceof ServerLevel serverLevel) {
            LargeFireball fireball = new LargeFireball(serverLevel, player,
                lookVec.x * 4, lookVec.y * 4, lookVec.z * 4,
                mobGriefing ? 2 : 0); // 爆発力（mobGriefingがoffなら0）

            fireball.setPos(
                player.getX() + lookVec.x * 2,
                player.getY() + player.getEyeHeight(),
                player.getZ() + lookVec.z * 2
            );

            // 火炎弾のダメージを設定（武器攻撃力 + 3）
            CompoundTag tag = fireball.getPersistentData();
            tag.putFloat("BonusDamage", 3.0f); // アイテムボーナス
            tag.putString("OwnerUUID", player.getStringUUID());

            serverLevel.addFreshEntity(fireball);

            // 発射エフェクト
            serverLevel.sendParticles(ParticleTypes.FLAME,
                player.getX() + lookVec.x * 2,
                player.getY() + player.getEyeHeight(),
                player.getZ() + lookVec.z * 2,
                30, 0.5, 0.5, 0.5, 0.1);
        }

        // サウンド
        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2.0f, 0.5f);
        }
    }

    /**
     * BubbleshotItem - 方向性ノックバック
     */
    private static void executeBubbleshotAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        double range = 10.0;

        // 泡エフェクト
        if (world instanceof ServerLevel serverLevel) {
            for (double d = 0; d <= range; d += 0.5) {
                Vec3 bubblePos = playerPos.add(lookVec.scale(d));
                serverLevel.sendParticles(ParticleTypes.BUBBLE,
                    bubblePos.x, bubblePos.y + 1, bubblePos.z,
                    10, 0.3, 0.3, 0.3, 0.02);
                serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                    bubblePos.x, bubblePos.y + 1, bubblePos.z,
                    5, 0.3, 0.3, 0.3, 0.01);
            }
        }

        // 攻撃範囲
        AABB searchArea = new AABB(
            playerPos.x - 1, playerPos.y - 1, playerPos.z - 1,
            playerPos.x + 1, playerPos.y + 2, playerPos.z + 1
        ).expandTowards(lookVec.scale(range));

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().subtract(playerPos);
                double dot = lookVec.dot(toEntity.normalize());
                return dot > 0.7 && toEntity.length() <= range;
            });

        for (LivingEntity target : targets) {
            // ダメージ（武器で実際に攻撃、ボーナスなし）
            player.attack(target);

            // ターゲットが向いている方向に強力なノックバック（10マス程度）
            Vec3 targetLookVec = target.getLookAngle();
            target.setDeltaMovement(
                targetLookVec.x * 2.5,
                0.3,
                targetLookVec.z * 2.5
            );

            // 鈍足効果3
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));

            // 泡エフェクト
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.BUBBLE_POP,
                    target.getX(), target.getY() + 1, target.getZ(),
                    20, 0.5, 0.5, 0.5, 0.05);
            }
        }

        // サウンド
        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 1.5f, 1.2f);
        }
    }

    /**
     * DarknessItem - 追尾弾
     */
    private static void executeDarknessAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent) {
        if (!(world instanceof Level level)) return;

        // 視線の先のターゲットを検索
        LivingEntity target = findTargetInSight(world, player, 30.0);

        // ダメージを計算（武器攻撃力 + 7）
        float bonusDamage = 7.0f; // アイテムボーナス

        // DarkProjectileEntityを生成
        DarkProjectileEntity projectile = new DarkProjectileEntity(level, player, target, bonusDamage);

        // 初期位置を設定
        Vec3 startPos = player.position().add(0, player.getEyeHeight() - 0.1, 0);
        Vec3 lookVec = player.getLookAngle();
        projectile.setPos(
            startPos.x + lookVec.x * 1.5,
            startPos.y,
            startPos.z + lookVec.z * 1.5
        );

        // 初期速度を設定
        if (target == null) {
            // ターゲットがいない場合は直進
            projectile.setDeltaMovement(lookVec.scale(0.5));
        } else {
            // ターゲットがいる場合は、ターゲットの方向に初期速度を設定
            Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2, 0).subtract(projectile.position());
            projectile.setDeltaMovement(toTarget.normalize().scale(0.3));
        }

        // ワールドに追加
        level.addFreshEntity(projectile);

        // 発射エフェクト
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                startPos.x, startPos.y, startPos.z,
                20, 0.5, 0.5, 0.5, 0.1);

            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                startPos.x, startPos.y, startPos.z,
                30, 0.3, 0.3, 0.3, 0.05);
        }

        // サウンド
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    /**
     * IceBookItem - 前方扇状に凍結攻撃 + Slowness + Frozen
     */
    private static void executeIceAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent, int elementLevel) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        double range = 8.0;

        // 氷の嵐パーティクル（扇状に大量展開）
        if (world instanceof ServerLevel serverLevel) {
            DustParticleOptions iceDust = new DustParticleOptions(
                new Vector3f(0.6f, 0.85f, 1.0f), 1.5f);
            DustParticleOptions iceWhite = new DustParticleOptions(
                new Vector3f(0.9f, 0.95f, 1.0f), 1.0f);
            Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();

            // 起点の冷気爆発
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                playerPos.x, playerPos.y + 1, playerPos.z,
                20, 0.3, 0.3, 0.3, 0.08);

            for (double d = 0.5; d <= range; d += 0.5) {
                double spread = d * 0.4; // 距離に応じて扇状に広がる
                int count = (int)(3 + d * 2);
                Vec3 center = playerPos.add(lookVec.scale(d));

                // 左右に広がる扇状の氷結晶
                for (int i = -count; i <= count; i++) {
                    double sideOffset = (i / (double) count) * spread;
                    Vec3 pos = center.add(rightVec.scale(sideOffset));

                    // メイン雪結晶
                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                        pos.x, pos.y + 0.8 + Math.random() * 0.5, pos.z,
                        2, 0.1, 0.15, 0.1, 0.01);

                    // 氷のdust（水色）
                    if (Math.random() < 0.5) {
                        serverLevel.sendParticles(iceDust,
                            pos.x, pos.y + 0.5 + Math.random(), pos.z,
                            1, 0.05, 0.1, 0.05, 0.003);
                    }
                }

                // 中央ライン：濃い吹雪
                serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                    center.x, center.y + 0.5, center.z,
                    8, spread * 0.3, 0.3, spread * 0.3, 0.06);

                // 白い冷気の霧
                if (d % 1.5 < 0.6) {
                    serverLevel.sendParticles(iceWhite,
                        center.x, center.y + 0.3, center.z,
                        5, spread * 0.5, 0.1, spread * 0.5, 0.01);
                    serverLevel.sendParticles(ParticleTypes.CLOUD,
                        center.x, center.y + 0.2, center.z,
                        3, spread * 0.3, 0.05, spread * 0.3, 0.02);
                }
            }

            // 地面に氷の跡（END_ROD で光る氷の粒）
            for (double d = 2; d <= range; d += 1.5) {
                Vec3 pos = playerPos.add(lookVec.scale(d));
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    pos.x, pos.y + 0.1, pos.z,
                    4, d * 0.15, 0.02, d * 0.15, 0.005);
            }
        }

        AABB searchArea = new AABB(
            playerPos.x - 1, playerPos.y - 1, playerPos.z - 1,
            playerPos.x + 1, playerPos.y + 3, playerPos.z + 1
        ).expandTowards(lookVec.scale(range)).inflate(2.0);

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().subtract(playerPos).normalize();
                double dot = lookVec.dot(toEntity);
                return dot > 0.5 && entity.distanceTo(player) <= range;
            });

        float levelBonus = 1.0f + elementLevel * 0.3f; // Lv10 = 4.0x
        for (LivingEntity target : targets) {
            player.attack(target);
            // レベルに応じた追加ダメージ
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), 2.0f * levelBonus);
            // Slowness + Frozen
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, Math.min(3 + elementLevel / 3, 6), false, true));
            int maxFrozen = target.getTicksRequiredToFreeze();
            target.setTicksFrozen(Math.min(target.getTicksFrozen() + 80, maxFrozen + 40));
        }

        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 1.8f);
        }
    }

    /**
     * ElectricBookItem - チェインライトニング（最初のターゲットから周囲に連鎖）
     */
    private static void executeElectricAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent, int elementLevel) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        double range = 12.0;

        // 最初のターゲットを視線で探す
        LivingEntity primaryTarget = findTargetInSight(world, player, range);

        if (primaryTarget != null) {
            // プライマリターゲットに攻撃
            float elecBonus = 1.0f + elementLevel * 0.3f;
            player.attack(primaryTarget);
            primaryTarget.invulnerableTime = 0;
            primaryTarget.hurt(player.damageSources().playerAttack(player), 3.0f * elecBonus);

            // 電撃エフェクト（プライマリ：大量スパーク + FLASH + 黄色dust）
            if (world instanceof ServerLevel serverLevel) {
                DustParticleOptions elecYellow = new DustParticleOptions(
                    new Vector3f(1.0f, 1.0f, 0.3f), 1.5f);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    primaryTarget.getX(), primaryTarget.getY() + 1, primaryTarget.getZ(),
                    60, 0.5, 0.8, 0.5, 0.4);
                serverLevel.sendParticles(ParticleTypes.FLASH,
                    primaryTarget.getX(), primaryTarget.getY() + 1, primaryTarget.getZ(),
                    2, 0, 0, 0, 0);
                serverLevel.sendParticles(elecYellow,
                    primaryTarget.getX(), primaryTarget.getY() + 1, primaryTarget.getZ(),
                    25, 0.4, 0.6, 0.4, 0.05);

                // プレイヤーからターゲットへの稲妻ライン
                Vec3 toTarget = primaryTarget.position().subtract(playerPos);
                for (double t = 0; t < 1.0; t += 0.08) {
                    Vec3 linePos = playerPos.add(toTarget.scale(t));
                    double jitter = 0.15;
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        linePos.x + (Math.random() - 0.5) * jitter,
                        linePos.y + 1 + (Math.random() - 0.5) * jitter,
                        linePos.z + (Math.random() - 0.5) * jitter,
                        3, 0.02, 0.02, 0.02, 0.1);
                }
            }

            // チェインライトニング: 周囲5ブロック内の敵にも連鎖
            double chainRange = 5.0;
            AABB chainArea = primaryTarget.getBoundingBox().inflate(chainRange);
            List<LivingEntity> chainTargets = world.getEntitiesOfClass(LivingEntity.class, chainArea,
                entity -> entity != player && entity != primaryTarget && entity.distanceTo(primaryTarget) <= chainRange);

            LivingEntity prevTarget = primaryTarget;
            int chainCount = 0;
            for (LivingEntity chainTarget : chainTargets) {
                if (chainCount >= 4 + elementLevel / 3) break;
                chainTarget.hurt(player.damageSources().playerAttack(player), 4.0f * elecBonus);

                if (world instanceof ServerLevel serverLevel) {
                    // チェイン先にスパーク
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        chainTarget.getX(), chainTarget.getY() + 1, chainTarget.getZ(),
                        30, 0.3, 0.5, 0.3, 0.3);
                    // チェイン間の稲妻ライン
                    Vec3 chainLine = chainTarget.position().subtract(prevTarget.position());
                    for (double t = 0; t < 1.0; t += 0.15) {
                        Vec3 lp = prevTarget.position().add(chainLine.scale(t));
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            lp.x, lp.y + 1, lp.z, 2, 0.05, 0.05, 0.05, 0.08);
                    }
                }
                prevTarget = chainTarget;
                chainCount++;
            }
        } else {
            // ターゲットがいない場合は前方に放電（派手版）
            if (world instanceof ServerLevel serverLevel) {
                DustParticleOptions elecYellow = new DustParticleOptions(
                    new Vector3f(1.0f, 1.0f, 0.3f), 1.2f);
                for (double d = 0.5; d <= range; d += 0.6) {
                    Vec3 pos = playerPos.add(lookVec.scale(d));
                    double jitter = d * 0.08;
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.x + (Math.random() - 0.5) * jitter,
                        pos.y + 1 + (Math.random() - 0.5) * jitter,
                        pos.z + (Math.random() - 0.5) * jitter,
                        8, 0.15, 0.2, 0.15, 0.15);
                    if (d % 2.0 < 0.7) {
                        serverLevel.sendParticles(elecYellow,
                            pos.x, pos.y + 1, pos.z,
                            3, 0.1, 0.15, 0.1, 0.02);
                    }
                }
                serverLevel.sendParticles(ParticleTypes.FLASH,
                    playerPos.x + lookVec.x * 2, playerPos.y + 1, playerPos.z + lookVec.z * 2,
                    1, 0, 0, 0, 0);
            }
        }

        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.8f);
        }
    }

    /**
     * CorrosionBookItem - 結晶侵食ブレス（fire breath風のコーン状噴射）
     * プレイヤーの視線方向に扇状に結晶の破片が広がり、当たった敵を結晶化して蝕む
     */
    private static void executeCorrosionAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent, int elementLevel) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        double range = 10.0;
        double coneAngle = Math.toRadians(30); // コーンの半角30度

        // 視線方向の右・上ベクトルを計算
        Vec3 forward = lookVec.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(up).normalize();
        if (right.lengthSqr() < 0.001) {
            right = new Vec3(1, 0, 0);
        }
        Vec3 coneUp = right.cross(forward).normalize();

        DustParticleOptions crystalDust = new DustParticleOptions(
            new Vector3f(0.55f, 0.15f, 0.65f), 1.5f);
        DustParticleOptions tipDust = new DustParticleOptions(
            new Vector3f(0.8f, 0.3f, 0.9f), 0.8f);
        DustParticleOptions darkDust = new DustParticleOptions(
            new Vector3f(0.3f, 0.05f, 0.4f), 1.0f);

        if (world instanceof ServerLevel serverLevel) {
            Vec3 startPos = playerPos.add(0, player.getEyeHeight() * 0.7, 0);

            // ブレス状に結晶パーティクルをコーン放射
            for (double d = 1.0; d <= range; d += 0.6) {
                // 距離に応じて広がる半径
                double spreadRadius = Math.tan(coneAngle) * d;
                // 距離が遠いほどパーティクル数を増やす（コーンを密に埋める）
                int ringCount = (int)(4 + d * 2.5);

                for (int i = 0; i < ringCount; i++) {
                    double angle = (i / (double) ringCount) * Math.PI * 2;
                    double offsetR = spreadRadius * Math.sqrt(Math.random()); // 均等分布
                    double ox = Math.cos(angle) * offsetR;
                    double oy = Math.sin(angle) * offsetR;

                    Vec3 particlePos = startPos
                        .add(forward.scale(d))
                        .add(right.scale(ox))
                        .add(coneUp.scale(oy));

                    // メイン結晶粒子（濃い紫）
                    serverLevel.sendParticles(crystalDust,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.05, 0.05, 0.05, 0.005);

                    // ランダムに明るい結晶の破片
                    if (Math.random() < 0.3) {
                        serverLevel.sendParticles(tipDust,
                            particlePos.x, particlePos.y, particlePos.z,
                            1, 0.02, 0.02, 0.02, 0.002);
                    }
                }

                // コーン中心ライン沿いに暗い侵食粒子
                Vec3 centerPos = startPos.add(forward.scale(d));
                serverLevel.sendParticles(darkDust,
                    centerPos.x, centerPos.y, centerPos.z,
                    2, 0.1, 0.1, 0.1, 0.01);

                // 遠くでイカスミ（侵食の残り香）
                if (d > range * 0.5 && Math.random() < 0.4) {
                    serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                        centerPos.x, centerPos.y, centerPos.z,
                        1, 0.2, 0.2, 0.2, 0.03);
                }
            }
        }

        // 攻撃範囲: コーン内の敵を検出
        Vec3 eyePos = playerPos.add(0, player.getEyeHeight() * 0.7, 0);
        AABB searchArea = new AABB(eyePos, eyePos).inflate(range + 2);

        double cosAngle = Math.cos(coneAngle);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.getEyePosition().subtract(eyePos);
                double dist = toEntity.length();
                if (dist > range || dist < 0.5) return false;
                double dot = forward.dot(toEntity.normalize());
                return dot >= cosAngle; // コーン内判定
            });

        float corrBonus = 1.0f + elementLevel * 0.3f;
        for (LivingEntity target : targets) {
            player.attack(target);
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), 2.0f * corrBonus);

            // 結晶侵食デバフ（レベルで効果時間・強度UP）
            int durScale = 1 + elementLevel / 4;
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140 * durScale, Math.min(2 + elementLevel / 3, 5), false, true));
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100 * durScale, Math.min(1 + elementLevel / 4, 3), false, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80 * durScale, Math.min(1 + elementLevel / 3, 4), false, true));

            // ヒットした敵に結晶化エフェクト（敵を覆う紫結晶の爆発）
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(crystalDust,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    50, 0.4, 0.6, 0.4, 0.04);
                serverLevel.sendParticles(tipDust,
                    target.getX(), target.getY() + target.getBbHeight(), target.getZ(),
                    20, 0.2, 0.4, 0.2, 0.03);
                serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    target.getX(), target.getY() + 0.3, target.getZ(),
                    15, 0.3, 0.1, 0.3, 0.06);
                // 結晶柱が敵から突き出る
                for (int spike = 0; spike < 6; spike++) {
                    double sAngle = spike * Math.PI / 3;
                    serverLevel.sendParticles(crystalDust,
                        target.getX() + Math.cos(sAngle) * 0.5,
                        target.getY() + 0.5 + spike * 0.2,
                        target.getZ() + Math.sin(sAngle) * 0.5,
                        8, 0.05, 0.3, 0.05, 0.01);
                }
            }
        }

        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 2.0f, 0.6f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 1.5f, 0.4f);
        }
    }

    /**
     * HolyBookItem - 聖なる裁き（前方範囲 + アンデッドに特効 + 自己回復）
     * 金色の光柱が降り注ぎ、聖なる魔法陣から浄化の光が放たれる
     */
    private static void executeHolyAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent, int elementLevel) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        double range = 8.0;

        // 聖なる光のパーティクル
        if (world instanceof ServerLevel serverLevel) {
            // 足元に金色の魔法陣（円形dustパーティクル）
            DustParticleOptions holyGold = new DustParticleOptions(
                new Vector3f(1.0f, 0.85f, 0.3f), 1.2f);
            DustParticleOptions holyWhite = new DustParticleOptions(
                new Vector3f(1.0f, 1.0f, 0.9f), 0.8f);
            for (int i = 0; i < 360; i += 10) {
                double rad = Math.toRadians(i);
                double circleR = 2.5;
                serverLevel.sendParticles(holyGold,
                    playerPos.x + Math.cos(rad) * circleR,
                    playerPos.y + 0.1,
                    playerPos.z + Math.sin(rad) * circleR,
                    1, 0, 0, 0, 0);
            }

            // 天から降り注ぐ光の柱（プレイヤー上空）
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                playerPos.x, playerPos.y + 4, playerPos.z,
                30, 0.5, 1.5, 0.5, 0.03);

            // 前方に向かって聖光のビーム
            for (double d = 1; d <= range; d += 0.4) {
                Vec3 pos = playerPos.add(lookVec.scale(d));

                // 金色のdust（メインビーム）
                serverLevel.sendParticles(holyGold,
                    pos.x, pos.y + 1, pos.z,
                    2, 0.15, 0.2, 0.15, 0.005);

                // 白い輝き（WAX_ON = 柔らかい光粒）
                serverLevel.sendParticles(ParticleTypes.WAX_ON,
                    pos.x, pos.y + 1, pos.z,
                    1, 0.1, 0.15, 0.1, 0.01);

                // 一定間隔で上空から光の筋
                if (d % 2.0 < 0.5) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                        pos.x, pos.y + 3, pos.z,
                        5, 0.1, 1.0, 0.1, 0.01);
                    serverLevel.sendParticles(holyWhite,
                        pos.x, pos.y + 1.5, pos.z,
                        3, 0.05, 0.5, 0.05, 0.002);
                }
            }
        }

        AABB searchArea = new AABB(
            playerPos.x - 1, playerPos.y - 1, playerPos.z - 1,
            playerPos.x + 1, playerPos.y + 3, playerPos.z + 1
        ).expandTowards(lookVec.scale(range)).inflate(2.0);

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().subtract(playerPos).normalize();
                double dot = lookVec.dot(toEntity);
                return dot > 0.4 && entity.distanceTo(player) <= range;
            });

        for (LivingEntity target : targets) {
            player.attack(target);

            // レベルに応じた追加ダメージ
            float holyBonus = 1.0f + elementLevel * 0.3f;
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), 2.0f * holyBonus);

            // アンデッドにはさらに追加ダメージ（発火なし）
            if (target.isInvertedHealAndHarm()) {
                target.invulnerableTime = 0;
                target.hurt(player.damageSources().playerAttack(player), 6.0f * holyBonus);
            }

            // 発光効果
            target.setGlowingTag(true);

            // 被弾エフェクト（金色の光柱が敵を包み込む）
            if (world instanceof ServerLevel serverLevel) {
                DustParticleOptions hitGold = new DustParticleOptions(
                    new Vector3f(1.0f, 0.85f, 0.3f), 1.5f);
                // 敵を包む金色の光球
                serverLevel.sendParticles(hitGold,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    40, 0.4, 0.6, 0.4, 0.04);
                // 上に昇る光の柱
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY(), target.getZ(),
                    25, 0.15, 1.5, 0.15, 0.03);
                // 足元の光の輪
                for (int ring = 0; ring < 360; ring += 30) {
                    double rr = Math.toRadians(ring);
                    serverLevel.sendParticles(ParticleTypes.WAX_ON,
                        target.getX() + Math.cos(rr) * 0.8,
                        target.getY() + 0.1,
                        target.getZ() + Math.sin(rr) * 0.8,
                        2, 0, 0.02, 0, 0.005);
                }
                // ENCHANTED_HIT で輝き
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    15, 0.3, 0.4, 0.3, 0.1);
            }
        }

        // 自己回復（HP 4回復）
        player.heal(4.0f);

        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }

    /**
     * ErrorBookItem - グリッチ攻撃（ランダム範囲 + ランダム効果 + テレポート）
     */
    private static void executeErrorAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent, int elementLevel) {
        Vec3 playerPos = player.position();
        Vec3 lookVec = player.getLookAngle();
        Vec3 eyePos = player.getEyePosition();

        // 斬撃の到達距離
        double slashRange = 20.0;
        float errBonus = 1.0f + elementLevel * 0.3f;

        // === 交互の斬撃方向を決定 ===
        // NBTで交互カウンタを管理（0=右上→中央, 1=左上→中央）
        CompoundTag data = player.getPersistentData();
        int slashSide = data.getInt("ErrorSlashSide");
        data.putInt("ErrorSlashSide", slashSide == 0 ? 1 : 0);

        // === 視線方向に対する右・上ベクトルを計算 ===
        Vec3 forward = lookVec.normalize();
        Vec3 worldUp = new Vec3(0, 1, 0);
        // 真上/真下を向いている場合の対策
        if (Math.abs(forward.y) > 0.99) {
            worldUp = new Vec3(0, 0, 1);
        }
        Vec3 right = forward.cross(worldUp).normalize();
        Vec3 up = right.cross(forward).normalize();

        // 斬撃の傾き角度（少し傾いた縦斬り）
        // side=0: 右上→中央（＼の形）、side=1: 左上→中央（／の形）
        double tiltAngle = Math.toRadians(25); // 25度傾き

        // 斬撃サイズ（大きく）
        double slashSize = 5.5;  // 斬撃の長さ（上端→下端）
        double endSize = 1.2;    // 下端の広がり

        // 斬撃ラインの始点と終点（ローカル座標、rightとupの係数）
        double startR, startU, endR, endU;
        if (slashSide == 0) {
            // 右上から中央やや左下へ（＼）
            startR = Math.sin(tiltAngle) * slashSize;
            startU = Math.cos(tiltAngle) * slashSize;
            endR = -Math.sin(tiltAngle) * endSize;
            endU = -Math.cos(tiltAngle) * endSize;
        } else {
            // 左上から中央やや右下へ（／）
            startR = -Math.sin(tiltAngle) * slashSize;
            startU = Math.cos(tiltAngle) * slashSize;
            endR = Math.sin(tiltAngle) * endSize;
            endU = -Math.cos(tiltAngle) * endSize;
        }

        // 斬撃カラー（薄い1本線）
        DustParticleOptions slashCore = new DustParticleOptions(
            new Vector3f(0.8f, 0.0f, 0.0f), 1.2f);     // 赤エラーの芯
        DustParticleOptions slashEdge = new DustParticleOptions(
            new Vector3f(1.0f, 0.15f, 0.1f), 0.8f);     // 明るい赤の縁

        // === 斬撃パーティクル生成 ===
        if (world instanceof ServerLevel serverLevel) {
            // 軸のノイズ：不規則な間隔でカクッとずれるデジタルノイズ
            // ずれの区間をランダムな長さで事前生成
            double currentR = 0, currentU = 0;
            double nextShiftDist = 1.0 + Math.random() * 1.5; // 最初のずれが起きる距離
            double shiftR = 0, shiftU = 0;

            for (double dist = 1.0; dist <= slashRange; dist += 0.35) {
                // 前方の基準点
                double baseX = eyePos.x + forward.x * dist;
                double baseY = eyePos.y + forward.y * dist;
                double baseZ = eyePos.z + forward.z * dist;

                // 次のずれポイントに達したらカクッと新しいオフセットに変わる
                if (dist >= nextShiftDist) {
                    shiftR = (Math.random() - 0.5) * 0.9;
                    shiftU = (Math.random() - 0.5) * 0.7;
                    // 次にずれるまでの距離もランダム（不規則な間隔）
                    nextShiftDist = dist + 0.7 + Math.random() * 2.5;
                }
                double axisR = shiftR;
                double axisU = shiftU;

                // 斬撃ラインに沿ってパーティクルを配置
                int slashPoints = 18;
                for (int s = 0; s < slashPoints; s++) {
                    double t = s / (double)(slashPoints - 1);
                    // 斬撃ライン上の位置 + 軸ノイズ（斬撃の形を保ったまま位置がずれる）
                    double localR = startR + (endR - startR) * t + axisR;
                    double localU = startU + (endU - startU) * t + axisU;

                    double px = baseX + right.x * localR + up.x * localU;
                    double py = baseY + right.y * localR + up.y * localU;
                    double pz = baseZ + right.z * localR + up.z * localU;

                    // 芯の暗黒パーティクル
                    serverLevel.sendParticles(slashCore,
                        px, py, pz, 1, 0.005, 0.005, 0.005, 0);

                    // 縁の紫パーティクル（片側だけ、薄く）
                    if (s % 2 == 0) {
                        double edgeOffset = 0.06;
                        serverLevel.sendParticles(slashEdge,
                            px + right.x * edgeOffset, py + right.y * edgeOffset, pz + right.z * edgeOffset,
                            1, 0.005, 0.005, 0.005, 0);
                    }
                }
            }

            // プレイヤーの手元にフラッシュ
            serverLevel.sendParticles(ParticleTypes.FLASH,
                eyePos.x + forward.x * 0.5, eyePos.y + forward.y * 0.5, eyePos.z + forward.z * 0.5,
                1, 0, 0, 0, 0);
        }

        // === 斬撃の軌道上にいる敵にダメージ ===
        // 斬撃の幅（斬撃ラインの太さ）
        double slashWidth = 2.0;
        AABB searchArea = new AABB(
            eyePos.x - slashRange, eyePos.y - slashRange, eyePos.z - slashRange,
            eyePos.x + slashRange, eyePos.y + slashRange, eyePos.z + slashRange
        );

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                // 前方判定：視線方向のdotが正
                Vec3 toEntity = entity.position().add(0, entity.getBbHeight() / 2, 0).subtract(eyePos);
                double dot = forward.dot(toEntity.normalize());
                if (dot < 0.5) return false; // 前方約60度以内
                // 距離判定
                double entityDist = toEntity.length();
                if (entityDist > slashRange) return false;
                // 斬撃幅判定：視線からの垂直距離
                double alongForward = toEntity.dot(forward);
                Vec3 closestOnLine = new Vec3(
                    eyePos.x + forward.x * alongForward,
                    eyePos.y + forward.y * alongForward,
                    eyePos.z + forward.z * alongForward);
                double perpDist = entity.position().add(0, entity.getBbHeight() / 2, 0).distanceTo(closestOnLine);
                return perpDist <= slashWidth;
            });

        for (LivingEntity target : targets) {
            float damage = (12.0f + (float)(Math.random() * 8.0)) * errBonus;
            target.hurt(player.damageSources().playerAttack(player), damage);

            // ヒットエフェクト：敵の体に斬撃跡
            if (world instanceof ServerLevel serverLevel) {
                Vec3 tPos = target.position();
                // 斬撃跡ライン
                for (int seg = 0; seg < 8; seg++) {
                    double t = seg / 7.0;
                    double lr = startR + (endR - startR) * t;
                    double lu = startU + (endU - startU) * t;
                    double hx = tPos.x + right.x * lr * 0.6 + up.x * lu * 0.6;
                    double hy = tPos.y + target.getBbHeight() / 2 + right.y * lr * 0.6 + up.y * lu * 0.6;
                    double hz = tPos.z + right.z * lr * 0.6 + up.z * lu * 0.6;
                    serverLevel.sendParticles(slashCore,
                        hx + (Math.random() - 0.5) * 0.1, hy + (Math.random() - 0.5) * 0.1, hz,
                        2, 0.02, 0.02, 0.02, 0);
                    serverLevel.sendParticles(slashEdge,
                        hx + (Math.random() - 0.5) * 0.15, hy + (Math.random() - 0.5) * 0.15, hz,
                        1, 0.02, 0.02, 0.02, 0);
                }
                // REVERSE_PORTALの散布
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    tPos.x, tPos.y + target.getBbHeight() / 2, tPos.z,
                    15, 0.3, 0.4, 0.3, 0.5);
            }

            // デバフ（空間に切り裂かれた影響）
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, true));
        }

        // サウンド
        if (world instanceof Level level) {
            // 空間を切り裂く音
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.5f);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }

    /**
     * デフォルトの魔法攻撃
     */
    private static void executeDefaultMagicAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        double range = 6.0;

        // 魔法陣エフェクト
        if (world instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 360; i += 30) {
                double rad = Math.toRadians(i);
                serverLevel.sendParticles(ParticleTypes.WITCH,
                    playerPos.x + Math.cos(rad) * 2,
                    playerPos.y,
                    playerPos.z + Math.sin(rad) * 2,
                    1, 0, 0.1, 0, 0.01);
            }
        }

        // 範囲攻撃
        AABB searchArea = new AABB(
            playerPos.x - range, playerPos.y - 1, playerPos.z - range,
            playerPos.x + range, playerPos.y + 3, playerPos.z + range
        );

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> entity != player && entity.distanceTo(player) <= range);

        for (LivingEntity target : targets) {
            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            float damage = 15.0f * (1.0f + chargePercent);
            DamageCalculator.dealDamage(player, target, damage, weapon);
        }

        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    /**
     * 視線の先のターゲットを検索
     */
    private static LivingEntity findTargetInSight(LevelAccessor world, Player player, double maxRange) {
        Vec3 start = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 end = start.add(lookVec.scale(maxRange));

        AABB searchArea = new AABB(start, end).inflate(1.0);

        List<LivingEntity> potentialTargets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().add(0, entity.getBbHeight() / 2, 0).subtract(start);
                double dot = lookVec.dot(toEntity.normalize());
                return dot > 0.95 && toEntity.length() <= maxRange;
            });

        // 最も近いターゲットを返す
        return potentialTargets.stream()
            .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
            .orElse(null);
    }

    // createHomingProjectile メソッドは削除（DarkProjectileEntityで置き換え）
}