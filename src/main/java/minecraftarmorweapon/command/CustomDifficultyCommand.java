package minecraftarmorweapon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber
public class CustomDifficultyCommand {

    /**
     * カスタム難易度の定義 — True Crafter Mode準拠の多面的パラメータ
     *
     * 各レベルで以下が段階的に変化する:
     *   damageMultiplier  … Mob→プレイヤーのダメージ倍率
     *   healthMultiplier  … Mobの最大HP倍率 (1.0 = バニラ)
     *   aiLevel           … AI強化レベル (0=なし, 1-5で段階的に強化)
     *   eliteSpawnChance  … エリートMobに昇格する確率 (0.0-1.0)
     *   reinforceChance   … 援軍が追加スポーンする確率 (0.0-1.0)
     *   buffEffectChance  … スポーン時にバフ効果が付く確率 (0.0-1.0)
     *   equipmentTier     … 装備品質 (0=なし, 1=革/木, 2=鉄, 3=ダイヤ, 4=ネザライト/MOD)
     *   shieldChance      … 盾を持つ確率 (0.0-1.0)
     *   blockPlaceEnabled … ブロック設置が可能か
     *   blockBreakEnabled … ブロック破壊が可能か
     *   wallSenseEnabled  … 壁越しにプレイヤーを感知するか
     *   bedSleepEnabled   … ベッドで眠れるか
     *   fallDmgImmune     … 追跡中の落下ダメージ無効
     *   colorCode         … チャット表示の色コード
     */
    public enum CustomDifficulty {
        //                    name              base              dmg   hp    ai  elite reinf buff  eqTier shield block break wall  bed   fall  trait  color
        // peaceful〜hard: バニラ準拠（MOD機能なし）
        PEACEFUL(       "peaceful",       Difficulty.PEACEFUL, 0.0f, 1.0f, 0, 0.00f, 0.00f, 0.00f, 0, 0.00f, false, false, false, true,  false, 0.00f, "§a"),
        EASY(           "easy",           Difficulty.EASY,     0.75f,1.0f, 0, 0.00f, 0.00f, 0.00f, 0, 0.00f, false, false, false, true,  false, 0.00f, "§a"),
        NORMAL(         "normal",         Difficulty.NORMAL,   1.0f, 1.0f, 0, 0.00f, 0.00f, 0.00f, 0, 0.00f, false, false, false, true,  false, 0.00f, "§f"),
        HARD(           "hard",           Difficulty.HARD,     1.5f, 1.0f, 0, 0.00f, 0.00f, 0.00f, 0, 0.00f, false, false, false, true,  false, 0.00f, "§e"),
        // nightmare以上: MOD機能が段階的に有効化
        NIGHTMARE(      "nightmare",      Difficulty.HARD,     2.0f, 1.15f,1, 0.08f, 0.00f, 0.10f, 1, 0.10f, false, false, false, true,  false, 0.05f, "§c"),
        REALISTIC(      "realistic",      Difficulty.HARD,     3.0f, 1.25f,2, 0.15f, 0.08f, 0.20f, 2, 0.25f, true,  true,  true,  true,  true,  0.12f, "§c"),
        CREATIVE_PLUS(  "creative+",      Difficulty.PEACEFUL, 0.5f, 1.0f, 0, 0.00f, 0.00f, 0.00f, 0, 0.00f, false, false, false, true,  false, 0.00f, "§b"),
        LUNATIC(        "lunatic",        Difficulty.HARD,     2.5f, 1.35f,3, 0.25f, 0.15f, 0.35f, 3, 0.40f, true,  true,  true,  false, true,  0.25f, "§5"),
        LUNATIC_PLUS(   "lunatic+",       Difficulty.HARD,     3.5f, 1.50f,4, 0.40f, 0.30f, 0.55f, 3, 0.55f, true,  true,  true,  false, true,  0.45f, "§5"),
        LUNATIC_EXTREME("lunatic_extreme",Difficulty.HARD,     5.0f, 1.75f,5, 0.60f, 0.50f, 0.80f, 4, 0.70f, true,  true,  true,  false, true,  0.80f, "§4");

        private final String name;
        private final Difficulty baseDifficulty;
        private final float damageMultiplier;
        private final float healthMultiplier;
        private final int aiLevel;
        private final float eliteSpawnChance;
        private final float reinforceChance;
        private final float buffEffectChance;
        private final int equipmentTier;
        private final float shieldChance;
        private final boolean blockPlaceEnabled;
        private final boolean blockBreakEnabled;
        private final boolean wallSenseEnabled;
        private final boolean bedSleepEnabled;
        private final boolean fallDmgImmune;
        private final float traitChance;
        private final String colorCode;

