package the_four_primitives_and_weapons.status;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加護システム
 *
 * 効果:
 *   1. 霊視 — 周囲のエンティティを敵対/中立/友好で色分けして発光させる
 *        敵対 = 赤 (RED)
 *        中立 = 黄 (YELLOW)
 *        友好 = 緑 (GREEN)
 *
 *   2. 霊視デバフ — 加護保持者の視界を制限する
 *        - 通常の視界は失われる（インベントリ画面のみ操作可能な暗闇）
 *        - ブロックのエッジラインが薄く見える（シェーダー的な演出想定）
 *
 * 付与方法: クエスト/儀式
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class BlessingSystem {

    // 霊視範囲（ブロック）
    private static final double SPIRIT_VISION_RANGE = 32.0;

    // 発光の色分けに使うチームカラーは ChatFormatting で管理
    // Forge 1.20.1 では setGlowingTag + ScoreboardTeam で色付け可能

    // 加護保持者のUUID集合
    private static final Map<UUID, BlessingEntry> blessingMap = new ConcurrentHashMap<>();

    public static class BlessingEntry {
        public boolean active;
        public BlessingEntry() { this.active = true; }
    }

    // ────────────────────────────────────────────────────────────────
    // 公開API
    // ────────────────────────────────────────────────────────────────

    /** 加護を付与する */
    public static void grant(LivingEntity entity) {
        blessingMap.put(entity.getUUID(), new BlessingEntry());
    }

    /** 加護を解除する */
    public static void revoke(LivingEntity entity) {
        blessingMap.remove(entity.getUUID());
        // 周囲エンティティの発光を解除
        clearNearbyGlow(entity);
    }

    /** 加護を持っているか */
    public static boolean hasBlessing(LivingEntity entity) {
        return blessingMap.containsKey(entity.getUUID());
    }

    // ────────────────────────────────────────────────────────────────
    // TickHandler — 毎tick周囲エンティティに発光を適用
    // ────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (UUID uuid : blessingMap.keySet()) {
                net.minecraft.world.entity.Entity e = level.getEntity(uuid);
                if (!(e instanceof LivingEntity holder)) continue;

                // 霊視デバフを維持（MiasmaHealMixin と同じ方式で視界制限は
                // BlessingVisionMixin が handle する）
                applyGlowToNearby(holder, level);
            }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 発光処理
    // ────────────────────────────────────────────────────────────────

    private static void applyGlowToNearby(LivingEntity holder, ServerLevel level) {
        // 範囲内の全LivingEntityを取得
        level.getEntitiesOfClass(
                LivingEntity.class,
                holder.getBoundingBox().inflate(SPIRIT_VISION_RANGE)
        ).forEach(target -> {
            if (target == holder) return;

            SpiritGlowType glowType = classifyEntity(holder, target);
            SpiritGlowManager.setGlow(target, glowType);
        });
    }

    private static void clearNearbyGlow(LivingEntity holder) {
        if (!(holder.level() instanceof ServerLevel serverLevel)) return;
        serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                holder.getBoundingBox().inflate(SPIRIT_VISION_RANGE)
        ).forEach(SpiritGlowManager::clearGlow);
    }

    /**
     * エンティティを敵対/中立/友好に分類する。
     */
    private static SpiritGlowType classifyEntity(LivingEntity holder, LivingEntity target) {
        // プレイヤーは友好
        if (target instanceof Player) return SpiritGlowType.FRIENDLY;

        // 敵対Mob判定（Forge の KnownMobs や EntityClassification を参照）
        net.minecraft.world.entity.MobCategory category =
                target.getType().getCategory();

        return switch (category) {
            case MONSTER -> SpiritGlowType.HOSTILE;
            case CREATURE, AMBIENT, WATER_CREATURE, WATER_AMBIENT -> SpiritGlowType.NEUTRAL;
            default -> SpiritGlowType.NEUTRAL;
        };
    }
}
