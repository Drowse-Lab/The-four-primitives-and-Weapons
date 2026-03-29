package minecraftarmorweapon;

import minecraftarmorweapon.damage.ElementType;
import minecraftarmorweapon.damage.ElementalDamageUtils;
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
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon", value = Dist.CLIENT)
public class ElementalTooltipEvent {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // NBTタグから属性情報を取得
        if (ElementalDamageUtils.hasElement(stack)) {
            ElementType type = ElementalDamageUtils.getElementType(stack);
            int level = ElementalDamageUtils.getElementLevel(stack);

            // 属性表示コンポーネントを作成
            String translationKey = getElementTranslationKey(type);
            String romanLevel = toRoman(level);
            String colorHex = getElementColorHex(type);

            Component elementText = Component.translatable(translationKey).append(Component.literal(" " + romanLevel))
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor(colorHex)).withItalic(false));

            // 挿入位置を決定：エンチャントの後、攻撃力などの属性の前
            int insertIndex = findInsertPosition(event.getToolTip());
            event.getToolTip().add(insertIndex, elementText);
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
            case ICE: return "tooltip.minecraft_armor_weapon.element.ice";
            case ELECTRIC: return "tooltip.minecraft_armor_weapon.element.electric";
            case CORROSION: return "tooltip.minecraft_armor_weapon.element.corrosion";
            case HOLY: return "tooltip.minecraft_armor_weapon.element.holy";
            default: return "";
        }
    }

    /**
     * 属性の色（16進数）を取得
     */
    private static String getElementColorHex(ElementType type) {
        switch (type) {
            case ICE: return "#55FFFF";        // 水色
            case ELECTRIC: return "#FFFF55";   // 黄色
            case CORROSION: return "#8B00FF";  // ピンク
            case HOLY: return "#FFAA00";       // 金色
            default: return "#AAAAAA";         // グレー
        }
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
