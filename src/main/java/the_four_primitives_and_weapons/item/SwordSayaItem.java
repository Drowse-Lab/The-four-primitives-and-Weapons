package the_four_primitives_and_weapons.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;

import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import the_four_primitives_and_weapons.util.SayaRegistry;

import java.util.List;

public class SwordSayaItem extends Item implements ICurioItem {

	public SwordSayaItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		// 抜刀は R キーのみで行う (鞘の右クリックでは何もしない)
		return InteractionResultHolder.pass(player.getItemInHand(hand));
	}

	@Override
	public void appendHoverText(ItemStack stack, Level world, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
		super.appendHoverText(stack, world, list, flag);

		if (stack.hasTag() && stack.getTag().contains("StoredSword")) {
			ItemStack storedSword = ItemStack.of(stack.getTag().getCompound("StoredSword"));
			list.add(Component.literal("§7納刀中: §f" + storedSword.getHoverName().getString()));
		} else {
			list.add(Component.literal("§7空の鞘"));
			list.add(Component.literal("§8Rキーで納刀"));
		}
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.NONE;
	}

	@Override
	public int getUseDuration(ItemStack itemstack) {
		return 0;
	}

	public static boolean canSheathe(ItemStack swordStack) {
		return SayaRegistry.isRegistered(SayaRegistry.SayaType.SWORD, swordStack);
	}

	public static void sheatheSword(Player player, ItemStack swordStack, ItemStack sheathStack,
									InteractionHand swordHand, InteractionHand sheathHand) {
		CompoundTag sheathTag = sheathStack.getOrCreateTag();

		if (!sheathTag.contains("StoredSword")) {
			CompoundTag swordData = swordStack.save(new CompoundTag());
			sheathTag.put("StoredSword", swordData);

			int modelData = getSwordModelData(swordStack);
			sheathTag.putInt("CustomModelData", modelData);

			sheathStack.setTag(sheathTag);

			player.setItemInHand(swordHand, ItemStack.EMPTY);
			player.setItemInHand(sheathHand, sheathStack);

			player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.8F);
		} else {
			player.displayClientMessage(Component.literal("§c鞘には既に刀が入っています"), true);
		}
	}

	@Override
	public boolean canEquip(SlotContext slotContext, ItemStack stack) {
		String id = slotContext.identifier();
		return id.equals("belt") || id.equals("back");
	}

	@Override
	public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
		return false;
	}

	/**
	 * 剣ごとのモデルデータを返す。
	 * data/&lt;namespace&gt;/maw_saya/*.json の "sword" セクションから読み込まれる。
	 */
	public static int getSwordModelData(ItemStack sword) {
		return SayaRegistry.getModelData(SayaRegistry.SayaType.SWORD, sword);
	}
}
