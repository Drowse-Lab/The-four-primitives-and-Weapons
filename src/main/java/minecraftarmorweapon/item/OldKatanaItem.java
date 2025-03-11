
package minecraftarmorweapon.item;

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
import minecraftarmorweapon.procedures.ZokuseiritokutyuwazaProcedure;
import minecraftarmorweapon.procedures.IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure;
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
		}, 3, -1.4f, new Item.Properties().tab(null));
	}
    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Old Sword")
            .setStyle(Style.EMPTY
            .withFont(new ResourceLocation("minecraft", "illageralt")) // フォント指定
            .withColor(TextColor.parseColor("#5477b3")) // 淡い青色 (#5477b3)
            .withItalic(false)); // イタリックを無効化
    }
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		ZokuseiritokutyuwazaProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
		return ar;
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (selected)
			IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}
}
