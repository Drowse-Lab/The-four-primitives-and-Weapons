package minecraftarmorweapon.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

import net.minecraft.world.entity.PathfinderMob;

public class CustomMeleeAttackGoal extends MeleeAttackGoal {
    private final PathfinderMob mob;
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
        if (this.target != null) {
            this.mob.getNavigation().moveTo(this.target, this.speed);
        }
    }

    @Override
    protected double getAttackReachSqr(LivingEntity target) {
        if (target == null) {
            return 4.0D;
        }
        return this.mob.getBbWidth() * this.mob.getBbWidth() + target.getBbWidth();
    }
    
    @Override
    public void tick() {
        // ターゲットの再取得
        LivingEntity currentTarget = this.mob.getTarget();
        if (currentTarget != null && currentTarget.isAlive()) {
            this.target = currentTarget;
            if (this.mob.distanceToSqr(this.target) < 4.0D) {
                this.mob.doHurtTarget(this.target);
            }
        }
    }
    
    @Override
    public void stop() {
        this.target = null;
        super.stop();
    }
    
    @Override
    public boolean canContinueToUse() {
        LivingEntity currentTarget = this.mob.getTarget();
        if (currentTarget == null || !currentTarget.isAlive()) {
            return false;
        }
        this.target = currentTarget;
        return super.canContinueToUse();
    }
}
