package minecraftarmorweapon.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.level().LevelAccessor;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level().ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class SkinOfDragondesProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity().level, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		execute(null, world, x, y, z, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		boolean found = false;
		double sx = 0;
		double sy = 0;
		double sz = 0;
		double fromZ = 0;
		double fromX = 0;
		double fromY = 0;
		double previousRecipe = 0;
		if (entity instanceof EnderDragon) {
			if (world.getBiome(new BlockPos(entity.getX(), entity.getY(), entity.getZ())).is(new ResourceLocation("the_end"))) {
				entity.getPersistentData().putDouble("minecraft_armor_weapon:skin_of_dragon_random", (Mth.nextInt(RandomSource.create(), 1, 10)));
				if (entity.getPersistentData().getDouble("minecraft_armor_weapon:skin_of_dragon_random") == 10) {
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/summon minecraft:item 0 69 0 {Item:{id:\"minecraft_armor_weapon:skin_of_dragon\",Count:3b},Glowing:1b,NoGravity:1b}");
						}
					}
				}
				if (entity.getPersistentData().getDouble("minecraft_armor_weapon:skin_of_dragon_random") == 9) {
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/summon minecraft:item 0 69 0 {Item:{id:\"minecraft_armor_weapon:skin_of_dragon\",Count:3b},Glowing:1b,NoGravity:1b}");
						}
					}
				}
				if (entity.getPersistentData().getDouble("minecraft_armor_weapon:skin_of_dragon_random") == 8) {
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/summon minecraft:item 0 69 0 {Item:{id:\"minecraft_armor_weapon:skin_of_dragon\",Count:2b},Glowing:1b,NoGravity:1b}");
						}
					}
				}
				if (entity.getPersistentData().getDouble("minecraft_armor_weapon:skin_of_dragon_random") == 7) {
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/summon minecraft:item 0 69 0 {Item:{id:\"minecraft_armor_weapon:skin_of_dragon\",Count:2b},Glowing:1b,NoGravity:1b}");
						}
					}
				}
				if (entity.getPersistentData().getDouble("minecraft_armor_weapon:skin_of_dragon_random") == 6) {
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/summon minecraft:item 0 69 0 {Item:{id:\"minecraft_armor_weapon:skin_of_dragon\",Count:2b},Glowing:1b,NoGravity:1b}");
						}
					}
				}
				if (entity.getPersistentData().getDouble("minecraft_armor_weapon:skin_of_dragon_random") == 5) {
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/summon minecraft:item 0 69 0 {Item:{id:\"minecraft_armor_weapon:skin_of_dragon\",Count:1b},Glowing:1b,NoGravity:1b}");
						}
					}
				}
				if (entity.getPersistentData().getDouble("minecraft_armor_weapon:skin_of_dragon_random") == 4) {
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/summon minecraft:item 0 69 0 {Item:{id:\"minecraft_armor_weapon:skin_of_dragon\",Count:1b},Glowing:1b,NoGravity:1b}");
						}
					}
				}
				if (entity.getPersistentData().getDouble("minecraft_armor_weapon:skin_of_dragon_random") == 3) {
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/summon minecraft:item 0 69 0 {Item:{id:\"minecraft_armor_weapon:skin_of_dragon\",Count:1b},Glowing:1b,NoGravity:1b}");
						}
					}
				}
				if (entity.getPersistentData().getDouble("minecraft_armor_weapon:skin_of_dragon_random") == 2) {
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/summon minecraft:item 0 69 0 {Item:{id:\"minecraft_armor_weapon:skin_of_dragon\",Count:1b},Glowing:1b,NoGravity:1b}");
						}
					}
				}
				if (entity.getPersistentData().getDouble("minecraft_armor_weapon:skin_of_dragon_random") == 1) {
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/summon minecraft:item 0 69 0 {Item:{id:\"minecraft_armor_weapon:skin_of_dragon\",Count:1b},Glowing:1b,NoGravity:1b}");
						}
					}
				}
			}
		}
		if (entity instanceof WitherSkeleton) {
			if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, (sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				for (int index0 = 0; index0 < Mth.nextInt(RandomSource.create(),
						(int) Math.ceil(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, (sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) / 2),
						(int) (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, (sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) + 2)); index0++) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"/summon minecraft:item ~ ~ ~ {Item:{id:\"minecraft_armor_weapon:wither_bone\",Count:1b}}");
				}
			} else {
				for (int index1 = 0; index1 < Mth.nextInt(RandomSource.create(), 0, 2); index1++) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"/summon minecraft:item ~ ~ ~ {Item:{id:\"minecraft_armor_weapon:wither_bone\",Count:1b}}");
				}
			}
		}
		if (entity instanceof Stray) {
			if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, (sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				for (int index2 = 0; index2 < Mth.nextInt(RandomSource.create(), 0,
						(int) (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MOB_LOOTING, (sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) + 2)); index2++) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"/summon minecraft:item ~ ~ ~ {Item:{id:\"minecraft_armor_weapon:stray_bone\",Count:1b},}");
				}
			} else {
				for (int index3 = 0; index3 < Mth.nextInt(RandomSource.create(), 0, 2); index3++) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"/summon minecraft:item ~ ~ ~ {Item:{id:\"minecraft_armor_weapon:stray_bone\",Count:1b},}");
				}
			}
		}
	}
}
