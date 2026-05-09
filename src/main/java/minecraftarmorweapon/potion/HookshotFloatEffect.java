package minecraftarmorweapon.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * フックショット浮遊効果 — vanilla LEVITATION の代替の独自 effect.
 *
 * 効果ロジックは {@link minecraftarmorweapon.event.HookshotEffectHandler} が
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
        return "effect.minecraft_armor_weapon.hookshot_float";
    }

    @Override
    public boolean isInstantenous() {
        return false;
    }
}
