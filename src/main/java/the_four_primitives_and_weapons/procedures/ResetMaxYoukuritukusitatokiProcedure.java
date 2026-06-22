package the_four_primitives_and_weapons.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import the_four_primitives_and_weapons.damage.CorrosionElementDamageHandler;
import the_four_primitives_and_weapons.damage.ElementalDoTHandler;
import the_four_primitives_and_weapons.damage.IceElementDamageHandler;
import the_four_primitives_and_weapons.damage.MiasmaElementDamageHandler;

import java.util.ArrayList;
import java.util.List;

public class ResetMaxYoukuritukusitatokiProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, ItemStack itemstack) {
		if (entity == null) return;

		if (entity instanceof LivingEntity living) {
			living.setHealth(living.getMaxHealth());

			// 全ての HARMFUL カテゴリの MobEffect を除去 (バニラ + mod 追加 effect 両方)
			List<net.minecraft.world.effect.MobEffect> toRemove = new ArrayList<>();
			for (MobEffectInstance e : living.getActiveEffects()) {
				if (e.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
					toRemove.add(e.getEffect());
				}
			}
			for (net.minecraft.world.effect.MobEffect eff : toRemove) {
				try { living.removeEffect(eff); } catch (Throwable ignored) {}
			}
			// 念のため代表的なものを明示除去 (HARMFUL 判定漏れ対策)
			try { living.removeEffect(MobEffects.POISON); } catch (Throwable ignored) {}
			try { living.removeEffect(MobEffects.WITHER); } catch (Throwable ignored) {}
			try { living.removeEffect(MobEffects.HUNGER); } catch (Throwable ignored) {}

			// mod 独自の属性デバフを解除
			try { MiasmaElementDamageHandler.clear(living); } catch (Throwable ignored) {}
			try { ElementalDoTHandler.clear(living); } catch (Throwable ignored) {}
			try { CorrosionElementDamageHandler.clear(living); } catch (Throwable ignored) {}
			try { IceElementDamageHandler.clear(living); } catch (Throwable ignored) {}
		}

		if (entity instanceof Player player) {
			player.getFoodData().setFoodLevel(20);
			player.getFoodData().setSaturation(0);
			player.getCooldowns().addCooldown(itemstack.getItem(), 0);
		}

		entity.setAirSupply(20);
		entity.setTicksFrozen(0);
		entity.setSecondsOnFire(0);

		if (world instanceof Level _level) {
			BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
			if (!_level.isClientSide()) {
				_level.playSound(null, pos,
						ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.anvil.place")),
						SoundSource.PLAYERS, 1, 1);
			} else {
				_level.playLocalSound(x, y, z,
						ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.anvil.place")),
						SoundSource.PLAYERS, 1, 1, false);
			}
		}
	}
}
