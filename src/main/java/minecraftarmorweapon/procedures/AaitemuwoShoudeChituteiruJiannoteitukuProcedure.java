package minecraftarmorweapon.procedures;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import minecraftarmorweapon.network.MinecraftArmorWeaponModVariables;

public class AaitemuwoShoudeChituteiruJiannoteitukuProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 4
				|| (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 6) {
			world.setBlock(new BlockPos((int) x, (int) (y - 1), (int) z), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 1), (int) (y - 1), (int) z), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 1), (int) z), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 1), (int) (y - 1), (int) z), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 1), (int) (z + 1)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 1), (int) (z - 1)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 1), (int) (y - 1), (int) (z - 1)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 1), (int) (y - 1), (int) (z - 1)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 1), (int) (y - 1), (int) (z + 1)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 2), (int) z), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 2), (int) (y - 2), (int) z), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 2), (int) z), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 2), (int) (y - 2), (int) z), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 2), (int) (z + 2)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 2), (int) (z - 2)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 2), (int) (y - 2), (int) (z - 2)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 2), (int) (y - 2), (int) (z - 2)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 2), (int) (y - 2), (int) (z + 2)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 3), (int) z), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 3), (int) (y - 3), (int) z), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 3), (int) z), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 3), (int) (y - 3), (int) z), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 3), (int) (z + 3)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 3), (int) (z - 3)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 3), (int) (y - 3), (int) (z - 3)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 3), (int) (y - 3), (int) (z - 3)), Blocks.WATER.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 3), (int) (y - 3), (int) (z + 3)), Blocks.WATER.defaultBlockState(), 3);
		}
		if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 3
				|| (entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 5) {
			world.setBlock(new BlockPos((int) x, (int) (y - 1), (int) z), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 1), (int) (y - 1), (int) z), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 1), (int) z), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 1), (int) (y - 1), (int) z), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 1), (int) (z + 1)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 1), (int) (z - 1)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 1), (int) (y - 1), (int) (z - 1)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 1), (int) (y - 1), (int) (z - 1)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 1), (int) (y - 1), (int) (z + 1)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 2), (int) z), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 2), (int) (y - 2), (int) z), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 2), (int) z), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 2), (int) (y - 2), (int) z), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 2), (int) (z + 2)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 2), (int) (z - 2)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 2), (int) (y - 2), (int) (z - 2)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 2), (int) (y - 2), (int) (z - 2)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 2), (int) (y - 2), (int) (z + 2)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 3), (int) z), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 3), (int) (y - 3), (int) z), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 3), (int) z), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 3), (int) (y - 3), (int) z), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 3), (int) (z + 3)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 3), (int) (z - 3)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 3), (int) (y - 3), (int) (z - 3)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 3), (int) (y - 3), (int) (z - 3)), Blocks.AIR.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 3), (int) (y - 3), (int) (z + 3)), Blocks.AIR.defaultBlockState(), 3);
		}
		if ((entity.getCapability(MinecraftArmorWeaponModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new MinecraftArmorWeaponModVariables.PlayerVariables())).aaa == 2) {
			world.setBlock(new BlockPos((int) x, (int) (y - 1), (int) z), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 1), (int) (y - 1), (int) z), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 1), (int) z), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 1), (int) (y - 1), (int) z), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 1), (int) (z + 1)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 1), (int) (z - 1)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 1), (int) (y - 1), (int) (z - 1)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 1), (int) (y - 1), (int) (z - 1)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 1), (int) (y - 1), (int) (z + 1)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 2), (int) z), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 2), (int) (y - 2), (int) z), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 2), (int) z), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 2), (int) (y - 2), (int) z), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 2), (int) (z + 2)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 2), (int) (z - 2)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 2), (int) (y - 2), (int) (z - 2)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 2), (int) (y - 2), (int) (z - 2)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 2), (int) (y - 2), (int) (z + 2)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 3), (int) z), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 3), (int) (y - 3), (int) z), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 3), (int) z), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 3), (int) (y - 3), (int) z), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 3), (int) (z + 3)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) x, (int) (y - 3), (int) (z - 3)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x - 3), (int) (y - 3), (int) (z - 3)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 3), (int) (y - 3), (int) (z - 3)), Blocks.DIRT.defaultBlockState(), 3);
			world.setBlock(new BlockPos((int) (x + 3), (int) (y - 3), (int) (z + 3)), Blocks.DIRT.defaultBlockState(), 3);
		}
	}
}
