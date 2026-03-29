package minecraftarmorweapon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー→クライアントへガード状態を同期するパケット
 * クライアント側で盾のような腕ポーズを表示するために使用
 */
public class GuardSyncPacket {
    private final int entityId;
    private final boolean guarding;

    public GuardSyncPacket(int entityId, boolean guarding) {
        this.entityId = entityId;
        this.guarding = guarding;
    }

    public GuardSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.guarding = buf.readBoolean();
    }

    public static void encode(GuardSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.guarding);
    }

    public static void handle(GuardSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                minecraftarmorweapon.client.GuardClientState.setGuarding(msg.entityId, msg.guarding);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
