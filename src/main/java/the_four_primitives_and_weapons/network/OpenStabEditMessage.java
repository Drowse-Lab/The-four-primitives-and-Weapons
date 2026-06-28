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
 * サーバー → クライアント: 指定エンティティの編集GUIを開く ( /stabedit から )。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class OpenStabEditMessage {

	public final int entityId;

	public OpenStabEditMessage(int entityId) {
		this.entityId = entityId;
	}

	public OpenStabEditMessage(FriendlyByteBuf buf) {
		this.entityId = buf.readInt();
	}

	public static void buffer(OpenStabEditMessage m, FriendlyByteBuf buf) {
		buf.writeInt(m.entityId);
	}

	public static void handler(OpenStabEditMessage m, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> the_four_primitives_and_weapons.client.screens.StabEditScreen.open(m.entityId)));
		ctx.setPacketHandled(true);
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		TheFourPrimitivesAndWeaponsMod.addNetworkMessage(
				OpenStabEditMessage.class, OpenStabEditMessage::buffer, OpenStabEditMessage::new, OpenStabEditMessage::handler);
	}
}
