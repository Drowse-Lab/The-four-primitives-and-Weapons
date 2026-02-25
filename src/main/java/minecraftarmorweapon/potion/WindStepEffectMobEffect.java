
package minecraftarmorweapon.potion;

import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;

import minecraftarmorweapon.procedures.WindStepEffectposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure;
import minecraftarmorweapon.procedures.WindStepEffectposiyonXiaoGuogaQieretaShiProcedure;
import minecraftarmorweapon.procedures.WindStepEffectehuekutogaYouXiaoShinoteitukuProcedure;

import net.minecraft.client.gui.GuiGraphics;

public class WindStepEffectMobEffect extends MobEffect {
	public WindStepEffectMobEffect() {
		super(MobEffectCategory.NEUTRAL, -16711936);
	}

	@Override
	public String getDescriptionId() {
		return "effect.minecraft_armor_weapon.wind_step_effect";
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		WindStepEffectposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure.execute(entity);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		WindStepEffectehuekutogaYouXiaoShinoteitukuProcedure.execute(entity);
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		super.removeAttributeModifiers(entity, attributeMap, amplifier);
		WindStepEffectposiyonXiaoGuogaQieretaShiProcedure.execute(entity);
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
