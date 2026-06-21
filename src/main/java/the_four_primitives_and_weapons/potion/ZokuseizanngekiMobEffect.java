
package the_four_primitives_and_weapons.potion;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;

import the_four_primitives_and_weapons.procedures.ZokuseizanngekiehuekutogaYouXiaoShinoteitukuProcedure;
import the_four_primitives_and_weapons.procedures.GyetonzangekiposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure;

import net.minecraft.client.gui.GuiGraphics;

public class ZokuseizanngekiMobEffect extends MobEffect {
	public ZokuseizanngekiMobEffect() {
		super(MobEffectCategory.NEUTRAL, -1);
	}

	@Override
	public String getDescriptionId() {
		return "effect.the_four_primitives_and_weapons.zokuseizanngeki";
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		GyetonzangekiposiyonnoXiaoGuogaKaiShiShiYongsaretatokiProcedure.execute(entity.getX(), entity.getY(), entity.getZ(), entity);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		ZokuseizanngekiehuekutogaYouXiaoShinoteitukuProcedure.execute(VersionHelper.getLevel(entity), entity);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		// 軽量化: 毎 tick (true) → 2 tick に 1 回 に間引き。
		// このプロシージャは 900 行 + 60+ vanilla command 呼び出しがあるため負荷大。
		// (player の体感上 0.1 秒の遅延は気付かないレベル)
		return (duration & 1) == 0;
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
