package the_four_primitives_and_weapons.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.events.MagicalKatanaCrystalHandler;

import java.util.function.Supplier;

/**
 * Magical Katana 専用アクションを R ホイール経由で発動するパケット。
 *   - SPAWN_CRYSTAL : 結晶を生成 ( 特殊技 ) — unlocked 限定
 *   - SHATTER       : 具現化版を破壊 ( shatter ) — 手に持ってる Materialized が対象
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class MagicalKatanaActionPacket {

    public enum Action { SPAWN_CRYSTAL, SHATTER }

    private final Action action;

    public MagicalKatanaActionPacket(Action action) {
        this.action = action;
    }

    public MagicalKatanaActionPacket(FriendlyByteBuf buf) {
        this.action = Action.values()[buf.readByte()];
    }

    public static void encode(MagicalKatanaActionPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.action.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (action == Action.SPAWN_CRYSTAL) {
                handleSpawnCrystal(player);
            } else {
                handleShatter(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleSpawnCrystal(ServerPlayer player) {
        ItemStack stack = pickMagicalKatana(player, false);
        if (stack.isEmpty()) return;
        if (!MagicalKatanaCrystalHandler.isUnlocked(stack)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "§7封印された Magical Katana §8— §c具現化版を破壊§7、 または §6Lv12 (CORROSION) §7をセット"), true);
            return;
        }
        MagicalKatanaCrystalHandler.spawnCrystal(player);
    }

    private void handleShatter(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (MagicalKatanaCrystalHandler.isMaterialized(main)) {
            MagicalKatanaCrystalHandler.shatterOnSheathe(player, main, InteractionHand.MAIN_HAND);
            return;
        }
        ItemStack off = player.getOffhandItem();
        if (MagicalKatanaCrystalHandler.isMaterialized(off)) {
            MagicalKatanaCrystalHandler.shatterOnSheathe(player, off, InteractionHand.OFF_HAND);
        }
    }

    /** Magical Katana が手のどちらかにあれば返す ( materialized 含む or 限定 ) */
    private static ItemStack pickMagicalKatana(ServerPlayer player, boolean materializedOnly) {
        ItemStack main = player.getMainHandItem();
        if (MagicalKatanaCrystalHandler.isMagicalKatana(main)
                && (!materializedOnly || MagicalKatanaCrystalHandler.isMaterialized(main))) return main;
        ItemStack off = player.getOffhandItem();
        if (MagicalKatanaCrystalHandler.isMagicalKatana(off)
                && (!materializedOnly || MagicalKatanaCrystalHandler.isMaterialized(off))) return off;
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(MagicalKatanaActionPacket.class,
                MagicalKatanaActionPacket::encode,
                MagicalKatanaActionPacket::new,
                (msg, ctx) -> msg.handle(ctx));
    }
}
