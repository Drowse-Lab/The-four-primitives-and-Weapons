package minecraftarmorweapon.procedures;

import net.minecraft.world.level().LevelAccessor;
import net.minecraft.world.level().Level;
import net.minecraft.world.level().GameRules;
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
import net.minecraft.server.level().ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
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

import minecraftarmorweapon.util.DamageCalculator;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.entity.DarkProjectileEntity;
import minecraftarmorweapon.entity.TornadoEntity;

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
                            itemName.equals("BubbleshotItem") || itemName.equals("DarknessItem")) {
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
            target.hurt(DamageSource.playerAttack(player), 2.0f);

            // プレイヤーの向いている方向に少しノックバック
            Vec3 knockbackVec = lookVec.normalize().multiply(0.5, 0.2, 0.5); // 水平方向に0.5、上に0.2
            target.setDeltaMovement(target.getDeltaMovement().add(knockbackVec));

            // 5秒間（100tick）のThunderboltエフェクトを付与
            target.addEffect(new MobEffectInstance(
                minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects.TUNDERBOLTEFFRCT.get(),
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