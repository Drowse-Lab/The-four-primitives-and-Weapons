package the_four_primitives_and_weapons.procedures;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModEnchantments;

import the_four_primitives_and_weapons.entity.SkeltonMobEntity;
import the_four_primitives_and_weapons.entity.OtiruyoEntity;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class OnazitakasaArrowehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putDouble("Xpos", (entity.getPersistentData().getDouble("X") + Math.sin(Math.toRadians(entity.getPersistentData().getDouble("yaw") + 180)) * entity.getPersistentData().getDouble("distance")));
		entity.getPersistentData().putDouble("Zpos", (entity.getPersistentData().getDouble("Z") + Math.cos(Math.toRadians(entity.getPersistentData().getDouble("yaw"))) * entity.getPersistentData().getDouble("distance")));
		for (int index0 = 0; index0 < 15; index0++) {
			if (world.getBlockState(new BlockPos((int) (entity.getPersistentData().getDouble("Xpos")), (int) (entity.getPersistentData().getDouble("Ypos")), (int) (entity.getPersistentData().getDouble("Zpos")))).canOcclude()) {
				entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
			} else {
				break;
			}
		}
		for (int index1 = 0; index1 < 15; index1++) {
			if (world.getBlockState(new BlockPos((int) (entity.getPersistentData().getDouble("Xpos")), (int) (entity.getPersistentData().getDouble("Ypos")), (int) (entity.getPersistentData().getDouble("Zpos")))).canOcclude()) {
				entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
				break;
			}
			entity.getPersistentData().putDouble("Ypos", (entity.getPersistentData().getDouble("Ypos")));
		}
		if ((world.getBlockState(new BlockPos((int) (entity.getPersistentData().getDouble("Xpos")), (int) (entity.getPersistentData().getDouble("Ypos")), (int) (entity.getPersistentData().getDouble("Zpos"))))).getBlock() instanceof LiquidBlock) {
			if ((world.getBlockState(new BlockPos((int) (entity.getPersistentData().getDouble("Xpos")), (int) (entity.getPersistentData().getDouble("Ypos")), (int) (entity.getPersistentData().getDouble("Zpos"))))).getBlock() == Blocks.WATER
					&& (world.getBlockState(new BlockPos((int) (entity.getPersistentData().getDouble("Xpos")), (int) (entity.getPersistentData().getDouble("Ypos")), (int) (entity.getPersistentData().getDouble("Zpos"))))).getBlock() == Blocks.WATER
					&& (world.getBlockState(new BlockPos((int) (entity.getPersistentData().getDouble("Xpos")), (int) (entity.getPersistentData().getDouble("Ypos")), (int) (entity.getPersistentData().getDouble("Zpos"))))).getBlock() == Blocks.BUBBLE_COLUMN) {
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, new BlockPos((int) (entity.getPersistentData().getDouble("Xpos")), (int) (entity.getPersistentData().getDouble("Ypos")), (int) (entity.getPersistentData().getDouble("Zpos"))),
								ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("ambient.underwater.loop")), SoundSource.NEUTRAL, 1, 1);
					} else {
						_level.playLocalSound((entity.getPersistentData().getDouble("Xpos")), (entity.getPersistentData().getDouble("Ypos")), (entity.getPersistentData().getDouble("Zpos")),
								ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("ambient.underwater.loop")), SoundSource.NEUTRAL, 1, 1, false);
					}
				}
			}
		} else {
			world.levelEvent(2001, new BlockPos((int) (entity.getPersistentData().getDouble("Xpos")), (int) (entity.getPersistentData().getDouble("Ypos")), (int) (entity.getPersistentData().getDouble("Zpos"))),
					Block.getId((world.getBlockState(new BlockPos((int) (entity.getPersistentData().getDouble("Xpos")), (int) (entity.getPersistentData().getDouble("Ypos")), (int) (entity.getPersistentData().getDouble("Zpos")))))));
		}
		if (world instanceof ServerLevel _level)
			_level.getServer().getCommands()
					.performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, new Vec3((entity.getPersistentData().getDouble("Xpos")), (entity.getPersistentData().getDouble("Ypos") + 1), (entity.getPersistentData().getDouble("Zpos"))),
							Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(), "/particle explosion ~ ~ ~ 0.1 0.1 0.1 1.5 1 normal");
		{
			final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("Xpos")), (entity.getPersistentData().getDouble("Ypos") + 1), (entity.getPersistentData().getDouble("Zpos")));
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
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
									entityiterator.hurt(entityiterator.damageSources().generic(), 15);
								}
							}
						}
					}
				}
			}
		}
		if (world.getBlockState(new BlockPos((int) (entity.getPersistentData().getDouble("Xpos")), (int) (entity.getPersistentData().getDouble("Ypos") + 1), (int) (entity.getPersistentData().getDouble("Zpos")))).canOcclude()) {
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(TheFourPrimitivesAndWeaponsModMobEffects.ONAZITAKASA_ARROW.get());
		}
		entity.getPersistentData().putDouble("distance", (entity.getPersistentData().getDouble("distance") + 0.8));
	}
}
