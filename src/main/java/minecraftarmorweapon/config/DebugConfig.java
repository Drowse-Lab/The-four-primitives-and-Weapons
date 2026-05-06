package minecraftarmorweapon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.Path;

/**
 * デバッグ表示のON/OFF設定。
 * config/debug_settings.json に保存される。
 */
public class DebugConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "debug_settings.json";

    /**
     * ON: 武器の属性/Lv/魔導書情報をアクションバーに表示する。
     * OFF: 表示しない（デフォルト）。
     */
    public static boolean elementalDebugEnabled = false;

    /**
     * ON: Re:Cross Hookshot の挙動を {@code logs/recross_hookshot.log} と
     *      {@code logs/datapack_hookshot.log} に記録.
     * OFF: 何もしない (デフォルト).
     */
    public static boolean recrossHookshotDebugEnabled = false;

    public static void load() {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            File file = configDir.resolve(FILE_NAME).toFile();
            if (file.exists()) {
                try (Reader reader = new FileReader(file)) {
                    ConfigData data = GSON.fromJson(reader, ConfigData.class);
                    if (data != null) {
                        elementalDebugEnabled = data.elementalDebugEnabled;
                        recrossHookshotDebugEnabled = data.recrossHookshotDebugEnabled;
                    }
                }
            } else {
                save();
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
            data.elementalDebugEnabled = elementalDebugEnabled;
            data.recrossHookshotDebugEnabled = recrossHookshotDebugEnabled;
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            // 保存失敗時は無視
        }
    }

    private static class ConfigData {
        boolean elementalDebugEnabled = false;
        boolean recrossHookshotDebugEnabled = false;
    }
}
