package minecraftarmorweapon.init;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.item.AntiGravityBraceletItem;
import minecraftarmorweapon.item.ExplosiveThrowingKnifeItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MCreator が {@code MinecraftArmorWeaponModItems} を再生成しても
 * 残るように、投げナイフ拡張 (爆発ナイフ / 反重力腕輪) は専用の
 * DeferredRegister で登録する。
 */
public final class KnifeExtrasRegistrar {

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, MinecraftArmorWeaponMod.MODID);

    public static final RegistryObject<Item> EXPLOSIVE_THROWING_KNIFE =
        ITEMS.register("explosive_throwing_knife", ExplosiveThrowingKnifeItem::new);

    public static final RegistryObject<Item> ANTI_GRAVITY_BRACELET =
        ITEMS.register("anti_gravity_bracelet", AntiGravityBraceletItem::new);

    private KnifeExtrasRegistrar() {}
}
