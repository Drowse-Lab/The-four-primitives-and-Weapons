package minecraftarmorweapon.events;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class DodgeAndBattouHandler {
    
    private static final Map<UUID, DodgeData> playerDodgeData = new HashMap<>();
    private static final int DODGE_WINDOW = 30; // 回避後1.5秒間のウィンドウ（延長）
    private static final int DODGE_COOLDOWN = 40; // 回避クールダウン2秒（20ticks × 2）
    private static final int FALL_DAMAGE_IMMUNITY_TIME = 30; // 落下ダメージ無効時間1.5秒
    
    private static class DodgeData {
        int dodgeTimer = 0;
        boolean hasDodged = false;
        int cooldownTimer = 0;
        int fallDamageImmunityTimer = 0;
        
        void reset() {
            dodgeTimer = 0;
            hasDodged = false;
        }
        
        boolean canDodge() {
            return cooldownTimer <= 0;
        }
        
        boolean isFallDamageImmune() {
            return fallDamageImmunityTimer > 0;
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        UUID playerId = player.getUUID();
        DodgeData data = playerDodgeData.computeIfAbsent(playerId, k -> new DodgeData());
        
        // 回避タイマーのカウントダウン
        if (data.dodgeTimer > 0) {
            data.dodgeTimer--;
            
            // ダッシュ攻撃可能時の視覚的フィードバック
            if (data.hasDodged && !player.level.isClientSide && data.dodgeTimer % 4 == 0) {
                ServerLevel serverWorld = (ServerLevel) player.level;
                serverWorld.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 1, player.getZ(),
                    2, 0.3, 0.3, 0.3, 0.02
                );
            }
            
            if (data.dodgeTimer == 0) {
                data.hasDodged = false;
            }
        }
        
        // クールダウンタイマーのカウントダウン
        if (data.cooldownTimer > 0) {
            data.cooldownTimer--;
            
            // クールダウン中は視覚的フィードバック
            if (player.level.isClientSide && data.cooldownTimer % 10 == 0) {
                float percent = (float)data.cooldownTimer / DODGE_COOLDOWN;
                player.displayClientMessage(
                    Component.literal(String.format("§7回避CD: %.1f秒", percent * 2.0f)), 
                    true
                );
            }
        }
        
        // 落下ダメージ無効タイマーのカウントダウン
        if (data.fallDamageImmunityTimer > 0) {
            data.fallDamageImmunityTimer--;
            
            // 落下ダメージ無効中のエフェクト
            if (!player.level.isClientSide && data.fallDamageImmunityTimer % 5 == 0) {
                ServerLevel serverWorld = (ServerLevel) player.level;
                serverWorld.sendParticles(
                    ParticleTypes.PORTAL,
                    player.getX(), player.getY(), player.getZ(),
                    3, 0.3, 0.1, 0.3, 0.01
                );
            }
        }
        
        // クライアント側で左クリックを検出（回避後のダッシュ攻撃用）
        if (player.level.isClientSide && data.hasDodged && data.dodgeTimer > 0) {
            checkDashAttackInput(player, data);
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void checkDashAttackInput(Player player, DodgeData data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) return;
        
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // 回避後に左クリック（攻撃キー）が押された場合
        // 武器をメインハンドかオフハンドに持っていればOK
        if (mc.options.keyAttack.isDown() && (isWeapon(mainHand) || isWeapon(offHand))) {
            performDashAttack(player);
            data.reset(); // ダッシュ攻撃後はリセット
        }
    }
    
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        
        // シフトキーが押されている場合は何もしない
        if (player.isShiftKeyDown()) {
            return;
        }
        
        // 武器を持っている場合のみ回避を実行
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (isWeapon(mainHand) || isWeapon(offHand)) {
            // 通常の右クリック（回避）
            performDodge(player);
        }
    }
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) return;
        
        Player player = event.getEntity();
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // 刀を持っている場合はブロックとの相互作用をキャンセル
        if (isWeapon(mainHand) || isWeapon(offHand)) {
            event.setCanceled(true);
            
            // シフトキーが押されている場合は何もしない
            if (player.isShiftKeyDown()) {
                return;
            }
            
            // 通常の右クリック（回避）
            performDodge(player);
        }
    }
    
    // ダッシュ攻撃（回避後の攻撃）
    private static void performDashAttack(Player player) {
        Level world = player.level;
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        
        // 前方への高速移動（少し速度を上げる）
        player.setDeltaMovement(player.getDeltaMovement().add(lookVec.scale(2.2)));
        
        // エフェクト
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            
            // ダッシュ攻撃のエフェクト（より派手に）
            for (int i = 0; i < 10; i++) {
                double d = i * 0.6;
                serverWorld.sendParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    playerPos.x + lookVec.x * d,
                    playerPos.y + 1,
                    playerPos.z + lookVec.z * d,
                    2, 0, 0, 0, 0
                );
                
                serverWorld.sendParticles(
                    ParticleTypes.CLOUD,
                    playerPos.x + lookVec.x * d,
                    playerPos.y + 0.1,
                    playerPos.z + lookVec.z * d,
                    3, 0.3, 0, 0.3, 0.01
                );
            }
        }
        
        // 前方の敵に大ダメージ（範囲と判定を大幅に拡大）
        double range = 7.0;  // 5.0から7.0に拡大
        Vec3 endPos = playerPos.add(lookVec.scale(range));
        AABB searchArea = new AABB(playerPos.add(-2, -1, -2), endPos.add(2, 2, 2));  // より大きな判定ボックス
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                // 前方180度の広い範囲で判定
                Vec3 toEntity = entity.position().subtract(playerPos).normalize();
                double dot = lookVec.dot(toEntity);
                return dot > -0.2 && entity.distanceTo(player) <= range;  // ほぼ360度に近い判定
            });
        
        for (LivingEntity target : targets) {
            // ダッシュ攻撃の高ダメージ（少し増加）
            target.hurt(DamageSource.playerAttack(player), 18.0f);
            
            // 強力なノックバック
            target.setDeltaMovement(lookVec.scale(1.5).add(0, 0.4, 0));
        }
        
        // サウンド
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 1.0f);
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
        
        player.displayClientMessage(Component.literal("§cダッシュ攻撃！"), true);
    }
    
    // 回避処理
    private static void performDodge(Player player) {
        Level world = player.level;
        Vec3 lookVec = player.getLookAngle();
        Vec3 dodgeVec;
        
        // 回避データを取得
        UUID playerId = player.getUUID();
        DodgeData data = playerDodgeData.computeIfAbsent(playerId, k -> new DodgeData());
        
        // クールダウン中は回避できない
        if (!data.canDodge()) {
            player.displayClientMessage(
                Component.literal(String.format("§c回避クールダウン中 (%.1f秒)", 
                    (float)data.cooldownTimer / 20.0f)), 
                true
            );
            return;
        }
        
        // 回避データを設定
        data.hasDodged = true;
        data.dodgeTimer = DODGE_WINDOW;
        data.cooldownTimer = DODGE_COOLDOWN;
        data.fallDamageImmunityTimer = FALL_DAMAGE_IMMUNITY_TIME;
        
        // 移動方向に基づいて回避方向を決定
        float forward = player.zza;
        float strafe = player.xxa;
        
        if (Math.abs(forward) > 0.01 || Math.abs(strafe) > 0.01) {
            // プレイヤーの移動方向に回避
            float yaw = player.getYRot();
            float moveAngle = (float) Math.atan2(-strafe, forward);
            float dodgeAngle = (float) Math.toRadians(yaw) + moveAngle;
            
            dodgeVec = new Vec3(
                -Math.sin(dodgeAngle),
                0,
                Math.cos(dodgeAngle)
            ).scale(1.5);  // 回避距離を増加
        } else {
            // 前方回避（移動していない場合）- 後方から前方へ変更
            dodgeVec = lookVec.scale(1.5);
        }
        
        // 回避移動
        player.setDeltaMovement(
            dodgeVec.x,
            player.getDeltaMovement().y + 0.3,
            dodgeVec.z
        );
        
        // エフェクト
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            Vec3 pos = player.position();
            
            // 煙のエフェクト
            serverWorld.sendParticles(
                ParticleTypes.CLOUD,
                pos.x, pos.y, pos.z,
                20, 0.3, 0.5, 0.3, 0.05
            );
        }
        
        // サウンド
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENDER_PEARL_THROW, SoundSource.PLAYERS, 0.8f, 1.5f);
        
        player.displayClientMessage(Component.literal("§b回避成功！ §e今なら左クリックでダッシュ攻撃！"), true);
    }
    
    private static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // SwordItemまたはカタナ系アイテムかチェック
        if (stack.getItem() instanceof SwordItem) return true;
        
        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.contains("Katana") || itemName.contains("Sword") || 
               itemName.contains("Blade") || itemName.contains("katana");
    }
    
    // 落下ダメージ無効化イベント
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        // 落下ダメージかチェック
        if (event.getSource() == DamageSource.FALL) {
            UUID playerId = player.getUUID();
            DodgeData data = playerDodgeData.get(playerId);
            
            // 落下ダメージ無効時間中はダメージをキャンセル
            if (data != null && data.isFallDamageImmune()) {
                event.setCanceled(true);
                
                // エフェクトと通知
                if (!player.level.isClientSide) {
                    ServerLevel serverWorld = (ServerLevel) player.level;
                    serverWorld.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        player.getX(), player.getY(), player.getZ(),
                        10, 0.5, 0.2, 0.5, 0.05
                    );
                }
                
                player.displayClientMessage(
                    Component.literal("§a落下ダメージ無効！"), 
                    true
                );
            }
        }
    }
}