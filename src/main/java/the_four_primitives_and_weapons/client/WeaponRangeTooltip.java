package the_four_primitives_and_weapons.client;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.skill.WeaponStatsRegistry;
import the_four_primitives_and_weapons.init.MawExtraAttributes;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 「When in Main Hand:」の 攻撃力/攻撃速度 と同じ緑スタイルで「攻撃範囲」を表示する。
 * 併せて、 自動で出る Entity Reach / Block Reach の属性行を消して重複を防ぐ。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, value = Dist.CLIENT)
public class WeaponRangeTooltip {

    private static final String ATTACK_RANGE_ATTRIBUTE =
            TheFourPrimitivesAndWeaponsMod.MODID + ":entity_reach";

    /** 素の近接攻撃リーチ ( 表示上の基礎値 )。 */
    private static final double BASE_REACH = 3.0;

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        try {
            ItemStack stack = event.getItemStack();
            if (stack.isEmpty()) return;

            // /give の AttributeModifiers や /attackrange で部位別のリーチが設定されている場合は、
            // バニラが正しい「When in ...」欄へ置いた行をその場で攻撃範囲表記へ変える。
            // この経路は MOD 外のアイテムにも使える。
            boolean hasSlottedRange = stack.hasTag()
                    && stack.getTag().contains("AttributeModifiers", Tag.TAG_LIST)
                    && stack.getTag().getList("AttributeModifiers", Tag.TAG_COMPOUND).stream()
                    .anyMatch(tag -> isAttackRangeAttribute(((net.minecraft.nbt.CompoundTag) tag)
                            .getString("AttributeName")));

            // weapon_stats に登録されたアドオン武器も対象にする。
            boolean hasWeaponStats = WeaponStatsRegistry.getStats(stack) != null;
            if (!hasWeaponStats && !hasSlottedRange) return;

            List<Component> tip = event.getToolTip();

            // 自動追加された Reach 系の行を除去 ( 重複防止 )。
            String entityReachName = I18n.get(ForgeMod.ENTITY_REACH.get().getDescriptionId());
            String customReachName = I18n.get(MawExtraAttributes.ENTITY_REACH.get().getDescriptionId());
            String blockReachName = I18n.get(ForgeMod.BLOCK_REACH.get().getDescriptionId());
            if (hasSlottedRange) {
                EquipmentSlot currentSlot = null;
                for (int i = 0; i < tip.size(); i++) {
                    Component old = tip.get(i);
                    String text = old.getString();
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        if (text.equals(I18n.get("item.modifiers." + slot.getName()))) {
                            currentSlot = slot;
                            break;
                        }
                    }
                    if (text.contains(entityReachName) || text.contains(customReachName)) {
                        double range = slottedRange(stack, currentSlot);
                        Component line = Component.translatable(
                                "attribute.modifier.equals.0",
                                ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(range),
                                Component.translatable("attribute.name.the_four_primitives_and_weapons.attack_range"))
                                .withStyle(ChatFormatting.DARK_GREEN);
                        tip.set(i, indentAttributeLine(line));
                    }
                }
            }
            tip.removeIf(c -> {
                String s = c.getString();
                return (!hasSlottedRange && s.contains(entityReachName)) || s.contains(blockReachName);
            });

            int idx = normalizeAttackSpeedTooltip(stack, tip);

            // 部位別設定は既に各「When in ...」欄へ表示済み。
            if (hasSlottedRange) return;

            // 攻撃速度の行を探す ( = 近接武器のとき )。 無ければ範囲は表示しない。
            if (idx < 0) return;

            double range = BASE_REACH + WeaponStatsRegistry.attackRangeBonus(stack);
            Component line = Component.translatable(
                    "attribute.modifier.equals.0",
                    ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(range),
                    Component.translatable("attribute.name.the_four_primitives_and_weapons.attack_range"))
                    .withStyle(ChatFormatting.DARK_GREEN);
            tip.add(idx + 1, indentAttributeLine(line));
        } catch (Throwable ignored) {
            // no-op: ツールチップ描画は失敗しても無視 ( クラッシュ防止 )
        }
    }

    /** バニラ武器の攻撃力・攻撃速度行と同じ左位置へ揃える。 */
    private static Component indentAttributeLine(Component line) {
        return Component.literal("  ").append(line);
    }

    /** JSON由来の負補正表示を、計算後の攻撃速度を示す緑表示へ揃える。 */
    private static int normalizeAttackSpeedTooltip(ItemStack stack, List<Component> tip) {
        String attackSpeedName = I18n.get(Attributes.ATTACK_SPEED.getDescriptionId());
        int index = -1;
        for (int i = 0; i < tip.size(); i++) {
            if (tip.get(i).getString().contains(attackSpeedName)) {
                index = i;
                break;
            }
        }
        if (index < 0) return -1;

        boolean hasNbtAttackSpeed = stack.hasTag()
                && stack.getTag().contains("AttributeModifiers", Tag.TAG_LIST)
                && stack.getTag().getList("AttributeModifiers", Tag.TAG_COMPOUND).stream()
                .anyMatch(tag -> {
                    String attribute = ((CompoundTag) tag).getString("AttributeName");
                    return "minecraft:generic.attack_speed".equals(attribute)
                            || "generic.attack_speed".equals(attribute);
                });
        WeaponStatsRegistry.WeaponStats stats = WeaponStatsRegistry.getStats(stack);
        if (!hasNbtAttackSpeed && stats != null && !Float.isNaN(stats.attackSpeed)) {
            double speed = 4.0 + stats.attackSpeed;
            tip.set(index, indentAttributeLine(Component.translatable(
                    "attribute.modifier.equals.0",
                    ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(speed),
                    Component.translatable(Attributes.ATTACK_SPEED.getDescriptionId()))
                    .withStyle(ChatFormatting.DARK_GREEN)));
        }
        return index;
    }

    /** 指定部位の reach modifier をバニラの attribute 計算順で基礎値3へ適用する。 */
    private static double slottedRange(ItemStack stack, EquipmentSlot slot) {
        double addition = 0.0;
        double multiplyBase = 0.0;
        double multiplyTotal = 1.0;
        var modifiers = stack.getTag().getList("AttributeModifiers", Tag.TAG_COMPOUND);
        for (int i = 0; i < modifiers.size(); i++) {
            CompoundTag modifier = modifiers.getCompound(i);
            if (!isAttackRangeAttribute(modifier.getString("AttributeName"))) continue;
            String configuredSlot = modifier.getString("Slot");
            if (!configuredSlot.isEmpty() && (slot == null || !configuredSlot.equals(slot.getName()))) continue;
            double amount = modifier.getDouble("Amount");
            switch (modifier.getInt("Operation")) {
                case 0 -> addition += amount;
                case 1 -> multiplyBase += amount;
                case 2 -> multiplyTotal *= 1.0 + amount;
                default -> { }
            }
        }
        return (BASE_REACH + addition) * (1.0 + multiplyBase) * multiplyTotal;
    }

    /** 新しいMOD属性IDと、既存アイテムに残るForge旧IDの両方を読む。 */
    private static boolean isAttackRangeAttribute(String attributeName) {
        return ATTACK_RANGE_ATTRIBUTE.equals(attributeName)
                || "forge:entity_reach".equals(attributeName);
    }
}
