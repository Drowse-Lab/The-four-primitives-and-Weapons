package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

import org.checkerframework.checker.units.qual.s;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class KitterukitteruehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double a = 0;
		double r = 0;
		double b = 0;
		double s = 0;
		double c = 0;
		double dis4 = 0;
		double dis3 = 0;
		double dis2 = 0;
		double dis1 = 0;
		double dis = 0;
		r = 1;
		s = 3;
		a = Mth.nextDouble(RandomSource.create(), -179, 180) + 90;
		c = 0;
		b = Mth.nextDouble(RandomSource.create(), -179, 180) + 80;
		for (int index0 = 0; index0 < 30; index0++) {
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()
					|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.FIREBALL.get()) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
														+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
												((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
												(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
														+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"particle dust 0.839 0.369 0.369 1 ~ ~ ~ 0.3 0.02 0.3 0.2 5");
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.FLAME,
								(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a))
										+ s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
								((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))), (z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a))
										+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90))),
								10, 0.02, 0.02, 0.02, 0);
				}
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WIND_STEP.get()) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
														+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
												((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
												(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
														+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"/particle dust 0.129 0.780 0.000 1 ~ ~ ~ 0.1 0.1 0.1 1 25");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
														+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
												((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
												(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
														+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"particle falling_dust ~ ~ ~ 0.5 0.1 0.5 .0 20 force @p");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
														+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
												((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
												(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
														+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"particle sneeze ~ ~ ~ 0.5 0.1 0.5 .0 20 force @p");
				}
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.BUBBLESHOT.get()) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
														+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
												((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
												(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
														+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"particle dolphin ~ ~ ~ 0.5 0.1 0.5 .0 25 force @p");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
														+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
												((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
												(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
														+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"particle bubble ~ ~ ~ 0.5 0.1 0.5 .0 25 force @p");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
														+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
												((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
												(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
														+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"particle block lapis_block ~ ~ ~ 0.5 0.1 0.5 .0 20 force @p");
				}
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.THUNDERBOLT.get()) {
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.FIREWORK,
								(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a))
										+ s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
								((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))), (z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a))
										+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90))),
								5, 0.02, 0.02, 0.02, 0.01);
				}
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.STORM.get()) {
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
														+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
												((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
												(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
														+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"particle dolphin ~ ~ ~ 0.5 0.1 0.5 .0 25 force @p");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
														+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
												((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
												(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
														+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"particle bubble ~ ~ ~ 0.5 0.1 0.5 .0 25 force @p");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
														+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
												((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
												(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
														+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"particle falling_dust ~ ~ ~ 0.5 0.1 0.5 .0 20 force @p");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
														+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
												((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
												(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
														+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"particle sneeze ~ ~ ~ 0.5 0.1 0.5 .0 20 force @p");
					if (world instanceof ServerLevel _level)
						_level.getServer().getCommands().performPrefixedCommand(
								new CommandSourceStack(CommandSource.NULL,
										new Vec3(
												(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
														+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
												((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
												(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
														+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
										Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
								"/particle dust 0.129 0.780 0.000 1 ~ ~ ~ 0.1 0.1 0.1 1 25");
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.FIREWORK,
								(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a))
										+ s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
								((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))), (z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a))
										+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90))),
								5, 0.02, 0.02, 0.02, 0.01);
				}
			} else if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL,
									new Vec3(
											(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
													+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
											((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
											(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
													+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
									Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"particle minecraft:item redstone ~ ~1.5 ~ 0.2 0.3 0.2 0.05 10 force");
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL,
									new Vec3(
											(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
													+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
											((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
											(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
													+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
									Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"/particle dust 0.639 0.169 0.169 1 ~ ~0.5 ~ 0.3 0.1 0.3 0.1 10 force");
			} else {
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL,
									new Vec3(
											(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a))
													+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
											((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))),
											(z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a))
													+ s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90)))),
									Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"particle minecraft:dust 1 1 1 1 ~ ~ ~ 0.1 0.1 0.1 0 5");
			}
			{
				final Vec3 _center = new Vec3(
						(x - (r * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(a)) + s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.sin(Math.toRadians(a))
								+ s * Math.sin(Math.toRadians(c)) * Math.sin(Math.toRadians(a - 90)))),
						((y + 1.5) - (r * Math.sin(Math.toRadians(b)) + s * Math.cos(Math.toRadians(c)) * Math.sin(Math.toRadians(b - 90)))), (z + r * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(a))
								+ s * Math.cos(Math.toRadians(c)) * Math.cos(Math.toRadians(b - 90)) * Math.cos(Math.toRadians(a)) + s * Math.sin(Math.toRadians(c)) * Math.cos(Math.toRadians(a - 90))));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
						.collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (entityiterator instanceof Arrow || entityiterator instanceof SpectralArrow || entityiterator instanceof ThrownTrident || entityiterator instanceof LargeFireball || entityiterator instanceof DragonFireball
							|| entityiterator instanceof Snowball || entityiterator instanceof ThrownEgg || entityiterator instanceof SmallFireball) {
						if (entityiterator.getPersistentData().getBoolean("Check") == false) {
							entityiterator.getPersistentData().putBoolean("Check", true);
							dis2 = Math.sqrt(Math.pow(entityiterator.getX() - entity.getX(), 0.8) + Math.pow(entityiterator.getY() - entity.getY(), 0.8) + Math.pow(entityiterator.getZ() - entity.getZ(), 0.8));
							if (dis2 <= 0.8) {
								entityiterator.getPersistentData().putBoolean("My arrow", true);
							} else {
								entityiterator.getPersistentData().putBoolean("My arrow", false);
							}
							if (entityiterator.getPersistentData().getBoolean("My arrow") == false) {
								if (entityiterator.getPersistentData().getBoolean("Check2") == false) {
									entityiterator.getPersistentData().putBoolean("Check2", true);
									{
										Entity _ent = entityiterator;
										if (!_ent.level().isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "kill");
										}
									}
									{
										Entity _ent = entityiterator;
										if (!_ent.level().isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/deta merge entity @s (Health:0)");
										}
									}
								} else {
									{
										Entity _ent = entityiterator;
										if (!_ent.level().isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "kill");
										}
									}
									{
										Entity _ent = entityiterator;
										if (!_ent.level().isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/deta merge entity @s (Health:0)");
										}
									}
								}
							}
						}
					}
				}
			}
			c = c + 6;
		}
	}
}
