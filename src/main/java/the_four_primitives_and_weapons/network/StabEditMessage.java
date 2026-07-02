package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.entity.StabbedWeaponEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 突き刺さった武器/杭を後から調整する ( クライアントのスクロール操作 → サーバーで反映 )。
 *   mode 0 = 向き(yaw) / 1 = 傾き(tilt) / 2 = 高さ(Y) / 3 = 判定半径 / 4 = ロール / 5 = スケール
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class StabEditMessage {

	public final int entityId;
	public final int mode;
	public final float delta;

	public StabEditMessage(int entityId, int mode, float delta) {
		this.entityId = entityId;
		this.mode = mode;
		this.delta = delta;
	}

	public StabEditMessage(FriendlyByteBuf buf) {
		this.entityId = buf.readInt();
		this.mode = buf.readInt();
		this.delta = buf.readFloat();
	}

	public static void buffer(StabEditMessage m, FriendlyByteBuf buf) {
		buf.writeInt(m.entityId);
		buf.writeInt(m.mode);
		buf.writeFloat(m.delta);
	}

	public static void handler(StabEditMessage m, Supplier<NetworkEvent.Context> ctxSupplier) {
		NetworkEvent.Context ctx = ctxSupplier.get();
		ctx.enqueueWork(() -> {
			ServerPlayer player = ctx.getSender();
			if (player == null) return;
			Entity e = player.level().getEntity(m.entityId);
			if (!(e instanceof StabbedWeaponEntity s)) return;
			if (s.distanceToSqr(player) > 100.0) return; // 近く ( 10 ブロック以内 ) のみ編集可
			switch (m.mode) {
				case 0 -> s.setStabYaw(wrap(s.getStabYaw() + m.delta));     // 向き ( 360° )
				case 1 -> s.setTilt(wrap(s.getTilt() + m.delta));          // 傾き ( pitch, 360° 自由 )
				case 2 -> s.setPos(s.getX(), s.getY() + m.delta, s.getZ()); // 高さ
				case 3 -> s.setRadius(s.getRadius() + m.delta);             // 当たり判定/編集球の半径
				case 4 -> s.setRoll(wrap(s.getRoll() + m.delta));          // ロール ( 360° )
				case 5 -> s.setScale(s.getScale() + m.delta);              // 表示スケール ( 0.2〜3.0 )
				default -> {}
			}
		});
		ctx.setPacketHandled(true);
	}

	/** -180..180 に正規化 ( 値が無限に膨らまないように )。 */
	private static float wrap(float deg) {
		float d = deg % 360f;
		if (d > 180f) d -= 360f;
		if (d < -180f) d += 360f;
		return d;
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		TheFourPrimitivesAndWeaponsMod.addNetworkMessage(
				StabEditMessage.class, StabEditMessage::buffer, StabEditMessage::new, StabEditMessage::handler);
	}
}
