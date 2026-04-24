package minecraftarmorweapon.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * バニラのアイテムスタック比較が {@code hasTag() ^ hasTag()} と生 {@code equals()} のみで、
 * 空タグ / デフォルト値のみの違いを "別物" と判定してしまう問題に対する共通正規化。
 *
 *   - {@link ItemStack#isSameItemSameTags} — インベントリ GUI 上での重ね合わせ判定
 *   - {@code ItemEntity.areMergable}       — 地面のドロップ同士の融合
 *   - {@code Inventory.canMergeItems}      — 拾った時に既存スタックへ合流できるか
 *
 *   これら 3 箇所すべてが同じ抜け方をするので、共通ヘルパーで統一的に Normalize して
 *   比較する。
 */
public final class StackNormalize {
    private StackNormalize() {}

    /**
     * 2 つのスタックを "意味的に同じ" とみなしてよいか判定。
     * {@code Item} の一致 + 正規化後 CompoundTag の equals。
     * 片方でも empty (空スタック) の場合は false — 呼び出し側のバニラ挙動に任せる。
     */
    public static boolean sameIgnoringEmptyTags(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.isEmpty() || b.isEmpty()) return false;
        if (!a.is(b.getItem())) return false;
        return Objects.equals(normalize(a.getTag()), normalize(b.getTag()));
    }

    /**
     * CompoundTag を "意味のある差分のみ残す" 形に正規化して返す。
     *   - null / 空 Compound → null
     *   - {@code RepairCost == 0} を除去 (金床未使用と同等)
     *   - 再帰的に空 sub-Compound を除去
     *   - 再帰後も空なら null
     */
    public static CompoundTag normalize(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return null;
        CompoundTag copy = tag.copy();

        if (copy.contains("RepairCost", Tag.TAG_INT) && copy.getInt("RepairCost") == 0) {
            copy.remove("RepairCost");
        }

        stripEmptySubCompounds(copy);

        return copy.isEmpty() ? null : copy;
    }

    private static void stripEmptySubCompounds(CompoundTag tag) {
        List<String> toRemove = new ArrayList<>();
        for (String key : tag.getAllKeys()) {
            Tag child = tag.get(key);
            if (child instanceof CompoundTag ct) {
                stripEmptySubCompounds(ct);
                if (ct.isEmpty()) toRemove.add(key);
            }
        }
        for (String k : toRemove) tag.remove(k);
    }
}
