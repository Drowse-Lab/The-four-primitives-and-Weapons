package the_four_primitives_and_weapons.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.init.CustomEntityInit;
import the_four_primitives_and_weapons.item.KnifeLauncherItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * HOMING ナイフ用「カーソルロック」の状態管理。
 *
 *   - サーバー tick で各プレイヤーの視線先 LivingEntity をレイキャスト
 *   - 同じ相手に一定 tick (LOCK_HOLD_TICKS) 視線を保持すると "lock" 成立
 *   - ロック成立すると {@link Player#getPersistentData()} に UUID を書き込む
 *   - 対象が死んだ / 見えない状態が LOCK_LOST_TICKS 続くとロック解除
 *   - 発射側 ({@code KnifeLauncherItem} / {@code ThrowingKnifeItem}) が
 *     {@link #getLockedTarget(Player)} を参照して spawn するナイフへ伝搬する
 *
 *   視認コストを抑えるため、KnifeLauncher を持っているプレイヤーのみ tick する。
 *   さらに 2 tick に 1 回だけ raycast して CPU を節約。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public final class HomingLockTracker {

    /** ロック成立に必要な連続視線保持 tick (2 秒 = 40 tick) */
    public static final int LOCK_HOLD_TICKS = 40;
    /** ロック中に対象を見失ってから解除までの猶予 tick */
    public static final int LOCK_LOST_GRACE = 100;
    /** 視線 raycast の最大距離 */
    public static final double RAY_REACH = 64.0;

    private static final String TAG_LOCK_UUID  = "MAW_HomingLockUUID";
    private static final String TAG_HOLD_UUID  = "MAW_HomingHoldUUID";
    private static final String TAG_HOLD_TICKS = "MAW_HomingHoldTicks";
    private static final String TAG_LOST_TICKS = "MAW_HomingLostTicks";

    private HomingLockTracker() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        // 負荷軽減: KnifeLauncher を持っていないなら tracking 不要
        if (!playerHoldsLauncher(sp)) return;
        // 2 tick に 1 回だけ処理
        if ((sp.tickCount & 1) != 0) return;

        CompoundTag pd = sp.getPersistentData();
        LivingEntity looking = raycastLookTarget(sp);

        if (looking == null) {
            // 見失い時は hold をリセット、ロック対象が生きていれば lost カウンタ進行
            pd.remove(TAG_HOLD_UUID);
            pd.remove(TAG_HOLD_TICKS);
            if (pd.hasUUID(TAG_LOCK_UUID)) {
                int lost = pd.getInt(TAG_LOST_TICKS) + 2;
                if (lost >= LOCK_LOST_GRACE) {
                    clearLock(sp);
                } else {
                    pd.putInt(TAG_LOST_TICKS, lost);
                }
            }
            return;
        }

        // 視線先 entity が死んでいたら無視
        if (!looking.isAlive()) return;

        UUID seen = looking.getUUID();
        UUID hold = pd.hasUUID(TAG_HOLD_UUID) ? pd.getUUID(TAG_HOLD_UUID) : null;
        if (!seen.equals(hold)) {
            // 別の対象を見始めた — hold 再スタート
            pd.putUUID(TAG_HOLD_UUID, seen);
            pd.putInt(TAG_HOLD_TICKS, 2);
        } else {
            int held = pd.getInt(TAG_HOLD_TICKS) + 2;
            pd.putInt(TAG_HOLD_TICKS, held);
            if (held >= LOCK_HOLD_TICKS) {
                // ロック成立 (既ロックと別相手なら置き換わる)
                UUID prior = pd.hasUUID(TAG_LOCK_UUID) ? pd.getUUID(TAG_LOCK_UUID) : null;
                if (!seen.equals(prior)) {
                    pd.putUUID(TAG_LOCK_UUID, seen);
                    pd.putInt(TAG_LOST_TICKS, 0);
                    sp.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "§6✦ ロックオン: " + looking.getDisplayName().getString()), true);
                }
            }
        }

        // 今ロック対象を見えているなら lost リセット
        if (pd.hasUUID(TAG_LOCK_UUID) && seen.equals(pd.getUUID(TAG_LOCK_UUID))) {
            pd.putInt(TAG_LOST_TICKS, 0);
        }
    }

    /** 発射時に呼ぶ: 現在ロック中の LivingEntity (生存) を返す。なし = null。 */
    public static LivingEntity getLockedTarget(Player player) {
        CompoundTag pd = player.getPersistentData();
        if (!pd.hasUUID(TAG_LOCK_UUID)) return null;
        UUID id = pd.getUUID(TAG_LOCK_UUID);
        if (!(player.level() instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(id);
        if (e instanceof LivingEntity le && le.isAlive()) return le;
        return null;
    }

    /** 発射時に呼ぶ: 現在ロック UUID (まだ成立中なら)。spawn 時に entity へ書き込む。 */
    public static UUID getLockedTargetUuid(Player player) {
        LivingEntity le = getLockedTarget(player);
        return le == null ? null : le.getUUID();
    }

    private static void clearLock(Player player) {
        CompoundTag pd = player.getPersistentData();
        pd.remove(TAG_LOCK_UUID);
        pd.remove(TAG_LOST_TICKS);
    }

    private static boolean playerHoldsLauncher(Player p) {
        var inv = p.getInventory();
        var target = CustomEntityInit.KNIFE_LAUNCHER.get();
        // hotbar + offhand を優先チェック (高頻度 path)
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).getItem() == target) return true;
        }
        if (inv.offhand.get(0).getItem() == target) return true;
        // 投げナイフ単体 HOMING 使用時用: 手持ちが homing knife
        ItemStack main = p.getMainHandItem();
        if (main.getItem() == CustomEntityInit.HOMING_KNIFE.get()) return true;
        return false;
    }

    /**
     * 視線方向を RAY_REACH まで飛ばして最初に当たる LivingEntity を返す。
     * 視線上の block で遮られている場合は null。
     */
    private static LivingEntity raycastLookTarget(Player p) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(RAY_REACH));

        // ブロック clip — 途中に不透明ブロックがあれば reach 短縮
        var bhr = p.level().clip(new net.minecraft.world.level.ClipContext(
            eye, end,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        double reach = bhr.getType() == net.minecraft.world.phys.HitResult.Type.MISS
            ? RAY_REACH
            : bhr.getLocation().distanceTo(eye);

        AABB aabb = p.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult ehr = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
            p.level(), p, eye, eye.add(look.scale(reach)), aabb,
            e -> e != p && e.isAlive() && e instanceof LivingEntity && !e.isSpectator());
        if (ehr == null) return null;
        return (LivingEntity) ehr.getEntity();
    }
}
