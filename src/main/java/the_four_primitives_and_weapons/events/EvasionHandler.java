package the_four_primitives_and_weapons.events;

import the_four_primitives_and_weapons.init.MawExtraAttributes;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 回避率アトリビュート (the_four_primitives_and_weapons:evasion) による自動回避と反射。
 *
 * 敵（他エンティティ）からの攻撃を受けたとき、回避率(%)の確率でダメージ・ノックバックごと
 * 攻撃を「流す」。矢などの飛び道具は当たらない位置で停止させ、刺さったり再命中したりしないようにする。
 * 判定はサーバー側でのみ行う。
 *
 * 100 を超えていると、回避した攻撃を攻撃者へ跳ね返す。跳ね返す量は 100 ごとの段階式。
 *   101〜200 → 敵ダメージの 5% / 201〜300 → 10% / 301〜400 → 15% ... （100 ごとに +5%）
 *   上限は設けていないため、2001 以上なら 100% 以上（等倍超え）の反射になる。
 * 近接攻撃は thorns ダメージとして返し、飛び道具は攻撃力を倍率ぶん変更したうえで射手へ撃ち返す。
 *
 * 設定例:
 *   /attribute @s the_four_primitives_and_weapons:evasion base set 25
 *   /give @s minecraft:iron_chestplate{AttributeModifiers:[
 *       {AttributeName:"the_four_primitives_and_weapons:evasion",
 *        Amount:15,Operation:0,UUID:[I;1,2,3,4],Slot:"chest"}]}
 *   （Name は WeaponStatsMixin が "evasion" で補完するため不要）
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class EvasionHandler {

    /** 反射で与えたダメージがさらに反射されるのを防ぐための再入ガード。 */
    private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return; // 乱数判定はサーバー側のみ（クライアントで別結果になるのを防ぐ）
        }

        DamageSource source = event.getSource();
        if (!isEvadable(target, source)) {
            return;
        }

        AttributeInstance instance = target.getAttribute(MawExtraAttributes.EVASION.get());
        if (instance == null) {
            return;
        }
        double value = instance.getValue();
        if (value <= 0.0) {
            return;
        }

        // 100 までが回避率。100 を超えていれば回避に加えて反射する
        double evadeChance = Math.min(value, 100.0);
        if (target.getRandom().nextDouble() * 100.0 >= evadeChance) {
            return;
        }

        event.setCanceled(true);

        boolean canReflect = value > 100.0 && !REFLECTING.get();

        if (source.getDirectEntity() instanceof Projectile projectile && projectile != target) {
            double speed = projectile.getDeltaMovement().length();
            pushOutOfHitbox(projectile, target);
            if (canReflect && reflectProjectile(projectile, target, source, reflectRatio(value), speed)) {
                return; // 撃ち返したので停止させない
            }
            stopProjectile(projectile);
            return;
        }

        if (canReflect) {
            reflectDamage(target, source, event.getAmount() * reflectRatio(value));
        }
    }

    /** 押し出し先の余裕(ブロック)。プレイヤーの当たり判定から少し離す。 */
    private static final double PROJECTILE_CLEARANCE = 0.2;
    /** 真上にいる飛び道具を水平にずらす距離(ブロック)。 */
    private static final double OVERHEAD_OFFSET = 0.35;
    /** 撃ち返すときの最低速度。元の速度が落ちていても届くようにする。 */
    private static final float MIN_REFLECT_SPEED = 1.0F;

    /**
     * 回避した飛び道具を、その場で停止させる。
     *
     * バニラは命中に失敗した矢を跳ね返して残すため、そのままだと回避したのに矢が刺さって見えたり、
     * 跳ね返った矢が次の tick で再命中したりする。速度を 0 にして重力で落ちるだけの状態にする。
     */
    private static void stopProjectile(Projectile projectile) {
        projectile.setDeltaMovement(Vec3.ZERO);
        projectile.hasImpulse = true;
    }

    /**
     * 回避した飛び道具を射手へ撃ち返す。矢の攻撃力は反射倍率ぶん変更する。
     *
     * @param ratio 反射倍率（101〜200 なら 0.05）
     * @param speed 命中直前の速度。同じ速度で返すことで元の威力比を保つ
     * @return 撃ち返せたら true
     */
    private static boolean reflectProjectile(Projectile projectile, LivingEntity target,
                                             DamageSource source, float ratio, double speed) {
        if (!(source.getEntity() instanceof LivingEntity shooter) || shooter == target || !shooter.isAlive()) {
            return false;
        }

        if (projectile instanceof AbstractArrow arrow) {
            arrow.setBaseDamage(arrow.getBaseDamage() * ratio);
            arrow.setCritArrow(false);
        }
        // 所有者を回避した側へ移し、射手に当たるようにする
        projectile.setOwner(target);

        Vec3 aim = new Vec3(
            shooter.getX() - projectile.getX(),
            shooter.getEyeY() - projectile.getY(),
            shooter.getZ() - projectile.getZ());
        projectile.shoot(aim.x, aim.y, aim.z, (float) Math.max(speed, MIN_REFLECT_SPEED), 0.0F);
        projectile.hasImpulse = true;
        return true;
    }

    /**
     * 飛び道具がプレイヤーの当たり判定の中にいる場合、当たらない位置まで押し出す。
     * 真上（水平距離がほぼ 0）の場合は落ちてきて再命中しないよう少しだけずらす。
     */
    private static void pushOutOfHitbox(Projectile projectile, LivingEntity target) {
        Vec3 pos = projectile.position();
        double dx = pos.x - target.getX();
        double dz = pos.z - target.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double clearance = target.getBbWidth() / 2.0 + PROJECTILE_CLEARANCE;

        if (horizontal >= clearance && !target.getBoundingBox().inflate(PROJECTILE_CLEARANCE).contains(pos)) {
            return; // 既に当たらない位置なので動かさない
        }

        if (horizontal < 1.0E-4) {
            // 真上（または真下）で水平方向が定まらない場合はランダムな向きへ少しだけずらす
            double angle = target.getRandom().nextDouble() * (Math.PI * 2.0);
            dx = Math.cos(angle);
            dz = Math.sin(angle);
            horizontal = 1.0;
            clearance = Math.max(clearance, OVERHEAD_OFFSET);
        }

        double x = target.getX() + dx / horizontal * clearance;
        double z = target.getZ() + dz / horizontal * clearance;
        projectile.moveTo(x, pos.y, z, projectile.getYRot(), projectile.getXRot());
    }

    /** 回避可能な攻撃か判定する。 */
    private static boolean isEvadable(LivingEntity target, DamageSource source) {
        // /kill・奈落など、無敵を貫通するダメージは回避できない
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        // 落下・窒息・飢餓などの環境ダメージは対象外。「敵の攻撃」だけを流す
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (attacker == null && direct == null) {
            return false;
        }
        // 自傷（自分の爆発・自分の矢など）は回避しない
        return attacker != target && direct != target;
    }

    /**
     * 反射で跳ね返すダメージの倍率。100 ごとの段階式で 5% ずつ増え、上限はない。
     * 101〜200 → 5% / 201〜300 → 10% / 301〜400 → 15% ... / 2001〜2100 → 100%（等倍）
     */
    private static float reflectRatio(double value) {
        int tier = (int) Math.ceil((value - 100.0) / 100.0);
        if (tier <= 0) {
            return 0.0F;
        }
        return tier * 0.05F;
    }

    /**
     * 回避した攻撃を攻撃者へ跳ね返す。
     *
     * @param amount 反射倍率を掛けたあとのダメージ量
     * @return 実際に反射ダメージが通ったら true
     */
    private static boolean reflectDamage(LivingEntity target, DamageSource source, float amount) {
        LivingEntity attacker = null;
        if (source.getEntity() instanceof LivingEntity owner) {
            attacker = owner;
        } else if (source.getDirectEntity() instanceof LivingEntity direct) {
            // 矢などで撃った本人が不明な場合は、直接当たったエンティティを対象にする
            attacker = direct;
        }
        if (attacker == null || attacker == target || amount <= 0.0F || !attacker.isAlive()) {
            return false;
        }

        REFLECTING.set(Boolean.TRUE);
        try {
            // thorns 扱いで跳ね返す（攻撃者側の回避判定は通常どおり働く）
            return attacker.hurt(target.damageSources().thorns(target), amount);
        } finally {
            REFLECTING.set(Boolean.FALSE);
        }
    }
}