        CustomDifficulty(String name, Difficulty baseDifficulty,
                         float damageMultiplier, float healthMultiplier,
                         int aiLevel, float eliteSpawnChance, float reinforceChance,
                         float buffEffectChance, int equipmentTier, float shieldChance,
                         boolean blockPlaceEnabled, boolean blockBreakEnabled,
                         boolean wallSenseEnabled, boolean bedSleepEnabled,
                         boolean fallDmgImmune, float traitChance, String colorCode) {
            this.name = name;
            this.baseDifficulty = baseDifficulty;
            this.damageMultiplier = damageMultiplier;
            this.healthMultiplier = healthMultiplier;
            this.aiLevel = aiLevel;
            this.eliteSpawnChance = eliteSpawnChance;
            this.reinforceChance = reinforceChance;
            this.buffEffectChance = buffEffectChance;
            this.equipmentTier = equipmentTier;
            this.shieldChance = shieldChance;
            this.blockPlaceEnabled = blockPlaceEnabled;
            this.blockBreakEnabled = blockBreakEnabled;
            this.wallSenseEnabled = wallSenseEnabled;
            this.bedSleepEnabled = bedSleepEnabled;
            this.fallDmgImmune = fallDmgImmune;
            this.traitChance = traitChance;
            this.colorCode = colorCode;
        }

        public String getName() { return name; }
        public Difficulty getBaseDifficulty() { return baseDifficulty; }
        public float getDamageMultiplier() { return damageMultiplier; }
        public float getHealthMultiplier() { return healthMultiplier; }
        public int getAiLevel() { return aiLevel; }
        public float getEliteSpawnChance() { return eliteSpawnChance; }
        public float getReinforceChance() { return reinforceChance; }
        public float getBuffEffectChance() { return buffEffectChance; }
        public int getEquipmentTier() { return equipmentTier; }
        public float getShieldChance() { return shieldChance; }
        public boolean isBlockPlaceEnabled() { return blockPlaceEnabled; }
        public boolean isBlockBreakEnabled() { return blockBreakEnabled; }
        public boolean isWallSenseEnabled() { return wallSenseEnabled; }
        public boolean isBedSleepEnabled() { return bedSleepEnabled; }
        public boolean isFallDmgImmune() { return fallDmgImmune; }
        public float getTraitChance() { return traitChance; }
        public String getColorCode() { return colorCode; }

        public static CustomDifficulty byName(String name) {
            for (CustomDifficulty diff : values()) {
                if (diff.name.equalsIgnoreCase(name)) {
                    return diff;
                }
            }
            return NORMAL;
        }

        public static List<String> getAllNames() {
            return Arrays.stream(values())
                .map(CustomDifficulty::getName)
                .toList();
        }
    }
    
