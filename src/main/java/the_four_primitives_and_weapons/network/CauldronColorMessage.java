package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.client.CauldronColorClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 大釜の染色色をクライアントへ同期する。
 *   replaceAll=true : 受信側のマップを一旦クリアしてから entries を反映（ログイン/次元移動時の一括同期）
 *   color == REMOVE : その位置の色を削除
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CauldronColorMessage {

	public static final int REMOVE = Integer.MIN_VALUE;

	public final boolean replaceAll;
	public final long[] positions;
	public final int[] colors;

	public CauldronColorMessage(boolean replaceAll, long[] positions, int[] colors) {
		this.replaceAll = replaceAll;
		this.positions = positions;
		this.colors = colors;
	}

	public CauldronColorMessage(FriendlyByteBuf buf) {
		this.replaceAll = buf.readBoolean();
		int n = buf.readVarInt();
		this.positions = new long[n];
		this.colors = new int[n];
		for (int i = 0; i < n; i++) {
			this.positions[i] = buf.readLong();
			this.colors[i] = buf.readInt();
		}
	}

	public static void buffer(CauldronColorMessage m, FriendlyByteBuf buf) {
		buf.writeBoolean(m.replaceAll);
		buf.writeVarInt(m.positions.length);
		for (int i = 0; i < m.positions.length; i++) {
			buf.writeLong(m.positions[i]);
			buf.writeInt(m.colors[i]);
		}
	}

	public static void handler(CauldronColorMessage m, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() ->
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CauldronColorClient.apply(m)));
		ctx.get().setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		TheFourPrimitivesAndWeaponsMod.addNetworkMessage(
				CauldronColorMessage.class, CauldronColorMessage::buffer, CauldronColorMessage::new, CauldronColorMessage::handler);
	}
}
