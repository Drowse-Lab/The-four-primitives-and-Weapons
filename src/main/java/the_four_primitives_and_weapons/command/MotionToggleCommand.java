package the_four_primitives_and_weapons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.skill.PlayerSkillData;
import the_four_primitives_and_weapons.skill.SkillRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * /motion toggle <motion_id>   — その技 (motion) の ON/OFF を切替
 * /motion enable <motion_id>   — 強制 ON
 * /motion disable <motion_id>  — 強制 OFF
 * /motion list                  — 現在 OFF の技一覧
 *
 * 技を OFF にすると、 該当 motion を発動する handler が「無効化」されているのを検知して
 * 通常攻撃などにフォールバックする。
 */
@Mod.EventBusSubscriber
public class MotionToggleCommand {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("motion")
            .requires(s -> s.hasPermission(0)) // 全プレイヤー可

            .then(Commands.literal("toggle")
                .then(Commands.argument("motion_id", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(allMotionIds(), b))
                    .executes(ctx -> setMotion(ctx.getSource(),
                            StringArgumentType.getString(ctx, "motion_id"), null))))

            .then(Commands.literal("enable")
                .then(Commands.argument("motion_id", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(allMotionIds(), b))
                    .executes(ctx -> setMotion(ctx.getSource(),
                            StringArgumentType.getString(ctx, "motion_id"), Boolean.TRUE))))

            .then(Commands.literal("disable")
                .then(Commands.argument("motion_id", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(allMotionIds(), b))
                    .executes(ctx -> setMotion(ctx.getSource(),
                            StringArgumentType.getString(ctx, "motion_id"), Boolean.FALSE))))

            .then(Commands.literal("list")
                .executes(ctx -> listDisabled(ctx.getSource())))

            .then(Commands.literal("why")
                .executes(ctx -> explainMotions(ctx.getSource())))
        );
    }

    /**
     * /motion why — 手に持っている武器について、 各スロットの技が
     * 「どの層から」来ているかを表示する。
     *
     * <p>技設定は 武器NBT &gt; 武器スロット &gt; タイプ別 &gt; JSON既定 &gt; グローバル既定 の
     * 5 層で解決される ( {@link PlayerSkillData.SkillStorage#getMotionForWeapon} )。
     * 上位に古い設定が残っていると、 スキル画面で下位の層を選び直しても何も起きない。
     * どの層が勝っているかはゲーム中から見えないため、 切り分け用に出す。</p>
     */
    private static int explainMotions(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cこのコマンドはプレイヤー専用"));
            return 0;
        }
        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            source.sendFailure(Component.literal("§c武器を手に持ってから実行してください"));
            return 0;
        }

        PlayerSkillData.SkillStorage sd = PlayerSkillData.getSkillData(player);
        if (sd == null) {
            source.sendFailure(Component.literal("§cスキルデータを取得できませんでした"));
            return 0;
        }

        the_four_primitives_and_weapons.skill.WeaponTypeRegistry.WeaponTypeData type =
                the_four_primitives_and_weapons.skill.WeaponTypeRegistry.getTypeForItem(held);
        source.sendSuccess(() -> Component.literal("§6" + held.getHoverName().getString()
                + " §7/ タイプ: §e" + (type != null ? type.getId() : "未登録")), false);

        for (PlayerSkillData.AttackSlot slot : PlayerSkillData.AttackSlot.values()) {
            String actual = sd.getMotionForWeapon(slot, held);
            String origin;

            String nbt = the_four_primitives_and_weapons.skill.WeaponSkillNBT.getMotion(held, slot);
            String loadoutMotion = null;
            for (int i = 0; i < PlayerSkillData.MAX_WEAPON_SLOTS; i++) {
                PlayerSkillData.WeaponLoadout lo = sd.getLoadoutAt(i);
                if (lo != null && lo.matchesItem(held) && lo.hasMotion(slot)) {
                    loadoutMotion = lo.getMotion(slot);
                    break;
                }
            }
            String typeMotion = (type != null) ? sd.getRawTypeMotion(type.getId(), slot) : null;
            String jsonDefault = (type != null) ? type.getDefaultMotion(slot) : null;

            if (nbt != null) origin = "§c武器NBT §7(優先度0)";
            else if (loadoutMotion != null) origin = "§e武器スロット §7(優先度1)";
            else if (typeMotion != null) origin = "§aタイプ別 §7(優先度2)";
            else if (jsonDefault != null) origin = "§bJSON既定 §7(優先度3)";
            else origin = "§7グローバル既定 (優先度4)";

            source.sendSuccess(() -> Component.literal(
                    "  §f" + slot.getId() + "§7: §f" + actual + " §7← " + origin), false);
        }
        source.sendSuccess(() -> Component.literal(
                "§7※ 上位の層が出ている場合、 スキル画面で下位のタブを選び直しても反映されません"), false);
        return 1;
    }

    private static List<String> allMotionIds() {
        List<String> ids = new ArrayList<>();
        try {
            SkillRegistry.getAllMotions().forEach(m -> ids.add(m.getId()));
        } catch (Throwable ignored) {}
        return ids;
    }

    /** force = null → トグル / true → ON / false → OFF */
    private static int setMotion(CommandSourceStack source, String motionId, Boolean force) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cこのコマンドはプレイヤー専用"));
            return 0;
        }
        boolean currentlyEnabled = PlayerSkillData.isMotionEnabled(player, motionId);
        boolean target = (force != null) ? force : !currentlyEnabled;
        PlayerSkillData.setMotionEnabled(player, motionId, target);

        final String label = target ? "§a有効" : "§c無効";
        source.sendSuccess(() -> Component.literal(
                "§7技 §e" + motionId + "§7 を " + label + " §7にしました"), false);
        return 1;
    }

    private static int listDisabled(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cこのコマンドはプレイヤー専用"));
            return 0;
        }
        List<String> disabled = new ArrayList<>();
        for (String id : allMotionIds()) {
            if (!PlayerSkillData.isMotionEnabled(player, id)) disabled.add(id);
        }
        if (disabled.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7無効化されている技はありません"), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "§7無効化中 (" + disabled.size() + "): §c" + String.join(", ", disabled)), false);
        }
        return 1;
    }
}
