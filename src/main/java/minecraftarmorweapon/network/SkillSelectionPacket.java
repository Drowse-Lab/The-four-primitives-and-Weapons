package minecraftarmorweapon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import minecraftarmorweapon.skill.PlayerSkillData;

import java.util.function.Supplier;

public class SkillSelectionPacket {
    private final String weaponType;
    private final String skillType;
    private final String itemId;
    private final Boolean uniqueSkillEnabled;
    
    public SkillSelectionPacket(PlayerSkillData.WeaponType weaponType, 
                                PlayerSkillData.SkillType skillType,
                                String itemId, Boolean uniqueSkillEnabled) {
        this.weaponType = weaponType != null ? weaponType.getId() : null;
        this.skillType = skillType != null ? skillType.getId() : null;
        this.itemId = itemId;
        this.uniqueSkillEnabled = uniqueSkillEnabled;
    }
    
    public SkillSelectionPacket(FriendlyByteBuf buf) {
        this.weaponType = buf.readBoolean() ? buf.readUtf() : null;
        this.skillType = buf.readBoolean() ? buf.readUtf() : null;
        this.itemId = buf.readBoolean() ? buf.readUtf() : null;
        this.uniqueSkillEnabled = buf.readBoolean() ? buf.readBoolean() : null;
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(weaponType != null);
        if (weaponType != null) buf.writeUtf(weaponType);
        
        buf.writeBoolean(skillType != null);
        if (skillType != null) buf.writeUtf(skillType);
        
        buf.writeBoolean(itemId != null);
        if (itemId != null) buf.writeUtf(itemId);
        
        buf.writeBoolean(uniqueSkillEnabled != null);
        if (uniqueSkillEnabled != null) buf.writeBoolean(uniqueSkillEnabled);
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(PlayerSkillData.SKILL_CAPABILITY).ifPresent(skillData -> {
                    // 武器タイプの更新
                    if (weaponType != null) {
                        for (PlayerSkillData.WeaponType type : PlayerSkillData.WeaponType.values()) {
                            if (type.getId().equals(weaponType)) {
                                skillData.setSelectedWeaponType(type);
                                break;
                            }
                        }
                    }
                    
                    // 固有スキルのトグル
                    if (itemId != null && uniqueSkillEnabled != null) {
                        skillData.setUniqueSkillEnabled(itemId, uniqueSkillEnabled);
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}