package minecraftarmorweapon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import minecraftarmorweapon.skill.PlayerSkillData;
import minecraftarmorweapon.skill.PlayerSkillData.AttackSlot;
import minecraftarmorweapon.skill.SkillRegistry;
import minecraftarmorweapon.MinecraftArmorWeaponMod;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SkillSelectionPacket {

    public enum PacketType {
        SELECT_LOADOUT_MOTION,  // ロードアウト（または既定）の技選択
        ADD_WEAPON_LOADOUT,     // 主手の武器をロードアウトに追加
        REMOVE_WEAPON_LOADOUT   // ロードアウトから武器を取り出し
    }

    private final PacketType type;
    // SELECT_LOADOUT_MOTION: slotId = loadoutIndex or "-1" (default), motionId = motionId, itemId = AttackSlot id
    // REMOVE_WEAPON_LOADOUT: itemId = loadout index as string
    private final String slotId;
    private final String motionId;
    private final String itemId;

    // 技選択（loadoutIndex = -1 でデフォルト設定）
    public static SkillSelectionPacket selectLoadoutMotion(int loadoutIndex, AttackSlot attackSlot, String motionId) {
        return new SkillSelectionPacket(PacketType.SELECT_LOADOUT_MOTION,
            String.valueOf(loadoutIndex), motionId, attackSlot.getId());
    }

    // 武器追加
    public static SkillSelectionPacket addWeaponLoadout() {
        return new SkillSelectionPacket(PacketType.ADD_WEAPON_LOADOUT, null, null, null);
    }

    // 武器取り出し
    public static SkillSelectionPacket removeWeaponLoadout(int index) {
        return new SkillSelectionPacket(PacketType.REMOVE_WEAPON_LOADOUT, null, null, String.valueOf(index));
    }

    private SkillSelectionPacket(PacketType type, String slotId, String motionId, String itemId) {
        this.type = type;
        this.slotId = slotId;
        this.motionId = motionId;
        this.itemId = itemId;
    }

    public SkillSelectionPacket(FriendlyByteBuf buf) {
        this.type = PacketType.values()[buf.readVarInt()];
        this.slotId = buf.readBoolean() ? buf.readUtf() : null;
        this.motionId = buf.readBoolean() ? buf.readUtf() : null;
        this.itemId = buf.readBoolean() ? buf.readUtf() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(type.ordinal());

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

            player.getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(skillData -> {
                switch (type) {
                    case SELECT_LOADOUT_MOTION -> handleSelectLoadoutMotion(skillData);
                    case ADD_WEAPON_LOADOUT -> handleAddWeaponLoadout(skillData, player);
                    case REMOVE_WEAPON_LOADOUT -> handleRemoveWeaponLoadout(skillData, player);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleSelectLoadoutMotion(PlayerSkillData.SkillStorage skillData) {
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
        } else {
            // 特定ロードアウトの設定
            var loadouts = skillData.getWeaponLoadouts();
            if (loadoutIndex < 0 || loadoutIndex >= loadouts.size()) return;

            // SPECIALの場合、その武器クラスに対応しているか確認
            if (motion.getCategory() == SkillRegistry.MotionCategory.SPECIAL) {
                String weaponClass = loadouts.get(loadoutIndex).getWeaponClass();
                if (!weaponClass.equals(motion.getRequiredWeaponClass())) return;
            }

            skillData.setLoadoutMotion(loadoutIndex, attackSlot, motionId);
        }
    }

    private void handleAddWeaponLoadout(PlayerSkillData.SkillStorage skillData, ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) return;

        String weaponClass = mainHand.getItem().getClass().getSimpleName();

        // 同じ武器クラスが既に登録されていたら追加しない
        if (skillData.hasLoadoutForWeapon(weaponClass)) return;

        // 武器を1つ消費（スロットに保存）
        ItemStack stored = mainHand.copy();
        stored.setCount(1);
        mainHand.shrink(1);

        skillData.addWeaponLoadout(stored);
    }

    private void handleRemoveWeaponLoadout(PlayerSkillData.SkillStorage skillData, ServerPlayer player) {
        if (itemId == null) return;
        int index;
        try {
            index = Integer.parseInt(itemId);
        } catch (NumberFormatException e) {
            return;
        }

        ItemStack weapon = skillData.removeWeaponLoadout(index);
        if (!weapon.isEmpty()) {
            // プレイヤーのインベントリに返す（空きがなければドロップ）
            if (!player.getInventory().add(weapon)) {
                player.drop(weapon, false);
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
