package minecraftarmorweapon.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

import net.minecraft.world.entity.PathfinderMob;

public class CustomMeleeAttackGoal extends MeleeAttackGoal {
    private final Mob mob;
    private LivingEntity target;
    
    private final double speed;

     public CustomMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followIfNotSeen) {
        super(mob, speedModifier, followIfNotSeen);
        this.mob = mob; 
        this.speed = speedModifier;  

    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        this.target = target;
        return true;
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(target, speed);
    }

    @Override
    protected double getAttackReachSqr(LivingEntity target) {
        return this.mob.getBbWidth() * this.mob.getBbWidth() + target.getBbWidth();
    }
    @Override
    public void tick() {
        if (target != null && mob.distanceToSqr(target) < 4.0D) {
            mob.doHurtTarget(target);
        }
    }
}
