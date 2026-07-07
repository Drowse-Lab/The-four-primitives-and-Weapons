package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: 指定エンティティの「炎を魂の炎(青い炎)として描画するか」フラグを同期する。
 * サーバー側 {@link the_four_primitives_and_weapons.damage.SoulFireHandler} が
 * 状態変化時のみ、そのエンティティを tracking しているプレイヤー(+本人)へ送る。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SoulFireSyncPacket {

	public final int entityId;
	public final boolean soul;

	public SoulFireSyncPacket(int entityId, boolean soul) {
		this.entityId = entityId;
		this.soul = soul;
	}

	public SoulFireSyncPacket(FriendlyByteBuf buf) {
		this.entityId = buf.readVarInt();
		this.soul = buf.readBoolean();
	}

	public static void encode(SoulFireSyncPacket msg, FriendlyByteBuf buf) {
		buf.writeVarInt(msg.entityId);
		buf.writeBoolean(msg.soul);
	}

	public static void handle(SoulFireSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> the_four_primitives_and_weapons.client.ClientSoulFireState.set(msg.entityId, msg.soul)));
		ctx.get().setPacketHandled(true);
	}

	@SubscribeEvent
	public static void register(FMLCommonSetupEvent event) {
		TheFourPrimitivesAndWeaponsMod.addNetworkMessage(SoulFireSyncPacket.class,
				SoulFireSyncPacket::encode, SoulFireSyncPacket::new, SoulFireSyncPacket::handle);
	}
}
