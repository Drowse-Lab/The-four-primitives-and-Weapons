package the_four_primitives_and_weapons;

import the_four_primitives_and_weapons.damage.ElementDamageKind;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.damage.ElementalParticles;
import org.joml.Vector3f;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 属性ダメージのツールチップ表示イベント
 * エンチャントの下、攻撃力表示の上に表示される
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons", value = Dist.CLIENT)
public class ElementalTooltipEvent {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // NBTタグから属性情報を取得
        if (ElementalDamageUtils.hasElement(stack)) {
            ElementType type = ElementalDamageUtils.getEffectiveElementType(stack);
            int level = ElementalDamageUtils.getEffectiveElementLevel(stack);

            // 属性表示コンポーネントを作成
            String translationKey = getElementTranslationKey(type);
            String romanLevel = toRoman(level);
            String colorHex = getElementColorHex(type);

            Component elementText = Component.translatable(translationKey).append(Component.literal(" " + romanLevel))
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor(colorHex)).withItalic(false));

            // 属性ダメージの与え方 ( 物理 / 魔法 / 蓄積 ) を同じ行に添える
            ElementDamageKind kind = ElementalDamageUtils.getElementKind(stack);
            Component line = Component.empty()
                .append(elementText)
                .append(Component.literal(" ").append(Component.translatable(kind.getTranslationKey()))
                    .setStyle(Style.EMPTY.withColor(getKindColor(kind)).withItalic(false)));

            // 挿入位置を決定：エンチャントの後、攻撃力などの属性の前
            int insertIndex = findInsertPosition(event.getToolTip());
            event.getToolTip().add(insertIndex, line);
        }
    }

    /**
     * 属性ダメージの与え方ごとの表示色。
     * 属性そのものの色と混ざらないよう、彩度を落とした固定色にする。
     */
    private static ChatFormatting getKindColor(ElementDamageKind kind) {
        switch (kind) {
            case MAGIC:   return ChatFormatting.LIGHT_PURPLE;  // 防具貫通
            case BUILDUP: return ChatFormatting.DARK_GREEN;    // 持続
            default:      return ChatFormatting.GRAY;          // 物理 (既定)
        }
    }

    /**
     * ツールチップの適切な挿入位置を見つける
     * エンチャントの後、攻撃力などの属性修飾子の前に挿入
     */
    private static int findInsertPosition(List<Component> tooltip) {
        if (tooltip.size() <= 1) {
            return tooltip.size(); // アイテム名の後
        }

        // 攻撃力などの属性修飾子を探す
        for (int i = 1; i < tooltip.size(); i++) {
            String line = tooltip.get(i).getString();

            // 空行をチェック（通常、エンチャントと属性の間）
            if (line.trim().isEmpty()) {
                return i;
            }

            // 属性修飾子のヘッダーを検出
            if (line.contains("手に持ったとき") || line.contains("When in") ||
                line.contains("装備中") || line.contains("When on")) {
                return i;
            }
        }

        // 見つからない場合は最後に追加
        return tooltip.size();
    }

    /**
     * 属性名を取得
     */
    private static String getElementTranslationKey(ElementType type) {
        switch (type) {
            case ICE:       return "tooltip.the_four_primitives_and_weapons.element.ice";
            case FIRE:      return "tooltip.the_four_primitives_and_weapons.element.fire";
            case WATER:     return "tooltip.the_four_primitives_and_weapons.element.water";
            case WIND:      return "tooltip.the_four_primitives_and_weapons.element.wind";
            case THUNDER:   return "tooltip.the_four_primitives_and_weapons.element.thunder";
            case DARK:      return "tooltip.the_four_primitives_and_weapons.element.dark";
            case ELECTRIC:  return "tooltip.the_four_primitives_and_weapons.element.electric";
            case CORROSION: return "tooltip.the_four_primitives_and_weapons.element.corrosion";
            case HOLY:      return "tooltip.the_four_primitives_and_weapons.element.holy";
            case ERASURE:     return "tooltip.the_four_primitives_and_weapons.element.erasure";
            case MIASMA:    return "tooltip.the_four_primitives_and_weapons.element.miasma";
            case BLOOD:     return "tooltip.the_four_primitives_and_weapons.element.blood";
            case SOUL:      return "tooltip.the_four_primitives_and_weapons.element.soul";
            case SOUL_FIRE: return "tooltip.the_four_primitives_and_weapons.element.soul_fire";
            default: return "";
        }
    }

    /** ツールチップで読める最低限の明るさ ( 闇や消滅の粒子色は暗すぎるため持ち上げる )。 */
    private static final float MIN_TOOLTIP_BRIGHTNESS = 0.75f;

    /**
     * 属性の色（16進数）を取得。
     * 攻撃/ヒット時のパーティクル色 ({@link ElementalParticles#colorOf}) を唯一の基準にして、
     * 表示と粒子で属性の色がずれないようにする。 暗い属性色だけ読める明るさまで持ち上げる。
     */
    private static String getElementColorHex(ElementType type) {
        Vector3f color = ElementalParticles.colorOf(type);
        if (color == null) return "#AAAAAA";   // NONE

        float max = Math.max(color.x(), Math.max(color.y(), color.z()));
        float scale = max < MIN_TOOLTIP_BRIGHTNESS && max > 0.0f
                ? MIN_TOOLTIP_BRIGHTNESS / max
                : 1.0f;

        return String.format("#%02X%02X%02X",
                toByte(color.x() * scale), toByte(color.y() * scale), toByte(color.z() * scale));
    }

    private static int toByte(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
    }

    /**
     * 数字をローマ数字に変換
     */
    public static String toRoman(int num) {
        if (num <= 0) return "";
        if (num == 1) return "I";
        if (num == 2) return "II";
        if (num == 3) return "III";
        if (num == 4) return "IV";
        if (num == 5) return "V";
        if (num == 6) return "VI";
        if (num == 7) return "VII";
        if (num == 8) return "VIII";
        if (num == 9) return "IX";
        if (num == 10) return "X";
        if (num == 11) return "XI";
        if (num == 12) return "XII";
        if (num == 13) return "XIII";
        if (num == 14) return "XIV";
        if (num == 15) return "XV";
        if (num == 16) return "XVI";
        if (num == 343014) return "零";
        return String.valueOf(num);
    }
}
