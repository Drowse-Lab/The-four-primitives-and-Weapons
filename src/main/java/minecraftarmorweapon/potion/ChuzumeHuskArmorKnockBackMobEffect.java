
package minecraftarmorweapon.potion;

import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;

import minecraftarmorweapon.procedures.ChuzumeHuskArmorKnockBackposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure;
import minecraftarmorweapon.procedures.ChuzumeHuskArmorKnockBackposiyonXiaoGuogaQieretaShiProcedure;

import net.minecraft.client.gui.GuiGraphics;

public class ChuzumeHuskArmorKnockBackMobEffect extends MobEffect {
	public ChuzumeHuskArmorKnockBackMobEffect() {
		super(MobEffectCategory.NEUTRAL, -1);
	}

	@Override
	public String getDescriptionId() {
		return "effect.minecraft_armor_weapon.chuzume_husk_armor_knock_back";
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		ChuzumeHuskArmorKnockBackposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure.execute(entity);
	}

	@Override
	public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		super.removeAttributeModifiers(entity, attributeMap, amplifier);
		ChuzumeHuskArmorKnockBackposiyonXiaoGuogaQieretaShiProcedure.execute(entity);
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
