package the_four_primitives_and_weapons.init;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.item.AntiGravityBraceletItem;
import the_four_primitives_and_weapons.item.ExplosiveThrowingKnifeItem;
import the_four_primitives_and_weapons.item.MaterializedPouchItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MCreator が {@code TheFourPrimitivesAndWeaponsModItems} を再生成しても
 * 残るように、投げナイフ拡張 (爆発ナイフ / 反重力腕輪) は専用の
 * DeferredRegister で登録する。
 */
public final class KnifeExtrasRegistrar {

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, TheFourPrimitivesAndWeaponsMod.MODID);

    public static final RegistryObject<Item> EXPLOSIVE_THROWING_KNIFE =
        ITEMS.register("explosive_throwing_knife", ExplosiveThrowingKnifeItem::new);

    public static final RegistryObject<Item> ANTI_GRAVITY_BRACELET =
        ITEMS.register("anti_gravity_bracelet", AntiGravityBraceletItem::new);

    /** 具現化武器を保管する結晶ポーチ ( 右クリックで中身 GUI ) */
    public static final RegistryObject<Item> MATERIALIZED_POUCH =
        ITEMS.register("materialized_pouch", MaterializedPouchItem::new);

    /** 戦地設営の杭: オフハンドに持つと、 メイン武器をブロック上面右クリックで地面に突き刺せる ( 戦場の建築用 )。 */
    public static final RegistryObject<Item> BATTLE_STAKE =
        ITEMS.register("battle_stake", () -> new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties()));

    private KnifeExtrasRegistrar() {}
}
