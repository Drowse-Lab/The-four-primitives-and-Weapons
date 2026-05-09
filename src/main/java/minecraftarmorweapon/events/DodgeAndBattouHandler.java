package minecraftarmorweapon.events;

import minecraftarmorweapon.util.VersionHelper;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.DiggerItem;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;
import net.minecraftforge.registries.ForgeRegistries;
import minecraftarmorweapon.util.DamageCalculator;
import minecraftarmorweapon.skill.PlayerSkillData;
import minecraftarmorweapon.skill.PlayerSkillData.AttackSlot;
import minecraftarmorweapon.skill.MotionExecutor;
import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.network.DodgeRequestPacket;
import minecraftarmorweapon.network.DashAttackPacket;
import minecraftarmorweapon.damage.ElementType;
import minecraftarmorweapon.config.DodgeConfig;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class DodgeAndBattouHandler {
    
    // クライアントとサーバーで別々のデータを保持（シングルプレイでの2重カウント防止）
    private static final Map<UUID, DodgeData> clientDodgeData = new ConcurrentHashMap<>();
    private static final Map<UUID, DodgeData> serverDodgeData = new ConcurrentHashMap<>();

    private static DodgeData getOrCreateData(Player player) {
        Map<UUID, DodgeData> map = player.level().isClientSide ? clientDodgeData : serverDodgeData;
        return map.computeIfAbsent(player.getUUID(), k -> new DodgeData());
    }

    private static DodgeData getData(Player player) {
        Map<UUID, DodgeData> map = player.level().isClientSide ? clientDodgeData : serverDodgeData;
        return map.get(player.getUUID());
    }

    private static final int DODGE_WINDOW = 30; // 回避後1.5秒間のウィンドウ（延長）
    private static final int DODGE_COOLDOWN = 40; // 回避クールダウン2秒
    private static final int FALL_DAMAGE_IMMUNITY_TIME = 30; // 落下ダメージ無効時間1.5秒
    
    private static class DodgeData {
        int dodgeTimer = 0;
        boolean hasDodged = false;
        int cooldownTimer = 0;
        int fallDamageImmunityTimer = 0;
        boolean isRightClickHeld = false; // 右クリック押し状態
        int dashAttackCooldown = 0; // ダッシュ攻撃のクールダウン
        int airDashCount = 0; // 空中ダッシュ回数
        boolean cooldownPending = false; // 頂点到達後にクールダウン開始

        void reset() {
            dodgeTimer = 0;
            hasDodged = false;
        }
        
        boolean canDodge() {
            return cooldownTimer <= 0 && !cooldownPending;
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
        DodgeData data = getOrCreateData(player);

        // 回避タイマーのカウントダウン
        if (data.dodgeTimer > 0) {
            data.dodgeTimer--;
            
            // ダッシュ攻撃可能時の視覚的フィードバック
            if (data.hasDodged && !player.level().isClientSide && data.dodgeTimer % 4 == 0) {
                ServerLevel serverWorld = (ServerLevel) VersionHelper.getLevel(player);
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
        
        // 頂点到達後にクールダウン開始（上昇が終わったら）
        if (data.cooldownPending) {
            Vec3 vel = player.getDeltaMovement();
            boolean peakReached = vel.y <= 0 && !DashSkillHandler.isAnyDashSkillActive(player);
            if (peakReached || player.onGround()) {
                data.cooldownPending = false;
                // 属性によるクールダウン短縮
                ElementType dashElement = DashSkillHandler.getLastDashElement(player);
                boolean wasShadow = DashSkillHandler.wasLastDashShadowStep(player);
                int cd = DODGE_COOLDOWN;
                if (dashElement == ElementType.DARK && wasShadow) {
                    cd = 25; // DARK + シャドステ: CD短縮（40→25）
                } else if (dashElement == ElementType.THUNDER && !wasShadow) {
                    cd = 25; // THUNDER + 非シャドステ: CD短縮（40→25）
                }
                data.cooldownTimer = cd;
            }
        }

        // クールダウンタイマーのカウントダウン
        if (data.cooldownTimer > 0) {
            data.cooldownTimer--;
            
            // クールダウン中は視覚的フィードバック
            if (player.level().isClientSide && data.cooldownTimer % 10 == 0) {
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
            if (!player.level().isClientSide && data.fallDamageImmunityTimer % 5 == 0) {
                ServerLevel serverWorld = (ServerLevel) VersionHelper.getLevel(player);
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
        if (player.onGround()) {
            data.airDashCount = 0;
        }
        
        // クライアント側で左クリックを検出（回避後のダッシュ攻撃用）
        if (player.level().isClientSide && data.hasDodged && data.dodgeTimer > 0) {
            checkDashAttackInput(player, data);
        }
    }
    
    // === ClientTickEvent で右クリック長押し自動回避を処理 ===
    @Mod.EventBusSubscriber(modid = "minecraft_armor_weapon", value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            DodgeAndBattouHandler.handleAutoRepeatDodge();
        }
    }

    @OnlyIn(Dist.CLIENT)
    static void handleAutoRepeatDodge() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return; // GUI表示中は回避しない

        Player player = mc.player;
        DodgeData data = getData(player);
        if (data == null) return;

        // GLFWで直接右クリック状態を確認 + 武器を持っている + クールダウンが終わった → 自動で回避
        if (isUseKeyHeld(mc) && !player.isShiftKeyDown() && data.canDodge()) {
            if (canDodgeWithHands(player)) {
                if (performDodge(player)) {
                    MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                            new DodgeRequestPacket(player.zza, player.xxa));
                }
            }
        }
    }

    /**
     * GLFWで直接入力状態を確認する。
     * mc.options.keyUse.isDown() はPlayerTickEventで不安定な場合があるため、
     * GLFWの低レベルAPIを使用して確実に入力を検出する。
     */
    @OnlyIn(Dist.CLIENT)
    private static boolean isUseKeyHeld(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        InputConstants.Key key = mc.options.keyUse.getKey();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        } else {
            return InputConstants.isKeyDown(window, key.getValue());
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void checkDashAttackInput(Player player, DodgeData data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) return;

        // クールダウン中はパケットを送らない
        if (!data.canDashAttack()) return;

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

        // 回避後に左クリック（攻撃キー）が押された場合 → サーバーにパケット送信
        if (mc.options.keyAttack.isDown() && (isWeapon(mainHand) || isWeapon(offHand))) {
            MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(new DashAttackPacket());
            data.hasDodged = false; // クライアント側のみリセット（重複パケット防止、サーバーとは別データ）
        }
    }
    
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();

        // シフトキーが押されている場合は何もしない（納刀はRキーに移行）
        if (player.isShiftKeyDown()) {
            return;
        }

        // 武器を持っている場合のみ回避を実行（オフハンドのみ武器の場合はメインハンドが空の時のみ）
        if (canDodgeWithHands(player)) {
            // RightClickEmptyはクライアント専用イベント
            // クライアント側のDodgeDataを更新しつつ、サーバーにWASD方向付きパケットを送信
            if (performDodge(player) && player.level().isClientSide) {
                MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                        new DodgeRequestPacket(player.zza, player.xxa));
            }
        }
    }

    // (遠距離武器の右クリックは vanilla 挙動に戻した — 弓/クロスボウの引き絞り/発射)
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) return;

        Player player = event.getEntity();

        // メインハンドがツール（つるはし等）でオフハンドが鞘の場合、ブロック操作を許可
        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHandItem = player.getItemInHand(InteractionHand.OFF_HAND);
        if (mainHandItem.getItem() instanceof DiggerItem && isSaya(offHandItem)) {
            return; // ツールによるブロック操作を許可
        }

        // functionalBlockRequiresShift設定がONの場合
        if (DodgeConfig.functionalBlockRequiresShift) {
            // メインハンドがブロックアイテムの場合
            if (mainHandItem.getItem() instanceof BlockItem) {
                // Shift+右クリックで回避、通常はブロック設置
                if (!player.isShiftKeyDown()) {
                    return; // ブロック設置を許可
                }
                // Shift押下時は回避を試行（回避モード選択時のみイベントをキャンセル）
                if (canDodgeWithHands(player) && isRightClickDodgeEnabled(player)) {
                    event.setCanceled(true);
                    if (player.level().isClientSide && performDodge(player)) {
                        MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                                new DodgeRequestPacket(player.zza, player.xxa));
                    }
                }
                return;
            }

            // クリック先がファンクショナルブロック（チェスト、かまど等）の場合
            if (isFunctionalBlock(player.level(), event.getPos())) {
                // Shift+右クリックで回避、通常はブロックを開く
                if (!player.isShiftKeyDown()) {
                    return; // ブロック操作を許可
                }
                // Shift押下時は回避を試行（回避モード選択時のみイベントをキャンセル）
                if (canDodgeWithHands(player) && isRightClickDodgeEnabled(player)) {
                    event.setCanceled(true);
                    if (player.level().isClientSide && performDodge(player)) {
                        MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                                new DodgeRequestPacket(player.zza, player.xxa));
                    }
                }
                return;
            }
        }

        // シフトキーが押されている場合はブロック操作を許可（納刀はRキーに移行）
        if (player.isShiftKeyDown()) {
            return;
        }

        // 刀を持っている場合、回避を実行（回避モード選択時のみキャンセル。
        // トライデントで "trident_throw" が選ばれている場合などは vanilla 挙動に任せる）
        if (canDodgeWithHands(player) && isRightClickDodgeEnabled(player)) {
            // ブロックを持っていて設置しようとしている場合は許可
            if (!player.getItemInHand(event.getHand()).isEmpty() &&
                player.getItemInHand(event.getHand()).getItem() instanceof BlockItem) {
                return; // ブロック設置を許可
            }
            // 武器ラック (ItemFrame派生エンティティを設置するアイテム) も設置を許可
            if (player.getItemInHand(event.getHand()).getItem()
                    instanceof minecraftarmorweapon.item.WeaponRackItem) {
                return;
            }

            // それ以外の場合は回避を実行
            event.setCanceled(true);
            if (player.level().isClientSide && performDodge(player)) {
                MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                        new DodgeRequestPacket(player.zza, player.xxa));
            }
        }
    }

    /**
     * ファンクショナルブロック（GUI付きブロック）かどうか判定。
     * チェスト、かまど、作業台、エンチャント台、金床など。
     */
    private static boolean isFunctionalBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        // MenuProviderを持つブロック（チェスト、かまど、醸造台、ホッパー等）
        if (state.getMenuProvider(level, pos) != null) return true;
        // 作業台はMenuProviderを直接返さないがBaseEntityBlockではない特殊ケース
        if (block instanceof net.minecraft.world.level.block.CraftingTableBlock) return true;
        if (block instanceof net.minecraft.world.level.block.EnchantmentTableBlock) return true;
        if (block instanceof net.minecraft.world.level.block.AnvilBlock) return true;
        if (block instanceof net.minecraft.world.level.block.LoomBlock) return true;
        if (block instanceof net.minecraft.world.level.block.CartographyTableBlock) return true;
        if (block instanceof net.minecraft.world.level.block.GrindstoneBlock) return true;
        if (block instanceof net.minecraft.world.level.block.StonecutterBlock) return true;
        if (block instanceof net.minecraft.world.level.block.SmithingTableBlock) return true;
        if (block instanceof net.minecraft.world.level.block.BedBlock) return true;
        if (block instanceof net.minecraft.world.level.block.DoorBlock) return true;
        if (block instanceof net.minecraft.world.level.block.FenceGateBlock) return true;
        if (block instanceof net.minecraft.world.level.block.TrapDoorBlock) return true;
        return false;
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();

        // シフトキーが押されている場合は通常の相互作用を許可
        if (player.isShiftKeyDown()) {
            return;
        }
        // 武器ラックへの着脱・回転は許可
        if (event.getTarget() instanceof minecraftarmorweapon.entity.WeaponRackEntity) {
            return;
        }

        // 武器を持っている場合は回避を優先（回避モード選択時のみキャンセル）
        if (canDodgeWithHands(player) && isRightClickDodgeEnabled(player)) {
            event.setCanceled(true);
            if (player.level().isClientSide && performDodge(player)) {
                MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                        new DodgeRequestPacket(player.zza, player.xxa));
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();

        // シフトキーが押されている場合は通常の相互作用を許可
        if (player.isShiftKeyDown()) {
            return;
        }
        // 武器ラックへの着脱・回転は許可
        if (event.getTarget() instanceof minecraftarmorweapon.entity.WeaponRackEntity) {
            return;
        }

        // 武器を持っている場合は回避を優先（回避モード選択時のみキャンセル）
        if (canDodgeWithHands(player) && isRightClickDodgeEnabled(player)) {
            event.setCanceled(true);
            if (player.level().isClientSide && performDodge(player)) {
                MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                        new DodgeRequestPacket(player.zza, player.xxa));
            }
        }
    }

    // ダッシュ攻撃（回避中に攻撃ボタンで発動、サーバー側で実行）
    public static void performDashAttack(Player player) {
        DodgeData data = getData(player);
        if (data == null) return;

        // 回避中でなければ実行しない
        if (!data.hasDodged || data.dodgeTimer <= 0) return;

        // クールダウン中は実行しない
        if (!data.canDashAttack()) return;

        // ダッシュスキル実行中は実行しない
        if (DashSkillHandler.isAnyDashSkillActive(player)) return;

        // クールダウンを設定＆回避ウィンドウをリセット
        data.dashAttackCooldown = 60; // 3秒のクールダウン
        data.reset();

        Level world = VersionHelper.getLevel(player);
        Vec3 lookVec = player.getLookAngle();
        Vec3 playerPos = player.position();

        // 竹を破壊する
        breakBambooInPath(world, playerPos, lookVec, 7.0);

        // スキルスロットに応じたモーション実行（DashSkillHandlerが移動・エフェクト・ダメージを処理）
        PlayerSkillData.SkillStorage skillData = PlayerSkillData.getSkillData(player);
        String motionId = skillData.getMotionForWeapon(AttackSlot.DASH, player.getMainHandItem());
        MotionExecutor.executeMotion(motionId, player, 0.0f);
    }
    
    // 回避処理（選択中のダッシュスキルを発動）
    // publicにしてDodgeRequestPacketから呼べるようにする
    // @return true=回避成功、false=クールダウン等でブロック
    public static boolean performDodge(Player player) {
        // 回避無効化設定チェック（グローバル設定）
        if (minecraftarmorweapon.config.DodgeConfig.dodgeDisabled) return false;

        // 右クリックスロットが「回避」以外に設定されている場合は回避しない
        ItemStack heldItem = player.getMainHandItem();
        if (!heldItem.isEmpty()) {
            minecraftarmorweapon.skill.PlayerSkillData.SkillStorage skillData =
                    minecraftarmorweapon.skill.PlayerSkillData.getSkillData(player);
            if (skillData != null) {
                String rightClickMotion = skillData.getMotionForWeapon(
                        minecraftarmorweapon.skill.PlayerSkillData.AttackSlot.RIGHT_CLICK, heldItem);
                if (!"dodge".equals(rightClickMotion)) return false;
            }
        }

        // メインハンドが近接武器で、オフハンドが右クリックで作用するアイテム（弓/クロスボウ/盾/投擲等）
        // なら回避せずにオフハンドのアイテムを使わせる。
        // 盾の場合も優先（防御/パリィ）。
        // メインハンドが遠距離武器の場合はこのチェックをスキップ（回避させる）。
        ItemStack offHand = player.getOffhandItem();
        if (isWeapon(heldItem) && !isRangedWeapon(heldItem) && !offHand.isEmpty()) {
            if (offHand.getItem() instanceof net.minecraft.world.item.ShieldItem) return false;
            if (offHand.getItem() instanceof net.minecraft.world.item.BowItem) return false;
            if (offHand.getItem() instanceof net.minecraft.world.item.CrossbowItem) return false;
            if (offHand.getItem() instanceof minecraftarmorweapon.item.ThrowingKnifeItem) return false;
            if (isActiveItem(offHand)) return false;
        }

        Level world = VersionHelper.getLevel(player);

        // 回避データを取得
        DodgeData data = getOrCreateData(player);

        // クールダウン中は回避できない（クライアント・サーバー両方でチェック）
        if (!data.canDodge()) {
            if (world.isClientSide) {
                player.displayClientMessage(
                    Component.literal(String.format("§c回避クールダウン中 (%.1f秒)",
                        (float)data.cooldownTimer / 20.0f)),
                    true
                );
            }
            return false;
        }

        // ダッシュスキル実行中は再回避を防止（サーバー側のみチェック、スキル状態はサーバーのみ管理）
        if (!world.isClientSide && DashSkillHandler.isAnyDashSkillActive(player)) {
            return false;
        }

        // 回避データを設定
        data.hasDodged = true;
        data.dodgeTimer = DODGE_WINDOW;
        data.cooldownPending = true; // 頂点到達後にクールダウン開始
        data.cooldownTimer = 0;
        data.fallDamageImmunityTimer = FALL_DAMAGE_IMMUNITY_TIME;
        data.dashAttackCooldown = Math.max(data.dashAttackCooldown, 15); // 既存の長いクールダウンを上書きしない

        // サーバー側で選択中のダッシュスキルを実行（スキル選択画面で変更可能）
        if (!world.isClientSide) {
            PlayerSkillData.SkillStorage skillData = PlayerSkillData.getSkillData(player);
            String dashMotionId = skillData.getMotionForWeapon(AttackSlot.DASH, player.getMainHandItem());
            MotionExecutor.executeMotion(dashMotionId, player, 0.0f);
        }

        // サウンド
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENDER_PEARL_THROW, SoundSource.PLAYERS, 0.8f, 1.5f);

        return true;
    }
    
    /**
     * 回避可能かどうかの武器判定。
     * メインハンドが武器ならOK。オフハンドが武器でもメインハンドが空でなければNG。
     * dodgeWithInertItems設定ON時: メインハンドが「何もしないアイテム」でもオフハンド武器があれば回避可能。
     */
    public static boolean canDodgeWithHands(Player player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        // フックショットは右クリックで自身の発射動作を行うので、回避と競合させない
        if (mainHand.getItem() instanceof minecraftarmorweapon.item.RecrossHookshotItem
                || offHand.getItem() instanceof minecraftarmorweapon.item.RecrossHookshotItem) {
            return false;
        }
        if (isWeapon(mainHand)) return true;
        if (isRangedWeapon(mainHand)) return true;
        if (isWeapon(offHand) && mainHand.isEmpty()) return true;
        if (isWeapon(offHand) && !mainHand.isEmpty()) {
            if (DodgeConfig.dodgeWithInertItems && isInertItem(mainHand)) return true;
            if (DodgeConfig.dodgeWithActiveItems && isActiveItem(mainHand)) return true;
        }
        return false;
    }

    /**
     * 現在の設定で右クリックが回避を発動するかどうか。
     * （スキル選択で RIGHT_CLICK が "dodge" に設定されている場合のみ true）
     * これを使って、event.setCanceled する前に実際に回避するかを確認する。
     */
    public static boolean isRightClickDodgeEnabled(Player player) {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) return true; // 素手のデフォルトは回避相当
        minecraftarmorweapon.skill.PlayerSkillData.SkillStorage skillData =
                minecraftarmorweapon.skill.PlayerSkillData.getSkillData(player);
        if (skillData == null) return true;
        String rightClickMotion = skillData.getMotionForWeapon(
                minecraftarmorweapon.skill.PlayerSkillData.AttackSlot.RIGHT_CLICK, heldItem);
        return "dodge".equals(rightClickMotion);
    }

    /**
     * 遠距離武器（弓/クロスボウ/投げナイフ）判定。
     * これらを持っている時、左クリック=回避/右クリック=vanilla発射 となる。
     * トライデントは除外（右クリック=投擲、左クリック=通常攻撃/コンボ）。
     */
    public static boolean isRangedWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
        if (item instanceof BowItem) return true;
        if (item instanceof CrossbowItem) return true;
        if (item instanceof minecraftarmorweapon.item.ThrowingKnifeItem) return true;
        return false;
    }

    /**
     * 右クリックしても何も起きない「不活性アイテム」かどうか判定。
     * ブロック、食べ物、弓、クロスボウ、盾、ポーション、バケツ、スポーンエッグ、
     * ツール（つるはし等）は除外。
     */
    private static boolean isInertItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
        if (item instanceof BlockItem) return false;
        if (item instanceof BowItem) return false;
        if (item instanceof CrossbowItem) return false;
        if (item instanceof ShieldItem) return false;
        if (item instanceof PotionItem) return false;
        if (item instanceof BucketItem) return false;
        if (item instanceof SpawnEggItem) return false;
        if (item instanceof DiggerItem) return false;
        if (stack.getFoodProperties(null) != null) return false;
        if (isWeapon(stack)) return false;
        if (isSaya(stack)) return false;
        return true;
    }

    /**
     * 右クリックで作用がある「アクティブアイテム」かどうか判定。
     * 弓、クロスボウ、盾、ポーション、バケツ、スポーンエッグ、食べ物など。
     */
    private static boolean isActiveItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        net.minecraft.world.item.Item item = stack.getItem();
        if (item instanceof BowItem) return true;
        if (item instanceof CrossbowItem) return true;
        if (item instanceof ShieldItem) return true;
        if (item instanceof PotionItem) return true;
        if (item instanceof BucketItem) return true;
        if (item instanceof SpawnEggItem) return true;
        if (stack.getFoodProperties(null) != null) return true;
        if (isWeapon(stack)) return false;
        if (isSaya(stack)) return false;
        if (item instanceof BlockItem) return false;
        if (item instanceof DiggerItem) return false;
        if (isInertItem(stack)) return false;
        return true;
    }

    public static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // SwordItemまたはカタナ系アイテムかチェック
        if (stack.getItem() instanceof SwordItem) return true;
        // トライデントも武器扱い（スキル選択の右クリックスロットで
        // "trident_throw"(投擲) / "dodge"(回避) をユーザーが選択できる）
        if (stack.getItem() instanceof net.minecraft.world.item.TridentItem) return true;

        String itemName = stack.getItem().getClass().getSimpleName();

        // Sayaアイテムの場合は、刀が入っているかチェック
        if (itemName.equals("SayaItem")) {
            // NBTタグを確認して、StoredKatanaが存在する場合のみtrue
            return stack.hasTag() && stack.getTag().contains("StoredKatana");
        }

        // SwordSayaItemは鞘そのもの（"Sword"部分文字列の誤検知防止）
        if (itemName.equals("SwordSayaItem")) {
            return stack.hasTag() && stack.getTag().contains("StoredSword");
        }

        // 直刀の判定
        if (minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.isStraightSword(stack)) {
            return true;
        }

        // その他の武器（大文字小文字を考慮）
        return itemName.contains("Katana") || itemName.contains("Sword") ||
               itemName.contains("Blade") || itemName.contains("katana") ||
               itemName.equals("RiversOfBloodItem") || itemName.equals("KatanaNiguHumerusItem");
    }
    
    public static boolean isSaya(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.equals("SayaItem") || itemName.equals("TyokutoSayaItem")
            || itemName.equals("SwordSayaItem");
    }

    private static boolean isTyokutouSaya(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.equals("TyokutoSayaItem");
    }

    private static boolean isSwordSaya(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.equals("SwordSayaItem");
    }
    
    // 納刀処理
    public static void performSheathing(Player player, ItemStack weaponStack, ItemStack sheathStack,
                                        InteractionHand weaponHand, InteractionHand sheathHand) {
        if (!isWeapon(weaponStack) || !isSaya(sheathStack)) return;

        // 直刀鞘の場合
        if (isTyokutouSaya(sheathStack)) {
            // 直刀のみ納刀可能
            boolean isStraightSword = minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.isStraightSword(weaponStack);

            if (isStraightSword) {
                minecraftarmorweapon.item.TyokutoSayaItem.sheatheSword(
                    player, weaponStack, sheathStack, weaponHand, sheathHand
                );
            } else {
                player.displayClientMessage(Component.literal("§cこの鞘には直刀のみ納刀可能です"), true);
            }
            return;
        }

        // 剣の鞘の場合
        if (isSwordSaya(sheathStack)) {
            if (minecraftarmorweapon.item.SwordSayaItem.canSheathe(weaponStack)) {
                minecraftarmorweapon.item.SwordSayaItem.sheatheSword(
                    player, weaponStack, sheathStack, weaponHand, sheathHand
                );
            } else {
                player.displayClientMessage(Component.literal("§cこの鞘にはこの剣を納刀できません"), true);
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
            // 剣の鞘対象アイテムは専用鞘が必要
            if (minecraftarmorweapon.item.SwordSayaItem.canSheathe(weaponStack)) {
                player.displayClientMessage(Component.literal("§cこの剣は専用の鞘が必要です"), true);
                return;
            }

            // 武器のNBTデータを保存
            CompoundTag weaponData = weaponStack.save(new CompoundTag());
            sheathTag.put("StoredKatana", weaponData);

            // SayaNBTタグを更新（霊刀スタイル判定用）
            String weaponItemName = weaponStack.getItem().getClass().getSimpleName();
            if (weaponItemName.equals("ReitouItem")) {
                sheathTag.putInt("SayaNBT", 1);
            }

            // 鞘の見た目を更新（CustomModelDataで刀が入っている状態を示す）
            sheathTag.putInt("CustomModelData", getWeaponModelData(weaponStack, sheathTag));

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
    
    private static int getWeaponModelData(ItemStack weapon, CompoundTag sheathTag) {
        String itemName = weapon.getItem().getClass().getSimpleName();

        // 霊刀はSayaNBT + feyn predicateで処理するのでCustomModelDataは0
        if (itemName.equals("ReitouItem")) return 0;
        if (itemName.equals("IronKatanaItem")) return 1;
        if (itemName.equals("GoldKatanaItem")) return 2;
        if (itemName.equals("StoneKatanaItem")) return 3;
        if (itemName.equals("NetheriteKatanaItem")) return 4;
        if (itemName.equals("WitherKatanaItem")) return 5;
        if (itemName.equals("DarknessKatanaItem")) return 7;
        if (itemName.equals("MagicalKatanaItem")) return 8;
        if (itemName.equals("MagischesFeenKatanaItem")) return 9;
        if (itemName.equals("PrototypeKatanaItem")) return 10;
        if (itemName.equals("OldKatanaItem")) return 11;
        if (itemName.equals("MyTestIronKatanaItem")) return 12;
        if (itemName.equals("RiversOfBloodItem")) return 13;
        if (itemName.equals("KatanaNiguHumerusItem")) return 14;
        if (itemName.equals("LokiTheTricksterItem")) return 15;
        if (itemName.equals("ReplicaSwordOfLightItem")) return 19;

        // リストにない武器でもSwordItemなら鉄刀の鞘モデルをフォールバック
        if (weapon.getItem() instanceof SwordItem) return 1;

        return 0; // デフォルト（空の鞘）
    }
    
    // 落下ダメージ無効化イベント
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        // 落下ダメージかチェック
        if (event.getSource() == player.damageSources().fall()) {
            DodgeData data = getData(player);

            // 落下ダメージ無効時間中はダメージをキャンセル
            if (data != null && data.isFallDamageImmune()) {
                event.setCanceled(true);
                
                // エフェクトと通知
                if (!player.level().isClientSide) {
                    ServerLevel serverWorld = (ServerLevel) VersionHelper.getLevel(player);
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
            if (VersionHelper.getLevel(player) instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1);
            }
            
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        
        return damage;
    }
    
    // 直刀の突進攻撃
    private static void performStraightSwordThrust(Player player, ItemStack weapon) {
        // 直刀の突進攻撃を実行
        minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.execute(
            VersionHelper.getLevel(player), player.getX(), player.getY(), player.getZ(), player
        );

        // クールダウン設定
        DodgeData data = getData(player);
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
                            (int)(checkPos.x + dx),
                            (int)(checkPos.y + dy),
                            (int)(checkPos.z + dz)
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