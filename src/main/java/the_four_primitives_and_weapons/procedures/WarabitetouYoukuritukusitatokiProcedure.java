package the_four_primitives_and_weapons.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;

import the_four_primitives_and_weapons.network.TheFourPrimitivesAndWeaponsModVariables;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import the_four_primitives_and_weapons.entity.SkeltonMobEntity;
import the_four_primitives_and_weapons.entity.OtiruyoEntity;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class WarabitetouYoukuritukusitatokiProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double r = 0;
		double alpha = 0;
		double beta = 0;
		double zknockback = 0;
		double yknockback = 0;
		double xknockback = 0;
		double dis = 0;
		double dx = 0;
		double dy = 0;
		double dz = 0;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.WARABITETOU.get()
				|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.HALLOWEEN_2023_10_31_SICKLE.get()) {
			if ((entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).aaa == 2) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.KAITEN.get(), 1, 1, true, false));
				if (!(entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.KURUTIMENASI.get()) : false)) {
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.WARABITETOU.get()) {
						if (entity instanceof Player _player)
							_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 40);
					}
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.HALLOWEEN_2023_10_31_SICKLE.get()) {
						if (entity instanceof Player _player)
							_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 40);
					}
				}
			}
			if ((entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).aaa == 4) {
				if (entity instanceof LivingEntity _entity)
					_entity.swing(InteractionHand.MAIN_HAND, true);
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(10 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
							.collect(Collectors.toList());
					for (Entity entityiterator : _entfound) {
						if (!(entityiterator == entity)) {
							if (!(entityiterator instanceof OtiruyoEntity)) {
								if (!(entityiterator instanceof SkeltonMobEntity)) {
									if (entityiterator instanceof LivingEntity) {
										{
											Entity _ent = entityiterator;
											_ent.teleportTo((entity.getX()), (entity.getY()), (entity.getZ()));
											if (_ent instanceof ServerPlayer _serverPlayer)
												_serverPlayer.connection.teleport((entity.getX()), (entity.getY()), (entity.getZ()), _ent.getYRot(), _ent.getXRot());
										}
									}
								}
							}
						}
					}
				}
				if (!(entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.KURUTIMENASI.get()) : false)) {
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.WARABITETOU.get()) {
						if (entity instanceof Player _player)
							_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 30);
					}
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.HALLOWEEN_2023_10_31_SICKLE.get()) {
						if (entity instanceof Player _player)
							_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 30);
					}
				}
			}
			if ((entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).aaa == 5) {
				if (entity instanceof LivingEntity _entity)
					_entity.swing(InteractionHand.MAIN_HAND, true);
				SyugekiOnEffectActiveTickProcedure.execute(world, y, entity);
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.WARABITETOU.get()) {
					if (entity instanceof Player _player)
						_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 40);
				}
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.HALLOWEEN_2023_10_31_SICKLE.get()) {
					if (entity instanceof Player _player)
						_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 40);
				}
			}
			if ((entity.getCapability(TheFourPrimitivesAndWeaponsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new TheFourPrimitivesAndWeaponsModVariables.PlayerVariables())).aaa == 3) {
				if (entity instanceof LivingEntity _entity)
					_entity.swing(InteractionHand.MAIN_HAND, true);
				entity.getPersistentData().putDouble("X", x);
				entity.getPersistentData().putDouble("Z", z);
				entity.getPersistentData().putDouble("Ypos", y);
				entity.getPersistentData().putDouble("yaw", (entity.getYRot()));
				entity.getPersistentData().putDouble("distance", 3);
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.NAGIHARAI.get(), 2, 1, true, false));
				if (!(entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.KURUTIMENASI.get()) : false)) {
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.WARABITETOU.get()) {
						if (entity instanceof Player _player)
							_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 40);
					}
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.HALLOWEEN_2023_10_31_SICKLE.get()) {
						if (entity instanceof Player _player)
							_player.getCooldowns().addCooldown((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem(), 40);
					}
				}
			}
		}
	}
}
