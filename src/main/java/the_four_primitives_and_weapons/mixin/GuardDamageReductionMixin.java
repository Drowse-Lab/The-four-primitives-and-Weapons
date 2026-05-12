package the_four_primitives_and_weapons.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * ガード中のダメージカットをMixinで実装
 *
 * PersistentDataの "SwordGuardTicks" > 0 の場合:
 *   - "SwordGuardFull" == true → 100%カット（Replica Sword of Light）
 *   - それ以外 → 25%カット（通常武器）
 *
 * Resistanceエフェクトもアーマースタンドも使わない
 */
@Mixin(LivingEntity.class)
public class GuardDamageReductionMixin {

    @ModifyVariable(
        method = "hurt",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private float applyGuardDamageReduction(float damage, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof Player player)) {
            return damage;
        }

        CompoundTag data = player.getPersistentData();
        int guardTicks = data.getInt("SwordGuardTicks");

        if (guardTicks <= 0) {
            return damage;
        }

        // 完全防御（Replica Sword of Light）
        if (data.getBoolean("SwordGuardFull")) {
            return 0f;
        }

        // 25%カット（通常武器）
        return damage * 0.75f;
    }
}
