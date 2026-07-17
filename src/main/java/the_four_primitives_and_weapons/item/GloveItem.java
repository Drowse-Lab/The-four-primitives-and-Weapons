package the_four_primitives_and_weapons.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 手袋 — Curios「hands」スロットに装備するアクセサリ ( 右クリックで装備可 )。
 *
 * <p>もともと明治制服 ( 上着 ) に含まれていた白手袋を独立アイテム化したもの。
 * 装備時の描画は {@code client/renderer/GloveCurioRenderer}
 * ( {@code ModelMeijiUniform} の right_glove / left_glove パーツ ) が行う。
 * 革手袋 ( defaultColor = 革色 ) や 弓懸 ( {@link ArcherGloveItem} ) 等の
 * バリエーションは defaultColor / 継承で表現する。</p>
 *
 * <p>染色可能 ( {@link DyeableLeatherItem} ): クラフト染色・本MODの大釜染色
 * ( {@code CauldronDyeHandler} ) の両方に対応。 色はバニラの display.color ではなく
 * 独自タグ GloveColor に保存する ( display.color に入れるとバニラが「Dyed」と
 * 表示してしまうため )。 ツールチップは {@code DyeTooltipHandler} が表示する。</p>
 */
public class GloveItem extends Item implements DyeableLeatherItem, ICurioItem {

	private static final String COLOR_TAG = "GloveColor";

	/** 未染色時の色 ( 白手袋 = 白、 革手袋 = 革色 … )。 描画時に乗算 tint される。 */
	private final int defaultColor;

	public GloveItem() {
		this(0xFFFFFF);
	}

	public GloveItem(int defaultColor) {
		super(new Item.Properties().stacksTo(1));
		this.defaultColor = defaultColor;
	}

	@Override
	public boolean canEquip(SlotContext ctx, ItemStack stack) {
		return true;
	}

	@Override
	public boolean canEquipFromUse(SlotContext ctx, ItemStack stack) {
		return true;
	}

	@Override
	public boolean hasCustomColor(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains(COLOR_TAG, Tag.TAG_INT)) return true;
		return displayColor(stack) >= 0; // 皮装備方式 display.color でも可
	}

	/** 手袋の色。 未染色なら defaultColor。 */
	@Override
	public int getColor(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains(COLOR_TAG, Tag.TAG_INT)) return tag.getInt(COLOR_TAG);
		int d = displayColor(stack);
		return d >= 0 ? d : defaultColor;
	}

	/** /give …{display:{color:N}} で入れた色。 無ければ -1。 */
	private static int displayColor(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains("display", 10)) {
			CompoundTag d = tag.getCompound("display");
			if (d.contains("color", 99)) return d.getInt("color") & 0xFFFFFF;
		}
		return -1;
	}

	@Override
	public void setColor(ItemStack stack, int color) {
		stack.getOrCreateTag().putInt(COLOR_TAG, color);
	}

	@Override
	public void clearColor(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag != null) tag.remove(COLOR_TAG);
	}
	// 「〜スロットに装備可能」ツールチップは CuriosEquippableTooltipHandler で表示する
}
