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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;
import net.minecraftforge.registries.ForgeRegistries;
import minecraftarmorweapon.util.DamageCalculator;

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
        boolean isRightClickHeld = false; // 右クリック押し状態
        boolean isDashReady = false; // ダッシュ攻撃準備状態
        int dashAttackCooldown = 0; // ダッシュ攻撃のクールダウン
        int airDashCount = 0; // 空中ダッシュ回数
        
        void reset() {
            dodgeTimer = 0;
            hasDodged = false;
            isDashReady = false;
        }
        
        boolean canDodge() {
            return cooldownTimer <= 0;
        }
        
        boolean isFallDamageImmune() {
            return fallDamageImmunityTimer > 0;
        }
        
        boolean canDashAttack() {
            return dashAttackCooldown <= 0;
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
        
        // ダッシュ攻撃クールダウンのカウントダウン
        if (data.dashAttackCooldown > 0) {
            data.dashAttackCooldown--;
        }
        
        // 地面にいる場合、空中ダッシュカウントをリセット
        if (player.isOnGround()) {
            data.airDashCount = 0;
        }
        
        // クライアント側で同時押しを検出（通常のダッシュ攻撃用）
        if (player.level.isClientSide) {
            checkSimultaneousInput(player, data);
        }
        
        // クライアント側で左クリックを検出（回避後のダッシュ攻撃用）
        if (player.level.isClientSide && data.hasDodged && data.dodgeTimer > 0) {
            checkDashAttackInput(player, data);
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    private static void checkSimultaneousInput(Player player, DodgeData data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) return;
        
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // 武器を持っていない場合はスキップ
        if (!isWeapon(mainHand) && !isWeapon(offHand)) {
            data.isDashReady = false;
            return;
        }
        
        boolean isRightClickHeld = mc.options.keyUse.isDown();
        boolean isLeftClickHeld = mc.options.keyAttack.isDown();
        
        // シフトキーが押されている場合はダッシュ攻撃しない
        if (player.isShiftKeyDown()) {
            data.isDashReady = false;
            return;
        }
        
        // 右クリック＆左クリック同時押しでダッシュ攻撃
        if (isRightClickHeld && isLeftClickHeld && !data.isDashReady) {
            data.isDashReady = true;
            performDashAttack(player);
        } else if (!isRightClickHeld || !isLeftClickHeld) {
            data.isDashReady = false;
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
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // シフトキーが押されている場合は納刀処理
        if (player.isShiftKeyDown()) {
            // 武器を持っていて鞘もある場合、納刀する
            if (isWeapon(mainHand) && isSaya(offHand)) {
                performSheathing(player, mainHand, offHand, InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND);
                return;
            } else if (isWeapon(offHand) && isSaya(mainHand)) {
                performSheathing(player, offHand, mainHand, InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND);
                return;
            }
            return;
        }
        
        // 武器を持っている場合のみ回避を実行
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
        
        // シフトキーが押されている場合はブロック操作を許可
        if (player.isShiftKeyDown()) {
            return;
        }
        
        // 刀を持っている場合、回避を実行
        if (isWeapon(mainHand) || isWeapon(offHand)) {
            // ブロックを持っていて設置しようとしている場合は許可
            if (!player.getItemInHand(event.getHand()).isEmpty() && 
                player.getItemInHand(event.getHand()).getItem() instanceof net.minecraft.world.item.BlockItem) {
                return; // ブロック設置を許可
            }
            
            // それ以外の場合は回避を実行
            event.setCanceled(true);
            performDodge(player);
        }
    }
    
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        
        // シフトキーが押されている場合は通常の相互作用を許可
        if (player.isShiftKeyDown()) {
            return;
        }
        
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // 武器を持っている場合は回避を優先
        if (isWeapon(mainHand) || isWeapon(offHand)) {
            event.setCanceled(true);  // エンティティとの相互作用をキャンセル
            performDodge(player);      // 回避を実行
        }
    }
    
    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();
        
        // シフトキーが押されている場合は通常の相互作用を許可
        if (player.isShiftKeyDown()) {
            return;
        }
        
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // 武器を持っている場合は回避を優先
        if (isWeapon(mainHand) || isWeapon(offHand)) {
            event.setCanceled(true);  // エンティティとの相互作用をキャンセル
            performDodge(player);      // 回避を実行
        }
    }
    
    // ダッシュ攻撃（回避後の攻撃）
    private static void performDashAttack(Player player) {
        // ダッシュ攻撃のデータを取得
        UUID playerId = player.getUUID();
        DodgeData data = playerDodgeData.get(playerId);
        if (data == null) {
            data = new DodgeData();
            playerDodgeData.put(playerId, data);
        }

        // 直刀の場合は突進攻撃を実行
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

        if (minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.isStraightSword(mainHand)) {
            performStraightSwordThrust(player, mainHand);
            return;
        }
        if (minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.isStraightSword(offHand)) {
            performStraightSwordThrust(player, offHand);
            return;
        }
        
        // クールダウン中は実行しない
        if (!data.canDashAttack()) {
            player.displayClientMessage(
                Component.literal(String.format("§cダッシュ攻撃クールダウン中 (%.1f秒)", 
                    (float)data.dashAttackCooldown / 20.0f)), 
                true
            );
            return;
        }
        
        // 空中でのダッシュ制限（1回まで）
        boolean isInAir = !player.isOnGround();
        if (isInAir) {
            // if (data.airDashCount >= 1) {
            //     player.displayClientMessage(Component.literal("§c空中ダッシュは1回まで！"), true);
            //     return;
            // }
            data.airDashCount++;
        }
        
        Level world = player.level;
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();
        
        // 竹を破壊する
        breakBambooInPath(world, playerPos, lookVec, 7.0);
        
        // 前方への高速移動（空中では移動量を減少）
        double dashStrength = isInAir ? 1.2 : 1.8;  // 空中では弱い推進力
        
        // 垂直方向の移動も制限
        Vec3 dashVec = lookVec;
        if (isInAir) {
            // 空中では水平方向の移動を重視
            dashVec = new Vec3(lookVec.x, Math.min(lookVec.y, 0.2), lookVec.z).normalize();
        }
        
        player.setDeltaMovement(player.getDeltaMovement().add(dashVec.scale(dashStrength)));
        
        // クールダウンを設定（地上: 2秒、空中: 3秒）
        data.dashAttackCooldown = isInAir ? 60 : 40;
        
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
        
        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        String weaponName = weapon.getItem().getClass().getSimpleName();
        
        for (LivingEntity target : targets) {
            // 基本ダメージ
            float baseDamage = 18.0f;
            float actualDamage = DamageCalculator.calculateDamage(player, target, baseDamage, weapon);

            // ダッシュ攻撃のダメージ
            target.hurt(DamageSource.playerAttack(player), actualDamage);
            
            // 武器エフェクトを適用
            DamageCalculator.applyWeaponEffects(player, target, actualDamage, weapon);
            
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
        
        // 垂直方向の視線を制限（真上や真下を向いている場合の対処）
        Vec3 horizontalLookVec = new Vec3(lookVec.x, 0, lookVec.z).normalize();
        if (horizontalLookVec.length() < 0.1) {
            // プレイヤーがほぼ真上か真下を向いている場合、前方方向を使用
            float yaw = player.getYRot();
            horizontalLookVec = new Vec3(
                -Math.sin(Math.toRadians(yaw)),
                0,
                Math.cos(Math.toRadians(yaw))
            );
        }
        
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
            ).scale(0.8);  // 回避距離を減少（1.5→0.8）
        } else {
            // 前方回避（移動していない場合）- 水平方向のみ
            dodgeVec = horizontalLookVec.scale(0.8);  // 回避距離を減少（1.5→0.8）
        }
        
        // 回避移動（上方向の移動量を調整）
        player.setDeltaMovement(
            dodgeVec.x,
            player.getDeltaMovement().y + 0.15,  // 上方向の加速を減少（0.3→0.15）
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
        
        // Sayaアイテムの場合は、刀が入っているかチェック
        if (itemName.equals("SayaItem")) {
            // NBTタグを確認して、StoredKatanaが存在する場合のみtrue
            return stack.hasTag() && stack.getTag().contains("StoredKatana");
        }
        
        // その他の武器（大文字小文字を考慮）
        return itemName.contains("Katana") || itemName.contains("Sword") || 
               itemName.contains("Blade") || itemName.contains("katana") ||
               itemName.equals("RiversOfBloodItem") || itemName.equals("KatanaNiguHumerusItem");
    }
    
    private static boolean isSaya(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.equals("SayaItem") || itemName.equals("TyokutouSayaItem");
    }

    private static boolean isTyokutouSaya(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.equals("TyokutouSayaItem");
    }
    
    // 納刀処理
    private static void performSheathing(Player player, ItemStack weaponStack, ItemStack sheathStack,
                                        InteractionHand weaponHand, InteractionHand sheathHand) {
        if (!isWeapon(weaponStack) || !isSaya(sheathStack)) return;

        // 直刀鞘の場合
        if (isTyokutouSaya(sheathStack)) {
            // 直刀のみ納刀可能
            if (minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.isStraightSword(weaponStack)) {
                minecraftarmorweapon.item.TyokutouSayaItem.sheatheSword(
                    player, weaponStack, sheathStack, weaponHand, sheathHand
                );
            } else {
                player.displayClientMessage(Component.literal("§cこの鞘には直刀のみ納刀可能です"), true);
            }
            return;
        }

        // 通常の鞘の処理
        CompoundTag sheathTag = sheathStack.getOrCreateTag();

        // 鞘が空の場合のみ納刀可能
        if (!sheathTag.contains("StoredKatana")) {
            // 直刀は通常の鞘には納刀不可
            if (minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.isStraightSword(weaponStack)) {
                player.displayClientMessage(Component.literal("§c直刀は専用の鞘が必要です"), true);
                return;
            }

            // 武器のNBTデータを保存
            CompoundTag weaponData = weaponStack.save(new CompoundTag());
            sheathTag.put("StoredKatana", weaponData);

            // 鞘の見た目を更新（CustomModelDataで刀が入っている状態を示す）
            sheathTag.putInt("CustomModelData", getWeaponModelData(weaponStack));

            // 鞘にタグを適用
            sheathStack.setTag(sheathTag);

            // 武器を削除
            player.setItemInHand(weaponHand, ItemStack.EMPTY);

            // 鞘を更新
            player.setItemInHand(sheathHand, sheathStack);

            // 納刀音を再生
            player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.8F);

            player.displayClientMessage(Component.literal("§7納刀"), true);
        }
    }
    
    private static int getWeaponModelData(ItemStack weapon) {
        String itemName = weapon.getItem().getClass().getSimpleName();
        
        // 武器ごとに異なるCustomModelDataを返す（saya.jsonと一致）
        if (itemName.equals("IronKatanaItem")) return 1;
        if (itemName.equals("GoldKatanaItem")) return 2;
        if (itemName.equals("StoneKatanaItem")) return 3;
        if (itemName.equals("NetheriteKatanaItem")) return 4;
        if (itemName.equals("WitherKatanaItem")) return 5;
        if (itemName.equals("MotoWitherKatanaItem")) return 6;
        if (itemName.equals("DarknessKatanaItem")) return 7;
        if (itemName.equals("MagicalKatanaItem")) return 8;
        if (itemName.equals("MagischesFeenKatanaItem")) return 9;
        if (itemName.equals("PrototypeKatanaItem")) return 10;
        if (itemName.equals("OldKatanaItem")) return 11;
        if (itemName.equals("MyTestIronKatanaItem")) return 12;
        if (itemName.equals("RiversOfBloodItem")) return 13;
        if (itemName.equals("KatanaNiguHumerusItem")) return 14;
        
        return 0; // デフォルト（空の鞘）
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
    
    // プレイヤーの実際の攻撃力を計算
    // @deprecated Use DamageCalculator.calculateDamage instead
    @Deprecated
    private static float calculateActualDamage(Player player, ItemStack weapon, LivingEntity target, float baseDamage) {
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
                // スローネス効果も付与
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
    
    // 直刀の突進攻撃
    private static void performStraightSwordThrust(Player player, ItemStack weapon) {
        // 直刀の突進攻撃を実行
        minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.execute(
            player.level, player.getX(), player.getY(), player.getZ(), player
        );

        // クールダウン設定
        DodgeData data = playerDodgeData.get(player.getUUID());
        if (data != null) {
            data.dashAttackCooldown = 30; // 1.5秒のクールダウン
            data.reset();
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
}