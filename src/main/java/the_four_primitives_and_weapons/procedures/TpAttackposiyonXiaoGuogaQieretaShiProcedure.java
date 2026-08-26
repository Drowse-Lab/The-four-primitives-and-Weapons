package the_four_primitives_and_weapons.procedures;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.client.Minecraft;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModEnchantments;

import the_four_primitives_and_weapons.entity.SkeltonMobEntity;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class TpAttackposiyonXiaoGuogaQieretaShiProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double zknockback = 0;
		double zPos = 0;
		double yPos = 0;
		double rad = 0;
		double yknockback = 0;
		double rad_now = 0;
		double xPos = 0;
		double xknockback = 0;
		double dis = 0;
		rad = Math.toRadians(entity.getYRot());
		dis = 0.1;
		rad_now = Math.toRadians(entity.getYRot());
		for (int index0 = 0; index0 < 36; index0++) {
			xPos = x + Math.cos(rad_now) * Math.cos(rad) * dis;
			yPos = y + 1.25 + Math.sin(rad_now) * dis;
			zPos = z + Math.cos(rad_now) * Math.sin(rad) * dis;
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(xPos, yPos, zPos), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"particle dust 0.99 1 0.58 1 ~ ~ ~ 0.01 0.01 0.01 1 1 force");
			rad_now = rad_now + Math.toRadians(10);
		}
		dis = 0;
		while (!world.getBlockState(new BlockPos((int) (x + Math.sin(Math.toRadians(entity.getYRot() + 180)) * dis), (int) (y + 1 + Math.sin(Math.toRadians(entity.getXRot())) * dis * (-1)), (int) (z + Math.cos(Math.toRadians(entity.getYRot())) * dis))).canOcclude()) {
			xPos = x + Math.sin(Math.toRadians(entity.getYRot() + 180)) * dis;
			yPos = y + 1.25 + Math.sin(Math.toRadians(entity.getXRot())) * dis * (-1);
			zPos = z + Math.cos(Math.toRadians(entity.getYRot())) * dis;
			if (world instanceof ServerLevel _level)
				_level.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3(xPos, yPos, zPos), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
						"particle dust 0.99 1 0.32 1 ~ ~ ~ 0.01 0.01 0.01 1 1 force");
			{
				final Vec3 _center = new Vec3(xPos, yPos, zPos);
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
						.collect(Collectors.toList());
				for (Entity entityiterator : _entfound) {
					if (!(entityiterator == entity)) {
						if (true) {
							if (!(entityiterator instanceof SkeltonMobEntity)) {
								if (entityiterator instanceof LivingEntity) {
									if (!(world.getDifficulty() == Difficulty.PEACEFUL) && !(new Object() {
										public boolean checkGamemode(Entity _ent) {
											if (_ent instanceof ServerPlayer _serverPlayer) {
												return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
											} else if (_ent.level().isClientSide() && _ent instanceof Player _player) {
												return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
														&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.CREATIVE;
											}
											return false;
										}
									}.checkGamemode(entity)) && !(new Object() {
										public boolean checkGamemode(Entity _ent) {
											if (_ent instanceof ServerPlayer _serverPlayer) {
												return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
											} else if (_ent.level().isClientSide() && _ent instanceof Player _player) {
												return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
														&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.SPECTATOR;
											}
											return false;
										}
									}.checkGamemode(entity))) {
										if (entityiterator instanceof Mob _entity && entity instanceof LivingEntity _ent)
											_entity.setTarget(_ent);
									}
									if (EnchantmentHelper.getItemEnchantmentLevel(TheFourPrimitivesAndWeaponsModEnchantments.KILL.get(), (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
										if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.TUNDERBOLTEFFRCT.get()) : false) {
											if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
												_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.THUNDER_HIT.get(), 100, 2, true, false));
										}
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
										if (entity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.TUNDERBOLTEFFRCT.get()) : false) {
											if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
												_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.THUNDER_HIT.get(), 100, 2, true, false));
										}
										if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.KURIKARAKEN.get()
												&& (entityiterator instanceof LivingEntity _livEnt ? _livEnt.getMobType() == MobType.UNDEAD : false)) {
											entityiterator.hurt(entityiterator.damageSources().generic(), 10);
										}
										if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.PROTOTYPE_KATANA.get()
												|| (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.MAGISCHES_FEEN_KATANA.get()) {
											if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.FIREBALL.get()
													|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.WIND_STEP.get()
													|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.BUBBLESHOT.get()
													|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.THUNDERBOLT.get()
													|| (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.STORM.get()) {
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.FIREBALL.get()) {
													entityiterator.hurt(entityiterator.damageSources().generic(), 20);
													entityiterator.setSecondsOnFire(30);
												}
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.WIND_STEP.get()) {
													entityiterator.hurt(entityiterator.damageSources().generic(), 40);
												}
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.BUBBLESHOT.get()) {
													entityiterator.hurt(entityiterator.damageSources().generic(), 20);
												}
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.THUNDERBOLT.get()) {
													entityiterator.hurt(entityiterator.damageSources().generic(), 20);
													if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
														_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
												}
												if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.PROTOTYPE_KATANA.get()) {
													if ((entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.STORM.get()) {
														entityiterator.hurt(entityiterator.damageSources().generic(), 20);
														if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
															_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
													}
												}
											}
										}
										entityiterator.hurt(entityiterator.damageSources().generic(), 10);
									}
									dis = Math.abs(xknockback) + Math.abs(yknockback) + Math.abs(zknockback);
								}
							}
						}
					}
				}
			}
			dis = dis + 0.5;
			if (dis >= 50) {
				break;
			}
		}
	}
}
