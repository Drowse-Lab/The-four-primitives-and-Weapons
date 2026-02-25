package minecraftarmorweapon.item.rarity;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.UUID;

/**
 * 武器レアリティ（5段階）
 */
public enum WeaponRarity {

    COMMON(0, "Common", "\u00A7f", ChatFormatting.WHITE, 0),
    UNCOMMON(1, "Uncommon", "\u00A7a", ChatFormatting.GREEN, 1),
    RARE(2, "Rare", "\u00A79", ChatFormatting.BLUE, 2),
    EPIC(3, "Epic", "\u00A75", ChatFormatting.DARK_PURPLE, 4),
    LEGENDARY(4, "Legendary", "\u00A76", ChatFormatting.GOLD, 7);

    private static final String NBT_KEY = "WeaponRarity";
    private static final UUID RARITY_DAMAGE_UUID = UUID.fromString("a3e6f5c8-7b2d-4e1a-9f0c-8d5b3a7e2c1f");

    private final int id;
    private final String displayName;
    private final String colorCode;
    private final ChatFormatting chatColor;
    private final double attackBonus;

    WeaponRarity(int id, String displayName, String colorCode, ChatFormatting chatColor, double attackBonus) {
        this.id = id;
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.chatColor = chatColor;
        this.attackBonus = attackBonus;
    }

    public int getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getColorCode() { return colorCode; }
    public ChatFormatting getChatColor() { return chatColor; }
    public double getAttackBonus() { return attackBonus; }

    public String getColoredName() {
        return colorCode + displayName;
    }

    public static WeaponRarity fromId(int id) {
        for (WeaponRarity r : values()) {
            if (r.id == id) return r;
        }
        return COMMON;
    }

    /**
     * ItemStackからレアリティを読み取る（なければnull）
     */
    public static WeaponRarity getFromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_KEY)) return null;
        return fromId(tag.getInt(NBT_KEY));
    }

    /**
     * ItemStackにレアリティを書き込む
     */
    public static void setToStack(ItemStack stack, WeaponRarity rarity) {
        if (stack.isEmpty()) return;
        stack.getOrCreateTag().putInt(NBT_KEY, rarity.id);
    }

    /**
     * レアリティ付きか判定
     */
    public static boolean hasRarity(ItemStack stack) {
        return getFromStack(stack) != null;
    }

    /**
     * レアリティに応じた攻撃力ボーナスのAttributeModifierを返す
     */
    public AttributeModifier createDamageModifier() {
        return new AttributeModifier(
                RARITY_DAMAGE_UUID,
                "Weapon Rarity Bonus",
                attackBonus,
                AttributeModifier.Operation.ADDITION
        );
    }

    public static String getNbtKey() {
        return NBT_KEY;
    }

    public static UUID getRarityDamageUuid() {
        return RARITY_DAMAGE_UUID;
    }
}
