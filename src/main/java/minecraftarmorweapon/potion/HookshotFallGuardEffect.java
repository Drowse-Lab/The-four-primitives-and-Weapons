package minecraftarmorweapon.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * フックショット落下ダメージ無効化効果 — vanilla SLOW_FALLING の代替の独自 effect.
 *
 * 付与中は落下ダメージが完全カットされる。
 * 効果ロジックは {@link minecraftarmorweapon.event.HookshotEffectHandler} が
 * PlayerTickEvent で fallDistance を 0 にリセット。
 *
 * 既存 vanilla effect と区別するため独自 ID。
 */
public class HookshotFallGuardEffect extends MobEffect {

    public HookshotFallGuardEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xfff0a0); // 薄い黄色
    }

    @Override
    public String getDescriptionId() {
        return "effect.minecraft_armor_weapon.hookshot_fall_guard";
    }

    @Override
    public boolean isInstantenous() {
        return false;
    }
}
