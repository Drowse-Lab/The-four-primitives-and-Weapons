package minecraftarmorweapon.init;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.enchantment.MultiJumpEnchantment;
import minecraftarmorweapon.enchantment.ReelInEnchantment;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MCreator再生成の影響を受けない独自エンチャント登録。
 * 「強引き寄せ」等の追加エンチャントをここに登録。
 */
public class CustomEnchantmentInit {
    public static final DeferredRegister<Enchantment> REGISTRY =
        DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MinecraftArmorWeaponMod.MODID);

    // 強引き寄せ（釣竿用）
    public static final RegistryObject<Enchantment> REEL_IN =
        REGISTRY.register("reel_in", () -> new ReelInEnchantment());

    // ジャンプ回数強化（レギンス / フックショット用）
    public static final RegistryObject<Enchantment> MULTI_JUMP =
        REGISTRY.register("multi_jump", () -> new MultiJumpEnchantment());
}
