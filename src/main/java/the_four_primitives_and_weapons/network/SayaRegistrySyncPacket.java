package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.util.SayaRegistry;
import the_four_primitives_and_weapons.util.SayaRegistry.Entry;
import the_four_primitives_and_weapons.util.SayaRegistry.SayaType;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * サーバー→クライアント: SayaRegistry の内容を同期する。
 * data/&lt;ns&gt;/maw_saya/*.json は専用サーバーではクライアントに自動配布されないため、
 * 鞘の見た目を解決する SayaModelWrapper がマルチプレイでフォールバックモデルしか
 * 返せなくなる問題を回避するためのパケット。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SayaRegistrySyncPacket {

    private final Map<SayaType, Map<ResourceLocation, Entry>> entries;

    public SayaRegistrySyncPacket(Map<SayaType, Map<ResourceLocation, Entry>> entries) {
        this.entries = entries;
    }

    public SayaRegistrySyncPacket(FriendlyByteBuf buffer) {
        Map<SayaType, Map<ResourceLocation, Entry>> read = new EnumMap<>(SayaType.class);
        int typeCount = buffer.readVarInt();
        for (int t = 0; t < typeCount; t++) {
            int typeOrdinal = buffer.readVarInt();
            SayaType type = (typeOrdinal >= 0 && typeOrdinal < SayaType.values().length)
                ? SayaType.values()[typeOrdinal] : null;
            int entryCount = buffer.readVarInt();
            Map<ResourceLocation, Entry> map = new HashMap<>(entryCount);
            for (int i = 0; i < entryCount; i++) {
                ResourceLocation itemId = buffer.readResourceLocation();
                byte flag = buffer.readByte();
                if (flag == 0) {
                    int slot = buffer.readVarInt();
                    if (type != null) map.put(itemId, Entry.ofSlot(slot));
                } else {
                    ResourceLocation modelLoc = buffer.readResourceLocation();
                    if (type != null) map.put(itemId, Entry.ofModel(modelLoc));
                }
            }
            if (type != null) read.put(type, map);
        }
        // 不足タイプは空マップで埋める
        for (SayaType t : SayaType.values()) read.computeIfAbsent(t, k -> new HashMap<>());
        this.entries = read;
    }

    public static void buffer(SayaRegistrySyncPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entries.size());
        for (Map.Entry<SayaType, Map<ResourceLocation, Entry>> typeEntry : message.entries.entrySet()) {
            buffer.writeVarInt(typeEntry.getKey().ordinal());
            Map<ResourceLocation, Entry> map = typeEntry.getValue();
            buffer.writeVarInt(map.size());
            for (Map.Entry<ResourceLocation, Entry> e : map.entrySet()) {
                buffer.writeResourceLocation(e.getKey());
                Entry val = e.getValue();
                if (val.hasCustomModel()) {
                    buffer.writeByte(1);
                    buffer.writeResourceLocation(val.modelLocation());
                } else {
                    buffer.writeByte(0);
                    buffer.writeVarInt(val.modelData());
                }
            }
        }
    }

    public static void handler(SayaRegistrySyncPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> SayaRegistry.replaceEntriesClient(message.entries));
        context.setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(
            SayaRegistrySyncPacket.class,
            SayaRegistrySyncPacket::buffer,
            SayaRegistrySyncPacket::new,
            SayaRegistrySyncPacket::handler
        );
    }
}
