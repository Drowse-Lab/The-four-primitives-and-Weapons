package minecraftarmorweapon.events;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import org.joml.Vector3f;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.network.GuardSyncPacket;

/**
 * Shift+右クリックで全SwordItemにガード発動
 * ダメージカットはMixin（GuardDamageReductionMixin）で処理
 * ビジュアルはパーティクルで表現（アーマースタンド不使用）
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class SwordGuardHandler {

    private static final String GUARD_TICKS_TAG = "SwordGuardTicks";
    private static final String GUARD_FULL_TAG = "SwordGuardFull";
    private static final String GUARD_POS_X = "SwordGuardPosX";
    private static final String GUARD_POS_Y = "SwordGuardPosY";
    private static final String GUARD_POS_Z = "SwordGuardPosZ";

    private static final int REPLICA_GUARD_DURATION = 9;
    private static final int NORMAL_GUARD_DURATION = 15;
    private static final int GUARD_COOLDOWN = 40;

    /**
     * Shift+右クリックでガード発動
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!player.isShiftKeyDown()) return;

        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof SwordItem)) return;

        CompoundTag data = player.getPersistentData();

        // 既にガード中なら無視
        if (data.getInt(GUARD_TICKS_TAG) > 0) return;

        // 既にGUARDエフェクト中なら無視（既存システムとの競合防止）
        if (player.hasEffect(MinecraftArmorWeaponModMobEffects.GUARD.get())) return;

        // クールダウン中なら無視
        if (player.getCooldowns().isOnCooldown(mainHand.getItem())) return;

        boolean isReplica = mainHand.getItem() == MinecraftArmorWeaponModItems.REPLICA_SWORD_OF_LIGHT.get();
        int duration = isReplica ? REPLICA_GUARD_DURATION : NORMAL_GUARD_DURATION;

        // NBTタグでガード状態を設定
        data.putInt(GUARD_TICKS_TAG, duration);
        data.putBoolean(GUARD_FULL_TAG, isReplica); // true=100%カット, false=25%カット

        // ガード位置を記録（位置固定用）
        data.putDouble(GUARD_POS_X, player.getX());
        data.putDouble(GUARD_POS_Y, player.getY());
        data.putDouble(GUARD_POS_Z, player.getZ());

        // ノックバック耐性を一時保存して1に設定
        data.putDouble("SwordGuardOrigKBRes",
                player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).getBaseValue());
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);

        // パーティクルとサウンド
        if (player.level() instanceof ServerLevel serverLevel) {
            float r = isReplica ? 1f : 0.8f;
            float g = isReplica ? 1f : 0.8f;
            float b = isReplica ? 0.5f : 0.8f;
            serverLevel.sendParticles(
                    new DustParticleOptions(new Vector3f(r, g, b), 0.8f),
                    player.getX(), player.getY() + 1, player.getZ(),
                    35, 0.4, 0.6, 0.4, 0.01);
            serverLevel.playSound(null, player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.5f, 2f);
            serverLevel.playSound(null, player.blockPosition(),
                    SoundEvents.ARMOR_EQUIP_GOLD, SoundSource.PLAYERS, 1f, 1f);
        }

        // ガード状態をクライアントに同期（腕ポーズ用）
        if (player instanceof ServerPlayer serverPlayer) {
            GuardSyncPacket packet = new GuardSyncPacket(player.getId(), true);
            MinecraftArmorWeaponMod.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer), packet);
        }

        // クールダウン
        player.getCooldowns().addCooldown(mainHand.getItem(), duration + GUARD_COOLDOWN);

        event.setCanceled(true);
    }

    /**
     * ガードタイマーのカウントダウン + 位置固定 + パーティクル
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide()) return;

        CompoundTag data = player.getPersistentData();
        int guardTicks = data.getInt(GUARD_TICKS_TAG);
        if (guardTicks <= 0) return;

        // 位置固定
        double gx = data.getDouble(GUARD_POS_X);
        double gy = data.getDouble(GUARD_POS_Y);
        double gz = data.getDouble(GUARD_POS_Z);
        player.teleportTo(gx, gy, gz);

        // ガード中パーティクル（毎tick）
        if (player.level() instanceof ServerLevel serverLevel) {
            boolean isFull = data.getBoolean(GUARD_FULL_TAG);
            float r = isFull ? 1f : 0.7f;
            float g = isFull ? 1f : 0.7f;
            float b = isFull ? 0.5f : 0.7f;
            serverLevel.sendParticles(
                    new DustParticleOptions(new Vector3f(r, g, b), 0.5f),
                    player.getX(), player.getY() + 1, player.getZ(),
                    5, 0.3, 0.5, 0.3, 0.01);
        }

        // タイマー減算
        data.putInt(GUARD_TICKS_TAG, guardTicks - 1);

        // ガード終了
        if (guardTicks - 1 <= 0) {
            endGuard(player, data);
        }
    }

    /**
     * ガード終了処理
     */
    private static void endGuard(Player player, CompoundTag data) {
        data.putInt(GUARD_TICKS_TAG, 0);
        data.remove(GUARD_FULL_TAG);

        // ノックバック耐性を元に戻す
        double origKBRes = data.getDouble("SwordGuardOrigKBRes");
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(origKBRes);
        data.remove("SwordGuardOrigKBRes");

        // ガード終了をクライアントに同期（腕ポーズ解除）
        if (player instanceof ServerPlayer serverPlayer) {
            GuardSyncPacket packet = new GuardSyncPacket(player.getId(), false);
            MinecraftArmorWeaponMod.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer), packet);
        }

        // 終了サウンド
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.blockPosition(),
                    SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 0.5f, 1.5f);
        }
    }
}
