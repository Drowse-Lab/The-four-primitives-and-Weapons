
package the_four_primitives_and_weapons.potion;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;

import the_four_primitives_and_weapons.procedures.ArmorstandtobasueffectkillehuekutogaYouXiaoShinoteitukuProcedure;
import the_four_primitives_and_weapons.procedures.ArmorStandTobasuEffectposiyonXiaoGuogaQieretaShiProcedure;

import net.minecraft.client.gui.GuiGraphics;

public class ArmorstandtobasueffectkillMobEffect extends MobEffect {
	public ArmorstandtobasueffectkillMobEffect() {
		super(MobEffectCategory.NEUTRAL, -1);
	}

	@Override
	public String getDescriptionId() {
		return "effect.the_four_primitives_and_weapons.armorstandtobasueffectkill";
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		ArmorstandtobasueffectkillehuekutogaYouXiaoShinoteitukuProcedure.execute(VersionHelper.getLevel(entity), entity.getX(), entity.getY(), entity.getZ(), entity);
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		super.removeAttributeModifiers(entity, attributeMap, amplifier);
		ArmorStandTobasuEffectposiyonXiaoGuogaQieretaShiProcedure.execute(entity);
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
