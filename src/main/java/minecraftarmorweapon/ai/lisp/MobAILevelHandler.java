package minecraftarmorweapon.ai.lisp;

import minecraftarmorweapon.command.CustomDifficultyCommand;
import minecraftarmorweapon.command.CustomDifficultyCommand.CustomDifficulty;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mob個体のAIレベルとLisp脳を管理するイベントハンドラ。
 *
 * - スポーン時に難易度に応じた個体レベルを割り当て
 * - MobAIBrainを作成し、ゲノムを設定
 * - 戦闘イベントでスコアを記録
 * - 死亡時にフィットネスを進化マネージャに報告
 * - AIレベルをPersistentDataに保存（NBT: "LispAILevel"）
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class MobAILevelHandler {

    private static final String NBT_AI_LEVEL = "LispAILevel";
    private static final Map<UUID, MobAIBrain> brains = new ConcurrentHashMap<>();

    // === イベント ===

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Monster monster)) return;

        // 既に脳がある場合はスキップ
        if (brains.containsKey(monster.getUUID())) return;

        CustomDifficulty diff = CustomDifficultyCommand.getCurrentDifficulty();
        if (diff.getAiLevel() < 1) return; // バニラ難易度以下はLisp AI無効

        // 個体レベルを決定
        int aiLevel = determineAILevel(monster, diff);

        // PersistentDataに保存
        monster.getPersistentData().putInt(NBT_AI_LEVEL, aiLevel);

        // 脳を作成
        String entityTypeId = getEntityTypeId(monster);
        MobAIBrain brain = new MobAIBrain(monster, aiLevel);
        brain.setGenome(AIEvolutionManager.getGenome(entityTypeId));
        brains.put(monster.getUUID(), brain);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // === JSONLイベントログ（Claude Code参照用） ===
        try {
            LivingEntity srcEnt = null;
            if (event.getSource().getEntity() instanceof LivingEntity le) srcEnt = le;
            String extra = "{\"damage_type\":\"" +
                event.getSource().typeHolder().unwrapKey()
                    .map(k -> k.location().toString()).orElse("unknown") + "\"}";
            CombatLogger.logEvent("hurt", srcEnt, event.getEntity(), event.getAmount(), extra);
        } catch (Exception ignored) {}

        // === プレイヤーの攻撃パターン記録 ===
        if (event.getSource().getEntity() instanceof Player player
            && event.getEntity() instanceof Mob targetMob) {
            PlayerBehaviorTracker.PlayerProfile profile =
                PlayerBehaviorTracker.getProfile(player.getUUID());

            // 攻撃距離・スプリント・側面判定
            double dist = player.distanceTo(targetMob);
            boolean sprinting = player.isSprinting();
            boolean flanked = isFlanked(player, targetMob);
            long tick = player.level().getGameTime();
            profile.recordAttack(tick, dist, sprinting, flanked);

            // 被弾側Mobのスコア記録
            MobAIBrain brain = brains.get(targetMob.getUUID());
            if (brain != null) {
                brain.recordDamageTaken(event.getAmount());
            }
        }

        // === Mobがプレイヤーを攻撃した ===
        if (event.getSource().getEntity() instanceof Mob mob
            && event.getEntity() instanceof Player player) {
            MobAIBrain brain = brains.get(mob.getUUID());
            if (brain != null) {
                brain.recordDamageDealt(event.getAmount());
            }

            // プレイヤーの盾ブロック判定
            PlayerBehaviorTracker.PlayerProfile profile =
                PlayerBehaviorTracker.getProfile(player.getUUID());
            if (player.isBlocking()) {
                profile.recordShieldBlock();
            } else {
                profile.recordHitReceived();
            }
        }

        // === Mob同士の戦闘 ===
        if (event.getSource().getEntity() instanceof Mob attackerMob
            && event.getEntity() instanceof Mob victimMob
            && !(event.getSource().getEntity() instanceof Player)) {
            MobAIBrain attackerBrain = brains.get(attackerMob.getUUID());
            if (attackerBrain != null) attackerBrain.recordDamageDealt(event.getAmount());
            MobAIBrain victimBrain = brains.get(victimMob.getUUID());
            if (victimBrain != null) victimBrain.recordDamageTaken(event.getAmount());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // === Mobの死亡 → 進化に報告 + ログ ===
        if (event.getEntity() instanceof Monster monster) {
            UUID id = monster.getUUID();
            MobAIBrain brain = brains.remove(id);
            if (brain != null) {
                // フィットネスを計算して進化マネージャに報告
                float fitness = brain.calculateFitness();
                String entityTypeId = getEntityTypeId(monster);
                String genome = brain.getGenomeString();

                if (!genome.isEmpty()) {
                    AIEvolutionManager.reportDeath(entityTypeId, genome, fitness);
                }

                // 戦闘ログ記録
                LivingEntity killer = event.getEntity().getKillCredit();
                CombatLogger.logMobDeath(monster, brain, killer);

                // プレイヤーの勝利を記録
                if (killer instanceof Player player) {
                    PlayerBehaviorTracker.getProfile(player.getUUID()).wins++;
                }
            }
        }

        // === プレイヤーの死亡 → ログ + Mob側の勝利記録 ===
        if (event.getEntity() instanceof Player player
            && event.getSource().getEntity() instanceof Mob killerMob) {
            MobAIBrain brain = brains.get(killerMob.getUUID());
            if (brain != null) {
                brain.recordKill();
                CombatLogger.logPlayerDeath(player, killerMob, brain);
            }
            // プレイヤーの敗北を記録
            PlayerBehaviorTracker.getProfile(player.getUUID()).losses++;
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        AIEvolutionManager.loadFromWorld(overworld);
        PlayerBehaviorTracker.loadFromWorld(overworld);
        ProgressionTracker.bindWorld(overworld);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        AIEvolutionManager.saveToWorld(overworld);
        PlayerBehaviorTracker.saveToWorld(overworld);
        brains.clear();
    }

    /**
     * プレイヤーがMobの正面以外から攻撃しているか（側面/背後）
     */
    private static boolean isFlanked(Player player, Mob mob) {
        double dx = player.getX() - mob.getX();
        double dz = player.getZ() - mob.getZ();
        double attackAngle = Math.atan2(dz, dx);
        double mobFacing = Math.toRadians(mob.getYRot());
        double diff = Math.abs(attackAngle - mobFacing);
        // 正面60度以外なら側面/背面判定
        return diff > Math.PI / 3;
    }

    // === AIレベル決定 ===

    /**
     * 難易度に応じた個体レベルを決定する。
     * 難易度が上がるとレベルの幅（個体差）が広がり、上限も上がる。
     *
     * aiLevel 1 (nightmare):      個体Lv 1〜2
     * aiLevel 2 (realistic):      個体Lv 1〜4
     * aiLevel 3 (lunatic):        個体Lv 2〜6
     * aiLevel 4 (lunatic+):       個体Lv 3〜8
     * aiLevel 5 (lunatic_extreme): 個体Lv 4〜10
     */
    private static int determineAILevel(Monster monster, CustomDifficulty diff) {
        int diffAiLevel = diff.getAiLevel();

        // NBTに既に保存されている場合はそれを使う
        CompoundTag data = monster.getPersistentData();
        if (data.contains(NBT_AI_LEVEL)) {
            return data.getInt(NBT_AI_LEVEL);
        }

        int minLevel = Math.max(1, diffAiLevel - 1);
        int maxLevel = Math.min(10, diffAiLevel * 2);
        int range = maxLevel - minLevel + 1;

        return minLevel + monster.getRandom().nextInt(range);
    }

    // === 公開API ===

    /**
     * 指定Mobの脳を取得する。
     */
    public static MobAIBrain getBrain(UUID entityId) {
        return brains.get(entityId);
    }

    /**
     * 指定MobのAIレベルを取得する（PersistentDataから）。
     * 未設定なら-1を返す。
     */
    public static int getAILevel(LivingEntity entity) {
        if (entity == null) return -1;
        CompoundTag data = entity.getPersistentData();
        return data.contains(NBT_AI_LEVEL) ? data.getInt(NBT_AI_LEVEL) : -1;
    }

    /**
     * AIレベルに対応する表示色コードを返す。
     */
    public static String getLevelColor(int level) {
        if (level <= 2) return "\u00A7a";       // 緑（弱い）
        if (level <= 4) return "\u00A7e";       // 黄色
        if (level <= 6) return "\u00A76";       // 金
        if (level <= 8) return "\u00A7c";       // 赤
        return "\u00A74";                        // 暗い赤（最強）
    }

    /**
     * AIレベルに対応するランク名を返す。
     */
    public static String getLevelRank(int level) {
        if (level <= 2) return "Lv." + level;
        if (level <= 4) return "Lv." + level;
        if (level <= 6) return "Lv." + level + " \u2605";         // ★
        if (level <= 8) return "Lv." + level + " \u2605\u2605";   // ★★
        return "Lv." + level + " \u2605\u2605\u2605";              // ★★★
    }

    private static String getEntityTypeId(Mob mob) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return id != null ? id.toString() : "unknown";
    }
}
