package the_four_primitives_and_weapons.events;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.util.CuriosElytraHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Curios「elytra」スロットでの滑空を実現するサーバー側ロジック (ElytraSlot mod 相当機能)。
 *
 * <p>バニラの {@code LivingEntity#updateFallFlying} は毎 tick チェスト装備の
 * {@code canElytraFly} を見て FALL_FLYING フラグを落とすため、Curios スロットの
 * エリトラでは飛行が継続しない。そこで Mixin を使わず、
 * {@code PlayerTickEvent(END, SERVER)} — updateFallFlying より後 — で
 * 「直前 tick まで滑空していた + 滑空継続条件を満たす + Curios にエリトラ」なら
 * {@link Player#startFallFlying()} でフラグを復元する。
 * エンティティデータの同期は tick 終了後に行われるため、tick 内での
 * false→true の揺れはクライアントには見えない。</p>
 *
 * <p>耐久消費は Forge 拡張 {@code ItemStack#elytraFlightTick} に委譲
 * (バニラ同様 1 秒毎に 1、ELYTRA_GLIDE GameEvent も発火)。</p>
 *
 * <p>離陸は {@link the_four_primitives_and_weapons.network.CuriosElytraTakeoffPacket}
 * から {@link #tryTakeoff(ServerPlayer)} が呼ばれる。</p>
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public class CuriosElytraFlightHandler {

    /** 直前 tick に滑空フラグが立っていたプレイヤー。 */
    private static final Set<UUID> wasFlying = ConcurrentHashMap.newKeySet();
    /** Curios エリトラでの連続滑空 tick 数 (耐久消費カウンタ)。 */
    private static final Map<UUID, Integer> flightTicks = new ConcurrentHashMap<>();

    /**
     * クライアントからの離陸要求 (空中でジャンプ) を処理する。
     * バニラ {@code Player#tryToStartFallFlying} と同条件 + Curios エリトラ判定。
     *
     * @return 離陸できたら true
     */
    public static boolean tryTakeoff(ServerPlayer player) {
        if (player.onGround() || player.isFallFlying() || player.isInWater()
                || player.isPassenger() || player.onClimbable()
                || player.getAbilities().flying
                || player.hasEffect(MobEffects.LEVITATION)) {
            return false;
        }
        // チェストのエリトラで飛べるならバニラに任せる (二重処理防止)
        if (player.getItemBySlot(EquipmentSlot.CHEST).canElytraFly(player)) {
            return false;
        }
        ItemStack elytra = CuriosElytraHelper.findFlyableElytra(player);
        if (elytra.isEmpty()) {
            return false;
        }
        player.startFallFlying();
        wasFlying.add(player.getUUID());
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        Player player = event.player;
        UUID id = player.getUUID();

        boolean flying = player.isFallFlying();

        // updateFallFlying がフラグを落とした直後の復元判定。
        // 継続条件はバニラ updateFallFlying と同じ ( onGround / passenger / levitation で終了 )。
        if (!flying && wasFlying.contains(id)
                && !player.onGround() && !player.isPassenger()
                && !player.hasEffect(MobEffects.LEVITATION)
                && !player.getAbilities().flying) {
            ItemStack elytra = CuriosElytraHelper.findFlyableElytra(player);
            if (!elytra.isEmpty()) {
                player.startFallFlying();
                flying = true;
                int ticks = flightTicks.merge(id, 1, Integer::sum);
                // バニラ ElytraItem と同じ耐久消費 (20tick 毎に 1) + GameEvent
                elytra.elytraFlightTick(player, ticks);
            }
        }

        if (flying) {
            wasFlying.add(id);
            // チェスト側エリトラで飛行中 ( バニラ処理 ) はカウンタ不要
            if (player.getItemBySlot(EquipmentSlot.CHEST).canElytraFly(player)) {
                flightTicks.remove(id);
            }
        } else {
            wasFlying.remove(id);
            flightTicks.remove(id);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        wasFlying.remove(id);
        flightTicks.remove(id);
    }
}
