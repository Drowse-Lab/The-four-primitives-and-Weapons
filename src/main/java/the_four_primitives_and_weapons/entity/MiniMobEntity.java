package the_four_primitives_and_weapons.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * ちび体型のミニmob。
 * 金のリンゴで手懐けると飼い主に追従し、飼い主が殴った相手/飼い主を殴った相手に反撃する。
 * 飼い主が素手で右クリックすると「おすわり」を切り替える。
 * 自然スポーンはせず、スポーンエッグでのみ出現する。
 */
public class MiniMobEntity extends TamableAnimal {

    /** 手懐け用の餌。テイム後は同じものが回復にも使える。 */
    private static final Ingredient TAME_FOOD = Ingredient.of(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);

    /** まばたきの間隔と長さ ( tick )。 */
    private static final int BLINK_PERIOD = 90;
    private static final int BLINK_TICKS = 3;

    /** 飼い主から預かったアイテムのドロップ率。1.0 超で「死亡時に必ず無傷で返る」扱いになる。 */
    private static final float GIVEN_ITEM_DROP_CHANCE = 2.0f;

    public MiniMobEntity(EntityType<? extends MiniMobEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.0f); // ちびでも段差を登れるようにする
        this.setPersistenceRequired();
        this.xpReward = 0;
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.2, 5.0f, 2.0f, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return TAME_FOOD.test(stack);
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null; // 繁殖はしない
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Creeper || target instanceof Ghast)
            return false;
        if (target instanceof MiniMobEntity mini)
            return !mini.isTame();
        if (target instanceof TamableAnimal tamable && tamable.isTame())
            return false;
        return !(target instanceof Player player) || !player.isCreative();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.level().isClientSide) {
            if (!this.isTame())
                return this.isFood(stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            if (!this.isOwnedBy(player))
                return InteractionResult.PASS;
            // 素手 + しゃがみで何も預けていない時だけ、通常のブロック操作へ通す。
            boolean nothingToTakeBack = stack.isEmpty() && player.isShiftKeyDown()
                    && this.getMainHandItem().isEmpty() && this.getOffhandItem().isEmpty();
            return nothingToTakeBack ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        if (this.isTame()) {
            if (!this.isOwnedBy(player))
                return super.mobInteract(player, hand);
            // 餌は体力が減っている時だけ回復に使う。満タンなら「持たせる」側に回す。
            if (this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
                if (!player.getAbilities().instabuild)
                    stack.shrink(1);
                this.heal(6.0f);
                this.gameEvent(net.minecraft.world.level.gameevent.GameEvent.EAT, this);
                return InteractionResult.SUCCESS;
            }
            // 何か持って右クリック = 持たせる ( しゃがみでオフハンド )。
            if (!stack.isEmpty())
                return this.giveHeldItem(player, stack, player.isShiftKeyDown() ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
            // 素手 + しゃがみ = 両手の持ち物を返してもらう。
            if (player.isShiftKeyDown())
                return this.takeBackHeldItems(player);
            // 素手 = おすわり切り替え。
            this.setOrderedToSit(!this.isOrderedToSit());
            this.jumping = false;
            this.getNavigation().stop();
            this.setTarget(null);
            return InteractionResult.SUCCESS;
        } else if (this.isFood(stack)) {
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
            if (this.random.nextInt(3) == 0) {
                this.tame(player);
                this.getNavigation().stop();
                this.setTarget(null);
                this.setOrderedToSit(true);
                this.level().broadcastEntityEvent(this, (byte) 7); // ハートの粒子
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6); // 煙の粒子
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** 飼い主が渡したアイテムを指定の手に持たせ、そこに入っていたものは飼い主へ返す。 */
    private InteractionResult giveHeldItem(Player player, ItemStack stack, EquipmentSlot slot) {
        ItemStack given = stack.copy();
        given.setCount(1);
        ItemStack previous = this.getItemBySlot(slot);
        this.setItemSlot(slot, given);
        // 1.0 を超える値にすると死亡時に必ず、しかも無傷でドロップする。預けた物を失わせない。
        this.setDropChance(slot, GIVEN_ITEM_DROP_CHANCE);
        if (!player.getAbilities().instabuild)
            stack.shrink(1);
        if (!previous.isEmpty())
            this.returnToPlayer(player, previous);
        this.playSound(SoundEvents.ARMOR_EQUIP_GENERIC, 0.7f, 1.5f);
        this.swing(InteractionHand.MAIN_HAND);
        return InteractionResult.SUCCESS;
    }

    /** 両手の持ち物を飼い主へ返す。 */
    private InteractionResult takeBackHeldItems(Player player) {
        boolean returnedAny = false;
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND }) {
            ItemStack held = this.getItemBySlot(slot);
            if (held.isEmpty())
                continue;
            this.setItemSlot(slot, ItemStack.EMPTY);
            this.setDropChance(slot, 0.0f);
            this.returnToPlayer(player, held);
            returnedAny = true;
        }
        if (!returnedAny)
            return InteractionResult.PASS;
        this.playSound(SoundEvents.ITEM_PICKUP, 0.7f, 1.5f);
        return InteractionResult.SUCCESS;
    }

    /** インベントリに入らない分はプレイヤーの足元に落とす。 */
    private void returnToPlayer(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack))
            player.drop(stack, false);
    }

    /**
     * まばたき中か。表情の差し替えだけなので同期はせず、個体IDとtickから決め打ちで求める。
     * ( 素材の player_head スキンが通常と閉じ目の2枚あるのでそれを使う )
     */
    public boolean isBlinking() {
        if (!this.isAlive())
            return false;
        return Math.floorMod(this.tickCount + this.getId() * 17, BLINK_PERIOD) < BLINK_TICKS;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.35f; // ちび体型に合わせて高い声にする
    }
}
