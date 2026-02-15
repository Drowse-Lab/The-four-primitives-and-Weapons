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
    
    // カスタム難易度の定義（シンプル版）
    public enum CustomDifficulty {
        PEACEFUL("peaceful", Difficulty.PEACEFUL, 0.0f),
        EASY("easy", Difficulty.EASY, 0.75f),
        NORMAL("normal", Difficulty.NORMAL, 1.0f),
        HARD("hard", Difficulty.HARD, 1.5f),
        NIGHTMARE("nightmare", Difficulty.HARD, 2.0f),
        REALISTIC("realistic", Difficulty.HARD, 3.0f),
        CREATIVE_PLUS("creative+", Difficulty.PEACEFUL, 0.5f),
        LUNATIC("lunatic", Difficulty.HARD, 2.5f),
        LUNATIC_PLUS("lunatic+", Difficulty.HARD, 3.5f),
        LUNATIC_EXTREME("lunatic_extreme", Difficulty.HARD, 5.0f);
        
        private final String name;
        private final Difficulty baseDifficulty;
        private final float damageMultiplier;
        
        CustomDifficulty(String name, Difficulty baseDifficulty, float damageMultiplier) {
            this.name = name;
            this.baseDifficulty = baseDifficulty;
            this.damageMultiplier = damageMultiplier;
        }
        
        public String getName() {
            return name;
        }
        
        public Difficulty getBaseDifficulty() {
            return baseDifficulty;
        }
        
        public float getDamageMultiplier() {
            return damageMultiplier;
        }
        
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
                    context.getSource().sendSuccess(
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
                    context.getSource().sendSuccess(
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
        customData.putString(NBT_KEY, newDifficulty.getName());
        
        // カスタム難易度の情報を表示
        String message = String.format(
            "難易度を %s に設定しました (ダメージ倍率: %.1fx)",
            newDifficulty.getName(),
            newDifficulty.getDamageMultiplier()
        );
        
        source.sendSuccess(Component.literal(message), true);
        
        // Lunaticモードの特別メッセージ
        if (newDifficulty.getName().contains("lunatic")) {
            source.sendSuccess(
                Component.literal("§c警告: 非常に難しくなります！"),
                false
            );
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
    
    // True Crafterモードが有効かチェック（nightmare以上で強制有効）
    public static boolean isTrueCrafterEnabled() {
        String name = currentDifficulty.getName();
        return name.equals("nightmare") || 
               name.equals("realistic") || 
               name.contains("lunatic");
    }
    
    // 旧メソッド名との互換性のため
    public static boolean isAIEnhanced() {
        return isTrueCrafterEnabled();
    }
    
    // サーバー起動時に難易度を復元
    @SubscribeEvent
    public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        net.minecraft.nbt.CompoundTag customData = event.getServer().getWorldData().getCustomBossEvents();
        if (customData.contains(NBT_KEY)) {
            String savedDifficulty = customData.getString(NBT_KEY);
            currentDifficulty = CustomDifficulty.byName(savedDifficulty);
            
            // ベース難易度も設定
            event.getServer().setDifficulty(currentDifficulty.getBaseDifficulty(), true);
            
            System.out.println("カスタム難易度を復元: " + currentDifficulty.getName());
        }
    }
}