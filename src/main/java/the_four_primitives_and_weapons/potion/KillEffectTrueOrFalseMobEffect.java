
package the_four_primitives_and_weapons.potion;

import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;

import the_four_primitives_and_weapons.procedures.KillEffectTrueOrFalseposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure;
import the_four_primitives_and_weapons.procedures.KillEffectTrueOrFalseposiyonXiaoGuogaQieretaShiProcedure;
import the_four_primitives_and_weapons.procedures.KillEffectTrueOrFalseehuekutogaYouXiaoShinoteitukuProcedure;

import net.minecraft.client.gui.GuiGraphics;

public class KillEffectTrueOrFalseMobEffect extends MobEffect {
	public KillEffectTrueOrFalseMobEffect() {
		super(MobEffectCategory.NEUTRAL, -1);
	}

	@Override
	public String getDescriptionId() {
		return "effect.the_four_primitives_and_weapons.kill_effect_true_or_false";
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		KillEffectTrueOrFalseposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure.execute(entity);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		KillEffectTrueOrFalseehuekutogaYouXiaoShinoteitukuProcedure.execute(

		);
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		super.removeAttributeModifiers(entity, attributeMap, amplifier);
		KillEffectTrueOrFalseposiyonXiaoGuogaQieretaShiProcedure.execute(entity);
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
