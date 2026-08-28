package the_four_primitives_and_weapons.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Allows the water-movement state machine to terminate vanilla's riptide pose reliably. */
@Mixin(LivingEntity.class)
public interface LivingEntityAutoSpinAccessor {
    @Accessor("autoSpinAttackTicks")
    void maw_setAutoSpinAttackTicks(int ticks);

    @Invoker("setLivingEntityFlag")
    void maw_invokeSetLivingEntityFlag(int flag, boolean value);
}
