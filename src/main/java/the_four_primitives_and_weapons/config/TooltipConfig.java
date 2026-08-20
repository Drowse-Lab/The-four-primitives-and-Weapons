package the_four_primitives_and_weapons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

/** Client settings for movable item tooltips. */
public final class TooltipConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "tooltip_settings.json";

    /** True swaps the default Shift+scroll direction. */
    public static boolean reverseScrollDirection = false;

    private TooltipConfig() {}

    public static void load() {
        try {
            File file = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME).toFile();
            if (file.exists()) {
                try (Reader reader = new FileReader(file)) {
                    ConfigData data = GSON.fromJson(reader, ConfigData.class);
                    if (data != null) reverseScrollDirection = data.reverseScrollDirection;
                }
            } else {
                save();
            }
        } catch (Exception ignored) {
            // Keep the default when the file cannot be read.
        }
    }

    public static void save() {
        try {
            File file = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME).toFile();
            ConfigData data = new ConfigData();
            data.reverseScrollDirection = reverseScrollDirection;
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception ignored) {
            // A failed optional client setting must not stop the game.
        }
    }

    private static final class ConfigData {
        boolean reverseScrollDirection = false;
    }
}
