package minecraftarmorweapon.trait;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プレイヤーの行動パターン追跡 — A-Life Mobが観測し学習した動きをパターン化する。
 *
 *  追跡対象アクション:
 *    SHIELD_BLOCK  — 盾構えの開始
 *    MELEE_ATTACK  — 近接攻撃
 *    BOW_SHOT      — 弓を撃った
 *    CROSSBOW_SHOT — クロスボウを撃った
 *    SPRINT_RUSH   — スプリント開始
 *    JUMP_DODGE    — ジャンプ (y速度 > 0.35)
 *    CONSUME       — 食事/回復ポーション使用
 *
 *  バイグラム (A→B) の出現頻度を記録し、
 *  任意のアクションAに対して最も多い後続Bを「予測」として返す。
 *
 *  予測成立条件: A後の記録が≥3件 かつ B/total(A→*) ≥ 50%
 */
@Mod.EventBusSubscriber
public class PlayerPatternTracker {

    public enum PlayerAction {
        SHIELD_BLOCK, MELEE_ATTACK, BOW_SHOT, CROSSBOW_SHOT, SPRINT_RUSH, JUMP_DODGE, CONSUME;
    }

    private static final class Bigram {
        final PlayerAction a, b;
        Bigram(PlayerAction a, PlayerAction b) { this.a = a; this.b = b; }
        @Override public boolean equals(Object o) {
            return o instanceof Bigram x && x.a == a && x.b == b;
        }
        @Override public int hashCode() { return a.ordinal() * 31 + b.ordinal(); }
    }

    static final class PlayerRecord {
        final Deque<PlayerAction> recent = new ArrayDeque<>();
        final Map<Bigram, Integer> bigrams = new HashMap<>();
        final Map<PlayerAction, Integer> totalAfter = new EnumMap<>(PlayerAction.class);
        boolean wasBlocking = false;
        boolean wasSprinting = false;
        boolean wasInAir = false;
        boolean wasUsingItem = false;
    }

    private static final Map<UUID, PlayerRecord> records = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 20;

    public static void recordAction(Player player, PlayerAction action) {
        UUID id = player.getUUID();
        PlayerRecord rec = records.computeIfAbsent(id, k -> new PlayerRecord());
        PlayerAction prev = rec.recent.peekLast();
        if (prev != null && prev != action) {
            rec.bigrams.merge(new Bigram(prev, action), 1, Integer::sum);
            rec.totalAfter.merge(prev, 1, Integer::sum);
        }
        rec.recent.offerLast(action);
        while (rec.recent.size() > MAX_HISTORY) rec.recent.pollFirst();

        // A-Life Mobに対策を促す
        LearningHandler.notifyPlayerAction(player, action);
    }

    /** 予測: current直後に最も出現頻度が高い行動を返す。根拠が弱い場合はnull。 */
    public static PlayerAction predictNext(UUID playerId, PlayerAction current) {
        PlayerRecord rec = records.get(playerId);
        if (rec == null) return null;
        int total = rec.totalAfter.getOrDefault(current, 0);
        if (total < 3) return null;

        PlayerAction best = null;
        int bestCount = 0;
        for (PlayerAction b : PlayerAction.values()) {
            if (b == current) continue;
            int c = rec.bigrams.getOrDefault(new Bigram(current, b), 0);
            if (c > bestCount) {
                bestCount = c;
                best = b;
            }
        }
        // 50%以上の支配率で予測成立
        if (bestCount * 2 >= total) return best;
        return null;
    }

    // ============================
    // イベント: 弓/クロスボウ発射・近接攻撃
    // ============================
    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getBow().getItem() instanceof BowItem) {
            recordAction(event.getEntity(), PlayerAction.BOW_SHOT);
        } else if (event.getBow().getItem() instanceof CrossbowItem) {
            recordAction(event.getEntity(), PlayerAction.CROSSBOW_SHOT);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide) return;
        recordAction(event.getEntity(), PlayerAction.MELEE_ATTACK);
    }

    // ============================
    // 毎tick: 盾構え/スプリント/ジャンプ/回復使用のエッジ検知
    // ============================
    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer p : level.players()) {
                observePlayer(p);
            }
        }
    }

    private static void observePlayer(Player player) {
        PlayerRecord rec = records.computeIfAbsent(player.getUUID(), k -> new PlayerRecord());

        // 盾構え
        boolean blocking = player.isBlocking();
        if (blocking && !rec.wasBlocking) recordAction(player, PlayerAction.SHIELD_BLOCK);
        rec.wasBlocking = blocking;

        // スプリント
        boolean sprinting = player.isSprinting();
        if (sprinting && !rec.wasSprinting) recordAction(player, PlayerAction.SPRINT_RUSH);
        rec.wasSprinting = sprinting;

        // ジャンプ (空中に出た瞬間)
        boolean inAir = !player.onGround();
        Vec3 vel = player.getDeltaMovement();
        if (inAir && !rec.wasInAir && vel.y > 0.30) {
            recordAction(player, PlayerAction.JUMP_DODGE);
        }
        rec.wasInAir = inAir;

        // 食事/ポーション使用完了
        boolean usingItem = player.isUsingItem();
        if (!usingItem && rec.wasUsingItem) {
            // アイテム使用完了の瞬間を検知
            recordAction(player, PlayerAction.CONSUME);
        }
        rec.wasUsingItem = usingItem;
    }

    /** 記録クリア (デバッグ/テスト用) */
    public static void clear(UUID playerId) {
        records.remove(playerId);
    }

    public static PlayerRecord getRecord(UUID playerId) {
        return records.get(playerId);
    }
}
