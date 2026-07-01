package the_four_primitives_and_weapons.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.util.KatanaFittings;
import the_four_primitives_and_weapons.util.SayaDesign;

/**
 * 拵え/鞘の色を 16進数(#RRGGBB) で細かく設定するコマンド。 手に持ったアイテムに適用。
 *
 * <pre>
 *   /tfpwcolor tsuka  #1b3a5c   … 柄(紐)の色
 *   /tfpwcolor tsuba  #c8b34a   … 鍔
 *   /tfpwcolor fuchi  #c8b34a   … 縁
 *   /tfpwcolor kashira #c8b34a  … 頭
 *   /tfpwcolor saya   #7a1f1f   … 鞘の地色
 *   /tfpwcolor <部位> clear      … その色を消す ( 既定に戻す )
 * </pre>
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public final class FittingColorCommand {

	private static final String[] PARTS = { "tsuka", "tsuba", "kashira", "saya" };

	@SubscribeEvent
	public static void onRegister(RegisterCommandsEvent event) {
		LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal("tfpwcolor")
				.requires(s -> s.hasPermission(0))
				.then(Commands.argument("part", StringArgumentType.word())
						.suggests((ctx, sb) -> SharedSuggestionProvider.suggest(PARTS, sb))
						.then(Commands.argument("hex", StringArgumentType.word())
								.suggests((ctx, sb) -> SharedSuggestionProvider.suggest(
										new String[]{ "#000000", "#ffffff", "#1b3a5c", "#c8b34a", "#7a1f1f", "clear" }, sb))
								.executes(FittingColorCommand::run)));
		event.getDispatcher().register(cmd);
	}

	private static int run(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		ServerPlayer player;
		try {
			player = src.getPlayerOrException();
		} catch (Exception e) {
			src.sendFailure(Component.literal("プレイヤーが必要です"));
			return 0;
		}
		String part = StringArgumentType.getString(ctx, "part").toLowerCase();
		String hex = StringArgumentType.getString(ctx, "hex").trim();
		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty()) {
			src.sendFailure(Component.literal("手にアイテムを持ってください"));
			return 0;
		}

		boolean clear = hex.equalsIgnoreCase("clear") || hex.equalsIgnoreCase("none");
		int rgb = -1;
		if (!clear) {
			String h = hex.startsWith("#") ? hex.substring(1) : hex;
			try {
				rgb = Integer.parseInt(h, 16) & 0xFFFFFF;
			} catch (NumberFormatException e) {
				src.sendFailure(Component.literal("色は #RRGGBB 形式で ( 例 #1b3a5c )"));
				return 0;
			}
		}

		boolean ok;
		switch (part) {
			case "saya":    ok = applySaya(stack, rgb, clear); break;
			case "tsuka":   ok = applyKatana(stack, "tsuka", rgb, clear); break;
			case "tsuba":   ok = applyKatana(stack, "tsuba", rgb, clear); break;
			case "kashira": ok = applyKatana(stack, "kashira", rgb, clear); break;
			default:
				src.sendFailure(Component.literal("部位: tsuka / tsuba / kashira / saya"));
				return 0;
		}
		if (!ok) {
			src.sendFailure(Component.literal("そのアイテムには " + part + " を設定できません"));
			return 0;
		}
		final int fr = rgb;
		src.sendSuccess(() -> Component.literal(clear ? (part + " の色を消しました")
				: (part + " を " + String.format("#%06X", fr) + " にしました")), false);
		return 1;
	}

	private static boolean applySaya(ItemStack s, int rgb, boolean clear) {
		if (!SayaDesign.isSaya(s)) return false;
		if (clear) { if (s.hasTag()) s.getTag().remove(SayaDesign.BASE_KEY); }
		else SayaDesign.setBaseColorRgb(s, rgb);
		return true;
	}

	private static boolean applyKatana(ItemStack s, String part, int rgb, boolean clear) {
		if (!KatanaFittings.isFittingWeapon(s)) return false;
		String key = switch (part) {
			case "tsuba" -> KatanaFittings.TSUBA_KEY;
			case "fuchi" -> KatanaFittings.FUCHI_KEY;
			case "kashira" -> KatanaFittings.KASHIRA_KEY;
			default -> KatanaFittings.TSUKA_KEY;
		};
		if (clear) { if (s.hasTag()) s.getTag().remove(key); }
		else s.getOrCreateTag().putInt(key, rgb & 0xFFFFFF);
		return true;
	}
}
