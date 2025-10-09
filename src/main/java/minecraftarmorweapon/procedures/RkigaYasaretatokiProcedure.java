package minecraftarmorweapon.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;

import minecraftarmorweapon.network.MinecraftArmorWeaponModVariables;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

public class RkigaYasaretatokiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;

		LevelAccessor world = entity.level;
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();

		// Check if player is holding any bluepurge item
		boolean isHoldingBluepurge = false;
		ItemStack mainHandItem = (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY);

		if (mainHandItem.getItem() == MinecraftArmorWeaponModItems.BLUEPURGE.get()
				|| mainHandItem.getItem() == MinecraftArmorWeaponModItems.BLUEPURGE_TYOKUTOU.get()
				|| mainHandItem.getItem() == MinecraftArmorWeaponModItems.BLUEPURGE_UTIGATANA.get()) {
			isHoldingBluepurge = true;
		}

		// Check if bluepurge is currently hidden
		boolean isBluepurgeHidden = (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null)
			.orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).bluepurge_hidden;

		// If hand is empty and bluepurge is hidden, restore it
		if (mainHandItem.isEmpty() && isBluepurgeHidden && entity instanceof Player) {
			// Get stored bluepurge item type
			String storedType = (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).bluepurge_item_type;

			// Create ItemStack based on stored type
			ItemStack restoredItem = ItemStack.EMPTY;
			if (storedType.equals("bluepurge")) {
				restoredItem = new ItemStack(MinecraftArmorWeaponModItems.BLUEPURGE.get());
			} else if (storedType.equals("bluepurge_tyokutou")) {
				restoredItem = new ItemStack(MinecraftArmorWeaponModItems.BLUEPURGE_TYOKUTOU.get());
			} else if (storedType.equals("bluepurge_utigatana")) {
				restoredItem = new ItemStack(MinecraftArmorWeaponModItems.BLUEPURGE_UTIGATANA.get());
			}

			if (!restoredItem.isEmpty()) {
				// Set item to reappearing state
				CompoundTag tag = restoredItem.getOrCreateTag();
				tag.putInt("BluepurgeState", 3); // Reappearing
				tag.putInt("BluepurgeTimer", 60);

				// Give item to player
				((Player) entity).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, restoredItem);

				// Clear hidden flag
				entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.bluepurge_hidden = false;
					capability.bluepurge_item_type = "";
					capability.syncPlayerVariables(entity);
				});

				// Create reappearing particle effect
				double particleAmount = 30;
				double particleRadius = 2.5;

				for (int index0 = 0; index0 < (int) particleAmount; index0++) {
					world.addParticle(ParticleTypes.GLOW,
						(x + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius),
						(y + 1 + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius),
						(z + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius),
						(Mth.nextDouble(RandomSource.create(), -0.1, 0.1)),
						(Mth.nextDouble(RandomSource.create(), 0.1, 0.3)),
						(Mth.nextDouble(RandomSource.create(), -0.1, 0.1)));

					world.addParticle(ParticleTypes.END_ROD,
						(x + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius),
						(y + 1 + Mth.nextDouble(RandomSource.create(), 0, 2)),
						(z + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius),
						0, 0.05, 0);
				}

				// Add speed boost
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide()) {
					_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
				}
			}
		}
		// Execute bluepurge effect only if holding bluepurge item
		else if (isHoldingBluepurge) {
			CompoundTag tag = mainHandItem.getOrCreateTag();
			int state = tag.getInt("BluepurgeState"); // 0=normal, 1=disappeared, 2=disappearing, 3=reappearing

			// Toggle state based on current state
			if (state == 0) {
				// Save which bluepurge item type for restoration later
				String itemType = "";
				if (mainHandItem.getItem() == MinecraftArmorWeaponModItems.BLUEPURGE.get()) {
					itemType = "bluepurge";
				} else if (mainHandItem.getItem() == MinecraftArmorWeaponModItems.BLUEPURGE_TYOKUTOU.get()) {
					itemType = "bluepurge_tyokutou";
				} else if (mainHandItem.getItem() == MinecraftArmorWeaponModItems.BLUEPURGE_UTIGATANA.get()) {
					itemType = "bluepurge_utigatana";
				}

				// Save to player variables
				final String finalItemType = itemType;
				entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.bluepurge_hidden = true;
					capability.bluepurge_item_type = finalItemType;
					capability.syncPlayerVariables(entity);
				});
				// Normal state -> Start disappearing animation
				tag.putInt("BluepurgeState", 2);
				tag.putInt("BluepurgeTimer", 60); // 3 seconds total

				// Create glowing particle effect
				double particleAmount = 30;
				double particleRadius = 2.5;

				for (int index0 = 0; index0 < (int) particleAmount; index0++) {
					// Light blue glowing particles
					world.addParticle(ParticleTypes.GLOW,
						(x + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius),
						(y + 1 + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius),
						(z + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius),
						(Mth.nextDouble(RandomSource.create(), -0.1, 0.1)),
						(Mth.nextDouble(RandomSource.create(), 0.1, 0.3)),
						(Mth.nextDouble(RandomSource.create(), -0.1, 0.1)));

					// Add soul flame particles for blue glow effect
					world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
						(x + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius),
						(y + 1 + Mth.nextDouble(RandomSource.create(), 0, 2)),
						(z + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius),
						0, 0.05, 0);
				}

				// Add speed boost for dramatic effect
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide()) {
					_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
				}
			}
		}
	}
}
