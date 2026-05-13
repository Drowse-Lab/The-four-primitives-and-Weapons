package the_four_primitives_and_weapons.init;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.potion.HookshotFloatEffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MCreator 自動生成 ({@code TheFourPrimitivesAndWeaponsModMobEffects}) の影響を受けない独自 MobEffect 登録。
 *
 *   - {@link #HOOKSHOT_FLOAT} : 浮遊 (vanilla LEVITATION 代替)
 *
 * かつて存在した HOOKSHOT_FALL_GUARD は MobEffect での管理を廃止し、
 * {@link the_four_primitives_and_weapons.event.RecrossPlayerHandler#applyFallGuard} の
 * サーバー側カウンタで管理する形に変更している。
 */
public class CustomMobEffectInit {
    public static final DeferredRegister<MobEffect> REGISTRY =
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TheFourPrimitivesAndWeaponsMod.MODID);

    public static final RegistryObject<MobEffect> HOOKSHOT_FLOAT =
        REGISTRY.register("hookshot_float", () -> new HookshotFloatEffect());
}
