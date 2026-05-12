package the_four_primitives_and_weapons.procedures;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModEnchantments;

import the_four_primitives_and_weapons.entity.SkeltonMobEntity;
import the_four_primitives_and_weapons.entity.OtiruyoEntity;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class GuwaazangekiEffectStartedappliedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double yknockback = 0;
		double ZRadius2 = 0;
		double dis = 0;
		double zknockback = 0;
		double r = 0;
		double loop = 0;
		double alpha = 0;
		double XRadius2 = 0;
		double X = 0;
		double Y = 0;
		double Z = 0;
		double Y_pos = 0;
		double xknockback = 0;
		double beta = 0;
		r = 1;
		alpha = entity.getYRot();
		beta = entity.getXRot();
		for (int index0 = 0; index0 < 1048576; index0++) {
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands()
						.performPrefixedCommand(new CommandSourceStack(CommandSource.NULL,
								new Vec3((x - r * Math.cos(Math.toRadians(beta)) * Math.sin(Math.toRadians(alpha))), ((y + 1) - r * Math.sin(Math.toRadians(beta))), (z + r * Math.cos(Math.toRadians(beta)) * Math.cos(Math.toRadians(alpha)))),
								Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "fill ^-30 ^ ^5 ^30 ^3 ^5 air");
			if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.WIND_STEP_EFFECT.get()) : false) {
				{
					final Vec3 _center = new Vec3((x - r * Math.cos(Math.toRadians(beta)) * Math.sin(Math.toRadians(alpha))), ((y + 1) - r * Math.sin(Math.toRadians(beta))), (z + r * Math.cos(Math.toRadians(beta)) * Math.cos(Math.toRadians(alpha))));
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
							.collect(Collectors.toList());
					for (Entity entityiterator : _entfound) {
						if (!(entityiterator == entity)) {
							if (!(entityiterator instanceof OtiruyoEntity)) {
								if (!(entityiterator instanceof SkeltonMobEntity)) {
									if (entityiterator instanceof LivingEntity) {
										if (EnchantmentHelper.getItemEnchantmentLevel(TheFourPrimitivesAndWeaponsModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
											{
												Entity _ent = entityiterator;
												if (!_ent.level().isClientSide() && _ent.getServer() != null) {
													_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
															VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
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
											entityiterator.hurt(entityiterator.damageSources().generic(), 10);
										}
									}
								}
							}
						}
					}
				}
			} else {
				{
					final Vec3 _center = new Vec3((x - r * Math.cos(Math.toRadians(beta)) * Math.sin(Math.toRadians(alpha))), ((y + 1) - r * Math.sin(Math.toRadians(beta))), (z + r * Math.cos(Math.toRadians(beta)) * Math.cos(Math.toRadians(alpha))));
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
							.collect(Collectors.toList());
					for (Entity entityiterator : _entfound) {
						if (!(entityiterator == entity)) {
							if (!(entityiterator instanceof OtiruyoEntity)) {
								if (!(entityiterator instanceof SkeltonMobEntity)) {
									if (entityiterator instanceof LivingEntity) {
										if (EnchantmentHelper.getItemEnchantmentLevel(TheFourPrimitivesAndWeaponsModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
											{
												Entity _ent = entityiterator;
												if (!_ent.level().isClientSide() && _ent.getServer() != null) {
													_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
															VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
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
											entityiterator.hurt(entityiterator.damageSources().generic(), 10);
										}
									}
								}
							}
						}
					}
				}
			}
			r = r + 0.2;
		}
		if (!(entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.KURUTIMENASI.get()) : false)) {
			if (entity instanceof Player _player)
				_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 50);
		}
	}
}
