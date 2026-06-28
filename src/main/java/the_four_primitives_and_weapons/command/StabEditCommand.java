package the_four_primitives_and_weapons.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.entity.StabbedWeaponEntity;
import the_four_primitives_and_weapons.network.OpenStabEditMessage;

/**
 * /stabedit : カーソルを合わせている突き刺さった武器/杭の編集GUIを開く。
 */
@Mod.EventBusSubscriber
public class StabEditCommand {

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		dispatcher.register(Commands.literal("stabedit")
				.requires(s -> s.hasPermission(0))
				.executes(ctx -> open(ctx.getSource())));
	}

	private static int open(CommandSourceStack source) {
		if (!(source.getEntity() instanceof ServerPlayer player)) {
			source.sendFailure(Component.literal("§cプレイヤーから実行してください"));
			return 0;
		}
		double reach = 6.0;
		Vec3 eye = player.getEyePosition(1.0f);
		Vec3 look = player.getViewVector(1.0f);
		// OBB ( カプセル ) 精密判定で、 視線が実際に武器に当たっているものを選ぶ
		AABB search = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(2.0);
		StabbedWeaponEntity s = null;
		double bestT = Double.MAX_VALUE;
		for (StabbedWeaponEntity cand : player.level().getEntitiesOfClass(StabbedWeaponEntity.class, search)) {
			double t = cand.clipWeapon(eye, look, reach);
			if (t >= 0 && t < bestT) { bestT = t; s = cand; }
		}
		if (s == null) {
			source.sendFailure(Component.literal("§c編集する刺さった武器/杭にカーソルを合わせてください"));
			return 0;
		}
		TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.send(
				PacketDistributor.PLAYER.with(() -> player), new OpenStabEditMessage(s.getId()));
		return 1;
	}
}
