package minecraftarmorweapon.procedures;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

public class FireballeffectehuekutogaYouXiaoShinoteitukuProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (!((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.MAGISCHES_FEEN_KATANA.get())
				&& !((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get())
				&& !((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == MinecraftArmorWeaponModItems.KATANA_NIGU_HUMERUS.get())) {
			{
				Entity _ent = entity;
				if (!_ent.level.isClientSide() && _ent.getServer() != null) {
					_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), VersionHelper.getLevel(_ent) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(_ent) : null, 4,
							_ent.getName().getString(), _ent.getDisplayName(), _ent.level.getServer(), _ent), "/particle dust 1.000 0.078 0.078 1 ~ ~ ~ 0.5 0.5 0.5 1 0 normal");
				}
			}
		}
	}
}
