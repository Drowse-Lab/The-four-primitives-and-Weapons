package the_four_primitives_and_weapons.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

/**
 * 鉄の籠手 — Curios hands スロットに装備する金属製の手袋 ( 右クリックで装備可 )。
 *
 * <p>装備中は防御力 +1 ( Curios の属性修飾子。 ツールチップにも自動表示される )。
 * 金属製なので {@link GloveItem} と違い染色はできない。 描画は
 * {@code GloveCurioRenderer} が {@link #TINT} を乗算して金属色にする。</p>
 */
public class IronGauntletsItem extends Item implements ICurioItem {

	/** 描画時の乗算 tint ( 鉄色 )。 */
	public static final int TINT = 0xC8C8D0;

	public IronGauntletsItem() {
		super(new Item.Properties().stacksTo(1));
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
	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
		Multimap<Attribute, AttributeModifier> map = HashMultimap.create();
		map.put(Attributes.ARMOR, new AttributeModifier(uuid, "iron_gauntlets_armor", 1.0D, AttributeModifier.Operation.ADDITION));
		return map;
	}
}
