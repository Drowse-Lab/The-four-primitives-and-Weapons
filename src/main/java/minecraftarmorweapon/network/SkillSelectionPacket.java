package minecraftarmorweapon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import minecraftarmorweapon.skill.PlayerSkillData;
import minecraftarmorweapon.skill.PlayerSkillData.AttackSlot;
import minecraftarmorweapon.skill.PlayerSkillData.WeaponLoadout;
import minecraftarmorweapon.skill.SkillRegistry;
import minecraftarmorweapon.world.inventory.SkillSelectionMenu;
import minecraftarmorweapon.MinecraftArmorWeaponMod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SkillSelectionPacket {

    // slotId = loadoutIndex ("-1" for default), motionId = motionId, itemId = AttackSlot id
    private final String slotId;
    private final String motionId;
    private final String itemId;

    public static SkillSelectionPacket selectLoadoutMotion(int loadoutIndex, AttackSlot attackSlot, String motionId) {
        return new SkillSelectionPacket(String.valueOf(loadoutIndex), motionId, attackSlot.getId());
    }

    public static SkillSelectionPacket setProficiency(String proficiencyId) {
        return new SkillSelectionPacket("proficiency", proficiencyId, null);
    }

    /**
     * 武器タイプ別モーション設定
     * slotId="type:<typeId>", motionId=motionId, itemId=attackSlot.getId()
     */
    public static SkillSelectionPacket setTypeMotion(String typeId, AttackSlot attackSlot, String motionId) {
        return new SkillSelectionPacket("type:" + typeId, motionId, attackSlot.getId());
    }

    private SkillSelectionPacket(String slotId, String motionId, String itemId) {
        this.slotId = slotId;
        this.motionId = motionId;
        this.itemId = itemId;
    }

    public SkillSelectionPacket(FriendlyByteBuf buf) {
        this.slotId = buf.readBoolean() ? buf.readUtf() : null;
        this.motionId = buf.readBoolean() ? buf.readUtf() : null;
        this.itemId = buf.readBoolean() ? buf.readUtf() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(slotId != null);
        if (slotId != null) buf.writeUtf(slotId);

        buf.writeBoolean(motionId != null);
        if (motionId != null) buf.writeUtf(motionId);

        buf.writeBoolean(itemId != null);
        if (itemId != null) buf.writeUtf(itemId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(sd -> {
                if ("proficiency".equals(slotId)) {
                    handleProficiency(sd);
                } else if (slotId != null && slotId.startsWith("type:")) {
                    handleTypeMotion(sd);
                } else {
                    handleSelectLoadoutMotion(sd, player);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleTypeMotion(PlayerSkillData.SkillStorage skillData) {
        if (slotId == null || motionId == null || itemId == null) return;
        String typeId = slotId.substring(5); // "type:" を除去
        AttackSlot attackSlot = AttackSlot.fromId(itemId);
        if (attackSlot == null) return;
        skillData.setTypeMotion(typeId, attackSlot, motionId);
    }

    private void handleProficiency(PlayerSkillData.SkillStorage skillData) {
        if (motionId == null) return;
        skillData.setWeaponProficiency(PlayerSkillData.WeaponProficiency.fromId(motionId));
    }

    private void handleSelectLoadoutMotion(PlayerSkillData.SkillStorage skillData, ServerPlayer player) {
        if (slotId == null || motionId == null || itemId == null) return;

        AttackSlot attackSlot = AttackSlot.fromId(itemId);
        if (attackSlot == null) return;

        SkillRegistry.MotionInfo motion = SkillRegistry.getById(motionId);
        if (motion == null) return;
        if (!motion.getCompatibleSlots().contains(attackSlot)) return;

        int loadoutIndex;
        try {
            loadoutIndex = Integer.parseInt(slotId);
        } catch (NumberFormatException e) {
            return;
        }

        if (loadoutIndex == -1) {
            // デフォルト設定（UNIVERSAL のみ）
            if (motion.getCategory() != SkillRegistry.MotionCategory.UNIVERSAL) return;
            skillData.setMotion(attackSlot, motionId);
            // 手持ち武器のタイプにもtypeMotionを設定（JSONデフォルトを上書き）
            net.minecraft.world.item.ItemStack held = player.getMainHandItem();
            minecraftarmorweapon.skill.WeaponTypeRegistry.WeaponTypeData wt =
                    minecraftarmorweapon.skill.WeaponTypeRegistry.getTypeForItem(held);
            if (wt != null) {
                skillData.setTypeMotion(wt.getId(), attackSlot, motionId);
            }
        } else {
            // 特定スロットの設定
            WeaponLoadout loadout = skillData.getLoadoutAt(loadoutIndex);

            // ロードアウトが未作成の場合、開いているメニューから武器を取得して作成
            if (loadout == null) {
                if (player.containerMenu instanceof SkillSelectionMenu) {
                    SkillSelectionMenu skillMenu = (SkillSelectionMenu) player.containerMenu;
                    net.minecraft.world.item.ItemStack weapon = skillMenu.getSlot(loadoutIndex).getItem();
                    if (!weapon.isEmpty()) {
                        skillData.setLoadoutAt(loadoutIndex, weapon);
                        loadout = skillData.getLoadoutAt(loadoutIndex);
                    }
                }
                if (loadout == null) return;
            }

            // SPECIALの場合、その武器クラスに対応しているか確認
            if (motion.getCategory() == SkillRegistry.MotionCategory.SPECIAL) {
                String weaponClass = loadout.getWeaponClass();
                if (!weaponClass.equals(motion.getRequiredWeaponClass())) return;
            }

            skillData.setLoadoutMotion(loadoutIndex, attackSlot, motionId);

            // 武器のNBTにも技設定を保存（アイテムと一緒に永続化）
            if (player.containerMenu instanceof SkillSelectionMenu) {
                SkillSelectionMenu skillMenu = (SkillSelectionMenu) player.containerMenu;
                net.minecraft.world.item.ItemStack weaponInSlot = skillMenu.getSlot(loadoutIndex).getItem();
                if (!weaponInSlot.isEmpty()) {
                    minecraftarmorweapon.skill.WeaponSkillNBT.setMotion(weaponInSlot, attackSlot, motionId);
                }
            }
        }
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(SkillSelectionPacket.class,
            SkillSelectionPacket::encode,
            SkillSelectionPacket::new,
            SkillSelectionPacket::handle);
    }
}
