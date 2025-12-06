package minecraftarmorweapon.damage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 属性ダメージ用のユーティリティクラス
 * NBTタグから属性情報を読み書きする
 */
public class ElementalDamageUtils {

    // NBTタグのキー
    private static final String ELEMENT_TYPE_KEY = "ElementType";
    private static final String ELEMENT_LEVEL_KEY = "ElementLevel";

    /**
     * アイテムに属性を設定
     * @param stack アイテムスタック
     * @param elementType 属性タイプ
     * @param level 属性レベル
     */
    public static void setElement(ItemStack stack, ElementType elementType, int level) {
        if (stack.isEmpty()) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(ELEMENT_TYPE_KEY, elementType.getName());
        tag.putInt(ELEMENT_LEVEL_KEY, level);
    }

    /**
     * アイテムから属性タイプを取得
     * @param stack アイテムスタック
     * @return 属性タイプ（属性がない場合はNONE）
     */
    public static ElementType getElementType(ItemStack stack) {
        if (stack.isEmpty()) {
            return ElementType.NONE;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ELEMENT_TYPE_KEY)) {
            return ElementType.NONE;
        }

        String elementName = tag.getString(ELEMENT_TYPE_KEY);
        return ElementType.fromString(elementName);
    }

    /**
     * アイテムから属性レベルを取得
     * @param stack アイテムスタック
     * @return 属性レベル（属性がない場合は0）
     */
    public static int getElementLevel(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ELEMENT_LEVEL_KEY)) {
            return 0;
        }

        return tag.getInt(ELEMENT_LEVEL_KEY);
    }

    /**
     * アイテムに属性があるかチェック
     * @param stack アイテムスタック
     * @return 属性がある場合true
     */
    public static boolean hasElement(ItemStack stack) {
        return getElementType(stack) != ElementType.NONE;
    }

    /**
     * アイテムから属性を削除
     * @param stack アイテムスタック
     */
    public static void removeElement(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(ELEMENT_TYPE_KEY);
            tag.remove(ELEMENT_LEVEL_KEY);
        }
    }
}
