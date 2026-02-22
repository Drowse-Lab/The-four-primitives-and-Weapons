
package minecraftarmorweapon.potion;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;

import minecraftarmorweapon.procedures.OtitaehuekutogaYouXiaoShinoteitukuProcedure;

import com.mojang.blaze3d.vertex.PoseStack;

public class OtitaMobEffect extends MobEffect {
	public OtitaMobEffect() {
		super(MobEffectCategory.NEUTRAL, -1);
	}

	@Override
	public String getDescriptionId() {
		return "effect.minecraft_armor_weapon.otita";
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		OtitaehuekutogaYouXiaoShinoteitukuProcedure.execute(VersionHelper.getLevel(entity), entity.getX(), entity.getY(), entity.getZ(), entity);
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
			public boolean renderInventoryText(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, PoseStack poseStack, int x, int y, int blitOffset) {
				return false;
			}

			@Override
			public boolean isVisibleInGui(MobEffectInstance effect) {
				return false;
			}
		});
	}
}
