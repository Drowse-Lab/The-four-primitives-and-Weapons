package minecraftarmorweapon.network;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.entity.ThrowingKnifeEntity.KnifeType;
import minecraftarmorweapon.item.KnifeLauncherItem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * ナイフランチャー: クライアント→サーバーでの設定変更パケット。
 *
 *   action=0 (SET_MODE):  param = KnifeType.ordinal() に相当する値
 *   action=1 (SET_COUNT): param = 1〜MAX_COUNT の本数
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class KnifeLauncherPacket {
    public static final int ACTION_SET_MODE  = 0;
    public static final int ACTION_SET_COUNT = 1;

    private final int action;
    private final int param;

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(KnifeLauncherPacket.class,
            KnifeLauncherPacket::encode, KnifeLauncherPacket::new, KnifeLauncherPacket::handle);
    }

    public KnifeLauncherPacket(int action, int param) {
        this.action = action;
        this.param = param;
    }

    public KnifeLauncherPacket(FriendlyByteBuf buf) {
        this.action = buf.readInt();
        this.param = buf.readInt();
    }

    public static void encode(KnifeLauncherPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.action);
        buf.writeInt(msg.param);
    }

    public static void handle(KnifeLauncherPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!(stack.getItem() instanceof KnifeLauncherItem)) return;

            if (msg.action == ACTION_SET_MODE) {
                KnifeType[] modes = { KnifeType.NORMAL, KnifeType.STUN, KnifeType.SCREW, KnifeType.HOMING };
                int idx = Math.max(0, Math.min(modes.length - 1, msg.param));
                KnifeLauncherItem.setMode(stack, modes[idx]);
            } else if (msg.action == ACTION_SET_COUNT) {
                // setCount 内部でモード別最大値へ clamp される
                KnifeLauncherItem.setCount(stack, msg.param);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
