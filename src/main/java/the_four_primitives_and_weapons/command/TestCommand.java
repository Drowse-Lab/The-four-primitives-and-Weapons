package the_four_primitives_and_weapons.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import the_four_primitives_and_weapons.client.renderer.GateProjectileRenderer;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.damage.IElementalDamageSource;
import the_four_primitives_and_weapons.trait.MobTrait;
import the_four_primitives_and_weapons.trait.MobTraitHandler;
import the_four_primitives_and_weapons.ai.lisp.CombatLogger;
import the_four_primitives_and_weapons.ai.lisp.CombatLogAnalyzer;
import the_four_primitives_and_weapons.ai.lisp.ProgressionTracker;
import the_four_primitives_and_weapons.event.MoonPhaseDifficulty;
import the_four_primitives_and_weapons.command.CustomDifficultyCommand.CustomDifficulty;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /test — MODのテスト用コマンド集
 *
 * サブコマンド:
 *   /test trait <trait>              — 全特性のゾンビをスポーン
 *   /test traitall                   — 全特性のゾンビを一括スポーン
 *   /test element <element> [level]  — 手持ち武器に属性を付与
 *   /test elementall [level]         — 各属性の剣を全部入手
 *   /test debugmob                   — デバッグMobをスポーン
 *   /test heal                       — 自分を全回復
 *   /test god                        — 無敵モード切替
 *   /test difficulty <name>          — 難易度を即変更
 *   /test info                       — 現在のMOD設定を表示
 *   /test clear                      — 周囲のMobを全削除
 *   /test log [on|off|status|tail|path|clear] — 戦闘ログの制御/閲覧
 */
@Mod.EventBusSubscriber
public class TestCommand {

    private static final List<String> TRAIT_NAMES = Arrays.stream(MobTrait.values())
        .map(t -> t.name().toLowerCase()).collect(Collectors.toList());

    private static final List<String> ELEMENT_NAMES = Arrays.stream(ElementType.values())
        .filter(e -> e != ElementType.NONE && e != ElementType.ERROR)
        .map(ElementType::getName).collect(Collectors.toList());

    private static final List<String> DIFF_NAMES = Arrays.stream(CustomDifficulty.values())
        .map(CustomDifficulty::getName).collect(Collectors.toList());

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("test")
            .requires(source -> source.hasPermission(2))

            // /test trait <trait>
            .then(Commands.literal("trait")
                .then(Commands.argument("trait", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(TRAIT_NAMES, b))
                    .executes(ctx -> spawnTraitZombie(ctx.getSource(),
                        StringArgumentType.getString(ctx, "trait")))
                )
            )

            // /test traitall
            .then(Commands.literal("traitall")
                .executes(ctx -> spawnAllTraits(ctx.getSource()))
            )

