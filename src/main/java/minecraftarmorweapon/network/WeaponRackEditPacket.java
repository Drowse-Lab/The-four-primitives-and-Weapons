package minecraftarmorweapon.network;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.entity.WeaponRackEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアントから WeaponRackEntity の追加回転を編集するためのパケット。
 *   axis 0 = X, 1 = Y, 2 = Z, 3 = reset (delta は無視)
 *   delta は度数 (例: +5 や -5)
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class WeaponRackEditPacket {

	private final int entityId;
	private final int axis;
	private final float delta;

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		MinecraftArmorWeaponMod.addNetworkMessage(WeaponRackEditPacket.class,
			WeaponRackEditPacket::encode, WeaponRackEditPacket::new, WeaponRackEditPacket::handle);
	}

	public WeaponRackEditPacket(int entityId, int axis, float delta) {
		this.entityId = entityId;
		this.axis = axis;
		this.delta = delta;
	}

	public WeaponRackEditPacket(FriendlyByteBuf buf) {
		this.entityId = buf.readInt();
		this.axis = buf.readByte();
		this.delta = buf.readFloat();
	}

	public static void encode(WeaponRackEditPacket msg, FriendlyByteBuf buf) {
		buf.writeInt(msg.entityId);
		buf.writeByte(msg.axis);
		buf.writeFloat(msg.delta);
	}

	public static void handle(WeaponRackEditPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			Entity e = player.level().getEntity(msg.entityId);
			if (!(e instanceof WeaponRackEntity rack)) return;
			// 距離チェック (作弊防止): 8ブロック以内のみ
			if (rack.distanceToSqr(player) > 64.0) return;
			switch (msg.axis) {
				case 0 -> rack.setExtraRotX(rack.getExtraRotX() + msg.delta);
				case 1 -> rack.setExtraRotY(rack.getExtraRotY() + msg.delta);
				case 2 -> rack.setExtraRotZ(rack.getExtraRotZ() + msg.delta);
				case 3 -> rack.resetExtraRot();
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