    // 現在のカスタム難易度を保存
    private static CustomDifficulty currentDifficulty = CustomDifficulty.NORMAL;
    private static final String NBT_KEY = "minecraft_armor_weapon:custom_difficulty";
    
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // /gamerule difficulty コマンドを登録
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("gamerule")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("difficulty")
                .executes(context -> {
                    // 現在の難易度を表示
                    context.getSource().sendSuccess(() ->
                        Component.literal("現在の難易度: " + currentDifficulty.getName()),
                        false
                    );
                    return 1;
                })
                .then(Commands.argument("value", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(CustomDifficulty.getAllNames(), builder))
                    .executes(context -> {
                        String difficultyName = StringArgumentType.getString(context, "value");
                        return setDifficulty(context.getSource(), difficultyName);
                    })
                )
            );
        
        dispatcher.register(command);
        
        // /difficulty コマンドも追加（標準コマンドの拡張）
        LiteralArgumentBuilder<CommandSourceStack> difficultyCommand = Commands.literal("difficulty")
            .requires(source -> source.hasPermission(2));
        
        // 標準の難易度
        for (Difficulty diff : Difficulty.values()) {
            difficultyCommand.then(Commands.literal(diff.getKey())
                .executes(context -> {
                    context.getSource().getLevel().getServer().setDifficulty(diff, true);
                    context.getSource().sendSuccess(() ->
                        Component.literal("難易度を " + diff.getKey() + " に設定しました"),
                        true
                    );
                    return 1;
                }));
        }
        
        // カスタム難易度も追加
        for (CustomDifficulty diff : CustomDifficulty.values()) {
            difficultyCommand.then(Commands.literal(diff.getName())
                .executes(context -> setDifficulty(context.getSource(), diff.getName())));
        }
        
        dispatcher.register(difficultyCommand);
    }
    
    private static int setDifficulty(CommandSourceStack source, String difficultyName) {
        CustomDifficulty newDifficulty = CustomDifficulty.byName(difficultyName);

        if (newDifficulty == null) {
            source.sendFailure(Component.literal("不明な難易度: " + difficultyName));
            return 0;
        }

        currentDifficulty = newDifficulty;

        // ベースの難易度を設定
        ServerLevel level = source.getLevel();
        level.getServer().setDifficulty(newDifficulty.getBaseDifficulty(), true);

        // NBTに保存
        net.minecraft.nbt.CompoundTag customData = level.getServer().getWorldData().getCustomBossEvents();
        if (customData != null) {
            customData.putString(NBT_KEY, newDifficulty.getName());
        }

        // 難易度変更の詳細表示
        String color = newDifficulty.getColorCode();
        source.sendSuccess(() -> Component.literal(
            color + "§l=== 難易度を " + newDifficulty.getName().toUpperCase() + " に設定 ==="
        ), true);

        source.sendSuccess(() -> Component.literal(String.format(
            "§7Mobダメージ倍率: §e%.1fx §7| MobHP倍率: §e%.2fx §7| AI Lv: §e%d",
            newDifficulty.getDamageMultiplier(),
            newDifficulty.getHealthMultiplier(),
            newDifficulty.getAiLevel()
        )), false);

        if (newDifficulty.getAiLevel() > 0) {
            StringBuilder features = new StringBuilder("§7有効な機能: ");
            if (newDifficulty.getEliteSpawnChance() > 0)
                features.append("§aエリート出現 ");
            if (newDifficulty.getReinforceChance() > 0)
                features.append("§a援軍 ");
            if (newDifficulty.getBuffEffectChance() > 0)
                features.append("§aバフ効果 ");
            if (newDifficulty.isBlockPlaceEnabled())
                features.append("§eブロック設置 ");
            if (newDifficulty.isBlockBreakEnabled())
                features.append("§eブロック破壊 ");
            if (newDifficulty.isWallSenseEnabled())
                features.append("§c壁越し感知 ");
            if (newDifficulty.isFallDmgImmune())
                features.append("§c落下耐性 ");
            if (!newDifficulty.isBedSleepEnabled())
                features.append("§4ベッド睡眠無効 ");
            source.sendSuccess(() -> Component.literal(features.toString()), false);
        }

        // 高難易度の警告
        if (newDifficulty.getAiLevel() >= 4) {
            source.sendSuccess(() -> Component.literal(
                "§4§l⚠ 警告: 極めて高い難易度です！Mobが非常に強化されます！"
            ), false);
        } else if (newDifficulty.getAiLevel() >= 2) {
            source.sendSuccess(() -> Component.literal(
                "§c警告: Mobが強化されます"
            ), false);
        }

        return 1;
    }

    public static CustomDifficulty getCurrentDifficulty() {
        return currentDifficulty;
    }

    public static void setCurrentDifficulty(CustomDifficulty difficulty) {
        currentDifficulty = difficulty;
    }

    public static float getDamageMultiplier() {
        return currentDifficulty.getDamageMultiplier();
    }

    // True Crafterモードが有効かチェック（aiLevel >= 1 で有効）
    public static boolean isTrueCrafterEnabled() {
        return currentDifficulty.getAiLevel() >= 1;
    }

    // 旧メソッド名との互換性のため
    public static boolean isAIEnhanced() {
        return currentDifficulty.getAiLevel() >= 3;
    }
    
    // サーバー起動時に難易度を復元
    @SubscribeEvent
    public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        net.minecraft.nbt.CompoundTag customData = event.getServer().getWorldData().getCustomBossEvents();
        if (customData != null && customData.contains(NBT_KEY)) {
            String savedDifficulty = customData.getString(NBT_KEY);
            currentDifficulty = CustomDifficulty.byName(savedDifficulty);

            // ベース難易度も設定
            event.getServer().setDifficulty(currentDifficulty.getBaseDifficulty(), true);

            System.out.println("カスタム難易度を復元: " + currentDifficulty.getName());
        }
    }
}