            // /test element <element> [level]
            .then(Commands.literal("element")
                .then(Commands.argument("element", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ELEMENT_NAMES, b))
                    .executes(ctx -> setWeaponElement(ctx.getSource(),
                        StringArgumentType.getString(ctx, "element"), 1))
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> setWeaponElement(ctx.getSource(),
                            StringArgumentType.getString(ctx, "element"),
                            IntegerArgumentType.getInteger(ctx, "level")))
                    )
                )
            )

            // /test elementall [level] [item]
            //   item を省略すると minecraft:diamond_sword。 item には mod 武器の id も指定可能。
            //   例: /test elementall 10 the_four_primitives_and_weapons:iron_katana
            .then(Commands.literal("elementall")
                .executes(ctx -> giveAllElementSwords(ctx.getSource(), 5, null))
                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                    .executes(ctx -> giveAllElementSwords(ctx.getSource(),
                        IntegerArgumentType.getInteger(ctx, "level"), null))
                    .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                        .executes(ctx -> giveAllElementSwords(ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "level"),
                            ItemArgument.getItem(ctx, "item")))
                    )
                )
            )

            // /test debugmob
            .then(Commands.literal("debugmob")
                .executes(ctx -> spawnDebugMob(ctx.getSource()))
            )

            // /test heal
            .then(Commands.literal("heal")
                .executes(ctx -> healSelf(ctx.getSource()))
            )

            // /test god
            .then(Commands.literal("god")
                .executes(ctx -> toggleGod(ctx.getSource()))
            )

            // /test difficulty <name>
            .then(Commands.literal("difficulty")
                .then(Commands.argument("name", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(DIFF_NAMES, b))
                    .executes(ctx -> setDifficulty(ctx.getSource(),
                        StringArgumentType.getString(ctx, "name")))
                )
            )

            // /test info
            .then(Commands.literal("info")
                .executes(ctx -> showInfo(ctx.getSource()))
            )

            // /test clear [radius]
            .then(Commands.literal("clear")
                .executes(ctx -> clearMobs(ctx.getSource(), 50))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 200))
                    .executes(ctx -> clearMobs(ctx.getSource(),
                        IntegerArgumentType.getInteger(ctx, "radius")))
                )
            )

            // /test cleanblocks [radius] — Mob設置のcobblestone/cobwebを強制削除
            .then(Commands.literal("cleanblocks")
                .executes(ctx -> cleanBlocks(ctx.getSource(), 30))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100))
                    .executes(ctx -> cleanBlocks(ctx.getSource(),
                        IntegerArgumentType.getInteger(ctx, "radius")))
                )
            )

            // /test damage <amount> <element> [level] — 最寄りのMobに属性ダメージ
            .then(Commands.literal("damage")
                .then(Commands.argument("amount", FloatArgumentType.floatArg(0.1f))
                    .then(Commands.argument("element", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ELEMENT_NAMES, b))
                        .executes(ctx -> testDamage(ctx.getSource(),
                            FloatArgumentType.getFloat(ctx, "amount"),
                            StringArgumentType.getString(ctx, "element"), 1))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                            .executes(ctx -> testDamage(ctx.getSource(),
                                FloatArgumentType.getFloat(ctx, "amount"),
                                StringArgumentType.getString(ctx, "element"),
                                IntegerArgumentType.getInteger(ctx, "level")))
                        )
                    )
                )
            )

            // /test damageall <amount> [level] — デバッグMobに全属性ダメージを順番に
            .then(Commands.literal("damageall")
                .then(Commands.argument("amount", FloatArgumentType.floatArg(0.1f))
                    .executes(ctx -> testDamageAll(ctx.getSource(),
                        FloatArgumentType.getFloat(ctx, "amount"), 1))
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> testDamageAll(ctx.getSource(),
                            FloatArgumentType.getFloat(ctx, "amount"),
                            IntegerArgumentType.getInteger(ctx, "level")))
                    )
                )
            )

            // /test gaterot <yaw> <pitch> <roll> <sx> <sy> <sz> — Gate剣の向きをリアルタイム調整
            .then(Commands.literal("gaterot")
                .then(Commands.argument("yaw", FloatArgumentType.floatArg())
                    .then(Commands.argument("pitch", FloatArgumentType.floatArg())
                        .then(Commands.argument("roll", FloatArgumentType.floatArg())
                            .executes(ctx -> setGateRot(ctx.getSource(),
                                FloatArgumentType.getFloat(ctx, "yaw"),
                                FloatArgumentType.getFloat(ctx, "pitch"),
                                FloatArgumentType.getFloat(ctx, "roll"), 0.8f, 0.8f, 0.8f))
                            .then(Commands.argument("sx", FloatArgumentType.floatArg(0.1f, 3.0f))
                                .then(Commands.argument("sy", FloatArgumentType.floatArg(0.1f, 3.0f))
                                    .then(Commands.argument("sz", FloatArgumentType.floatArg(0.1f, 3.0f))
                                        .executes(ctx -> setGateRot(ctx.getSource(),
                                            FloatArgumentType.getFloat(ctx, "yaw"),
                                            FloatArgumentType.getFloat(ctx, "pitch"),
                                            FloatArgumentType.getFloat(ctx, "roll"),
                                            FloatArgumentType.getFloat(ctx, "sx"),
                                            FloatArgumentType.getFloat(ctx, "sy"),
                                            FloatArgumentType.getFloat(ctx, "sz")))
                                    )
                                )
                            )
                        )
                    )
                )
                .executes(ctx -> showGateRot(ctx.getSource()))
            )

            // /test log [on|off|status|tail|clear|path]
            //   戦闘ログ (logs/combat_ai/combat_events_YYYY-MM-DD.jsonl) を制御
            .then(Commands.literal("log")
                .executes(ctx -> logStatus(ctx.getSource()))
                .then(Commands.literal("on")
                    .executes(ctx -> setLogEnabled(ctx.getSource(), true)))
                .then(Commands.literal("off")
                    .executes(ctx -> setLogEnabled(ctx.getSource(), false)))
                .then(Commands.literal("status")
                    .executes(ctx -> logStatus(ctx.getSource())))
                .then(Commands.literal("path")
                    .executes(ctx -> logPath(ctx.getSource())))
                .then(Commands.literal("clear")
                    .executes(ctx -> logClear(ctx.getSource())))
                .then(Commands.literal("tail")
                    .executes(ctx -> logTail(ctx.getSource(), 10))
                    .then(Commands.argument("n", IntegerArgumentType.integer(1, 200))
                        .executes(ctx -> logTail(ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "n")))))
                .then(Commands.literal("analyze")
                    .executes(ctx -> logAnalyze(ctx.getSource(), 5))
                    .then(Commands.argument("topN", IntegerArgumentType.integer(1, 20))
                        .executes(ctx -> logAnalyze(ctx.getSource(),
                            IntegerArgumentType.getInteger(ctx, "topN")))))
                .then(Commands.literal("refresh")
                    .executes(ctx -> logRefresh(ctx.getSource())))
            )

            // /test dps <element> <level> <seconds> — DPSテスト（秒数分連続ダメージ）
            .then(Commands.literal("dps")
                .then(Commands.argument("element", StringArgumentType.word())
                    .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ELEMENT_NAMES, b))
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 30))
                            .executes(ctx -> testDps(ctx.getSource(),
                                StringArgumentType.getString(ctx, "element"),
                                IntegerArgumentType.getInteger(ctx, "level"),
                                IntegerArgumentType.getInteger(ctx, "seconds")))
                        )
                    )
                )
            )
        );
    }

    // === /test trait <trait> ===
    private static int spawnTraitZombie(CommandSourceStack source, String traitName) {
        MobTrait trait = null;
        for (MobTrait t : MobTrait.values()) {
            if (t.name().equalsIgnoreCase(traitName) || t.getDisplayName().equals(traitName)) {
                trait = t;
                break;
            }
        }
        if (trait == null) {
            source.sendFailure(Component.literal("§c不明な特性: " + traitName));
            return 0;
        }

        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Entity entity = EntityType.ZOMBIE.spawn(level, (net.minecraft.nbt.CompoundTag) null, null, pos, MobSpawnType.COMMAND, true, false);
        if (entity instanceof Monster monster) {
            MobTraitHandler.applyTraitToMob(monster, trait);
            final MobTrait finalTrait = trait;
            source.sendSuccess(() -> Component.literal(
                "§a特性 " + finalTrait.getFormattedName() + " §aのゾンビをスポーンしました"), false);
        }
        return 1;
    }

    // === /test traitall ===
    private static int spawnAllTraits(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        int i = 0;
        for (MobTrait trait : MobTrait.values()) {
            // 円形に配置
            double angle = (2.0 * Math.PI * i) / MobTrait.values().length;
            int x = center.getX() + (int)(5.0 * Math.cos(angle));
            int z = center.getZ() + (int)(5.0 * Math.sin(angle));
            BlockPos spawnPos = new BlockPos(x, center.getY(), z);

            Entity entity = EntityType.ZOMBIE.spawn(level, (net.minecraft.nbt.CompoundTag) null, null, spawnPos, MobSpawnType.COMMAND, true, false);
            if (entity instanceof Monster monster) {
                MobTraitHandler.applyTraitToMob(monster, trait);
            }
            i++;
        }
        source.sendSuccess(() -> Component.literal(
            "§a全" + MobTrait.values().length + "種の特性ゾンビをスポーンしました"), false);
        return MobTrait.values().length;
    }

    // === /test element <element> [level] ===
    private static int setWeaponElement(CommandSourceStack source, String elementName, int lvl) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cプレイヤー専用コマンドです"));
            return 0;
        }
        ElementType element = ElementType.fromString(elementName);
        if (element == ElementType.NONE) {
            source.sendFailure(Component.literal("§c不明な属性: " + elementName));
            return 0;
        }
        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) {
            source.sendFailure(Component.literal("§c手にアイテムを持ってください"));
            return 0;
        }
        ElementalDamageUtils.setElement(weapon, element, lvl);
        source.sendSuccess(() -> Component.literal(
            "§a武器に §6" + element.getName().toUpperCase() + " Lv." + lvl + " §aを付与しました"), false);
        return 1;
    }

    // === /test elementall [level] [item] ===
    //   itemInput が null の場合は minecraft:diamond_sword をベースに使う。
    //   ItemInput を渡すと、 その item の ItemStack ( nbt 含む ) を各属性版で複製する。
    private static int giveAllElementSwords(CommandSourceStack source, int lvl, ItemInput itemInput) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cプレイヤー専用コマンドです"));
            return 0;
        }
        int count = 0;
        String baseName;
        for (ElementType elem : ElementType.values()) {
            if (elem == ElementType.NONE || elem == ElementType.ERROR) continue;
            ItemStack stack;
            try {
                stack = (itemInput != null)
                        ? itemInput.createItemStack(1, false)
                        : new ItemStack(Items.DIAMOND_SWORD);
            } catch (Exception e) {
                source.sendFailure(Component.literal("§citem 生成失敗: " + e.getMessage()));
                return 0;
            }
            ElementalDamageUtils.setElement(stack, elem, lvl);
            stack.setHoverName(Component.literal("§6" + elem.getName().toUpperCase() + "§f " + stack.getItem().getDescription().getString()));
            player.addItem(stack);
            count++;
        }
        baseName = (itemInput != null)
                ? itemInput.getItem().toString()
                : "minecraft:diamond_sword";
        final int total = count;
        final String fBase = baseName;
        source.sendSuccess(() -> Component.literal(
            "§a全" + total + "属性の §e" + fBase + " §aを付与しました (Lv." + lvl + ")"), false);
        return count;
    }

    // === /test debugmob ===
    private static int spawnDebugMob(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        ResourceLocation debugMobId = new ResourceLocation("the_four_primitives_and_weapons", "debug_mob_spawn_egg");
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(debugMobId);
        if (type != null) {
            type.spawn(level, (net.minecraft.nbt.CompoundTag) null, null, pos, MobSpawnType.COMMAND, true, false);
            source.sendSuccess(() -> Component.literal("§aデバッグMobをスポーンしました"), false);
            return 1;
        }
        source.sendFailure(Component.literal("§cデバッグMobが見つかりません"));
        return 0;
    }

    // === /test heal ===
    private static int healSelf(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cプレイヤー専用コマンドです"));
            return 0;
        }
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0f);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 200, 1));
        source.sendSuccess(() -> Component.literal("§a全回復しました"), false);
        return 1;
    }

    // === /test god ===
    private static int toggleGod(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cプレイヤー専用コマンドです"));
            return 0;
        }
        boolean invulnerable = !player.isInvulnerable();
        player.setInvulnerable(invulnerable);
        if (invulnerable) {
            source.sendSuccess(() -> Component.literal("§6無敵モード: §aON"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§6無敵モード: §cOFF"), false);
        }
        return 1;
    }

    // === /test difficulty <name> ===
    private static int setDifficulty(CommandSourceStack source, String name) {
        CustomDifficulty diff = CustomDifficulty.byName(name);
        CustomDifficultyCommand.setCurrentDifficulty(diff);
        source.sendSuccess(() -> Component.literal("§a難易度を §e" + diff.getName() + " §aに変更しました"), false);
        return 1;
    }

    // === /test info ===
    private static int showInfo(CommandSourceStack source) {
        CustomDifficulty diff = CustomDifficultyCommand.getCurrentDifficulty();
        source.sendSuccess(() -> Component.literal("§6=== MOD設定 ==="), false);
        source.sendSuccess(() -> Component.literal("§f難易度: §e" + diff.getName()), false);
        source.sendSuccess(() -> Component.literal("§fAIレベル: §e" + diff.getAiLevel()), false);
        source.sendSuccess(() -> Component.literal("§f特性確率: §e" + (int)(diff.getTraitChance() * 100) + "%"), false);
        source.sendSuccess(() -> Component.literal("§fエリート確率: §e" + (int)(diff.getEliteSpawnChance() * 100) + "%"), false);
        source.sendSuccess(() -> Component.literal("§fバフ確率: §e" + (int)(diff.getBuffEffectChance() * 100) + "%"), false);
        source.sendSuccess(() -> Component.literal(
            "§fブロック設置/破壊: §e" + diff.isBlockPlaceEnabled() + "/" + diff.isBlockBreakEnabled()), false);
        source.sendSuccess(() -> Component.literal(
            "§fTrueCrafter: §e" + CustomDifficultyCommand.isTrueCrafterEnabled()), false);
        source.sendSuccess(() -> Component.literal(
            "§f進行度: §e" + ProgressionTracker.describe()), false);
        // 月相情報
        if (source.getLevel() != null) {
            int phase = MoonPhaseDifficulty.getMoonPhase(source.getLevel());
            int bonus = MoonPhaseDifficulty.getDifficultyBonus(source.getLevel());
            source.sendSuccess(() -> Component.literal(
                "§f月相: " + MoonPhaseDifficulty.getPhaseName(phase)
                + " §f(難易度補正 §c+" + bonus + "§f)"), false);
        }
        return 1;
    }

    // === /test damage <amount> <element> [level] ===
    private static int testDamage(CommandSourceStack source, float amount, String elementName, int lvl) {
        ElementType element = ElementType.fromString(elementName);
        if (element == ElementType.NONE) {
            source.sendFailure(Component.literal("§c不明な属性: " + elementName));
            return 0;
        }
        // 最寄りのLivingEntityを取得（プレイヤー以外）
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
            new net.minecraft.world.phys.AABB(
                center.getX() - 20, center.getY() - 10, center.getZ() - 20,
                center.getX() + 20, center.getY() + 10, center.getZ() + 20),
            e -> !(e instanceof ServerPlayer));
        if (nearby.isEmpty()) {
            source.sendFailure(Component.literal("§c近くにMobがいません（半径20）"));
            return 0;
        }
        // 最も近いエンティティ
        LivingEntity target = nearby.stream()
            .min((a, b) -> Double.compare(
                a.distanceToSqr(source.getPosition()), b.distanceToSqr(source.getPosition())))
            .get();

        net.minecraft.world.damagesource.DamageSource ds = target.damageSources().magic();
        if (ds instanceof IElementalDamageSource elemSource) {
            elemSource.setElementType(element);
            elemSource.setElementLevel(lvl);
        }
        target.hurt(ds, amount);
        source.sendSuccess(() -> Component.literal(
            String.format("§a%s に §6%s §aLv.%d で §c%.1f §aダメージ",
                target.getName().getString(), element.getName().toUpperCase(), lvl, amount)), false);
        return 1;
    }

    // === /test damageall <amount> [level] ===
    private static int testDamageAll(CommandSourceStack source, float amount, int lvl) {
        // 最寄りのLivingEntity（プレイヤー以外）に全属性ダメージを順番に
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
            new net.minecraft.world.phys.AABB(
                center.getX() - 20, center.getY() - 10, center.getZ() - 20,
                center.getX() + 20, center.getY() + 10, center.getZ() + 20),
            e -> !(e instanceof ServerPlayer));
        if (nearby.isEmpty()) {
            source.sendFailure(Component.literal("§c近くにMobがいません（半径20）"));
            return 0;
        }
        LivingEntity target = nearby.stream()
            .min((a, b) -> Double.compare(
                a.distanceToSqr(source.getPosition()), b.distanceToSqr(source.getPosition())))
            .get();

        int count = 0;
        for (ElementType elem : ElementType.values()) {
            if (elem == ElementType.NONE || elem == ElementType.ERROR) continue;
            net.minecraft.world.damagesource.DamageSource ds = target.damageSources().magic();
            if (ds instanceof IElementalDamageSource elemSource) {
                elemSource.setElementType(elem);
                elemSource.setElementLevel(lvl);
            }
            target.hurt(ds, amount);
            source.sendSuccess(() -> Component.literal(
                String.format("  §6%s §fLv.%d §c%.1fダメージ", elem.getName().toUpperCase(), lvl, amount)), false);
            count++;
        }
        final int total = count;
        source.sendSuccess(() -> Component.literal(
            "§a全" + total + "属性のダメージテスト完了"), false);
        return count;
    }

    // === /test dps <element> <level> <seconds> ===
    private static int testDps(CommandSourceStack source, String elementName, int lvl, int seconds) {
        ElementType element = ElementType.fromString(elementName);
        if (element == ElementType.NONE) {
            source.sendFailure(Component.literal("§c不明な属性: " + elementName));
            return 0;
        }
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
            new net.minecraft.world.phys.AABB(
                center.getX() - 20, center.getY() - 10, center.getZ() - 20,
                center.getX() + 20, center.getY() + 10, center.getZ() + 20),
            e -> !(e instanceof ServerPlayer));
        if (nearby.isEmpty()) {
            source.sendFailure(Component.literal("§c近くにMobがいません（半径20）"));
            return 0;
        }
        LivingEntity target = nearby.stream()
            .min((a, b) -> Double.compare(
                a.distanceToSqr(source.getPosition()), b.distanceToSqr(source.getPosition())))
            .get();

        float hpBefore = target.getHealth();
        int ticks = seconds * 20;
        // 毎tick1ダメージを予約（サーバーティックでスケジュール）
        for (int i = 0; i < ticks; i++) {
            final int delay = i;
            level.getServer().execute(() -> {
                level.getServer().execute(new java.util.TimerTask() {
                    // 使えないのでシンプルに即時実行
                    public void run() {}
                });
            });
        }
        // 簡易版: 即時で全ダメージを与えてDPSを計算表示
        float totalDmg = 0;
        for (int i = 0; i < seconds * 4; i++) { // 0.25秒間隔で計算
            net.minecraft.world.damagesource.DamageSource ds = target.damageSources().magic();
            if (ds instanceof IElementalDamageSource elemSource) {
                elemSource.setElementType(element);
                elemSource.setElementLevel(lvl);
            }
            float dmg = 2.0f * lvl; // 基礎ダメージ
            target.hurt(ds, dmg);
            totalDmg += dmg;
        }
        float hpAfter = target.getHealth();
        float actualDmg = hpBefore - hpAfter;
        float dps = totalDmg / seconds;

        final float fTotal = totalDmg;
        final float fDps = dps;
        final float fActual = actualDmg;
        source.sendSuccess(() -> Component.literal("§6=== DPSテスト結果 ==="), false);
        source.sendSuccess(() -> Component.literal(
            String.format("§f属性: §6%s §fLv.%d §f期間: %d秒", element.getName().toUpperCase(), lvl, seconds)), false);
        source.sendSuccess(() -> Component.literal(
            String.format("§f合計ダメージ: §c%.1f §f実ダメージ: §c%.1f", fTotal, fActual)), false);
        source.sendSuccess(() -> Component.literal(
            String.format("§fDPS: §c%.1f/秒", fDps)), false);
        return 1;
    }

    // === /test gaterot ===
    private static int showGateRot(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§6=== Gate回転パラメータ ==="), false);
        source.sendSuccess(() -> Component.literal(String.format(
            "§f  YAW=§e%.1f §fPITCH=§e%.1f §fROLL=§e%.1f",
            GateProjectileRenderer.YAW_OFFSET,
            GateProjectileRenderer.PITCH_OFFSET,
            GateProjectileRenderer.ROLL_OFFSET)), false);
        source.sendSuccess(() -> Component.literal(String.format(
            "§f  SCALE X=§e%.2f §fY=§e%.2f §fZ=§e%.2f",
            GateProjectileRenderer.SCALE_X,
            GateProjectileRenderer.SCALE_Y,
            GateProjectileRenderer.SCALE_Z)), false);
        source.sendSuccess(() -> Component.literal("§7変更: /test gaterot <yaw> <pitch> <roll> <sx> <sy> <sz>"), false);
        return 1;
    }

    private static int setGateRot(CommandSourceStack source, float yaw, float pitch, float roll, float sx, float sy, float sz) {
        GateProjectileRenderer.YAW_OFFSET = yaw;
        GateProjectileRenderer.PITCH_OFFSET = pitch;
        GateProjectileRenderer.ROLL_OFFSET = roll;
        GateProjectileRenderer.SCALE_X = sx;
        GateProjectileRenderer.SCALE_Y = sy;
        GateProjectileRenderer.SCALE_Z = sz;
        source.sendSuccess(() -> Component.literal(String.format(
            "§aGate回転を更新: §fYAW=§e%.1f §fPITCH=§e%.1f §fROLL=§e%.1f §fS=§e%.2f/%.2f/%.2f",
            yaw, pitch, roll, sx, sy, sz)), false);
        return 1;
    }

    // === /test log ... ===

    private static int setLogEnabled(CommandSourceStack source, boolean on) {
        CombatLogger.setEnabled(on);
        source.sendSuccess(() -> Component.literal(
            on ? "§a戦闘ログ: §e有効 §7(logs/combat_ai/ に出力)"
               : "§a戦闘ログ: §c無効"), false);
        return 1;
    }

    private static int logStatus(CommandSourceStack source) {
        java.nio.file.Path file = CombatLogger.getTodayEventFile();
        boolean exists = java.nio.file.Files.exists(file);
        long size = 0;
        long lines = 0;
        if (exists) {
            try {
                size = java.nio.file.Files.size(file);
                try (java.util.stream.Stream<String> s = java.nio.file.Files.lines(file)) {
                    lines = s.count();
                }
            } catch (Exception ignored) {}
        }
        final long fSize = size;
        final long fLines = lines;
        final boolean fExists = exists;
        source.sendSuccess(() -> Component.literal("§6=== 戦闘ログ状態 ==="), false);
        source.sendSuccess(() -> Component.literal(
            "§f有効: " + (CombatLogger.isEnabled() ? "§aON" : "§cOFF")), false);
        source.sendSuccess(() -> Component.literal(
            "§f今日のファイル: §e" + file.getFileName()), false);
        source.sendSuccess(() -> Component.literal(
            "§f存在: " + (fExists ? "§a○" : "§c×")
            + " §fサイズ: §e" + fSize + "B §fイベント数: §e" + fLines), false);
        source.sendSuccess(() -> Component.literal(
            "§7/test log tail [n] で最新ログを表示"), false);
        return 1;
    }

    private static int logPath(CommandSourceStack source) {
        java.nio.file.Path dir = CombatLogger.getLogDirectory();
        source.sendSuccess(() -> Component.literal(
            "§6ログディレクトリ: §e" + dir.toAbsolutePath()), false);
        source.sendSuccess(() -> Component.literal(
            "§6今日のファイル: §e" + CombatLogger.getTodayEventFile().toAbsolutePath()), false);
        return 1;
    }

    private static int logClear(CommandSourceStack source) {
        java.nio.file.Path file = CombatLogger.getTodayEventFile();
        try {
            boolean deleted = java.nio.file.Files.deleteIfExists(file);
            source.sendSuccess(() -> Component.literal(
                deleted ? "§a今日の戦闘ログを削除しました" : "§7削除対象がありません"), false);
            return deleted ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c削除失敗: " + e.getMessage()));
            return 0;
        }
    }

    private static int logAnalyze(CommandSourceStack source, int topN) {
        List<String> lines = CombatLogAnalyzer.summary(topN);
        source.sendSuccess(() -> Component.literal("§6=== 戦闘ログ集計 (Lisp注入中) ==="), false);
        for (String line : lines) {
            source.sendSuccess(() -> Component.literal("§7" + line), false);
        }
        source.sendSuccess(() -> Component.literal(
            "§7Lisp変数: log-mob-type-* / log-player-* として各Mobの脳に注入中"), false);
        return 1;
    }

    private static int logRefresh(CommandSourceStack source) {
        CombatLogAnalyzer.refresh();
        source.sendSuccess(() -> Component.literal(
            "§a戦闘ログを再解析しました (総イベント数: §e"
            + CombatLogAnalyzer.getTotalEvents() + "§a)"), false);
        return 1;
    }

    private static int logTail(CommandSourceStack source, int n) {
        java.nio.file.Path file = CombatLogger.getTodayEventFile();
        if (!java.nio.file.Files.exists(file)) {
            source.sendFailure(Component.literal("§c今日のログはまだありません"));
            return 0;
        }
        try {
            List<String> all = java.nio.file.Files.readAllLines(file);
            int from = Math.max(0, all.size() - n);
            List<String> tail = all.subList(from, all.size());
            source.sendSuccess(() -> Component.literal(
                "§6=== 戦闘ログ 最新" + tail.size() + "件 (全" + all.size() + "件) ==="), false);
            for (String line : tail) {
                String display = line.length() > 180 ? line.substring(0, 177) + "..." : line;
                source.sendSuccess(() -> Component.literal("§7" + display), false);
            }
            return tail.size();
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c読込失敗: " + e.getMessage()));
            return 0;
        }
    }

    // === /test cleanblocks [radius] ===
    private static int cleanBlocks(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        int removed = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                    if (state.is(net.minecraft.world.level.block.Blocks.COBBLESTONE)
                        || state.is(net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE)
                        || state.is(net.minecraft.world.level.block.Blocks.COBWEB)) {
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        removed++;
                    }
                }
            }
        }
        final int total = removed;
        source.sendSuccess(() -> Component.literal(
            "§a半径" + radius + "内の§c" + total + "個§aのMob設置ブロックを削除しました"), false);
        return removed;
    }

    // === /test clear [radius] ===
    private static int clearMobs(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPos.containing(source.getPosition());
        List<Monster> mobs = level.getEntitiesOfClass(Monster.class,
            new net.minecraft.world.phys.AABB(
                center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                center.getX() + radius, center.getY() + radius, center.getZ() + radius));
        int count = mobs.size();
        mobs.forEach(Entity::discard);
        final int total = count;
        source.sendSuccess(() -> Component.literal(
            "§a半径" + radius + "ブロック以内の§c" + total + "体§aのMobを削除しました"), false);
        return count;
    }
}
