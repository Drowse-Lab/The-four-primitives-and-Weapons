package the_four_primitives_and_weapons.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.item.CorrosionCrystalItem;

/** 剣の原への往還だけに使う、既存コンテンツから独立した鍵アイテム。 */
public final class BladeDimensionItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TheFourPrimitivesAndWeaponsMod.MODID);

    public static final RegistryObject<Item> BOUNDARY_BLADE = ITEMS.register("boundary_blade",
            () -> new SwordItem(Tiers.IRON, 4, -2.4F,
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> BLADE_FIELD_TOME = ITEMS.register("blade_field_tome",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)) {
                @Override public boolean isFoil(net.minecraft.world.item.ItemStack stack) { return true; }
            });

    public static final RegistryObject<Item> CORROSION_CRYSTAL = ITEMS.register("corrosion_crystal",
            () -> new CorrosionCrystalItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static void register(IEventBus bus) { ITEMS.register(bus); }
    private BladeDimensionItems() {}
}
