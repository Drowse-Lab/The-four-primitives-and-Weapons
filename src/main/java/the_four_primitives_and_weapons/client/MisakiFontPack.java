package the_four_primitives_and_weapons.client;

import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.resource.PathPackResources;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

public final class MisakiFontPack {
    private static final String PACK_ID = TheFourPrimitivesAndWeaponsMod.MODID + ":misaki_font";
    private static final String PACK_PATH = "resourcepacks/misaki_font_pack";
    private static final Component PACK_TITLE = Component.literal("Misaki Gothic Japanese Font");

    private MisakiFontPack() {
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        event.addRepositorySource(consumer -> {
            IModFileInfo modFileInfo = ModList.get().getModFileById(TheFourPrimitivesAndWeaponsMod.MODID);
            if (modFileInfo == null) {
                TheFourPrimitivesAndWeaponsMod.LOGGER.warn("Could not find mod file for {}", TheFourPrimitivesAndWeaponsMod.MODID);
                return;
            }

            Path packPath = modFileInfo.getFile().findResource(PACK_PATH);
            if (!Files.exists(packPath)) {
                TheFourPrimitivesAndWeaponsMod.LOGGER.warn("Misaki font pack is missing at {}", packPath);
                return;
            }

            Pack.ResourcesSupplier supplier = id -> new PathPackResources(id, true, packPath);
            Pack pack = Pack.readMetaAndCreate(
                    PACK_ID,
                    PACK_TITLE,
                    false,
                    supplier,
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN);
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }
}
