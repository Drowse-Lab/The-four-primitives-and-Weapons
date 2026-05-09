package minecraftarmorweapon.init;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.potion.HookshotFallGuardEffect;
import minecraftarmorweapon.potion.HookshotFloatEffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MCreator 自動生成 ({@link MinecraftArmorWeaponModMobEffects}) の影響を受けない独自 MobEffect 登録。
 *
 *   - {@link #HOOKSHOT_FLOAT}      : 浮遊 (vanilla LEVITATION 代替)
 *   - {@link #HOOKSHOT_FALL_GUARD} : 落下ダメージ無効化 (vanilla SLOW_FALLING 代替)
 */
public class CustomMobEffectInit {
    public static final DeferredRegister<MobEffect> REGISTRY =
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MinecraftArmorWeaponMod.MODID);

    public static final RegistryObject<MobEffect> HOOKSHOT_FLOAT =
        REGISTRY.register("hookshot_float", () -> new HookshotFloatEffect());

    public static final RegistryObject<MobEffect> HOOKSHOT_FALL_GUARD =
        REGISTRY.register("hookshot_fall_guard", () -> new HookshotFallGuardEffect());
}
