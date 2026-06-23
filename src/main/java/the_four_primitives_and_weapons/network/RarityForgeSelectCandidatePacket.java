package the_four_primitives_and_weapons.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.world.inventory.RarityForgeMenu;

import java.util.function.Supplier;

/**
 * クライアント → サーバー: レアリティ作業台の候補リストから選んだ結果アイテム ID を送る。
 *   - itemId 空文字列 : 選択解除 ( 自動 preview に戻す )
 *   - それ以外        : 該当アイテムを結果に持つレシピ ( legacy 優先、 次にバニラ ) を選択
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RarityForgeSelectCandidatePacket {

    private final String itemId;

    public RarityForgeSelectCandidatePacket(String itemId) {
        this.itemId = itemId == null ? "" : itemId;
    }

    public RarityForgeSelectCandidatePacket(FriendlyByteBuf buf) {
        this.itemId = buf.readUtf();
    }

    public static void encode(RarityForgeSelectCandidatePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.itemId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.containerMenu instanceof RarityForgeMenu menu) {
                menu.selectCandidateByItemId(itemId);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(RarityForgeSelectCandidatePacket.class,
                RarityForgeSelectCandidatePacket::encode,
                RarityForgeSelectCandidatePacket::new,
                (msg, ctx) -> msg.handle(ctx));
    }
}
