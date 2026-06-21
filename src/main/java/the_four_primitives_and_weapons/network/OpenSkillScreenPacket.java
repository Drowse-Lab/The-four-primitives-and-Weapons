package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.skill.PlayerSkillData;
import the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot;
import the_four_primitives_and_weapons.skill.PlayerSkillData.WeaponLoadout;
import the_four_primitives_and_weapons.world.inventory.SkillSelectionMenu;
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
                // 武器タイプ別モーション設定 (multiplayer 同期: クライアント capability は空のため、
                //   この extraData を経由して UI 表示に必要なデータを渡す必要がある)
                // フォーマット: typeId 数 (int) → 各 typeId について [typeId(utf), AttackSlot 数(int),
                //   各 slot について slotId(utf) + motionId(utf)]
                java.util.Map<String, java.util.Map<AttackSlot, String>> allTypeMotions =
                        skillData.getAllTypeMotions();
                buf.writeInt(allTypeMotions.size());
                for (java.util.Map.Entry<String, java.util.Map<AttackSlot, String>> e : allTypeMotions.entrySet()) {
                    buf.writeUtf(e.getKey());
                    java.util.Map<AttackSlot, String> slotMap = e.getValue();
                    buf.writeInt(slotMap.size());
                    for (java.util.Map.Entry<AttackSlot, String> s : slotMap.entrySet()) {
                        buf.writeUtf(s.getKey().getId());
                        buf.writeUtf(s.getValue());
                    }
                }
                // 武器適正 (proficiency) も同期 — サーバーが保存している現在値を UI に反映するため
                buf.writeUtf(skillData.getWeaponProficiency().getId());
            });
        });
        ctx.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(OpenSkillScreenPacket.class,
            OpenSkillScreenPacket::encode,
            OpenSkillScreenPacket::new,
            OpenSkillScreenPacket::handle);
    }
}
