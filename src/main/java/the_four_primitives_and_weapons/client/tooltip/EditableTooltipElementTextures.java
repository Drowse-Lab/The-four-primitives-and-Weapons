package the_four_primitives_and_weapons.client.tooltip;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import the_four_primitives_and_weapons.damage.ElementType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.EnumMap;
import java.util.Map;

/** config内の16x16 PNGを属性紋様としてツールチップ上辺に描画する。 */
public final class EditableTooltipElementTextures {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SIZE = 16;
    private static final Path DIRECTORY = FMLPaths.CONFIGDIR.get()
            .resolve("the_four_primitives_and_weapons").resolve("tooltip_elements");
    private static final Map<ElementType, LoadedTexture> CACHE = new EnumMap<>(ElementType.class);

    private EditableTooltipElementTextures() {}

    public static void draw(GuiGraphics graphics, ElementType type, int centerX, int top) {
        if (type == ElementType.NONE) return;
        ResourceLocation texture = getTexture(type);
        if (texture != null) graphics.blit(texture, centerX - SIZE / 2, top, 0, 0, SIZE, SIZE, SIZE, SIZE);
    }

    private static ResourceLocation getTexture(ElementType type) {
        try {
            Files.createDirectories(DIRECTORY);
            Path path = DIRECTORY.resolve(type.name().toLowerCase() + ".png");
            if (Files.notExists(path)) return null;
            FileTime modified = Files.getLastModifiedTime(path);
            LoadedTexture cached = CACHE.get(type);
            if (cached != null && cached.modified().equals(modified)) return cached.location();

            try (InputStream stream = Files.newInputStream(path)) {
                NativeImage image = NativeImage.read(stream);
                if (image.getWidth() != SIZE || image.getHeight() != SIZE) {
                    image.close();
                    LOGGER.error("Tooltip element image must be 16x16: {}", path);
                    return cached == null ? null : cached.location();
                }
                ResourceLocation location = new ResourceLocation("the_four_primitives_and_weapons",
                        "dynamic/tooltip_element_" + type.name().toLowerCase());
                Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
                CACHE.put(type, new LoadedTexture(location, modified));
                return location;
            }
        } catch (IOException exception) {
            LOGGER.error("Could not load editable tooltip element PNG", exception);
            LoadedTexture cached = CACHE.get(type);
            return cached == null ? null : cached.location();
        }
    }

    private record LoadedTexture(ResourceLocation location, FileTime modified) {}
}
