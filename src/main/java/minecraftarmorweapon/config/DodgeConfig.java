package minecraftarmorweapon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.Path;

/**
 * 回避に関するクライアント設定。
 * config/dodge_settings.json に保存される。
 */
public class DodgeConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "dodge_settings.json";

    // === 設定項目 ===

    /**
     * ON: ファンクショナルブロック（チェスト、かまど等）を右クリックした時、
     *     Shift+右クリックでないと回避が発動しない（通常はブロックを開く）。
     *     メインハンドにブロックアイテムを持っている時も同様。
     * OFF: 従来通り、武器を持っていれば常に回避が優先される。
     */
    public static boolean functionalBlockRequiresShift = true;

    /**
     * ON: メインハンドに「右クリックで何も起きないアイテム」（骨、棒など）を持ち、
     *     オフハンドに武器がある場合でも回避が発動する。
     * OFF: メインハンドが武器でない場合、メインハンドが空の時のみ回避可能。
     */
    public static boolean dodgeWithInertItems = false;

    /**
     * ON: メインハンドに「右クリックで作用があるアイテム」（弓、ポーション、バケツ等）を持ち、
     *     オフハンドに武器がある場合でも回避が発動する。
     * OFF: 右クリック作用アイテムが利き手の場合、回避しない（アイテム本来の動作を優先）。
     */
    public static boolean dodgeWithActiveItems = false;

    /**
     * ON: 回避を完全に無効化する（右クリックはアイテム本来の動作になる）。
     * OFF: 従来通り、右クリックで回避が発動する。
     */
    public static boolean dodgeDisabled = false;

    // === 保存/読み込み ===

    public static void load() {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            File file = configDir.resolve(FILE_NAME).toFile();
            if (file.exists()) {
                try (Reader reader = new FileReader(file)) {
                    ConfigData data = GSON.fromJson(reader, ConfigData.class);
                    if (data != null) {
                        functionalBlockRequiresShift = data.functionalBlockRequiresShift;
                        dodgeWithInertItems = data.dodgeWithInertItems;
                        dodgeWithActiveItems = data.dodgeWithActiveItems;
                        dodgeDisabled = data.dodgeDisabled;
                    }
                }
            } else {
                save(); // デフォルト設定を保存
            }
        } catch (Exception e) {
            // 読み込み失敗時はデフォルト値を使用
        }
    }

    public static void save() {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            File file = configDir.resolve(FILE_NAME).toFile();
            ConfigData data = new ConfigData();
            data.functionalBlockRequiresShift = functionalBlockRequiresShift;
            data.dodgeWithInertItems = dodgeWithInertItems;
            data.dodgeWithActiveItems = dodgeWithActiveItems;
            data.dodgeDisabled = dodgeDisabled;
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            // 保存失敗時は無視
        }
    }

    private static class ConfigData {
        boolean functionalBlockRequiresShift = true;
        boolean dodgeWithInertItems = false;
        boolean dodgeWithActiveItems = false;
        boolean dodgeDisabled = false;
    }
}
