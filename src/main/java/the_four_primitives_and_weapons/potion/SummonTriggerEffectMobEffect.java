
package the_four_primitives_and_weapons.potion;

import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;

import the_four_primitives_and_weapons.procedures.SummonTriggerEffectEffectStartedappliedProcedure;
import the_four_primitives_and_weapons.procedures.SummonTriggerEffectEffectExpiresProcedure;

import net.minecraft.client.gui.GuiGraphics;

public class SummonTriggerEffectMobEffect extends MobEffect {
	public SummonTriggerEffectMobEffect() {
		super(MobEffectCategory.NEUTRAL, -1);
	}

	@Override
	public String getDescriptionId() {
		return "effect.the_four_primitives_and_weapons.summon_trigger_effect";
	}

@Override
public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
	SummonTriggerEffectEffectStartedappliedProcedure.execute(entity, amplifier);
}

@Override
public void applyEffectTick(LivingEntity entity, int amplifier) {
	// addAttributeModifiersで実行されるため、ここでは何もしない
}

@Override
public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
	super.removeAttributeModifiers(entity, attributeMap, amplifier);
	SummonTriggerEffectEffectExpiresProcedure.execute(entity);
}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return false; // 毎tickの処理は不要
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
