package minecraftarmorweapon.network;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.skill.PlayerSkillData;
import minecraftarmorweapon.skill.PlayerSkillData.AttackSlot;
import minecraftarmorweapon.skill.PlayerSkillData.WeaponLoadout;
import minecraftarmorweapon.world.inventory.SkillSelectionMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

/**
 * クライアント→サーバーパケット。サーバー側でスキル選択コンテナを開く。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class OpenSkillScreenPacket {

    public OpenSkillScreenPacket() {}

    public OpenSkillScreenPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            PlayerSkillData.SkillStorage skillData = player.getCapability(PlayerSkillData.SKILL_CAPABILITY)
                    .orElse(new PlayerSkillData.SkillStorage());

            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("\u6280\u306E\u9078\u629E");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new SkillSelectionMenu(id, inv, null);
                }
            }, buf -> {
                // ロードアウトのモーション設定をクライアントに送信
                for (int i = 0; i < PlayerSkillData.MAX_WEAPON_SLOTS; i++) {
                    WeaponLoadout loadout = skillData.getLoadoutAt(i);
                    if (loadout != null) {
                        buf.writeBoolean(true);
                        for (AttackSlot slot : AttackSlot.values()) {
                            buf.writeUtf(loadout.getMotion(slot));
                        }
                    } else {
                        buf.writeBoolean(false);
                    }
                }
                // デフォルトモーション設定
                for (AttackSlot slot : AttackSlot.values()) {
                    buf.writeUtf(skillData.getMotion(slot));
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(OpenSkillScreenPacket.class,
            OpenSkillScreenPacket::encode,
            OpenSkillScreenPacket::new,
            OpenSkillScreenPacket::handle);
    }
}
