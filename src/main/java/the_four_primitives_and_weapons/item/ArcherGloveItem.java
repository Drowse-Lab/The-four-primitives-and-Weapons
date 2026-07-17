package the_four_primitives_and_weapons.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 弓懸 ( ゆがけ ) — 弓を引くときに使う鹿革の手袋。
 *
 * <p>Curios hands スロットに装備中、 弓の引き絞りが速くなる
 * ( {@code events/ArcherGloveHandler} が {@code ArrowLooseEvent} のチャージを加速 )。
 * 未染色時は鹿革色。 染色は {@link GloveItem} と同様に可能。</p>
 */
public class ArcherGloveItem extends GloveItem {

	/** 鹿革色 ( 未染色時 )。 */
	public static final int DEER_LEATHER = 0xC8A165;

	/** 弓チャージの加速倍率 ( 1.5 = フルチャージまで約 2/3 の時間 )。 */
	public static final float CHARGE_MULTIPLIER = 1.5F;

	public ArcherGloveItem() {
		super(DEER_LEATHER);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.the_four_primitives_and_weapons.archer_glove_effect"));
		super.appendHoverText(stack, level, tooltip, flag);
	}
}
