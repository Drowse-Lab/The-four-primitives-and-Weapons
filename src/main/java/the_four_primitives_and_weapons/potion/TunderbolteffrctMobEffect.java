
package the_four_primitives_and_weapons.potion;

import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;

import the_four_primitives_and_weapons.procedures.TunderbolteffrctposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure;
import the_four_primitives_and_weapons.procedures.TunderbolteffrctposiyonXiaoGuogaQieretaShiProcedure;
import the_four_primitives_and_weapons.procedures.TunderbolteffrctehuekutogaYouXiaoShinoteitukuProcedure;

import net.minecraft.client.gui.GuiGraphics;

public class TunderbolteffrctMobEffect extends MobEffect {
	public TunderbolteffrctMobEffect() {
		super(MobEffectCategory.NEUTRAL, -852224);
	}

	@Override
	public String getDescriptionId() {
		return "effect.the_four_primitives_and_weapons.tunderbolteffrct";
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		TunderbolteffrctposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure.execute(entity);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		TunderbolteffrctehuekutogaYouXiaoShinoteitukuProcedure.execute(entity);
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		super.removeAttributeModifiers(entity, attributeMap, amplifier);
		TunderbolteffrctposiyonXiaoGuogaQieretaShiProcedure.execute(entity);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public void initializeClient(java.util.function.Consumer<IClientMobEffectExtensions> consumer) {
		consumer.accept(new IClientMobEffectExtensions() {
			@Override
			public boolean isVisibleInInventory(MobEffectInstance effect) {
				return false;
			}

			@Override
			public boolean renderInventoryText(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics guiGraphics, int x, int y, int blitOffset) {
				return false;
			}

			@Override
			public boolean isVisibleInGui(MobEffectInstance effect) {
				return false;
			}
		});
	}
}
