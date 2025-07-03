package minecraftarmorweapon.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

import net.minecraft.world.entity.PathfinderMob;

public class CustomMeleeAttackGoal extends MeleeAttackGoal {
    public CustomMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followIfNotSeen) {
        super(mob, speedModifier, followIfNotSeen);
    }

    @Override
    protected double getAttackReachSqr(LivingEntity target) {
        return this.mob.getBbWidth() * this.mob.getBbWidth() + target.getBbWidth();
    }
}
