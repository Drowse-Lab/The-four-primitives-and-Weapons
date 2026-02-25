package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class CometKillenteiteigaSiWangsitaShiProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world instanceof ServerLevel _level)
			_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
					"/summon minecraft:creeper ~ ~ ~ {Tags:[\"NoKill\"],Fuse:0s,ExplosionRadius:6b,CanPickUpLoot:0b,NoAI:1,Marker:true,CustomName:'[{\"text\":\"Comet\"}]',active_effects:[{id:resistance,duration:255,amplifier:255,show_particles:1b}],attributes:[{id:\"generic.armor\",base:0}]}");
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(10 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (!(entityiterator == entity)) {
					if (entityiterator instanceof ItemEntity) {
						{
							Entity _ent = entityiterator;
							if (!_ent.level().isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
										_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
							}
						}
						if (!entityiterator.level().isClientSide())
							entityiterator.discard();
					}
					MinecraftArmorWeaponMod.queueServerWork(2, () -> {
						if (entityiterator instanceof ItemEntity) {
							{
								Entity _ent = entityiterator;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
											_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
								}
							}
							if (!entityiterator.level().isClientSide())
								entityiterator.discard();
						}
						MinecraftArmorWeaponMod.queueServerWork(2, () -> {
							if (entityiterator instanceof ItemEntity) {
								{
									Entity _ent = entityiterator;
									if (!_ent.level().isClientSide() && _ent.getServer() != null) {
										_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null,
												4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
									}
								}
								if (!entityiterator.level().isClientSide())
									entityiterator.discard();
							}
							MinecraftArmorWeaponMod.queueServerWork(2, () -> {
								if (entityiterator instanceof ItemEntity) {
									{
										Entity _ent = entityiterator;
										if (!_ent.level().isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
										}
									}
									if (!entityiterator.level().isClientSide())
										entityiterator.discard();
								}
								MinecraftArmorWeaponMod.queueServerWork(2, () -> {
									if (entityiterator instanceof ItemEntity) {
										{
											Entity _ent = entityiterator;
											if (!_ent.level().isClientSide() && _ent.getServer() != null) {
												_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
														VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
											}
										}
										if (!entityiterator.level().isClientSide())
											entityiterator.discard();
									}
									MinecraftArmorWeaponMod.queueServerWork(2, () -> {
										if (entityiterator instanceof ItemEntity) {
											{
												Entity _ent = entityiterator;
												if (!_ent.level().isClientSide() && _ent.getServer() != null) {
													_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
															VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
												}
											}
											if (!entityiterator.level().isClientSide())
												entityiterator.discard();
										}
										MinecraftArmorWeaponMod.queueServerWork(2, () -> {
											if (entityiterator instanceof ItemEntity) {
												{
													Entity _ent = entityiterator;
													if (!_ent.level().isClientSide() && _ent.getServer() != null) {
														_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
																VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
													}
												}
												if (!entityiterator.level().isClientSide())
													entityiterator.discard();
											}
											MinecraftArmorWeaponMod.queueServerWork(2, () -> {
												if (entityiterator instanceof ItemEntity) {
													{
														Entity _ent = entityiterator;
														if (!_ent.level().isClientSide() && _ent.getServer() != null) {
															_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
																	VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
														}
													}
													if (!entityiterator.level().isClientSide())
														entityiterator.discard();
												}
												MinecraftArmorWeaponMod.queueServerWork(2, () -> {
													if (entityiterator instanceof ItemEntity) {
														{
															Entity _ent = entityiterator;
															if (!_ent.level().isClientSide() && _ent.getServer() != null) {
																_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
																		VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
															}
														}
														if (!entityiterator.level().isClientSide())
															entityiterator.discard();
													}
													MinecraftArmorWeaponMod.queueServerWork(2, () -> {
														if (entityiterator instanceof ItemEntity) {
															{
																Entity _ent = entityiterator;
																if (!_ent.level().isClientSide() && _ent.getServer() != null) {
																	_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
																			VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/kill @s");
																}
															}
															if (!entityiterator.level().isClientSide())
																entityiterator.discard();
														}
													});
												});
											});
										});
									});
								});
							});
						});
					});
				}
			}
		}
	}
}
