package the_four_primitives_and_weapons.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.init.CustomEntityInit;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    /** ナイフホルダーの「手に見えるナイフ本数」の段階数 ( モデル knife_x1..x5 と対応 )。 */
    public static final int KNIFE_HOLDER_MODEL_STEPS = 5;


    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Saya 用カスタム ItemDisplayContext (SAYA_BACK / SAYA_BELT) を class-init
        // で登録。ScabbardCurioRenderer がこれらを参照する。
        MawDisplayContexts.init();

        event.enqueueWork(() -> {
            // Register item properties for all items
            for (RegistryObject<Item> itemObj : TheFourPrimitivesAndWeaponsModItems.REGISTRY.getEntries()) {
                ItemPropertyInit.registerItemProperties(itemObj.get());
            }

            // 上腕骨刀: 溜め中(使用中)だけ別モデルに切替えるための "charging" predicate。
            // item/katana_nigu_humerus.json の overrides で溜め専用モデルを参照する。
            ItemProperties.register(
                TheFourPrimitivesAndWeaponsModItems.KATANA_NIGU_HUMERUS.get(),
                new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "charging"),
                (stack, lvl, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1f : 0f);

            // ナイフホルダー: 装填数に応じて手に持つナイフの本数を増やす。
            // item/knife_launcher.json の overrides が このプロパティの値でモデルを選ぶ。
            //   0 本      → 0.0 ( どの override にも当たらず 素のホルダーモデル )
            //   1〜4 本   → 0.2 ( ナイフ 1 本 )
            //   5〜8 本   → 0.4 ( 2 本 ) … 以降 4 本ごとに 1 段階
            //   17 本以上 → 1.0 ( 5 本 = 上限 )
            ItemProperties.register(
                CustomEntityInit.KNIFE_LAUNCHER.get(),
                new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "stored_level"),
                (stack, lvl, entity, seed) -> {
                    int stored = the_four_primitives_and_weapons.item.KnifeLauncherItem.getStored(stack);
                    if (stored <= 0) return 0f;
                    int level = Math.min(KNIFE_HOLDER_MODEL_STEPS, (stored + 3) / 4);
                    return level / (float) KNIFE_HOLDER_MODEL_STEPS;
                });
        });
    }
}
