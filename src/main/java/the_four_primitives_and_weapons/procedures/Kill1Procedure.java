package the_four_primitives_and_weapons.procedures;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModEnchantments;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class Kill1Procedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingAttackEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		execute(null, world, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (!world.isClientSide()) {
			if ((sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.WITHER_KATANA.get()) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 2, false, true));
			}
		}
		if (!world.isClientSide()) {
			if ((sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == TheFourPrimitivesAndWeaponsModItems.RIVERS_OF_BLOOD.get()) {
				if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.DEVOUR_BLOOD.get(), 1, 1, true, false));
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL, new Vec3((entity.getX()), (entity.getY()), (entity.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"/particle dust 0.639 0.169 0.169 1 ~ ~0.5 ~ 0.3 0.1 0.3 0.1 10 force");
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL, new Vec3((entity.getX()), (entity.getY()), (entity.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"particle item redstone ~ ~1 ~ 0.3 0.1 0.3 0.1 100 force");
			}
		}
		if (!world.isClientSide()) {
			if (sourceentity instanceof ServerPlayer _plr17 && VersionHelper.getLevel(_plr17) instanceof ServerLevel
					&& _plr17.getAdvancements().getOrStartProgress(_plr17.server.getAdvancements().getAdvancement(new ResourceLocation("the_four_primitives_and_weapons:you_have_become_a_vampire"))).isDone()) {
				if ((sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1) >= (sourceentity instanceof Player _plr ? _plr.getFoodData().getFoodLevel() : 0)) {
					if (sourceentity instanceof LivingEntity _entity)
						_entity.setHealth((float) ((sourceentity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) + 1));
					if (sourceentity instanceof Player _player)
						_player.getFoodData().setFoodLevel((int) ((sourceentity instanceof Player _plr ? _plr.getFoodData().getFoodLevel() : 0) + 1));
				}
			}
		}
		if (!world.isClientSide()) {
			if (sourceentity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.SWORD_OF_NIGHT_EFFECT.get()) : false) {
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL, new Vec3((entity.getX()), (entity.getY()), (entity.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"playsound minecraft:entity.wither.break_block neutral @a ~ ~ ~ 1 1");
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL, new Vec3((entity.getX()), (entity.getY()), (entity.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"particle crit ~ ~1 ~ 0 0 0 0.5 12");
				if (world instanceof ServerLevel _level)
					_level.getServer().getCommands().performPrefixedCommand(
							new CommandSourceStack(CommandSource.NULL, new Vec3((entity.getX()), (entity.getY()), (entity.getZ())), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
							"particle end_rod ~ ~1 ~ 0 0 0 0.1 12");
			}
		}
		if (!world.isClientSide()) {
			if (EnchantmentHelper.getItemEnchantmentLevel(TheFourPrimitivesAndWeaponsModEnchantments.DEMONIZED.get(), (sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY)) != 0) {
				if (sourceentity instanceof LivingEntity _entity)
					_entity.setHealth((float) ((sourceentity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1)
							+ EnchantmentHelper.getItemEnchantmentLevel(TheFourPrimitivesAndWeaponsModEnchantments.DEMONIZED.get(), (sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY))));
				if (sourceentity instanceof Player _player)
					_player.getFoodData().setSaturation((float) ((sourceentity instanceof Player _plr ? _plr.getFoodData().getSaturationLevel() : 0)
							+ EnchantmentHelper.getItemEnchantmentLevel(TheFourPrimitivesAndWeaponsModEnchantments.DEMONIZED.get(), (sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY))));
				if (sourceentity instanceof Player _player)
					_player.getFoodData().setFoodLevel((int) ((sourceentity instanceof Player _plr ? _plr.getFoodData().getFoodLevel() : 0)
							+ EnchantmentHelper.getItemEnchantmentLevel(TheFourPrimitivesAndWeaponsModEnchantments.DEMONIZED.get(), (sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY))));
			}
		}
		if (!world.isClientSide()) {
			if (sourceentity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.BUBBLESHOT_EFFECT.get()) : false) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.TISSOKU.get(), 120, 6, true, false));
			}
		}
		if (!world.isClientSide()) {
			if (sourceentity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.FIREBALLEFFECT.get()) : false) {
				entity.setSecondsOnFire(15);
			}
		}
		if (!world.isClientSide()) {
			if (sourceentity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.TUNDERBOLTEFFRCT.get()) : false) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
			}
		}
		if (!world.isClientSide()) {
			if (sourceentity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.BOW_ATTACK.get()) : false) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.ATTACK_BOW.get(), 1, 1, true, false));
			}
		}
		if (!world.isClientSide()) {
			if (sourceentity instanceof LivingEntity _livEnt ? _livEnt.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.STORM_EFFECT.get()) : false) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.THUNDER_HIT.get(), 120, 6, true, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.TISSOKU.get(), 120, 6, true, false));
			}
		}
	}
}
