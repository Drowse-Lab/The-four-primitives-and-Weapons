package minecraftarmorweapon.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraftforge.registries.ForgeRegistries;

import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SwordSayaItem extends Item implements ICurioItem {

	// 納刀対象の剣 → CustomModelData の対応表。後で増やすときはここに1行足すだけ。
	// 0 は空鞘で予約済み。
	private static final Map<ResourceLocation, Integer> SHEATHABLE_SWORDS = new LinkedHashMap<>();
	static {
		SHEATHABLE_SWORDS.put(new ResourceLocation("minecraft", "iron_sword"), 1);
		SHEATHABLE_SWORDS.put(new ResourceLocation("minecraft", "golden_sword"), 2);
	}

	public SwordSayaItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack sayaStack = player.getItemInHand(hand);

		if (sayaStack.hasTag() && sayaStack.getTag().contains("StoredSword")) {
			CompoundTag tag = sayaStack.getOrCreateTag();
			ItemStack swordStack = ItemStack.of(tag.getCompound("StoredSword"));

			InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ?
				InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
			ItemStack otherItem = player.getItemInHand(otherHand);

			if (otherItem.isEmpty()) {
				player.setItemInHand(otherHand, swordStack);

				tag.remove("StoredSword");
				tag.putInt("CustomModelData", 0);
				sayaStack.setTag(tag);

				world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8f, 1.2f);

				player.displayClientMessage(Component.literal("§e抜刀！"), true);

				return InteractionResultHolder.sidedSuccess(sayaStack, world.isClientSide());
			} else {
				player.displayClientMessage(Component.literal("§c反対の手が空いていません"), true);
				return InteractionResultHolder.fail(sayaStack);
			}
		}

		return InteractionResultHolder.pass(sayaStack);
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
		ResourceLocation id = ForgeRegistries.ITEMS.getKey(swordStack.getItem());
		return id != null && SHEATHABLE_SWORDS.containsKey(id);
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

	public static int getSwordModelData(ItemStack sword) {
		ResourceLocation id = ForgeRegistries.ITEMS.getKey(sword.getItem());
		if (id == null) return 0;
		return SHEATHABLE_SWORDS.getOrDefault(id, 0);
	}
}
