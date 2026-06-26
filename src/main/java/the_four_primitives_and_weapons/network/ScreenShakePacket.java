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
 * S2C: クライアントの視界揺れ (画面揺れ) を開始する。
 * 地叩きの着弾点から半径16m以内のプレイヤーへ、距離減衰した intensity で送る。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ScreenShakePacket {

	public final float intensity; // 0..1
	public final int duration;    // tick

	public ScreenShakePacket(float intensity, int duration) {
		this.intensity = intensity;
		this.duration = duration;
	}

	public ScreenShakePacket(FriendlyByteBuf buf) {
		this.intensity = buf.readFloat();
		this.duration = buf.readInt();
	}

	public static void encode(ScreenShakePacket msg, FriendlyByteBuf buf) {
		buf.writeFloat(msg.intensity);
		buf.writeInt(msg.duration);
	}

	public static void handle(ScreenShakePacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> the_four_primitives_and_weapons.client.event.ScreenShakeHandler.start(msg.intensity, msg.duration)));
		ctx.get().setPacketHandled(true);
	}

	@SubscribeEvent
	public static void register(FMLCommonSetupEvent event) {
		TheFourPrimitivesAndWeaponsMod.addNetworkMessage(ScreenShakePacket.class,
				ScreenShakePacket::encode, ScreenShakePacket::new, ScreenShakePacket::handle);
	}
}
