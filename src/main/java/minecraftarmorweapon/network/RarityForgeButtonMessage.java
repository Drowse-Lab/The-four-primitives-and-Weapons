package minecraftarmorweapon.network;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.world.inventory.RarityForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー: クラフト候補クリック通知
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RarityForgeButtonMessage {

    private final int x, y, z;
    private final int recipeIndex;

    public RarityForgeButtonMessage(int x, int y, int z, int recipeIndex) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.recipeIndex = recipeIndex;
    }

    public RarityForgeButtonMessage(FriendlyByteBuf buffer) {
        this.x = buffer.readInt();
        this.y = buffer.readInt();
        this.z = buffer.readInt();
        this.recipeIndex = buffer.readInt();
    }

    public static void buffer(RarityForgeButtonMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.x);
        buffer.writeInt(message.y);
        buffer.writeInt(message.z);
        buffer.writeInt(message.recipeIndex);
    }

    public static void handler(RarityForgeButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Player player = context.getSender();
            if (player == null) return;

            Level world = player.level();
            if (!world.hasChunkAt(new BlockPos(message.x, message.y, message.z))) return;

            if (player.containerMenu instanceof RarityForgeMenu menu) {
                menu.performForge(message.recipeIndex);
            }
        });
        context.setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(
                RarityForgeButtonMessage.class,
                RarityForgeButtonMessage::buffer,
                RarityForgeButtonMessage::new,
                RarityForgeButtonMessage::handler
        );
    }
}
