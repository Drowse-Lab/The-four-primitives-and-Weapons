package minecraftarmorweapon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import minecraftarmorweapon.MinecraftArmorWeaponMod;

import java.util.function.Supplier;

public class AttackPacket {
    private final int attackType; // 0 = normal, 1 = charged, 2 = falling
    private final float chargePercent;
    
    static {
        // パケットを静的初期化ブロックで登録
        MinecraftArmorWeaponMod.addNetworkMessage(AttackPacket.class, AttackPacket::encode, AttackPacket::new, AttackPacket::handle);
    }

    public AttackPacket(int attackType, float chargePercent) {
        this.attackType = attackType;
        this.chargePercent = chargePercent;
    }
    
    public AttackPacket(FriendlyByteBuf buf) {
        this.attackType = buf.readInt();
        this.chargePercent = buf.readFloat();
    }

    public static void encode(AttackPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.attackType);
        buf.writeFloat(msg.chargePercent);
    }


    public static void handle(AttackPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                if (msg.attackType == 0) {
                    // 通常攻撃
                    minecraftarmorweapon.events.ChargedAttackHandler.performNormalAttack(player);
                } else if (msg.attackType == 1) {
                    // 溜め攻撃
                    minecraftarmorweapon.events.ChargedAttackHandler.performChargedAttack(player, msg.chargePercent);
                } else if (msg.attackType == 2) {
                    // 落下攻撃
                    minecraftarmorweapon.events.ChargedAttackHandler.performFallingAttack(player, msg.chargePercent);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}