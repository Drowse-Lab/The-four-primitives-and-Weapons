package the_four_primitives_and_weapons.client.tooltip;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import the_four_primitives_and_weapons.item.rarity.WeaponRarity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * config 内のPNGをそのままツールチップの四隅として描画する。
 * 32x32 PNGの各16x16区画が、左上・右上・左下・右下に対応する。
 */
public final class EditableTooltipCornerTextures {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int ATLAS_SIZE = 32;
    private static final int CORNER_SIZE = 16;
    private static final Path DIRECTORY = FMLPaths.CONFIGDIR.get()
            .resolve("the_four_primitives_and_weapons").resolve("tooltip_corners");
    private static final Map<WeaponRarity, LoadedTexture> CACHE = new EnumMap<>(WeaponRarity.class);

    private EditableTooltipCornerTextures() {}

    public static void draw(GuiGraphics graphics, WeaponRarity rarity, int left, int top, int right, int bottom) {
        ResourceLocation texture = getTexture(rarity);
        if (texture == null) return;

        graphics.blit(texture, left, top, 0, 0, CORNER_SIZE, CORNER_SIZE, ATLAS_SIZE, ATLAS_SIZE);
        graphics.blit(texture, right - CORNER_SIZE + 1, top, 16, 0,
                CORNER_SIZE, CORNER_SIZE, ATLAS_SIZE, ATLAS_SIZE);
        graphics.blit(texture, left, bottom - CORNER_SIZE + 1, 0, 16,
                CORNER_SIZE, CORNER_SIZE, ATLAS_SIZE, ATLAS_SIZE);
        graphics.blit(texture, right - CORNER_SIZE + 1, bottom - CORNER_SIZE + 1, 16, 16,
                CORNER_SIZE, CORNER_SIZE, ATLAS_SIZE, ATLAS_SIZE);
    }

    private static ResourceLocation getTexture(WeaponRarity rarity) {
        try {
            ensureFilesExist();
            Path path = DIRECTORY.resolve(rarity.name().toLowerCase() + ".png");
            FileTime modified = Files.getLastModifiedTime(path);
            LoadedTexture cached = CACHE.get(rarity);
            if (cached != null && cached.modified().equals(modified)) return cached.location();

            try (InputStream stream = Files.newInputStream(path)) {
                NativeImage image = NativeImage.read(stream);
                if (image.getWidth() != ATLAS_SIZE || image.getHeight() != ATLAS_SIZE) {
                    image.close();
                    LOGGER.error("Tooltip corner image must be 32x32: {}", path);
                    return cached == null ? null : cached.location();
                }
                ResourceLocation location = new ResourceLocation("the_four_primitives_and_weapons",
                        "dynamic/tooltip_corner_" + rarity.name().toLowerCase());
                Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
                CACHE.put(rarity, new LoadedTexture(location, modified));
                return location;
            }
        } catch (IOException exception) {
            LOGGER.error("Could not load editable tooltip corner PNG", exception);
            LoadedTexture cached = CACHE.get(rarity);
            return cached == null ? null : cached.location();
        }
    }

    private static void ensureFilesExist() throws IOException {
        Files.createDirectories(DIRECTORY);
        Path guide = DIRECTORY.resolve("README.txt");
        if (Files.notExists(guide)) {
            Files.writeString(guide,
                    "Each PNG is 32x32 pixels. Edit it in Paint without resizing.\n" +
                    "top-left 16x16 = top-left corner, top-right 16x16 = top-right corner\n" +
                    "bottom-left 16x16 = bottom-left corner, bottom-right 16x16 = bottom-right corner\n" +
                    "Transparent pixels are not drawn. Changes reload automatically after saving.\n");
        }
        for (WeaponRarity rarity : WeaponRarity.values()) {
            Path path = DIRECTORY.resolve(rarity.name().toLowerCase() + ".png");
            if (Files.notExists(path)) createDefault(path, rarity);
        }
    }

    private static void createDefault(Path path, WeaponRarity rarity) throws IOException {
        String[] template = template(rarity);
        int argb = color(rarity);
        int abgr = FastColor.ABGR32.color(FastColor.ARGB32.alpha(argb),
                FastColor.ARGB32.blue(argb), FastColor.ARGB32.green(argb), FastColor.ARGB32.red(argb));
        try (NativeImage image = new NativeImage(ATLAS_SIZE, ATLAS_SIZE, true)) {
            for (int y = 0; y < template.length; y++) {
                for (int x = 0; x < template[y].length(); x++) {
                    if (template[y].charAt(x) != '1') continue;
                    image.setPixelRGBA(x, y, abgr);
                    image.setPixelRGBA(31 - x, y, abgr);
                    image.setPixelRGBA(x, 31 - y, abgr);
                    image.setPixelRGBA(31 - x, 31 - y, abgr);
                }
            }
            image.writeToFile(path);
        }
    }

    private static String[] template(WeaponRarity rarity) {
        return switch (rarity) {
            case COMMON -> new String[]{"010111", "000100", "001100", "001000", "001000", "010000"};
            case RARE -> new String[]{"111111", "100100", "111100", "110000", "110000", "100000"};
            case LEGENDARY -> new String[]{"11111111", "10000000", "11101101", "11011100", "11000000", "11010000", "10000000", "10000000"};
            case EPIC -> new String[]{"111111", "101000", "111110", "110100", "111000", "110000"};
            case UNCOMMON -> new String[]{"1111111", "1000000", "1111011", "1101100", "1110000", "1101000", "1100000"};
            case FORBIDDEN -> new String[]{"11111111", "11010100", "10111010", "11101000", "11010000", "10100000", "11000000", "10000000"};
        };
    }

    private static int color(WeaponRarity rarity) {
        return switch (rarity) {
            case COMMON -> 0xFFCBE8F7;
            case UNCOMMON -> 0xFF35FF50;
            case RARE -> 0xFF419BFF;
            case EPIC -> 0xFFC02FFF;
            case LEGENDARY -> 0xFFFFB000;
            case FORBIDDEN -> 0xFFFF3B3B;
        };
    }

    private record LoadedTexture(ResourceLocation location, FileTime modified) {}
}
