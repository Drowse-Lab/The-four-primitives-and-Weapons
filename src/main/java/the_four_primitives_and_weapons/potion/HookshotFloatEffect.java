package the_four_primitives_and_weapons.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * フックショット浮遊効果 — vanilla LEVITATION の代替の独自 effect.
 *
 * 効果ロジックは {@link the_four_primitives_and_weapons.event.HookshotEffectHandler} が
 * PlayerTickEvent で適用する (motion.y を上向きに保つ + fallDistance リセット).
 *
 * 既存 vanilla effect 群と区別するため独自 ID にしている。
 */
public class HookshotFloatEffect extends MobEffect {

    public HookshotFloatEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xa0d8ef); // 薄い水色
    }

    @Override
    public String getDescriptionId() {
        return "effect.the_four_primitives_and_weapons.hookshot_float";
    }

    @Override
    public boolean isInstantenous() {
        return false;
    }
}
