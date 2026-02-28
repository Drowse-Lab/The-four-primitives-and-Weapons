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

import java.util.List;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SkillSelectionPacket {

    public enum PacketType {
        SELECT_MOTION,     // モーション選択
        LOCK_WEAPON,       // 武器ロック（消費して特殊技解放）
        TOGGLE_UNIQUE_SKILL // 固有スキルのON/OFF
    }

    private final PacketType type;
    private final String slotId;     // for SELECT_MOTION
    private final String motionId;   // for SELECT_MOTION
    private final String itemId;     // for LOCK_WEAPON / TOGGLE_UNIQUE_SKILL
    private final Boolean enabled;   // for TOGGLE_UNIQUE_SKILL

    // モーション選択用
    public static SkillSelectionPacket selectMotion(AttackSlot slot, String motionId) {
        return new SkillSelectionPacket(PacketType.SELECT_MOTION, slot.getId(), motionId, null, null);
    }

    // 武器ロック用
    public static SkillSelectionPacket lockWeapon(String weaponClassName) {
        return new SkillSelectionPacket(PacketType.LOCK_WEAPON, null, null, weaponClassName, null);
    }

    // 固有スキルトグル用
    public static SkillSelectionPacket toggleUniqueSkill(String itemId, boolean enabled) {
        return new SkillSelectionPacket(PacketType.TOGGLE_UNIQUE_SKILL, null, null, itemId, enabled);
    }

    private SkillSelectionPacket(PacketType type, String slotId, String motionId, String itemId, Boolean enabled) {
        this.type = type;
        this.slotId = slotId;
        this.motionId = motionId;
        this.itemId = itemId;
        this.enabled = enabled;
    }

    public SkillSelectionPacket(FriendlyByteBuf buf) {
        this.type = PacketType.values()[buf.readVarInt()];
        this.slotId = buf.readBoolean() ? buf.readUtf() : null;
        this.motionId = buf.readBoolean() ? buf.readUtf() : null;
        this.itemId = buf.readBoolean() ? buf.readUtf() : null;
        this.enabled = buf.readBoolean() ? buf.readBoolean() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(type.ordinal());

        buf.writeBoolean(slotId != null);
        if (slotId != null) buf.writeUtf(slotId);

        buf.writeBoolean(motionId != null);
        if (motionId != null) buf.writeUtf(motionId);

        buf.writeBoolean(itemId != null);
        if (itemId != null) buf.writeUtf(itemId);

        buf.writeBoolean(enabled != null);
        if (enabled != null) buf.writeBoolean(enabled);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(skillData -> {
                switch (type) {
                    case SELECT_MOTION -> handleSelectMotion(skillData);
                    case LOCK_WEAPON -> handleLockWeapon(skillData, player);
                    case TOGGLE_UNIQUE_SKILL -> handleToggleUniqueSkill(skillData);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleSelectMotion(PlayerSkillData.SkillStorage skillData) {
        if (slotId == null || motionId == null) return;

        AttackSlot slot = AttackSlot.fromId(slotId);
        if (slot == null) return;

        // モーションが有効か確認
        SkillRegistry.MotionInfo motion = SkillRegistry.getById(motionId);
        if (motion == null) return;
        if (!motion.getCompatibleSlots().contains(slot)) return;

        // 特殊スキルの場合、解放済みか確認
        if (motion.getCategory() == SkillRegistry.MotionCategory.SPECIAL
            && !skillData.isSpecialUnlocked(motionId)) return;

        skillData.setMotion(slot, motionId);
    }

    private void handleLockWeapon(PlayerSkillData.SkillStorage skillData, ServerPlayer player) {
        if (itemId == null) return;

        // プレイヤーのメインハンドの武器を確認
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty()) return;

        String weaponClass = mainHand.getItem().getClass().getSimpleName();
        if (!weaponClass.equals(itemId)) return;

        // この武器で解放される特殊スキルを取得
        List<String> specialIds = SkillRegistry.getSpecialIdsForWeapon(weaponClass);
        if (specialIds.isEmpty()) return;

        // すでに全て解放済みなら何もしない
        boolean anyNew = false;
        for (String sid : specialIds) {
            if (!skillData.isSpecialUnlocked(sid)) {
                anyNew = true;
                break;
            }
        }
        if (!anyNew) return;

        // 武器を消費してロック
        skillData.lockWeapon(mainHand.copy());
        mainHand.shrink(1);

        // 特殊スキルを解放
        for (String sid : specialIds) {
            skillData.unlockSpecial(sid);
        }
    }

    private void handleToggleUniqueSkill(PlayerSkillData.SkillStorage skillData) {
        if (itemId == null || enabled == null) return;
        skillData.setUniqueSkillEnabled(itemId, enabled);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        MinecraftArmorWeaponMod.addNetworkMessage(SkillSelectionPacket.class,
            SkillSelectionPacket::encode,
            SkillSelectionPacket::new,
            SkillSelectionPacket::handle);
    }
}
