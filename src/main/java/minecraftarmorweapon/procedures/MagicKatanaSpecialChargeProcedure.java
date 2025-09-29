package minecraftarmorweapon.procedures;

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
import minecraftarmorweapon.init.MinecraftArmorWeaponModCustomEntities;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;
import minecraftarmorweapon.entity.DarkProjectileEntity;
import minecraftarmorweapon.entity.TornadoEntity;

import java.util.List;
import java.util.ArrayList;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class MagicKatanaSpecialChargeProcedure {

    /**
     * Execute special charge attack for MagischesFeenKatana or MagicalKatana
     * @param world World instance
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param entity Player entity
     * @param chargePercent Charge percentage
     */
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, float chargePercent) {
        if (!(entity instanceof Player player)) return;

        // Check player's inventory
        ItemStack specialItem = findSpecialItem(player);

        if (specialItem.isEmpty()) {
            // Execute default magic attack if no special item
            executeDefaultMagicAttack(world, x, y, z, player, chargePercent);
            return;
        }

        String itemName = specialItem.getItem().getClass().getSimpleName();

        // Execute special attack based on item type
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
     * Find special item from player's Curios slot (book)
     */
    private static ItemStack findSpecialItem(Player player) {
        // Curiosスロットから取得
        if (player.level.isClientSide) return ItemStack.EMPTY;

        // Curiosの"book"スロットをチェック
        top.theillusivec4.curios.api.CuriosApi.getCuriosHelper().getEquippedCurios(player).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    String itemName = stack.getItem().getClass().getSimpleName();
                    if (itemName.equals("StormItem") || itemName.equals("WindStepItem") ||
                        itemName.equals("ThunderboltItem") || itemName.equals("FireballItem") ||
                        itemName.equals("BubbleshotItem") || itemName.equals("DarknessItem")) {
                        return;
                    }
                }
            }
        });

        // Curiosの"book"スロット専用の取得
        var curiosOptional = top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
            .findFirstCurio(player, stack -> {
                String itemName = stack.getItem().getClass().getSimpleName();
                return itemName.equals("StormItem") || itemName.equals("WindStepItem") ||
                       itemName.equals("ThunderboltItem") || itemName.equals("FireballItem") ||
                       itemName.equals("BubbleshotItem") || itemName.equals("DarknessItem");
            });

        return curiosOptional.map(slotResult -> slotResult.stack()).orElse(ItemStack.EMPTY);
    }

    /**
     * StormItem - Tornado with electric shock effect (using TornadoEntity)
     */
    private static void executeStormAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent) {
        if (!(world instanceof Level level)) return;

        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position().add(lookVec.scale(2.0));

        // Get weapon attack damage
        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        float baseDamage = getWeaponDamage(weapon);

        // Calculate full damage with DamageCalculator (including enchantments and player attributes)
        float calculatedDamage = DamageCalculator.calculateDamage(player, null, baseDamage + 5.0f, weapon);
        float damage = calculatedDamage * (1.0f + chargePercent);

        // Generate tornado entity (with electricity effect)
        TornadoEntity tornado = new TornadoEntity(MinecraftArmorWeaponModCustomEntities.TORNADO.get(), level);
        tornado.setOwner(player);
        tornado.setPos(startPos.x, startPos.y, startPos.z);
        tornado.setDirection(lookVec);
        tornado.setWithElectricity(true);
        tornado.setDamage(damage);
        tornado.setWeapon(weapon);
        tornado.setSpeed(0.8f); // 高速移動
        tornado.setLifespan(200); // 10秒間
        tornado.setRadius(4.0f);
        tornado.setMaxHeight(15.0f);

        // ワールドに追加
        level.addFreshEntity(tornado);

        // 追加の竜巻を生成（扇状に3つ）
        for (int i = -1; i <= 1; i++) {
            if (i == 0) continue; // 中央は既に生成済み

            // 左右15度ずつずらした方向
            double angle = Math.toRadians(i * 15);
            double newX = lookVec.x * Math.cos(angle) - lookVec.z * Math.sin(angle);
            double newZ = lookVec.x * Math.sin(angle) + lookVec.z * Math.cos(angle);
            Vec3 newDirection = new Vec3(newX, lookVec.y, newZ).normalize();
            Vec3 sideStartPos = player.position().add(newDirection.scale(2.0));

            TornadoEntity sideTornado = new TornadoEntity(MinecraftArmorWeaponModCustomEntities.TORNADO.get(), level);
            sideTornado.setOwner(player);
            sideTornado.setPos(sideStartPos.x, sideStartPos.y, sideStartPos.z);
            sideTornado.setDirection(newDirection);
            sideTornado.setWithElectricity(true);
            sideTornado.setDamage(damage * 0.8f);
            sideTornado.setWeapon(weapon);
            sideTornado.setSpeed(0.8f);
            sideTornado.setLifespan(160); // 少し短め
            sideTornado.setRadius(3.0f); // 少し小さめ
            sideTornado.setMaxHeight(12.0f);

            level.addFreshEntity(sideTornado);
        }

        // サウンド
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    /**
     * WindStepItem - Tornado effect without electricity (using TornadoEntity)
     */
    private static void executeWindStepAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent) {
        if (!(world instanceof Level level)) return;

        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.position().add(lookVec.scale(2.0));

        // Get weapon attack damage
        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        float baseDamage = getWeaponDamage(weapon);

        // DamageCalculatorで完全なダメージ計算
        float calculatedDamage = DamageCalculator.calculateDamage(player, null, baseDamage + 4.0f, weapon);
        float damage = calculatedDamage * (1.0f + chargePercent);

        // Generate tornado entity (without electricity effect)
        TornadoEntity tornado = new TornadoEntity(MinecraftArmorWeaponModCustomEntities.TORNADO.get(), level);
        tornado.setOwner(player);
        tornado.setPos(startPos.x, startPos.y, startPos.z);
        tornado.setDirection(lookVec);
        tornado.setWithElectricity(false);
        tornado.setDamage(damage);
        tornado.setWeapon(weapon);
        tornado.setSpeed(1.0f); // より高速
        tornado.setLifespan(240); // 12秒間
        tornado.setRadius(5.0f); // 大きめ
        tornado.setMaxHeight(20.0f); // 高め

        // ワールドに追加
        level.addFreshEntity(tornado);

        // サウンド
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    /**
     * ThunderboltItem - Horizontal slash with lightning strike
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
            // ダメージ計算（エンチャントとKill効果を含む）
            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            float baseDamage = getWeaponDamage(weapon);
            float damage = DamageCalculator.calculateDamage(player, target, baseDamage + 2.0f, weapon) * (1.0f + chargePercent);

            // ダメージを与えてエンチャント効果も適用
            DamageCalculator.dealDamage(player, target, damage, weapon);
            DamageCalculator.applyWeaponEffects(player, target, damage, weapon);

            // 3秒間のフラッシュ点滅効果の後に雷を落とす
            if (world instanceof ServerLevel serverLevel) {
                final LivingEntity finalTarget = target;

                // 点滅効果のタスクを作成（3秒間、0.15秒ごとに点滅）
                Runnable blinkTask = new Runnable() {
                    private int tickCount = 0;
                    private final int maxTicks = 60; // 3秒間（60ティック）

                    @Override
                    public void run() {
                        if (tickCount < maxTicks) {
                            // 3ティックごとに点滅（0.15秒ごと）
                            if (tickCount % 3 == 0) {
                                // FLASHパーティクル（点滅効果）
                                serverLevel.sendParticles(ParticleTypes.FLASH,
                                    finalTarget.getX(),
                                    finalTarget.getY() + 1,
                                    finalTarget.getZ(),
                                    1, 0, 0, 0, 0);

                                // 電気のスパークエフェクト
                                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                    finalTarget.getX(), finalTarget.getY() + 1, finalTarget.getZ(),
                                    10, 0.5, 0.5, 0.5, 0.1);
                            }

                            tickCount++;

                            // 次のティックでも実行
                            serverLevel.getServer().execute(this);
                        } else {
                            // 3秒経過後、雷を落とす
                            // 複数の雷を落とす
                            for (int i = 0; i < 5; i++) {
                                // 大きなFLASHパーティクル
                                serverLevel.sendParticles(ParticleTypes.FLASH,
                                    finalTarget.getX() + (Math.random() - 0.5) * 2,
                                    finalTarget.getY() + 1,
                                    finalTarget.getZ() + (Math.random() - 0.5) * 2,
                                    3, 0, 0, 0, 0);

                                // 実際の雷を召喚
                                if (i == 0) { // 最初の1本だけ実際の雷
                                    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
                                    if (lightning != null) {
                                        lightning.moveTo(Vec3.atBottomCenterOf(finalTarget.blockPosition()));
                                        lightning.setVisualOnly(false);
                                        serverLevel.addFreshEntity(lightning);
                                    }
                                }
                            }

                            // 追加エフェクト（大量の電気スパーク）
                            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                finalTarget.getX(), finalTarget.getY() + 1, finalTarget.getZ(),
                                100, 1.0, 1.0, 1.0, 0.3);

                            // 雷のサウンド
                            serverLevel.playSound(null, finalTarget.getX(), finalTarget.getY(), finalTarget.getZ(),
                                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 1.0f);
                        }
                    }
                };

                // タスクを開始
                serverLevel.getServer().execute(blinkTask);
            }
        }

        // サウンド
        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.5f);
        }
    }

    /**
     * FireballItem - Fireball attack
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

            // 火炎弾のダメージを設定
            CompoundTag tag = fireball.getPersistentData();
            tag.putFloat("CustomDamage", 25.0f * (1.0f + chargePercent));

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
     * BubbleshotItem - Directional knockback
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
            // ダメージ計算（エンチャントとKill効果を含む）
            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            float baseDamage = getWeaponDamage(weapon);
            float damage = DamageCalculator.calculateDamage(player, target, baseDamage, weapon) * (1.0f + chargePercent);

            // ダメージを与えてエンチャント効果も適用
            DamageCalculator.dealDamage(player, target, damage, weapon);
            DamageCalculator.applyWeaponEffects(player, target, damage, weapon);

            // Slowness III
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));

            // Strong knockback away from player
            Vec3 knockbackDirection = target.position().subtract(playerPos).normalize();
            target.setDeltaMovement(
                knockbackDirection.x * 3.0,
                0.5,
                knockbackDirection.z * 3.0
            );

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
     * DarknessItem - Homing projectile
     */
    private static void executeDarknessAttack(LevelAccessor world, double x, double y, double z, Player player, float chargePercent) {
        if (!(world instanceof Level level)) return;

        // 視線の先のターゲットを検索
        LivingEntity target = findTargetInSight(world, player, 30.0);

        // ダメージを計算（エンチャント効果込み）
        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        float baseDamage = getWeaponDamage(weapon);
        float damage = DamageCalculator.calculateDamage(player, target, baseDamage + 7.0f, weapon) * (1.0f + chargePercent);

        // Generate dark projectile entity
        DarkProjectileEntity projectile = new DarkProjectileEntity(MinecraftArmorWeaponModCustomEntities.DARK_PROJECTILE.get(), level);
        projectile.setOwner(player);
        projectile.setTarget(target);
        projectile.setDamage(damage);
        projectile.setWeapon(weapon);

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
     * Default magic attack
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
            float baseDamage = getWeaponDamage(weapon);
            float damage = DamageCalculator.calculateDamage(player, target, baseDamage + 8.0f, weapon) * (1.0f + chargePercent);

            // ダメージを与えてエンチャント効果も適用
            DamageCalculator.dealDamage(player, target, damage, weapon);
            DamageCalculator.applyWeaponEffects(player, target, damage, weapon);
        }

        if (world instanceof Level level) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    /**
     * 竜巻エフェクトを生成（大型の螺旋状竜巻）
     * @deprecated TornadoEntityを使用してください
     */
    @Deprecated
    private static void createTornadoEffect(ServerLevel world, Vec3 pos, boolean withElectricity) {
        // 竜巻の高さ（より大きく）
        double maxHeight = 15.0;

        // 時間経過に基づく回転オフセット（より高速回転）
        double timeOffset = System.currentTimeMillis() * 0.003;

        // 縦の竜巻を描画（より密度の高いエフェクト）
        for (double h = 0; h <= maxHeight; h += 0.2) {
            // 高さに応じて半径を変える（下部は広く、上部に向けて徐々に狭く、そして最上部で少し広がる）
            double heightRatio = h / maxHeight;
            double radius;
            if (heightRatio < 0.1) {
                // 地面付近は地面から巻き上げる効果で広い
                radius = 4.0 * (1.0 + (0.1 - heightRatio) * 2);
            } else if (heightRatio < 0.8) {
                // 中間部は標準的な漏斗形状
                radius = 4.0 * (1.0 - heightRatio * 0.5);
            } else {
                // 上部は少し広がる（雲に繋がる効果）
                radius = 2.5 * (1.0 + (heightRatio - 0.8) * 0.5);
            }

            // この高さでの円周上のパーティクル数（より多くのパーティクル）
            int particleCount = Math.max(16, (int)(radius * 20));

            for (int i = 0; i < particleCount; i++) {
                // 複数層の螺旋を作成（二重螺旋）
                for (int layer = 0; layer < 2; layer++) {
                    // 螺旋状の回転を加える（層ごとにオフセット）
                    double angle = (i / (double)particleCount) * Math.PI * 2
                                 + h * 1.2 // より強い螺旋
                                 + timeOffset * (layer + 1) // 層ごとに異なる速度
                                 + layer * Math.PI; // 層をずらす

                    double layerRadius = radius * (1.0 - layer * 0.2); // 内側の層は少し小さく
                    double xOffset = Math.cos(angle) * layerRadius;
                    double zOffset = Math.sin(angle) * layerRadius;

                    // メインの竜巻パーティクル（複数種類の煙）
                    if (layer == 0) {
                        // 外側の層：濃い煙
                        world.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                            pos.x + xOffset, pos.y + h, pos.z + zOffset,
                            1, 0, 0.15, 0, 0.03);
                    } else {
                        // 内側の層：薄い煙
                        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            pos.x + xOffset, pos.y + h, pos.z + zOffset,
                            1, 0, 0.1, 0, 0.02);
                    }

                    // 渦巻きの流れを表現（速度ベクトル付きパーティクル）
                    if (i % 3 == 0) {
                        double nextAngle = angle + 0.3;
                        double vx = (Math.cos(nextAngle) - Math.cos(angle)) * 0.1;
                        double vz = (Math.sin(nextAngle) - Math.sin(angle)) * 0.1;

                        world.sendParticles(ParticleTypes.CLOUD,
                            pos.x + xOffset, pos.y + h, pos.z + zOffset,
                            1, vx, 0.05, vz, 0.05);
                    }
                }

                // 感電エフェクト（より派手に）
                if (withElectricity && i % 2 == 0) {
                    double angle = (i / (double)particleCount) * Math.PI * 2 + h * 0.8 + timeOffset;
                    double xOffset = Math.cos(angle) * radius;
                    double zOffset = Math.sin(angle) * radius;

                    // 電気スパーク
                    world.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.x + xOffset, pos.y + h, pos.z + zOffset,
                        3, 0.3, 0.3, 0.3, 0.1);

                    // 稲妻のような線（より頻繁に）
                    if (Math.random() < 0.2) {
                        world.sendParticles(ParticleTypes.END_ROD,
                            pos.x + xOffset, pos.y + h, pos.z + zOffset,
                            1, 0, 1.0, 0, 0.2);
                    }
                }
            }

            // 地面付近の巻き上げ効果
            if (h < 2.0) {
                int groundParticles = 30;
                for (int i = 0; i < groundParticles; i++) {
                    double angle = (i / (double)groundParticles) * Math.PI * 2 + timeOffset * 2;
                    double groundRadius = (5.0 - h) * 1.5;
                    double xOffset = Math.cos(angle) * groundRadius;
                    double zOffset = Math.sin(angle) * groundRadius;

                    // 土埃のエフェクト
                    world.sendParticles(ParticleTypes.POOF,
                        pos.x + xOffset, pos.y + h * 0.5, pos.z + zOffset,
                        1, 0.1, 0.05, 0.1, 0.02);
                }
            }
        }

        // 竜巻の中心に強力な上昇気流エフェクト
        for (double h = 0; h <= maxHeight; h += 0.3) {
            world.sendParticles(ParticleTypes.SMOKE,
                pos.x, pos.y + h, pos.z,
                2, 0.1, 0, 0.1, 0.1);
        }

        // 地面のエフェクト（巻き上げられる土煙）
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * Math.PI * 2;
            double groundRadius = 3.0;
            world.sendParticles(ParticleTypes.POOF,
                pos.x + Math.cos(angle) * groundRadius,
                pos.y + 0.1,
                pos.z + Math.sin(angle) * groundRadius,
                1, 0, 0, 0, 0);
        }
    }

    /**
     * エンティティを竜巻で巻き上げて回転させる（より強力な効果）
     * @deprecated TornadoEntityの内部処理で実装されています
     */
    @Deprecated
    private static void liftAndRotateEntity(LivingEntity entity, Vec3 tornadoCenter) {
        Vec3 toCenter = tornadoCenter.subtract(entity.position());
        double horizontalDistance = Math.sqrt(toCenter.x * toCenter.x + toCenter.z * toCenter.z);

        if (horizontalDistance > 0.1) {
            // 中心に向かって強く引き寄せ
            double pullStrength = Math.max(0, 1.0 - horizontalDistance / 4.0) * 0.6;
            Vec3 pullVec = new Vec3(toCenter.x, 0, toCenter.z).normalize().scale(pullStrength);

            // 回転方向のベクトル（反時計回り、より高速）
            Vec3 rotationVec = new Vec3(-toCenter.z, 0, toCenter.x).normalize().scale(0.8);

            // 高さに応じた上昇力（螺旋状に上昇）
            double currentHeight = entity.getY() - tornadoCenter.y;
            double liftForce;
            if (currentHeight < 5.0) {
                // 低高度では強い上昇力
                liftForce = 1.2;
            } else if (currentHeight < 10.0) {
                // 中高度では中程度の上昇力
                liftForce = 0.7;
            } else {
                // 高高度では弱い上昇力（最終的に落下）
                liftForce = 0.3;
            }

            // 螺旋状の動きを追加
            double spiralAngle = System.currentTimeMillis() * 0.01;
            double spiralRadius = 0.3;
            double spiralX = Math.cos(spiralAngle) * spiralRadius;
            double spiralZ = Math.sin(spiralAngle) * spiralRadius;

            // 最終的な運動量
            entity.setDeltaMovement(
                pullVec.x + rotationVec.x + spiralX,
                liftForce,
                pullVec.z + rotationVec.z + spiralZ
            );

            // エンティティを高速回転させる
            entity.setYRot(entity.getYRot() + 35);
            entity.setXRot(entity.getXRot() + 10); // ピッチも回転

            // 落下ダメージを無効化（一時的に）
            entity.fallDistance = 0;
        }
    }

    /**
     * Find target in player's line of sight
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

    /**
     * Get weapon base damage (actual item damage + enchantments)
     */
    private static float getWeaponDamage(ItemStack weapon) {
        if (weapon.isEmpty()) return 4.0f;

        float baseDamage = 0.0f;

        // 武器の実際の攻撃力を取得（SwordItemの場合）
        if (weapon.getItem() instanceof net.minecraft.world.item.SwordItem swordItem) {
            baseDamage = swordItem.getDamage();
        } else {
            // 剣以外の場合は属性から取得を試みる
            var attributes = weapon.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            if (attributes.containsKey(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
                for (var modifier : attributes.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
                    baseDamage += modifier.getAmount();
                }
            }
            // それでも0の場合はデフォルト値
            if (baseDamage == 0.0f) {
                baseDamage = 7.0f; // 魔法刀のデフォルト攻撃力
            }
        }

        // シャープネスエンチャント
        int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, weapon);
        if (sharpnessLevel > 0) {
            baseDamage += 0.5f * sharpnessLevel + 0.5f;
        }

        // 武器固有の名前によるボーナス（必要に応じて）
        String weaponName = weapon.getItem().getClass().getSimpleName();
        if (weaponName.equals("MagischesFeenKatanaItem")) {
            baseDamage += 2.0f; // 魔法の妖精刀ボーナス
        } else if (weaponName.equals("MagicalKatanaItem")) {
            baseDamage += 1.0f; // 魔法刀ボーナス
        }

        return baseDamage;
    }
}