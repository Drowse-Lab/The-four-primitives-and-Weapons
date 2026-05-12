
package the_four_primitives_and_weapons.item;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.nbt.CompoundTag;
import the_four_primitives_and_weapons.procedures.ZokuseiritokutyuwazaProcedure;
import the_four_primitives_and_weapons.procedures.IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import net.minecraft.network.chat.TextColor;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class OldKatanaItem extends SwordItem {
	public OldKatanaItem() {
		super(new Tier() {
			public int getUses() {
				return 0;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 6f;
			}

			public int getLevel() {
				return 1;
			}

			public int getEnchantmentValue() {
				return 2;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of();
			}
		}, 3, -2.4f, new Item.Properties());
	}
    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Old Sword")
            .setStyle(Style.EMPTY
            .withFont(new ResourceLocation("minecraft", "illageralt")) // フォント指定
            .withColor(TextColor.parseColor("#5477b3")) // 淡い青色 (#5477b3)
            .withItalic(false)); // イタリックを無効化
    }
	
//	@Override
//	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
//		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
//		ZokuseiritokutyuwazaProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
//		return ar;
//	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		
		if (entity instanceof Player player) {
			// 手に持っている場合
			if (selected) {
				IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
			}
			
			// 鞘に納刀している場合もSliceGuard効果を付与
			boolean isSheathed = false;
			
			// インベントリ内の全ての鞘をチェック
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack slotItem = player.getInventory().getItem(i);
				
				// 鞘アイテムかチェック
				if (slotItem.getItem() == TheFourPrimitivesAndWeaponsModItems.SAYA.get()) {
					// 鞘に格納されている刀をチェック
					if (slotItem.hasTag()) {
						CompoundTag nbt = slotItem.getTag();
						if (nbt.contains("StoredKatana")) {
							CompoundTag storedKatana = nbt.getCompound("StoredKatana");
							String katanaId = storedKatana.getString("id");
							
							// OldKatanaが納刀されているかチェック
							if (katanaId.equals("the_four_primitives_and_weapons:old_katana")) {
								isSheathed = true;
								break;
							}
						}
					}
				}
			}
			
			// 納刀中の場合、SliceGuard効果を付与
			if (isSheathed && !player.level().isClientSide()) {
				player.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.SLICE_GUARD.get(), 60, 1, true, false));
			}
		}
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}
}
