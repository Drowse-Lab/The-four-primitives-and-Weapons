package minecraftarmorweapon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.command.CustomDifficultyCommand;
import minecraftarmorweapon.command.CustomDifficultyCommand.CustomDifficulty;

import java.util.function.Supplier;

/**
 * クライアントからサーバーにカスタム難易度の変更を要求するパケット
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CustomDifficultyPacket {

    private final String difficultyName;

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(CustomDifficultyPacket.class,
                CustomDifficultyPacket::encode, CustomDifficultyPacket::new, CustomDifficultyPacket::handle);
    }

    public CustomDifficultyPacket(String difficultyName) {
        this.difficultyName = difficultyName;
    }

    public CustomDifficultyPacket(FriendlyByteBuf buf) {
        this.difficultyName = buf.readUtf(32);
    }

    public static void encode(CustomDifficultyPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.difficultyName, 32);
    }

    public static void handle(CustomDifficultyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) {
                CustomDifficulty newDifficulty = CustomDifficulty.byName(msg.difficultyName);

                // 難易度を設定
                CustomDifficultyCommand.setCurrentDifficulty(newDifficulty);

                // ベースの難易度を設定
                ServerLevel level = player.serverLevel();
                level.getServer().setDifficulty(newDifficulty.getBaseDifficulty(), true);

                // NBTに保存
                net.minecraft.nbt.CompoundTag customData = level.getServer().getWorldData().getCustomBossEvents();
                if (customData != null) {
                    customData.putString("minecraft_armor_weapon:custom_difficulty", newDifficulty.getName());
                }

                // メッセージを送信
                String message = String.format(
                    "難易度を %s に設定しました (Mobダメージ倍率: %.1fx)",
                    newDifficulty.getName(),
                    newDifficulty.getDamageMultiplier()
                );
                player.sendSystemMessage(Component.literal(message));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
