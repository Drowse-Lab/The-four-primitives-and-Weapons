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
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

public class RkigaYasaretatokiProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;

		LevelAccessor world = entity.level();
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

		// Get entity persistent data
		CompoundTag entityData = entity.getPersistentData();

		// If hand is empty and there's a covered up item, restore it
		if (mainHandItem.isEmpty() && entityData.contains("CoverUp") && entity instanceof Player) {
			// Restore ItemStack from CoverUp tag (preserves enchantments, CustomModelData, etc.)
			CompoundTag savedItemTag = entityData.getCompound("CoverUp");
			ItemStack restoredItem = ItemStack.of(savedItemTag);

			if (!restoredItem.isEmpty()) {
				// Set item to reappearing state
				CompoundTag tag = restoredItem.getOrCreateTag();
				tag.putInt("BluepurgeState", 3); // Reappearing
				tag.putInt("BluepurgeTimer", 60);

				// Give item to player
				((Player) entity).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, restoredItem);

				// Clear CoverUp tag
				entityData.remove("CoverUp");

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
				// Check if already have a covered up item
				if (entityData.contains("CoverUp")) {
					// Display message: already have one item covered up
					if (entity instanceof Player player) {
						player.displayClientMessage(Component.translatable("message.minecraft_armor_weapon.coverup_limit"), true);
					}
					return; // Don't cover up another item
				}

				// Save ItemStack to entity NBT (preserves enchantments, CustomModelData, etc.)
				CompoundTag savedItemStack = new CompoundTag();
				mainHandItem.save(savedItemStack);

				// Save to entity NBT with CoverUp tag
				entityData.put("CoverUp", savedItemStack);

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
