package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

public class ThunderHitehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
			_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
					entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get()) ? _livEnt.getEffect(MinecraftArmorWeaponModMobEffects.THUNDER_HIT.get()).getDuration() : 0, 6, true, false));
		MinecraftArmorWeaponMod.queueServerWork(5, () -> {
			if (entity instanceof LivingEntity _entity)
				_entity.hurt(_entity.damageSources().magic(), 1);
		});
		{
			Entity _ent = entity;
			if (!_ent.level().isClientSide() && _ent.getServer() != null) {
				_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
						_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/particle dust 1.000 0.969 0.059 1 ~ ~1 ~ 0.1 0.1 0.1 1 25");
			}
		}
	}
}
