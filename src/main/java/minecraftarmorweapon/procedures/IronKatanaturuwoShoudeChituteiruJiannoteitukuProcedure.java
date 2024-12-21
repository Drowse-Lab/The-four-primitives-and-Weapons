package minecraftarmorweapon.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.network.MinecraftArmorWeaponModVariables;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double a = 0;
		double r = 0;
		double b = 0;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.LOKI_THE_TRICKSTER.get()) {
			if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
				_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.LONG_RANGE_WEAPON_CUT.get(), 2, 1, true, false));
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WITHER_KATANA.get()) {
			if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
				_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.LONG_RANGE_WEAPON_CUT.get(), 2, 1, true, false));
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.REPLICA_SWORD_OF_LIGHT.get()) {
			if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
				_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.LONG_RANGE_WEAPON_CUT.get(), 2, 1, true, false));
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.SCYTHE.get()) {
			if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ARROW_1.get(), 2, 1, true, false));
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.DARKNESS_KATANA.get()) {
			if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ARROW_1.get(), 2, 1, true, false));
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.GOLD_KATANA.get()) {
			if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ARROW_1.get(), 2, 1, true, false));
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.NINJATOU.get()) {
			if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
				_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.LONG_RANGE_WEAPON_CUT.get(), 2, 1, true, false));
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 6) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.TEST.get(), 2, 1, true, false));
				{
					String _setval = "\u65AC\u3063\u3066\u308B\u3093\u3068\u3061\u3083\u3046\u305E";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KATANA_NIGU_HUMERUS.get()) {
			if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
				_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.LONG_RANGE_WEAPON_CUT.get(), 2, 1, true, false));
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.SMALL_SWORD.get()) {
			if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
				_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.LONG_RANGE_WEAPON_CUT.get(), 2, 1, true, false));
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()) {
			if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
				_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.LONG_RANGE_WEAPON_CUT.get(), 2, 1, true, false));
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.RIVERS_OF_BLOOD.get()) {
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 4) {
				{
					String _setval = "\u00A74\u00A7kdevour blood";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
		} else {
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 4) {
				{
					String _setval = "\u7A81\u304D";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.RIVERS_OF_BLOOD.get()) {
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 4) {
				{
					String _setval = "\u00A74\u00A7kdevour blood";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ARROW_1.get(), 2, 1, true, false));
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get()) {
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 2) {
				{
					String _setval = "\u00A7d\u307D\u3093\u30D1\u30C1\u30D1\u30C1";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 3) {
				{
					String _setval = "\u00A71\u3066\u3085\u30FC\u3093\u3068\u3093";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 4) {
				{
					String _setval = "\u8599\u304E\u6255\u3044";
					entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ddd = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 3) {
				if (!world.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(new Vec3(x, y, z), 50, 50, 50), e -> true).isEmpty()) {
					{
						final Vec3 _center = new Vec3(x, y, z);
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(50 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
								.collect(Collectors.toList());
						for (Entity entityiterator : _entfound) {
							if (!(entity == entityiterator)) {
								if (entityiterator.getPersistentData().getDouble("gyamigyapitonndeyaru") == 1) {
									{
										Entity _ent = entity;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													_ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent),
													"playsound minecraft:block.beacon.activate neutral @a ~ ~ ~ 2 2");
										}
									}
									{
										Entity _ent = entity;
										if (!_ent.level.isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(),
													_ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4, _ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent),
													"playsound minecraft:entity.enderman.teleport player @a ~ ~ ~ 2 1");
										}
									}
									if (!(entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.KURUTIMENASI.get()) : false)) {
										if (entity instanceof Player _player)
											_player.getCooldowns().addCooldown(MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get(), 40);
									}
									if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
										_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.SWORD_OF_NIGHT_EFFECT.get(), 20, 1, true, false));
									entityiterator.getPersistentData().putDouble("gyamigyapitonndeyaru", 0);
									{
										Entity _ent = entity;
										_ent.teleportTo((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ()));
										if (_ent instanceof ServerPlayer _serverPlayer)
											_serverPlayer.connection.teleport((entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ()), _ent.getYRot(), _ent.getXRot());
									}
								}
							}
						}
					}
				}
				r = 1;
				for (int index0 = 0; index0 < 20; index0++) {
					if (!world.getEntitiesOfClass(LivingEntity.class,
							AABB.ofSize(new Vec3((entity.getX() + r * entity.getLookAngle().x), (entity.getY() + 1.5 + r * entity.getLookAngle().y), (entity.getZ() + r * entity.getLookAngle().z)), 0.5, 0.5, 0.5), e -> true).isEmpty()) {
						if (!(entity == ((Entity) world
								.getEntitiesOfClass(LivingEntity.class,
										AABB.ofSize(new Vec3((entity.getX() + r * entity.getLookAngle().x), (entity.getY() + 1.5 + r * entity.getLookAngle().y), (entity.getZ() + r * entity.getLookAngle().z)), 0.5, 0.5, 0.5), e -> true)
								.stream().sorted(new Object() {
									Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
										return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
									}
								}.compareDistOf((entity.getX() + r * entity.getLookAngle().x), (entity.getY() + 1.5 + r * entity.getLookAngle().y), (entity.getZ() + r * entity.getLookAngle().z))).findFirst().orElse(null)))) {
							if (((Entity) world
									.getEntitiesOfClass(LivingEntity.class,
											AABB.ofSize(new Vec3((entity.getX() + r * entity.getLookAngle().x), (entity.getY() + 1.5 + r * entity.getLookAngle().y), (entity.getZ() + r * entity.getLookAngle().z)), 0.5, 0.5, 0.5), e -> true)
									.stream().sorted(new Object() {
										Comparator<Entity> compareDistOf(double _x, double _y, double _z) {
											return Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_x, _y, _z));
										}
									}.compareDistOf((entity.getX() + r * entity.getLookAngle().x), (entity.getY() + 1.5 + r * entity.getLookAngle().y), (entity.getZ() + r * entity.getLookAngle().z))).findFirst()
									.orElse(null)) instanceof LivingEntity _entity && !_entity.level.isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 2, 1, true, false));
							break;
						}
					}
					r = r + 1;
				}
				if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
					if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
						_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ARROW_1.get(), 2, 1, true, false));
				}
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.HALLOWEEN_2023_10_31_SICKLE.get()) {
			if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ARROW_1.get(), 2, 1, true, false));
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.MAGISCHES_FEEN_KATANA.get()) {
			{
				Entity _ent = entity;
				if (!_ent.level.isClientSide() && _ent.getServer() != null) {
					_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
							_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "particle minecraft:enchant ~ ~1 ~ 0.5 0.5 0.5 0.1 1 normal @s");
				}
			}
			if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ARROW_1.get(), 2, 1, true, false));
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.MAGICAL_KATANA.get()) {
			{
				Entity _ent = entity;
				if (!_ent.level.isClientSide() && _ent.getServer() != null) {
					_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level instanceof ServerLevel ? (ServerLevel) _ent.level : null, 4,
							_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "particle minecraft:enchant ~ ~1 ~ 0.5 0.5 0.5 0.1 1 normal @s");
				}
			}
			if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ARROW_1.get(), 2, 1, true, false));
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.WARABITETOU.get()) {
			if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ARROW_1.get(), 2, 1, true, false));
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKEN.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKENSWORD.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KURIKARAKENUTIGATANA.get()) {
			if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ARROW_1.get(), 2, 1, true, false));
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.LUNA.get()) {
			if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ARROW_1.get(), 2, 1, true, false));
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.MY_TEST_IRON_KATANA.get()) {
			if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ARROW_1.get(), 2, 1, true, false));
			}
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.IRON_KATANA.get()) {
			if (EnchantmentHelper.getItemEnchantmentLevel(MinecraftArmorWeaponModEnchantments.DEMONIZED.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (entity instanceof LivingEntity _entity && !_entity.level.isClientSide())
					_entity.addEffect(new MobEffectInstance(MinecraftArmorWeaponModMobEffects.ARROW_1.get(), 2, 1, true, false));
			}
		}
	}
}
