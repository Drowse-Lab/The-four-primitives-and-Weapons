package minecraftarmorweapon.events;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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

import minecraftarmorweapon.skill.PlayerSkillData;
import minecraftarmorweapon.skill.PlayerSkillData.WeaponType;
import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.procedures.SwordOfNightTpProcedure;
import minecraftarmorweapon.procedures.SwordOfNightShotProcedure;
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
        
        void reset() {
            isCharging = false;
            chargeTime = 0;
            chargingItem = ItemStack.EMPTY;
        }
        
        void resetCombo() {
            comboCounter = 0;
        }
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
        
        // 刀/剣を持っているかチェック
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
        float damage = 15.0f * (1.0f + chargePercent);
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
            target.hurt(DamageSource.playerAttack(player), damage);
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
        float damage = 12.0f * (1.0f + chargePercent * 1.5f);
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
            target.hurt(DamageSource.playerAttack(player), damage);
            
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
        float baseDamage = 8.0f;
        float damage = baseDamage * (1.0f + chargePercent * 2.0f);
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
            target.hurt(DamageSource.playerAttack(player), damage);
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
        
        // 固有スキルのチェック（SwordOfNight）
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        String itemName = mainHand.getItem().getClass().getSimpleName();
        
        if (itemName.equals("SwordOfNightItem") && skillData.isUniqueSkillEnabled("SwordOfNight")) {
            // SwordOfNightの通常攻撃（ショット）
            SwordOfNightShotProcedure.execute(world, player.getX(), player.getY(), player.getZ(), player);
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
        // 突き攻撃のエフェクト
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            
            // 直線的な突きエフェクト
            for (int i = 0; i < 5; i++) {
                double d = i * 0.5 + 1;
                serverWorld.sendParticles(
                    ParticleTypes.CRIT,
                    playerPos.x + lookVec.x * d,
                    playerPos.y + 1,
                    playerPos.z + lookVec.z * d,
                    2, 0.05, 0.05, 0.05, 0
                );
            }
        }
        
        // 突き攻撃（狭い範囲、長いリーチ）
        double range = 4.5;
        double width = 0.5;
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class,
            new AABB(playerPos.add(lookVec.scale(0.5)), playerPos.add(lookVec.scale(range))).inflate(width),
            entity -> entity != player);
        
        for (LivingEntity target : targets) {
            target.hurt(DamageSource.playerAttack(player), 7.0f);
            // 突きによる後退
            target.setDeltaMovement(lookVec.scale(0.8).add(0, 0.1, 0));
        }
        
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 1.2f);
    }
    
    private static void performKatanaCombo(Player player, Level world, Vec3 lookVec, Vec3 playerPos, ChargeData data) {
        // コンボ段階に応じた攻撃
        int combo = data.comboCounter % 3;
        
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
        
        // 攻撃範囲と処理
        double range = 3.5;
        float damage = combo == 2 ? 10.0f : 8.0f; // 3段目は強い
        
        AABB searchArea = new AABB(
            playerPos.x - range, playerPos.y - 1, playerPos.z - range,
            playerPos.x + range, playerPos.y + 2, playerPos.z + range
        );
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().subtract(playerPos).normalize();
                double dot = lookVec.dot(toEntity);
                return dot > 0.3 && entity.distanceTo(player) <= range;
            });
        
        for (LivingEntity target : targets) {
            target.hurt(DamageSource.playerAttack(player), damage);
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
            for (int i = 0; i < 3; i++) {
                serverWorld.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    playerPos.x + lookVec.x * (i + 1),
                    playerPos.y + 1,
                    playerPos.z + lookVec.z * (i + 1),
                    1, 0, 0, 0, 0
                );
            }
        }
        
        double range = 3.0;
        AABB searchArea = new AABB(
            playerPos.x - range, playerPos.y - 1, playerPos.z - range,
            playerPos.x + range, playerPos.y + 2, playerPos.z + range
        );
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().subtract(playerPos).normalize();
                double dot = lookVec.dot(toEntity);
                return dot > 0.5 && entity.distanceTo(player) <= range;
            });
        
        for (LivingEntity target : targets) {
            target.hurt(DamageSource.playerAttack(player), 8.0f);
            Vec3 knockback = target.position().subtract(playerPos).normalize().scale(0.4);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.1, knockback.z));
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
    
}