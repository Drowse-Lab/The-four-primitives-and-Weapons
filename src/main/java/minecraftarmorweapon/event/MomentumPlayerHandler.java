package minecraftarmorweapon.event;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.init.CustomEntityInit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Momentum Hookshot のプレイヤー側挙動 — 元データパック {@code jump.mcfunction} の忠実移植.
 *
 * 元データパックの動作 (line-by-line 再検証):
 *   1. velocity リセット ({@code tp @s 0 0 0; tp ~ ~0.09 ~}) → vy=0, position +0.09
 *   2. {@code attribute knockback_resistance base set 1.0} → KB 耐性 (40 tick)
 *   3. {@code temp = 2*(hookY - playerY) + 20}, clamp to 127
 *      - temp ≤ 4 (フックが 8+ block 下): {@code levitation 1 180 + jump_boost 1 255} を 3 tick
 *        → 下向きでも激しく上昇 (item の説明文 "物理法則を完全に無視している" の正体)
 *      - temp ≥ 5: AEC で {@code levitation amp=temp} を 6 tick → 垂直方向に急上昇
 *   4. {@code col_slime} を {@code horizDist*5 + 5} 個 summon → 1 tick だけ存在する collision で
 *      水平方向に強烈に押し出す
 *
 * 結果: 着弾点を**通過**する momentum-swing 挙動。これが原作の「物理無視」の正体。
 *
 * 私の Java 移植:
 *   - levitation effect で垂直制御 (元と同じ amp 式)
 *   - horizontal は friction-compensated × overshoot で col_slime 等価
 *   - setDeltaMovement.y = 0 にして levitation だけが垂直を制御
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID)
public class MomentumPlayerHandler {

    public static final int FALL_RESIST_TICKS = 40;
    public static final int KB_RESIST_TICKS = 40;
    public static final int FLOAT_FUEL_MAX = 40;
    public static final int LEVITATION_DURATION_NORMAL = 6;
    public static final int LEVITATION_DURATION_DOWNWARD = 3;
    /** levitation amp の上限. 元データパックは 127 だが行き過ぎなので 100 に. */
    public static final int LEV_AMP_MAX = 100;
    /** 下向き分岐 (temp ≤ 4) の levitation/jump amp. 元 180/255 → 100 に控えめ化. */
    public static final int LEV_AMP_DOWNWARD = 100;
    public static final double AIR_FRICTION = 0.91;
    public static final double MAX_HORIZ_VEL = 10.0;
    /** col_slime 等価の overshoot 係数. 1.0 = 着弾点ピッタリ, 1.2 = 少し通り過ぎる. */
    public static final double HORIZ_OVERSHOOT = 1.2;
    public static final double LIFT_Y = 0.15;

    public static final String PD_FALL_RESIST = "MoH_FallResist";
    public static final String PD_KB_RESIST = "MoH_KbResist";
    public static final String PD_FLOAT_FUEL = "MoH_FloatFuel";

