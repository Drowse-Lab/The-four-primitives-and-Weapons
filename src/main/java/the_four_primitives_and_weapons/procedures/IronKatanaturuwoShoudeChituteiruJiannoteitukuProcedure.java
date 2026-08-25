package the_four_primitives_and_weapons.procedures;

import the_four_primitives_and_weapons.util.VersionHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;

public class IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure {
	//   - mainHand を 1 回だけ取得して使い回し
	//   - 粒子コマンド (MAGISCHES_FEEN_KATANA / MAGICAL_KATANA) を 4 tick に 1 回に間引き
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null) return;
		if (!(entity instanceof LivingEntity living)) return;
		net.minecraft.world.item.Item heldItem = living.getMainHandItem().getItem();
		boolean serverSide = !entity.level().isClientSide();

		// OLD_KATANA → SLICE_GUARD
		if (heldItem == TheFourPrimitivesAndWeaponsModItems.OLD_KATANA.get()) {
			if (serverSide)
				living.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.SLICE_GUARD.get(), 60, 1, true, false));
		}

		// LONG_RANGE_WEAPON_CUT 共通付与 (WITHER/REPLICA/NINJATOU/KATANA_NIGU/SMALL_SWORD/PROTOTYPE)
		if (heldItem == TheFourPrimitivesAndWeaponsModItems.WITHER_KATANA.get()
				|| heldItem == TheFourPrimitivesAndWeaponsModItems.NINJATOU.get()
				|| heldItem == TheFourPrimitivesAndWeaponsModItems.KATANA_NIGU_HUMERUS.get()
				|| heldItem == TheFourPrimitivesAndWeaponsModItems.SMALL_SWORD.get()
				|| heldItem == TheFourPrimitivesAndWeaponsModItems.PROTOTYPE_KATANA.get()) {
			if (serverSide)
				living.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.LONG_RANGE_WEAPON_CUT.get(), 2, 1, true, false));
		}
		if (false) {
			if (serverSide)
				living.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.LONG_RANGE_WEAPON_CUT.get(), 2, 1, false, true));
		}

		if (false) {
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class,
					new AABB(_center, _center).inflate(16 / 2d),
					e -> e != entity && e.getPersistentData().getDouble("gyamigyapitonndeyaru") == 1)
				.stream()
				.sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
				.limit(4) // 1 tick で連続テレポートは最大 4 体まで
				.collect(Collectors.toList());
			for (Entity entityiterator : _entfound) {
				if (serverSide && entity.getServer() != null) {
					var cmds = entity.getServer().getCommands();
					var src = new CommandSourceStack(CommandSource.NULL, entity.position(), entity.getRotationVector(),
							VersionHelper.getLevel(entity) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(entity) : null,
							4, entity.getName().getString(), entity.getDisplayName(), entity.level().getServer(), entity);
					cmds.performPrefixedCommand(src, "playsound minecraft:block.beacon.activate neutral @a ~ ~ ~ 2 2");
					cmds.performPrefixedCommand(src, "playsound minecraft:entity.enderman.teleport player @a ~ ~ ~ 2 1");
				}
				if (!living.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.KURUTIMENASI.get())) {
					if (entity instanceof Player _player)
						_player.getCooldowns().addCooldown(net.minecraft.world.item.Items.AIR, 40);
				}
				entityiterator.getPersistentData().putDouble("gyamigyapitonndeyaru", 0);
				entity.teleportTo(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ());
				if (entity instanceof ServerPlayer _serverPlayer)
					_serverPlayer.connection.teleport(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ(), entity.getYRot(), entity.getXRot());
			}
		}

		// MAGISCHES_FEEN_KATANA / MAGICAL_KATANA の自分への enchant 粒子: 4 tick に 1 回に間引き
		boolean particleTick = (entity.tickCount & 3) == 0;
		if (particleTick
				&& (heldItem == TheFourPrimitivesAndWeaponsModItems.MAGISCHES_FEEN_KATANA.get()
				 || heldItem == TheFourPrimitivesAndWeaponsModItems.MAGICAL_KATANA.get())
				&& world instanceof ServerLevel _sl
				&& _sl.getServer() != null) {
			_sl.getServer().getCommands().performPrefixedCommand(
					new CommandSourceStack(CommandSource.NULL, new Vec3(x, y, z), Vec2.ZERO, _sl,
							4, "", Component.literal(""), _sl.getServer(), entity).withSuppressedOutput(),
					"particle minecraft:enchant ~ ~1 ~ 0.5 0.5 0.5 0.1 1 normal @s");
		}
	}
}
