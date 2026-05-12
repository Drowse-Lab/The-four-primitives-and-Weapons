package the_four_primitives_and_weapons.mixin;

import the_four_primitives_and_weapons.damage.MiasmaElementDamageHandler;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * LivingEntity.heal() に介入し、瘴気状態の場合に回復量を削減するMixin。
 *
 * 登録先: the_four_primitives_and_weapons.mixins.json の "mixins" 配列に追加
 *   "the_four_primitives_and_weapons.mixin.MiasmaHealMixin"
 */
@Mixin(LivingEntity.class)
public abstract class MiasmaHealMixin {

    /**
     * heal() の第1引数（healAmount）を瘴気の阻害率に応じて削減する。
     *
     * 阻害率が 1.0（100%）の場合、healAmount を 0 にして回復を完全に無効化する。
     * それ以外の場合は割合に応じて削減する。
     */
    @ModifyVariable(
            method = "heal(F)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private float miasma_modifyHealAmount(float healAmount) {
        LivingEntity self = (LivingEntity)(Object)this;

        if (!MiasmaElementDamageHandler.isUnderMiasma(self)) {
            return healAmount;
        }

        float reductionRate = MiasmaElementDamageHandler.getHealReductionRate(self);

        // 完全阻害（Lv4+）
        if (reductionRate >= 1.0f) {
            return 0.0f;
        }

        // 部分阻害（Lv1〜3）
        return healAmount * (1.0f - reductionRate);
    }
}
