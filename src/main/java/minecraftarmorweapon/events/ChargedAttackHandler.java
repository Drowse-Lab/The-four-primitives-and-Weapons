package minecraftarmorweapon.events;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BambooBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import minecraftarmorweapon.skill.PlayerSkillData;
import minecraftarmorweapon.skill.PlayerSkillData.WeaponType;
import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.procedures.SwordOfNightTpProcedure;
import minecraftarmorweapon.procedures.SwordOfNightShotProcedure;
import minecraftarmorweapon.procedures.SwordOfNightChargingGlowProcedure;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;
import minecraftarmorweapon.network.AttackPacket;
import minecraftarmorweapon.MinecraftArmorWeaponMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class ChargedAttackHandler {
    
    private static final Map<UUID, ChargeData> playerChargeData = new HashMap<>();
    private static final int MAX_CHARGE_TIME = 60; // 3秒 (20 ticks/秒 × 3)
    private static final int MIN_CHARGE_TIME = 20; // 最小チャージ時間 1秒
    
    private static class ChargeData {
        boolean isCharging = false;
        int chargeTime = 0;
        ItemStack chargingItem = ItemStack.EMPTY;
        long lastAttackTime = 0;
        boolean wasLeftClickPressed = false;
        int clickReleaseTimer = 0;
        int comboCounter = 0; // 連撃カウンター
        boolean isFallingCharge = false; // 落下中のチャージ
        int fallTime = 0; // 落下時間
        
        void reset() {
            isCharging = false;
            chargeTime = 0;
            chargingItem = ItemStack.EMPTY;
            isFallingCharge = false;
            fallTime = 0;
        }
        
        void resetCombo() {
            comboCounter = 0;
        }
    }
    
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Sword of Nightの発光効果を管理
        SwordOfNightChargingGlowProcedure.managGlowTicks(entity);
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        UUID playerId = player.getUUID();
        ChargeData data = playerChargeData.computeIfAbsent(playerId, k -> new ChargeData());
        
        // クライアント側で左クリックの状態を検出
        if (player.level.isClientSide) {
            checkMouseInput(player, data);
        }
        
        // チャージ中の処理
        if (data.isCharging && !data.chargingItem.isEmpty()) {
            data.chargeTime++;
            
            // チャージエフェクト
            if (data.chargeTime % 10 == 0) {
                displayChargeEffect(player, data.chargeTime);
            }
            
            // Sword of Nightの場合、ターゲットを発光させる
            String itemName = data.chargingItem.getItem().getClass().getSimpleName();
            if (itemName.equals("SwordOfNightItem") && !player.level.isClientSide) {
                SwordOfNightChargingGlowProcedure.execute(
                    player.level, 
                    player.getX(), 
                    player.getY(), 
                    player.getZ(), 
                    player, 
                    true // チャージ中
                );
            }
            
            // 最大チャージ到達
            if (data.chargeTime >= MAX_CHARGE_TIME) {
                // player.displayClientMessage(Component.literal("§e最大チャージ！"), true);
                if (!player.level.isClientSide) {
                    ((ServerLevel) player.level).sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1, player.getZ(),
                        10, 0.5, 0.5, 0.5, 0.1
                    );
                }
            }
        }
    }
    
    private static void checkMouseInput(Player player, ChargeData data) {
        // クライアント側でのみ実行
        if (!player.level.isClientSide) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) return;
        
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // 鞘を持っていて刀が納刀されている場合の落下中抜刀攻撃
        boolean hasSheathWithKatana = (isSaya(mainHand) && hasStoredKatana(mainHand)) || 
                                      (isSaya(offHand) && hasStoredKatana(offHand));
        
        // 落下中かチェック
        boolean isFalling = !player.isOnGround() && player.getDeltaMovement().y < -0.1;
        
        if (isFalling && hasSheathWithKatana) {
            boolean isLeftClickHeld = mc.options.keyAttack.isDown();
            
            // 落下中に左クリック長押し開始
            if (isLeftClickHeld && !data.isFallingCharge) {
                data.isFallingCharge = true;
                data.fallTime = 0;
                data.chargingItem = mainHand.copy();
            }
            // 落下中のチャージ継続
            else if (isLeftClickHeld && data.isFallingCharge) {
                data.fallTime++;
                
                // 落下チャージエフェクト
                if (data.fallTime % 5 == 0) {
                    displayFallingChargeEffect(player, data.fallTime);
                }
            }
            // 落下チャージ解除または着地
            else if (!isLeftClickHeld && data.isFallingCharge) {
                // 落下攻撃をサーバーに送信
                float fallPower = Math.min((float) data.fallTime / 40.0f, 2.0f); // 最大2倍
                MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(new AttackPacket(2, fallPower));
                data.reset();
            }
            
            data.wasLeftClickPressed = isLeftClickHeld;
            return;
        }
        
        // 通常の武器を持っている場合
        if (isWeapon(mainHand)) {
            boolean isLeftClickHeld = mc.options.keyAttack.isDown();
            
            // 左クリックが押された瞬間を検出（通常攻撃）
            if (isLeftClickHeld && !data.wasLeftClickPressed) {
                data.clickReleaseTimer = 0;
                // サーバーに攻撃パケットを送信
                MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(new AttackPacket(0, 0));
            }
            
            // チャージ開始（左クリック長押し）
            if (isLeftClickHeld && !data.isCharging && data.clickReleaseTimer > 5) {
                data.isCharging = true;
                data.chargeTime = 0;
                data.chargingItem = mainHand.copy();
            }
            // チャージ解除
            else if (!isLeftClickHeld && data.isCharging) {
                releaseChargedAttack(player, data);
            }
            
            // 左クリック押し続けている時間をカウント
            if (isLeftClickHeld) {
                data.clickReleaseTimer++;
            }
            
            data.wasLeftClickPressed = isLeftClickHeld;
        } else {
            data.reset();
        }
    }
    
    private static void releaseChargedAttack(Player player, ChargeData data) {
        // Sword of Nightの場合、発光を解除
        String itemName = data.chargingItem.getItem().getClass().getSimpleName();
        if (itemName.equals("SwordOfNightItem") && !player.level.isClientSide) {
            SwordOfNightChargingGlowProcedure.execute(
                player.level,
                player.getX(),
                player.getY(), 
                player.getZ(),
                player,
                false // チャージ解除
            );
        }
        
        if (data.chargeTime >= MIN_CHARGE_TIME) {
            float chargePercent = Math.min((float) data.chargeTime / MAX_CHARGE_TIME, 1.0f);
            // サーバーに攻撃パケットを送信
            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(new AttackPacket(1, chargePercent));
        }
        data.reset();
    }
    
    public static void performChargedAttack(Player player, float chargePercent) {
        Level world = player.level;
        Vec3 playerPos = player.position();
        Vec3 lookVec = player.getLookAngle();
        
        // プレイヤーのスキルデータを取得
        PlayerSkillData.SkillStorage skillData = PlayerSkillData.getSkillData(player);
        WeaponType weaponType = skillData.getSelectedWeaponType();
        
        // 固有スキルのチェック
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        String itemName = mainHand.getItem().getClass().getSimpleName();
        
        // ReplicaSwordOfLightの固有スキル（ガード）
        if (itemName.equals("ReplicaSwordOfLightItem") && skillData.isUniqueSkillEnabled("ReplicaSwordOfLight")) {
            player.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.GUARD.get(), 100, 0));
            //player.displayClientMessage(Component.literal("§b光の加護！"), true);
        }
        
        // SwordOfNightの固有スキル（テレポート攻撃）
        if (itemName.equals("SwordOfNightItem") && skillData.isUniqueSkillEnabled("SwordOfNight")) {
            SwordOfNightTpProcedure.execute(world, player.getX(), player.getY(), player.getZ(), player);
            return;
        }
        
        if (weaponType == WeaponType.STRAIGHT_SWORD) {
            // 直刀: 強力な突き一撃
            performChargedThrust(player, world, lookVec, playerPos, chargePercent);
        } else if (weaponType == WeaponType.KATANA) {
            // 刀: 周囲回転斬り
            performSpinSlash(player, world, playerPos, chargePercent);
        } else {
            // デフォルト強化攻撃
            performDefaultChargedAttack(player, world, playerPos, chargePercent);
        }
    }
    
    private static void performChargedThrust(Player player, Level world, Vec3 lookVec, Vec3 playerPos, float chargePercent) {
        // Luna専用の強化突き攻撃処理
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        String itemName = mainHand.getItem().getClass().getSimpleName();

        if (minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.isStraightSword(mainHand)) {
            // 直刀の強化突進攻撃を実行
            minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.execute(
                world, player.getX(), player.getY(), player.getZ(), player
            );
            return;
        }

        float baseDamage = 15.0f * (1.0f + chargePercent);

        // 竹を破壊する範囲を設定
        breakBambooInPath(world, playerPos, lookVec, 6.0);
        double range = 6.0f + chargePercent * 2.0f;
        
        // 貫通突きエフェクト
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            
            // 巨大な突きエフェクト
            for (double d = 0; d <= range; d += 0.3) {
                serverWorld.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    playerPos.x + lookVec.x * d,
                    playerPos.y + 1,
                    playerPos.z + lookVec.z * d,
                    5, 0.2, 0.2, 0.2, 0.05
                );
                
                if (chargePercent >= 1.0f) {
                    serverWorld.sendParticles(
                        ParticleTypes.END_ROD,
                        playerPos.x + lookVec.x * d,
                        playerPos.y + 1,
                        playerPos.z + lookVec.z * d,
                        2, 0.1, 0.1, 0.1, 0
                    );
                }
            }
        }
        
        // 貫通攻撃（直線上の全ての敵）
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
            float actualDamage = calculateActualDamage(player, target, baseDamage);
            target.hurt(DamageSource.playerAttack(player), actualDamage);
            
            // 武器特殊効果を適用
            applyWeaponEffects(player, target, actualDamage);
            
            // 貫通による吹き飛ばし
            target.setDeltaMovement(lookVec.scale(2.0 * chargePercent).add(0, 0.5, 0));
            
            if (chargePercent >= 1.0f) {
                // 最大チャージで出血効果
                target.setSecondsOnFire(5);
            }
        }
        
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.0f);
        //player.displayClientMessage(Component.literal("§c貫通突き！"), true);
    }
    
    private static void performSpinSlash(Player player, Level world, Vec3 playerPos, float chargePercent) {
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
                        playerPos.x + Math.cos(rad) * r,
                        playerPos.y + 1 + ring * 0.3,
                        playerPos.z + Math.sin(rad) * r,
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
                        playerPos.x + Math.cos(angle) * range,
                        playerPos.y + 1,
                        playerPos.z + Math.sin(angle) * range,
                        10, 0.2, 0.5, 0.2, 0.1
                    );
                }
            }
        }
        
        // 周囲の敵全てにダメージ
        AABB searchArea = new AABB(
            playerPos.x - range, playerPos.y - 1, playerPos.z - range,
            playerPos.x + range, playerPos.y + 3, playerPos.z + range
        );
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> entity != player && entity.distanceTo(player) <= range);
        
        for (LivingEntity target : targets) {
            float actualDamage = calculateActualDamage(player, target, baseDamage);
            target.hurt(DamageSource.playerAttack(player), actualDamage);
            
            // 武器特殊効果を適用
            applyWeaponEffects(player, target, actualDamage);
            
            // 円形ノックバック
            Vec3 knockback = target.position().subtract(playerPos).normalize().scale(1.0 + chargePercent);
            target.setDeltaMovement(knockback.x, 0.4, knockback.z);
            
            if (chargePercent >= 1.0f) {
                // スタン効果（仮）
                target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
            }
        }
        
        // プレイヤーも回転
        player.setYRot(player.getYRot() + 720);
        
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.7f);
        //player.displayClientMessage(Component.literal("§e旋風斬！"), true);
    }
    
    private static void performDefaultChargedAttack(Player player, Level world, Vec3 playerPos, float chargePercent) {
        float baseDamage = 8.0f * (1.0f + chargePercent * 2.0f);
        float range = 3.0f + chargePercent * 2.0f;
        
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                chargePercent >= 1.0f ? SoundEvents.LIGHTNING_BOLT_IMPACT : SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0f, 0.8f + chargePercent * 0.4f);
        
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            for (int i = 0; i < 360; i += 15) {
                double rad = Math.toRadians(i);
                double x = player.getX() + Math.cos(rad) * range;
                double z = player.getZ() + Math.sin(rad) * range;
                
                serverWorld.sendParticles(
                    chargePercent >= 1.0f ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT,
                    x, player.getY() + 1, z,
                    3, 0.1, 0.1, 0.1, 0.1
                );
            }
        }
        
        AABB searchArea = new AABB(
            playerPos.x - range, playerPos.y - 1, playerPos.z - range,
            playerPos.x + range, playerPos.y + 2, playerPos.z + range
        );
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> entity != player && entity.distanceTo(player) <= range);
        
        for (LivingEntity target : targets) {
            float actualDamage = calculateActualDamage(player, target, baseDamage);
            target.hurt(DamageSource.playerAttack(player), actualDamage);
            
            // 武器特殊効果を適用
            applyWeaponEffects(player, target, actualDamage);
            
            double knockbackStrength = 0.5 + chargePercent;
            Vec3 knockback = target.position().subtract(playerPos).normalize().scale(knockbackStrength);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.3, knockback.z));
            
            if (chargePercent >= 1.0f) {
                target.setSecondsOnFire(3);
            }
        }
        
        //player.displayClientMessage(
        //    Component.literal(String.format("§6強化攻撃！ (チャージ: %.0f%%)", chargePercent * 100)),
        //    true
        //);
    }
    
    public static void performNormalAttack(Player player) {
        Level world = player.level;
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        
        // プレイヤーのスキルデータを取得
        PlayerSkillData.SkillStorage skillData = PlayerSkillData.getSkillData(player);
        WeaponType weaponType = skillData.getSelectedWeaponType();
        
        // 固有スキルのチェック
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        String itemName = mainHand.getItem().getClass().getSimpleName();
        
        if (itemName.equals("SwordOfNightItem") && skillData.isUniqueSkillEnabled("SwordOfNight")) {
            // Sword of Night Effectがアクティブな間はショットを撃たない
            if (!player.hasEffect(MinecraftArmorWeaponModMobEffects.SWORD_OF_NIGHT_EFFECT.get())) {
                // SwordOfNightの通常攻撃（ショット）
                SwordOfNightShotProcedure.execute(world, player.getX(), player.getY(), player.getZ(), player);
            }
            return;
        }
        
        // Lunaの固有スキル（直刀の場合のみ）
        if (itemName.equals("LunaItem") && weaponType == WeaponType.STRAIGHT_SWORD && skillData.isUniqueSkillEnabled("Luna")) {
            // Lunaの特殊攻撃
            minecraftarmorweapon.procedures.LunaenteiteigaaitemuwoZhentutaShiProcedure.execute(world, player.getX(), player.getY(), player.getZ(), player);
            return;
        }
        
        // コンボカウンターを取得
        UUID playerId = player.getUUID();
        ChargeData data = playerChargeData.computeIfAbsent(playerId, k -> new ChargeData());
        
        if (weaponType == WeaponType.STRAIGHT_SWORD) {
            // 直刀: 素早い突きの連打
            performThrustAttack(player, world, lookVec, playerPos);
        } else if (weaponType == WeaponType.KATANA) {
            // 刀: 三段斬り
            performKatanaCombo(player, world, lookVec, playerPos, data);
        } else {
            // デフォルト攻撃
            performDefaultAttack(player, world, lookVec, playerPos);
        }
    }
    
    private static void performThrustAttack(Player player, Level world, Vec3 lookVec, Vec3 playerPos) {
        // Luna専用の突き攻撃処理
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        String itemName = mainHand.getItem().getClass().getSimpleName();

        if (minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.isStraightSword(mainHand)) {
            // 直刀の通常突き攻撃を実行
            minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.executeNormalThrust(
                world, player.getX(), player.getY(), player.getZ(), player
            );
            return;
        }

        // 突き攻撃のエフェクト
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            
            // 直線的な突きエフェクト（横に広がるように）
            for (int i = 0; i < 5; i++) {
                double d = i * 0.5 + 1;
                Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
                
                // 中央のエフェクト
                serverWorld.sendParticles(
                    ParticleTypes.CRIT,
                    playerPos.x + lookVec.x * d,
                    playerPos.y + 1,
                    playerPos.z + lookVec.z * d,
                    2, 0.05, 0.05, 0.05, 0
                );
                
                // 左右のエフェクト
                for (double side = -1.5; side <= 1.5; side += 0.5) {
                    if (side != 0) {
                        serverWorld.sendParticles(
                            ParticleTypes.ENCHANTED_HIT,
                            playerPos.x + lookVec.x * d + rightVec.x * side,
                            playerPos.y + 1,
                            playerPos.z + lookVec.z * d + rightVec.z * side,
                            1, 0.02, 0.02, 0.02, 0
                        );
                    }
                }
            }
        }
        
        // 突き攻撃（横に広い範囲、長いリーチ）
        double range = 5.0;  // 前方リーチ
        double horizontalWidth = 2.5;  // 横幅を大幅に拡大（0.8 → 2.5）
        double verticalHeight = 1.0;  // 縦の高さは控えめに
        
        // 右ベクトルを計算（横方向）
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
        
        // 攻撃範囲を手動で構築（横長の矩形）
        Vec3 minPoint = playerPos.add(lookVec.scale(0.5))
            .add(rightVec.scale(-horizontalWidth))
            .add(0, -verticalHeight * 0.5, 0);
        Vec3 maxPoint = playerPos.add(lookVec.scale(range))
            .add(rightVec.scale(horizontalWidth))
            .add(0, verticalHeight * 1.5, 0);
        
        AABB attackBox = new AABB(minPoint, maxPoint);
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, attackBox,
            entity -> entity != player);
        
        for (LivingEntity target : targets) {
            float baseDamage = 7.0f;
            float actualDamage = calculateActualDamage(player, target, baseDamage);
            target.hurt(DamageSource.playerAttack(player), actualDamage);
            
            // 武器特殊効果を適用
            applyWeaponEffects(player, target, actualDamage);
            
            // 突きによる後退
            target.setDeltaMovement(lookVec.scale(0.8).add(0, 0.1, 0));
        }
        
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 1.2f);
    }
    
    private static void performKatanaCombo(Player player, Level world, Vec3 lookVec, Vec3 playerPos, ChargeData data) {
        // コンボ段階に応じた攻撃
        int combo = data.comboCounter % 3;
        
        // 攻撃範囲の竹を破壊
        breakBambooInPath(world, playerPos, lookVec, 5.0);
        
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            
            // コンボに応じたエフェクト
            if (combo == 0) {
                // 左上から右下への斬撃
                for (int i = -2; i <= 2; i++) {
                    serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        playerPos.x + lookVec.x * 2 - 0.5 + i * 0.2,
                        playerPos.y + 1.5 - i * 0.2,
                        playerPos.z + lookVec.z * 2,
                        1, 0, 0, 0, 0);
                }
                player.displayClientMessage(Component.literal("§7左上斬り"), true);
            } else if (combo == 1) {
                // 右上から左下への斬撃
                for (int i = -2; i <= 2; i++) {
                    serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        playerPos.x + lookVec.x * 2 + 0.5 - i * 0.2,
                        playerPos.y + 1.5 - i * 0.2,
                        playerPos.z + lookVec.z * 2,
                        1, 0, 0, 0, 0);
                }
                player.displayClientMessage(Component.literal("§7右上斬り"), true);
            } else {
                // 横一文字斬り
                Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();
                for (int i = -3; i <= 3; i++) {
                    serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        playerPos.x + lookVec.x * 2 + rightVec.x * i * 0.3,
                        playerPos.y + 1,
                        playerPos.z + lookVec.z * 2 + rightVec.z * i * 0.3,
                        1, 0, 0, 0, 0);
                }
                player.displayClientMessage(Component.literal("§7横一文字"), true);
            }
        }
        
        // 攻撃範囲と処理（横に広い範囲）
        double forwardRange = 4.5;  // 前方リーチ
        double horizontalRange = 3.0;  // 横幅を大幅に拡大
        float baseDamage = combo == 2 ? 12.0f : 9.0f;
        
        // 右ベクトルを計算
        Vec3 rightVec = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
        
        // 横長の攻撃範囲を構築
        Vec3 minPoint = playerPos.add(lookVec.scale(-0.5))
            .add(rightVec.scale(-horizontalRange))
            .add(0, -0.5, 0);
        Vec3 maxPoint = playerPos.add(lookVec.scale(forwardRange))
            .add(rightVec.scale(horizontalRange))
            .add(0, 2.5, 0);
        
        AABB searchArea = new AABB(minPoint, maxPoint);
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().subtract(playerPos).normalize();
                double dot = lookVec.dot(toEntity);
                // より広い横方向の判定（180度近い範囲）
                return dot > -0.3 && entity.distanceTo(player) <= forwardRange + horizontalRange;
            });
        
        for (LivingEntity target : targets) {
            float actualDamage = calculateActualDamage(player, target, baseDamage);
            target.hurt(DamageSource.playerAttack(player), actualDamage);
            
            // 武器特殊効果を適用
            applyWeaponEffects(player, target, actualDamage);
            
            Vec3 knockback = target.position().subtract(playerPos).normalize().scale(0.4);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.1, knockback.z));
        }
        
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 
            1.0f, 0.9f + combo * 0.1f);
        
        // コンボカウンターを増やす
        data.comboCounter++;
    }
    
    private static void performDefaultAttack(Player player, Level world, Vec3 lookVec, Vec3 playerPos) {
        // デフォルト攻撃処理
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            for (int i = 0; i < 4; i++) {
                serverWorld.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    playerPos.x + lookVec.x * (i + 1),
                    playerPos.y + 1,
                    playerPos.z + lookVec.z * (i + 1),
                    1, 0, 0, 0, 0
                );
            }
        }
        
        double range = 4.5;  // 範囲を3.0から4.5に拡大
        AABB searchArea = new AABB(
            playerPos.x - range, playerPos.y - 1, playerPos.z - range,
            playerPos.x + range, playerPos.y + 3, playerPos.z + range
        );
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().subtract(playerPos).normalize();
                double dot = lookVec.dot(toEntity);
                return dot > 0.3 && entity.distanceTo(player) <= range;  // 判定角度を0.5から0.3に緩和
            });
        
        for (LivingEntity target : targets) {
            float baseDamage = 8.0f;
            float actualDamage = calculateActualDamage(player, target, baseDamage);
            target.hurt(DamageSource.playerAttack(player), actualDamage);
            
            // 武器特殊効果を適用
            applyWeaponEffects(player, target, actualDamage);
            
            Vec3 knockback = target.position().subtract(playerPos).normalize().scale(0.5);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.15, knockback.z));
        }
        
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
    
    private static void displayChargeEffect(Player player, int chargeTime) {
        if (player.level.isClientSide) return;
        
        ServerLevel world = (ServerLevel) player.level;
        float chargePercent = Math.min((float) chargeTime / MAX_CHARGE_TIME, 1.0f);
        
        // チャージレベルに応じたパーティクル
        if (chargePercent < 0.33f) {
            world.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1, player.getZ(),
                5, 0.3, 0.3, 0.3, 0.01);
        } else if (chargePercent < 0.66f) {
            world.sendParticles(ParticleTypes.FLAME,
                player.getX(), player.getY() + 1, player.getZ(),
                5, 0.3, 0.3, 0.3, 0.01);
        } else {
            world.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1, player.getZ(),
                8, 0.3, 0.3, 0.3, 0.02);
        }
    }
    
    private static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // SwordItemまたはカタナ系アイテムかチェック
        if (stack.getItem() instanceof SwordItem) return true;
        
        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.contains("Katana") || itemName.contains("Sword") || 
               itemName.contains("Blade") || itemName.contains("katana");
    }
    
    // プレイヤーの実際の攻撃力を計算
    private static float calculateActualDamage(Player player, LivingEntity target, float baseDamage) {
        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        float damage = baseDamage;
        
        // 武器の基本攻撃力を取得
        if (weapon.getItem() instanceof SwordItem swordItem) {
            // ソードの基本ダメージを追加
            damage += swordItem.getDamage();
        }
        
        // プレイヤーの攻撃力属性を取得
        double attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        damage += (float)attackDamage;
        
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
                // スロウネス効果も付与
                int duration = 20 + (int)(Math.random() * 10 * baneLevel);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 3));
            }
        }
        
        // クリティカルダメージの計算（ランダムで発生）
        if (Math.random() < 0.1) { // 10%の確率でクリティカル
            damage *= 1.5f;
            
            // クリティカルエフェクト
            if (player.level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1);
            }
            
            player.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        
        return damage;
    }
    
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        UUID playerId = player.getUUID();
        ChargeData data = playerChargeData.get(playerId);
        
        // チャージ中はブロック破壊をキャンセル
        if (data != null && data.isCharging) {
            event.setCanceled(true);
        }
    }
    
    // 武器特殊効果を適用するヘルパーメソッド
    private static void applyWeaponEffects(Player player, LivingEntity target, float damage) {
        Level world = player.level;
        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        String weaponName = weapon.getItem().getClass().getSimpleName();
        
        // RiversOfBloodの吸血効果
        if (weaponName.equals("RiversOfBloodItem")) {
            // ターゲットが呪われているかチェック
            boolean isCursed = target.hasEffect(MobEffects.WITHER) || 
                               (target.getPersistentData().contains("Feyn") && 
                                "cursed".equals(target.getPersistentData().getString("Feyn")));
            
            float healAmount = isCursed ? damage * 0.5f : damage * 0.2f;
            player.heal(healAmount);
            
            if (isCursed) {
                // 呪われた敵への追加効果
                target.hurt(DamageSource.MAGIC, damage * 0.3f);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            }
            
            // 血のエフェクト
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1);
            }
            
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.2f);
        }
        
        // WitherKatanaのウィザー効果
        if (weaponName.equals("WitherKatanaItem")) {
            // ターゲットが呪われているかチェック
            boolean isCursed = target.getPersistentData().contains("Feyn") && 
                               "cursed".equals(target.getPersistentData().getString("Feyn"));
            
            if (isCursed) {
                // 呪われた敵には強化されたウィザー効果
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));
                target.hurt(DamageSource.WITHER, damage * 0.5f);
                
                // 闇のオーラエフェクト
                if (world instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SOUL,
                        target.getX(), target.getY() + 1, target.getZ(),
                        15, 0.5, 0.5, 0.5, 0.05);
                }
            } else {
                // 通常のウィザー効果
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            }
            
            // ウィザーサウンド
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.5f, 1.0f);
        }
        
        // Killエンチャントの効果
        if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.KILL.get(), weapon) > 0) {
            // 即死判定（低確率）
            if (Math.random() < 0.05) { // 5%の確率
                target.hurt(DamageSource.MAGIC, target.getMaxHealth() * 2);
                
                // 即死エフェクト
                if (world instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                        target.getX(), target.getY() + 1, target.getZ(),
                        20, 0.5, 0.5, 0.5, 0.1);
                }
                
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 2.0f);
            }
        }
    }
    
    // 攻撃経路上の竹を破壊する
    private static void breakBambooInPath(Level world, Vec3 startPos, Vec3 direction, double range) {
        if (world.isClientSide) return;
        
        // 攻撃経路に沿って竹をチェック
        for (double d = 0; d <= range; d += 0.5) {
            Vec3 checkPos = startPos.add(direction.scale(d));
            
            // 上下左右も含めて範囲をチェック
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos pos = new BlockPos(
                            checkPos.x + dx,
                            checkPos.y + dy,
                            checkPos.z + dz
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
    
    // 円形範囲の竹を破壊する
    private static void breakBambooInRadius(Level world, Vec3 centerPos, double radius) {
        if (world.isClientSide) return;
        
        BlockPos center = new BlockPos(centerPos.x, centerPos.y, centerPos.z);
        int radiusInt = (int) Math.ceil(radius);
        
        // 円形範囲内の竹をチェック
        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -radiusInt; z <= radiusInt; z++) {
                    if (x * x + z * z <= radius * radius) {
                        BlockPos pos = center.offset(x, y, z);
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
    
    private static void displayFallingChargeEffect(Player player, int fallTime) {
        if (player.level.isClientSide) return;
        
        ServerLevel serverWorld = (ServerLevel) player.level;
        double radius = Math.min(fallTime / 20.0, 2.0);
        
        // 落下中の円形エフェクト
        for (int i = 0; i < 360; i += 30) {
            double angle = Math.toRadians(i);
            serverWorld.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                player.getX() + Math.cos(angle) * radius,
                player.getY(),
                player.getZ() + Math.sin(angle) * radius,
                1, 0, 0.1, 0, 0.01
            );
        }
    }
    
    // 落下攻撃の実行
    public static void performFallingAttack(Player player, float fallPower) {
        Level world = player.level;
        Vec3 playerPos = player.position();
        
        // 抜刀処理（鞘から刀を抜く）
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        ItemStack sheathStack = null;
        InteractionHand sheathHand = null;
        
        if (isSaya(mainHand) && hasStoredKatana(mainHand)) {
            sheathStack = mainHand;
            sheathHand = InteractionHand.MAIN_HAND;
        } else if (isSaya(offHand) && hasStoredKatana(offHand)) {
            sheathStack = offHand;
            sheathHand = InteractionHand.OFF_HAND;
        }
        
        if (sheathStack != null) {
            // 抜刀
            CompoundTag tag = sheathStack.getOrCreateTag();

            // StoredKatanaまたはStoredSwordをチェック
            String storedKey = tag.contains("StoredKatana") ? "StoredKatana" : "StoredSword";
            ItemStack katanaStack = ItemStack.of(tag.getCompound(storedKey));

            // 反対の手に刀を配置
            InteractionHand katanaHand = sheathHand == InteractionHand.MAIN_HAND ?
                                         InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            player.setItemInHand(katanaHand, katanaStack);

            // 鞘から刀を削除
            tag.remove(storedKey);
            tag.putInt("CustomModelData", 0);
            sheathStack.setTag(tag);
            player.setItemInHand(sheathHand, sheathStack);
        }
        
        // 大ダメージの範囲攻撃
        double range = 5.0 * (1.0 + fallPower);
        float baseDamage = 20.0f * (1.0f + fallPower * 2.0f);
        
        // 地面衝撃エフェクト
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            
            // 衝撃波エフェクト
            for (int ring = 0; ring < 3; ring++) {
                double r = range * (ring + 1) / 3.0;
                for (int i = 0; i < 360; i += 10) {
                    double angle = Math.toRadians(i);
                    serverWorld.sendParticles(
                        ParticleTypes.EXPLOSION,
                        playerPos.x + Math.cos(angle) * r,
                        playerPos.y + 0.1,
                        playerPos.z + Math.sin(angle) * r,
                        1, 0, 0, 0, 0
                    );
                }
            }
            
            // 縦の衝撃エフェクト
            for (int i = 0; i < 20; i++) {
                serverWorld.sendParticles(
                    ParticleTypes.CLOUD,
                    playerPos.x, playerPos.y + i * 0.2, playerPos.z,
                    5, 0.3, 0, 0.3, 0.1
                );
            }
        }
        
        // 範囲内の全ての敵にダメージ
        AABB searchArea = new AABB(
            playerPos.x - range, playerPos.y - 2, playerPos.z - range,
            playerPos.x + range, playerPos.y + 4, playerPos.z + range
        );
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> entity != player && entity.distanceTo(player) <= range);
        
        for (LivingEntity target : targets) {
            float actualDamage = calculateActualDamage(player, target, baseDamage);
            target.hurt(DamageSource.playerAttack(player), actualDamage);
            
            // 武器特殊効果を適用
            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (weapon.isEmpty()) {
                weapon = player.getItemInHand(InteractionHand.OFF_HAND);
            }
            applyWeaponEffects(player, target, actualDamage);
            
            // 強烈な吹き飛ばし
            Vec3 knockback = target.position().subtract(playerPos).normalize();
            target.setDeltaMovement(
                knockback.x * (1.5 + fallPower),
                0.5 + fallPower * 0.5,
                knockback.z * (1.5 + fallPower)
            );
            
            // スタン効果
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 
                                                  (int)(40 * fallPower), 2));
        }
        
        // 地震音
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.5f);
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, 0.8f);
        
        player.displayClientMessage(
            Component.literal(fallPower >= 1.5f ? "§c§l落下斬撃！！" : "§c落下斬撃！"), 
            true
        );
    }
    
    private static boolean isSaya(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.equals("SayaItem") || itemName.equals("TyokutouSayaItem");
    }

    private static boolean hasStoredKatana(ItemStack stack) {
        return stack.hasTag() && (stack.getTag().contains("StoredKatana") || stack.getTag().contains("StoredSword"));
    }
    
}