    /** 着弾点に向けてプレイヤーを射出する。MomentumHookEntity から呼ばれる。 */
    public static void launchPlayer(Player owner, Vec3 hookPos) {
        Vec3 from = owner.position();
        Vec3 toVec = hookPos.subtract(from);
        double dx = toVec.x;
        double dy = toVec.y;
        double dz = toVec.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        // (1) 既存 effect を除去 (重複/干渉防止)
        owner.removeEffect(MobEffects.SLOW_FALLING);
        owner.removeEffect(MobEffects.LEVITATION);
        owner.removeEffect(MobEffects.JUMP);

        // (2) Velocity リセット + lift (元 jump.mcfunction: tp @s 0 0 0; tp ~ ~0.09 ~)
        //    setDeltaMovement(0,0,0) で vy=0、setPos で 0.15 持ち上げ (地面摩擦回避)
        owner.setDeltaMovement(0, 0, 0);
        owner.setPos(owner.getX(), owner.getY() + LIFT_Y, owner.getZ());

        // (3) 垂直: levitation effect で 6 tick の急上昇 (元 jump.mcfunction の核心部分)
        //     temp = 2*(hookY - playerY) + 20, clamp 1〜127
        int temp = (int) Mth.clamp(2 * dy + 20, -100, 127);
        if (temp <= 4) {
            // 下向き分岐: フックがプレイヤーより 8+ block 下 → 救済の急上昇
            // 元: levitation 1 180 + jump_boost 1 255 (3 tick で E.Anchor_Effect=3 により clear)
            owner.addEffect(new MobEffectInstance(MobEffects.LEVITATION,
                LEVITATION_DURATION_DOWNWARD, LEV_AMP_DOWNWARD, false, false, false));
            owner.addEffect(new MobEffectInstance(MobEffects.JUMP,
                LEVITATION_DURATION_DOWNWARD, LEV_AMP_DOWNWARD, false, false, false));
        } else {
            // 通常: AEC で levitation amp = temp を 6 tick (元 NBT Effects[0].Amplifier:byte = temp)
            int amp = Math.min(LEV_AMP_MAX, temp);
            owner.addEffect(new MobEffectInstance(MobEffects.LEVITATION,
                LEVITATION_DURATION_NORMAL, amp, false, false, false));
        }

        // (4) 水平: friction-compensated impulse (元 col_slime push 等価)
        //     6 tick の levitation 持続中に着弾点を通過するよう設定
        //     air friction 0.91/tick の累積補正: cumDist = v0 * (1 - 0.91^T)/0.09
        double vx = 0, vz = 0;
        if (horizDist > 0.001) {
            double T = LEVITATION_DURATION_NORMAL;
            double horizFactor = (1.0 - Math.pow(AIR_FRICTION, T)) / (1.0 - AIR_FRICTION);
            vx = (dx / horizFactor) * HORIZ_OVERSHOOT;
            vz = (dz / horizFactor) * HORIZ_OVERSHOOT;
            // クランプ
            double mag = Math.sqrt(vx * vx + vz * vz);
            if (mag > MAX_HORIZ_VEL) {
                vx *= MAX_HORIZ_VEL / mag;
                vz *= MAX_HORIZ_VEL / mag;
            }
        }
        // setDeltaMovement の y は 0 — levitation effect が垂直を完全に制御
        owner.setDeltaMovement(vx, 0, vz);

        // (5) サーバー → クライアント同期
        owner.fallDistance = 0;
        owner.hurtMarked = true;
        owner.hasImpulse = true;

        // (6) 耐性カウンタ
        CompoundTag pd = owner.getPersistentData();
        pd.putInt(PD_FALL_RESIST, FALL_RESIST_TICKS);
        pd.putInt(PD_KB_RESIST, KB_RESIST_TICKS);
        pd.putInt(PD_FLOAT_FUEL, 0);

        // (7) 効果音 (元 jump.mcfunction: goat.long_jump + zombie.infect + witch.throw)
        Level lv = owner.level();
        lv.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
            SoundEvents.GOAT_LONG_JUMP, SoundSource.PLAYERS, 1.5f, 1.2f);
        lv.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
            SoundEvents.ZOMBIE_INFECT, SoundSource.PLAYERS, 1.5f, 2.0f);
        lv.playSound(null, owner.getX(), owner.getY(), owner.getZ(),
            SoundEvents.WITCH_THROW, SoundSource.PLAYERS, 1.5f, 1.0f);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player p = event.player;
        if (p.level().isClientSide) return;
        CompoundTag pd = p.getPersistentData();

        if (pd.contains(PD_FALL_RESIST)) {
            int t = pd.getInt(PD_FALL_RESIST) - 1;
            if (t <= 0) pd.remove(PD_FALL_RESIST);
            else pd.putInt(PD_FALL_RESIST, t);
        }
        if (pd.contains(PD_KB_RESIST)) {
            int t = pd.getInt(PD_KB_RESIST) - 1;
            if (t <= 0) pd.remove(PD_KB_RESIST);
            else pd.putInt(PD_KB_RESIST, t);
        }

        // Sneak Float (slow_falling のみ — 上には浮かない、launch 中は無効)
        if (isHoldingHookshot(p) && !pd.contains(PD_FALL_RESIST)) {
            int fuel = pd.getInt(PD_FLOAT_FUEL);
            if (p.isShiftKeyDown() && !p.onGround() && !p.isInWater() && !p.isFallFlying()) {
                if (fuel < FLOAT_FUEL_MAX) {
                    p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 5, 1,
                        false, false, false));
                    pd.putInt(PD_FLOAT_FUEL, fuel + 1);
                    if (fuel == 20 || fuel == 30) {
                        p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                            SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0f, 2.0f);
                    } else if (fuel == FLOAT_FUEL_MAX - 1) {
                        p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                            SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                }
            } else if (p.onGround() && fuel != 0) {
                pd.putInt(PD_FLOAT_FUEL, 0);
            }
        } else if (pd.getInt(PD_FLOAT_FUEL) != 0 && p.onGround()) {
            pd.putInt(PD_FLOAT_FUEL, 0);
        }
    }

    private static boolean isHoldingHookshot(Player p) {
        ItemStack m = p.getMainHandItem();
        if (m.getItem() == CustomEntityInit.MOMENTUM_HOOKSHOT.get()) return true;
        return p.getOffhandItem().getItem() == CustomEntityInit.MOMENTUM_HOOKSHOT.get();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player p) {
            CompoundTag pd = p.getPersistentData();
            if (pd.contains(PD_FALL_RESIST)) {
                event.setDistance(0f);
                event.setDamageMultiplier(0f);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player p) {
            CompoundTag pd = p.getPersistentData();
            if (pd.contains(PD_KB_RESIST)) {
                event.setStrength(event.getStrength() * 0.05f);
            }
        }
    }
}
