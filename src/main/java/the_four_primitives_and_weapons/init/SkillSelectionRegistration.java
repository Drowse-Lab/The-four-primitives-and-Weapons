package the_four_primitives_and_weapons.init;

import the_four_primitives_and_weapons.world.inventory.SkillSelectionMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * MCreatorのinit再生成に影響されない独自の登録クラス。
 * RegisterEventで直接登録するため、initファイルを編集する必要がない。
 * 画面の登録はRarityForgeRegistration$Clientで一括して行う。
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons", bus = Mod.EventBusSubscriber.Bus.MOD)
public class SkillSelectionRegistration {

    private static MenuType<SkillSelectionMenu> menuType;

    public static MenuType<SkillSelectionMenu> getMenuType() { return menuType; }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.MENU_TYPES, helper -> {
            menuType = IForgeMenuType.create(SkillSelectionMenu::new);
            helper.register(
                new ResourceLocation("the_four_primitives_and_weapons", "skill_selection_menu"),
                menuType
            );
        });
    }
}
