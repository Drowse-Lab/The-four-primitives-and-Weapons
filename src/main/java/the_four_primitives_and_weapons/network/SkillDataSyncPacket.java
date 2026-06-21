package the_four_primitives_and_weapons.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.skill.PlayerSkillData;

import java.util.function.Supplier;

/**
 * サーバー → クライアント: PlayerSkillData の全状態を NBT で同期するパケット。
 *
 * 同期する内容 (PlayerSkillData.SkillStorage.serializeNBT で網羅):
 *   - selectedMotions (デフォルトモーション)
 *   - weaponSlots (5 個の登録武器ロードアウト)
 *   - typeMotions  (武器タイプ別モーション設定)
 *   - uniqueSkillToggle (固有スキル ON/OFF)
 *   - weaponProficiency (得意武器タイプ)
 *
 * 発火タイミング:
 *   - PlayerLoggedInEvent  (ログイン時)
 *   - PlayerRespawnEvent   (死亡 → 復活時)
 *   - PlayerChangedDimensionEvent (次元移動時)
 *
 * これによりクライアント capability がサーバー保存値と常に一致し、
 * UI 表示・回避判定・通常モーション選択など、 client capability を読む
 * 全ての処理がマルチプレイでも正しく機能する。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SkillDataSyncPacket {

    private final CompoundTag nbt;

    public SkillDataSyncPacket(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public SkillDataSyncPacket(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(nbt);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // クライアント側でのみ動作
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> applyClient(nbt));
        });
        ctx.get().setPacketHandled(true);
    }

    private static void applyClient(CompoundTag nbt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(sd ->
                sd.deserializeNBT(nbt));
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(SkillDataSyncPacket.class,
                SkillDataSyncPacket::encode,
                SkillDataSyncPacket::new,
                SkillDataSyncPacket::handle);
    }

    /**
     * 指定 ServerPlayer 1 名にだけ同期 (login/respawn/dimension 時に呼ぶ)。
     */
    public static void sendTo(ServerPlayer sp) {
        if (sp == null) return;
        sp.getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(sd ->
                TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendTo(
                        new SkillDataSyncPacket(sd.serializeNBT()),
                        sp.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT));
    }

    /** プレイヤーイベント自動ハンドラー。 */
    @Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
    public static class Auto {
        @SubscribeEvent
        public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer sp) sendTo(sp);
        }

        @SubscribeEvent
        public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer sp) sendTo(sp);
        }

        @SubscribeEvent
        public static void onDimChange(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer sp) sendTo(sp);
        }
    }
}
