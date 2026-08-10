package the_four_primitives_and_weapons.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.client.event.BloodTargetGlowRenderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/** 攻撃者本人だけに Rivers of Blood の対象 Entity ID を同期する。 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BloodTargetGlowPacket {
    private final boolean clear;
    private final List<Integer> entityIds;

    public BloodTargetGlowPacket(boolean clear, Collection<Integer> entityIds) {
        this.clear = clear;
        this.entityIds = new ArrayList<>(entityIds);
    }

    public BloodTargetGlowPacket(FriendlyByteBuf buf) {
        this.clear = buf.readBoolean();
        int count = buf.readVarInt();
        this.entityIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) entityIds.add(buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(clear);
        buf.writeVarInt(entityIds.size());
        for (int id : entityIds) buf.writeVarInt(id);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().level == null) return;
            if (clear) BloodTargetGlowRenderer.clear();
            else BloodTargetGlowRenderer.setTargets(entityIds);
        }));
        context.get().setPacketHandled(true);
    }

    public static void show(ServerPlayer viewer, Collection<? extends LivingEntity> targets) {
        List<Integer> ids = targets.stream().map(LivingEntity::getId).toList();
        send(viewer, new BloodTargetGlowPacket(false, ids));
    }

    public static void clear(ServerPlayer viewer) {
        send(viewer, new BloodTargetGlowPacket(true, List.of()));
    }

    private static void send(ServerPlayer viewer, BloodTargetGlowPacket packet) {
        TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendTo(
                packet, viewer.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(BloodTargetGlowPacket.class,
                BloodTargetGlowPacket::encode,
                BloodTargetGlowPacket::new,
                BloodTargetGlowPacket::handle);
    }
}
