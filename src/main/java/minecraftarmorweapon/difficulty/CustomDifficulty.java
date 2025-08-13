package minecraftarmorweapon.difficulty;

import net.minecraft.world.Difficulty;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.Map;

public class CustomDifficulty {
    // カスタム難易度の定義
    public static final String NIGHTMARE = "nightmare";
    public static final String REALISTIC = "realistic";
    public static final String CREATIVE_PLUS = "creative_plus";
    public static final String LUNATIC = "lunatic";
    public static final String LUNATIC_PLUS = "lunatic_plus";
    public static final String LUNATIC_EXTREME = "lunatic_extreme";
    
    // カスタム難易度の設定値
    private static final Map<String, DifficultySettings> CUSTOM_DIFFICULTIES = new HashMap<>();
    
    static {
        // ナイトメアモード: ハードコアより難しい
        CUSTOM_DIFFICULTIES.put(NIGHTMARE, new DifficultySettings(
            2.0f,     // ダメージ倍率
            0.5f,     // 回復倍率
            1.5f,     // モブのスポーン率倍率
            2.0f,     // モブの体力倍率
            1.5f,     // モブの攻撃力倍率
            0.7f,     // 空腹度減少速度倍率
            true,     // 暗闇でダメージ
            true,     // 死亡時アイテムロスト
            0         // AIレベル（標準）
        ));
        
        // リアリスティックモード: 現実的な難易度
        CUSTOM_DIFFICULTIES.put(REALISTIC, new DifficultySettings(
            1.5f,     // ダメージ倍率
            0.8f,     // 回復倍率
            1.0f,     // モブのスポーン率倍率
            1.0f,     // モブの体力倍率
            1.2f,     // モブの攻撃力倍率
            1.2f,     // 空腹度減少速度倍率
            false,    // 暗闇でダメージ
            false,    // 死亡時アイテムロスト
            0         // AIレベル（標準）
        ));
        
        // クリエイティブ+モード: 制限付きクリエイティブ
        CUSTOM_DIFFICULTIES.put(CREATIVE_PLUS, new DifficultySettings(
            0.0f,     // ダメージ倍率（無敵）
            2.0f,     // 回復倍率
            0.5f,     // モブのスポーン率倍率
            1.0f,     // モブの体力倍率
            0.5f,     // モブの攻撃力倍率
            0.0f,     // 空腹度減少速度倍率
            false,    // 暗闇でダメージ
            false,    // 死亡時アイテムロスト
            0         // AIレベル（標準）
        ));
        
        // ルナティックモード: AIが賢くなる（レベル1）
        CUSTOM_DIFFICULTIES.put(LUNATIC, new DifficultySettings(
            1.5f,     // ダメージ倍率
            0.7f,     // 回復倍率
            1.2f,     // モブのスポーン率倍率
            1.5f,     // モブの体力倍率
            1.3f,     // モブの攻撃力倍率
            1.0f,     // 空腹度減少速度倍率
            false,    // 暗闇でダメージ
            false,    // 死亡時アイテムロスト
            1         // AIレベル1: 基本的な賢さ（回避、ブロック1個設置）
        ));
        
        // ルナティック+モード: AIがより賢くなる（レベル2）
        CUSTOM_DIFFICULTIES.put(LUNATIC_PLUS, new DifficultySettings(
            2.0f,     // ダメージ倍率
            0.5f,     // 回復倍率
            1.5f,     // モブのスポーン率倍率
            2.0f,     // モブの体力倍率
            1.7f,     // モブの攻撃力倍率
            1.2f,     // 空腹度減少速度倍率
            true,     // 暗闇でダメージ
            false,    // 死亡時アイテムロスト
            2         // AIレベル2: 高度な戦術（タワー建築、橋建設）
        ));
        
        // ルナティックエクストリームモード: AIが極限まで賢くなる（レベル3）
        CUSTOM_DIFFICULTIES.put(LUNATIC_EXTREME, new DifficultySettings(
            3.0f,     // ダメージ倍率
            0.3f,     // 回復倍率
            2.0f,     // モブのスポーン率倍率
            3.0f,     // モブの体力倍率
            2.5f,     // モブの攻撃力倍率
            1.5f,     // 空腹度減少速度倍率
            true,     // 暗闇でダメージ
            true,     // 死亡時アイテムロスト
            3         // AIレベル3: 超知能（罠設置、集団戦術、プレイヤー分析）
        ));
    }
    
    // 現在のカスタム難易度
    private static String currentCustomDifficulty = null;
    
    public static void setCustomDifficulty(String difficulty) {
        if (CUSTOM_DIFFICULTIES.containsKey(difficulty)) {
            currentCustomDifficulty = difficulty;
        }
    }
    
    public static String getCurrentCustomDifficulty() {
        return currentCustomDifficulty;
    }
    
    public static boolean isCustomDifficultyActive() {
        return currentCustomDifficulty != null;
    }
    
    public static void clearCustomDifficulty() {
        currentCustomDifficulty = null;
    }
    
    public static DifficultySettings getSettings(String difficulty) {
        return CUSTOM_DIFFICULTIES.get(difficulty);
    }
    
    public static DifficultySettings getCurrentSettings() {
        if (currentCustomDifficulty == null) return null;
        return CUSTOM_DIFFICULTIES.get(currentCustomDifficulty);
    }
    
    public static Component getDisplayName(String difficulty) {
        switch (difficulty) {
            case NIGHTMARE:
                return Component.literal("Nightmare");
            case REALISTIC:
                return Component.literal("Realistic");
            case CREATIVE_PLUS:
                return Component.literal("Creative+");
            case LUNATIC:
                return Component.literal("Lunatic");
            case LUNATIC_PLUS:
                return Component.literal("Lunatic+");
            case LUNATIC_EXTREME:
                return Component.literal("Lunatic Extreme");
            default:
                return Component.literal("Unknown");
        }
    }
    
    public static class DifficultySettings {
        public final float damageMultiplier;
        public final float healingMultiplier;
        public final float mobSpawnRateMultiplier;
        public final float mobHealthMultiplier;
        public final float mobDamageMultiplier;
        public final float hungerRateMultiplier;
        public final boolean darknessHurts;
        public final boolean loseItemsOnDeath;
        public final int aiLevel; // 0=標準, 1=賢い, 2=とても賢い, 3=超知能
        
        public DifficultySettings(float damageMultiplier, float healingMultiplier,
                                 float mobSpawnRateMultiplier, float mobHealthMultiplier,
                                 float mobDamageMultiplier, float hungerRateMultiplier,
                                 boolean darknessHurts, boolean loseItemsOnDeath,
                                 int aiLevel) {
            this.damageMultiplier = damageMultiplier;
            this.healingMultiplier = healingMultiplier;
            this.mobSpawnRateMultiplier = mobSpawnRateMultiplier;
            this.mobHealthMultiplier = mobHealthMultiplier;
            this.mobDamageMultiplier = mobDamageMultiplier;
            this.hungerRateMultiplier = hungerRateMultiplier;
            this.darknessHurts = darknessHurts;
            this.loseItemsOnDeath = loseItemsOnDeath;
            this.aiLevel = aiLevel;
        }
    }
}