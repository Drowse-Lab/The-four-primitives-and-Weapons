package minecraftarmorweapon.mixin;

import minecraftarmorweapon.damage.ElementType;
import minecraftarmorweapon.damage.ElementalDamageUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * ItemStackのツールチップに属性情報を追加するMixin
 * すべてのアイテムに適用される
 */
@Mixin(ItemStack.class)
public class ItemStackTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void addElementalTooltip(Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        List<Component> tooltip = cir.getReturnValue();

        // NBTタグから属性情報を取得
        if (ElementalDamageUtils.hasElement(stack)) {
            ElementType type = ElementalDamageUtils.getElementType(stack);
            int level = ElementalDamageUtils.getElementLevel(stack);

            // 重複チェック：既に属性情報が追加されていないか確認
            String elementName = getElementName(type);
            boolean alreadyAdded = false;
            for (Component component : tooltip) {
                if (component.getString().contains(elementName)) {
                    alreadyAdded = true;
                    break;
                }
            }

            if (!alreadyAdded) {
                // 属性表示を作成（エンチャント風）
                ChatFormatting color = getElementColor(type);
                String romanLevel = toRoman(level);
                Component elementText = Component.literal(getElementName(type) + " " + romanLevel)
                    .withStyle(color);

                // 挿入位置を決定：攻撃力などの属性修飾子の直前（エンチャントの直後）
                int insertIndex = findInsertPosition(tooltip);

                if (insertIndex <= tooltip.size()) {
                    tooltip.add(insertIndex, elementText);
                }
            }
        }
    }

    /**
     * ツールチップの適切な挿入位置を見つける
     * エンチャントの後、攻撃力などの属性修飾子の前に挿入
     */
    private int findInsertPosition(List<Component> tooltip) {
        if (tooltip.size() <= 1) {
            return 1; // アイテム名の後
        }

        int lastEnchantmentIndex = 0;
        int firstAttributeIndex = -1;

        for (int i = 1; i < tooltip.size(); i++) {
            String line = tooltip.get(i).getString();

            // 空行をチェック（エンチャントと属性の区切り）
            if (line.trim().isEmpty()) {
                if (lastEnchantmentIndex > 0 && firstAttributeIndex == -1) {
                    return i; // 空行の位置に挿入
                }
            }

            // 属性修飾子を検出（グレーで始まる、または"When"で始まる）
            // 「手に持ったとき:」「When in Main Hand:」などのパターン
            if (line.contains("手に持ったとき") || line.contains("When in") ||
                line.contains("攻撃ダメージ") || line.contains("Attack Damage") ||
                line.contains("攻撃速度") || line.contains("Attack Speed")) {
                if (firstAttributeIndex == -1) {
                    firstAttributeIndex = i;
                }
            }

            // エンチャントを検出（色付きテキストで、"When"などではない）
            // エンチャントは通常、特定の色（AQUA, RED, GOLDなど）で表示される
            if (!line.contains("手に持ったとき") && !line.contains("When in") &&
                !line.trim().isEmpty() && i > lastEnchantmentIndex) {
                // 色コードをチェック（§で始まる）
                if (line.contains("§") || hasEnchantmentStyle(tooltip.get(i))) {
                    lastEnchantmentIndex = i;
                }
            }
        }

        // 属性修飾子が見つかった場合、その直前に挿入
        if (firstAttributeIndex != -1) {
            return firstAttributeIndex;
        }

        // エンチャントが見つかった場合、その直後に挿入
        if (lastEnchantmentIndex > 0) {
            return lastEnchantmentIndex + 1;
        }

        // どちらも見つからない場合、アイテム名の直後に挿入
        return 1;
    }

    /**
     * コンポーネントがエンチャントスタイルを持っているかチェック
     */
    private boolean hasEnchantmentStyle(Component component) {
        // エンチャントは通常、GRAY以外の色を持つ
        String str = component.getString();
        return !str.isEmpty() && !str.contains("手に持ったとき") && !str.contains("When");
    }

    /**
     * 属性名を取得
     */
    private String getElementName(ElementType type) {
        switch (type) {
            case ICE: return "氷属性";
            case ELECTRIC: return "電気属性";
            case CORROSION: return "侵食属性";
            case HOLY: return "聖属性";
            default: return "";
        }
    }

    /**
     * 属性の色を取得
     */
    private ChatFormatting getElementColor(ElementType type) {
        switch (type) {
            case ICE: return ChatFormatting.AQUA;
            case ELECTRIC: return ChatFormatting.YELLOW;
            case CORROSION: return ChatFormatting.DARK_PURPLE;
            case HOLY: return ChatFormatting.GOLD;
            default: return ChatFormatting.GRAY;
        }
    }

    /**
     * 数字をローマ数字に変換
     */
    private String toRoman(int num) {
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
        return String.valueOf(num);
    }
}
