package minecraftarmorweapon.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 距離ベースMob強化システム
 *
 * /gamerule distanceScaling true で有効化
 * ワールドスポーン地点からの距離に応じてMobの基礎ステータスが上昇する。
 *
 * スケーリング計算:
 *   距離レベル = floor(距離 / 500)   ← 500ブロックごとに1レベル
 *   最大レベル = 20                  ← 10000ブロック以上で頭打ち
 *
 *   HP倍率       = 1.0 + level × 0.15  (最大 4.0x)
 *   攻撃力倍率    = 1.0 + level × 0.10  (最大 3.0x)
 *   防御力加算    = level × 1.0         (最大 +20)
 *   移動速度倍率  = 1.0 + level × 0.02  (最大 1.4x)
 *   追跡範囲加算  = level × 2.0         (最大 +40)
 */
@Mod.EventBusSubscriber
public class DistanceScalingHandler {

    private static boolean enabled = false;
    private static final String NBT_KEY = "minecraft_armor_weapon:distance_scaling";
    private static final Set<UUID> distanceScaled = ConcurrentHashMap.newKeySet();

    // スケーリングパラメータ
    private static final int BLOCKS_PER_LEVEL = 500;   // 500ブロックごとに1レベル
    private static final int MAX_LEVEL = 20;            // 最大レベル

    // ============================
    // コマンド登録: /gamerule distanceScaling true/false
    // ============================
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("gamerule")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("distanceScaling")
                .executes(context -> {
                    context.getSource().sendSuccess(() ->
                        Component.literal("§7distanceScaling: " + (enabled ? "§atrue" : "§cfalse")),
                        false
                    );
                    return 1;
                })
                .then(Commands.argument("value", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean value = BoolArgumentType.getBool(context, "value");
                        enabled = value;

                        // NBTに保存
                        ServerLevel level = context.getSource().getLevel();
                        net.minecraft.nbt.CompoundTag customData = level.getServer().getWorldData().getCustomBossEvents();
                        if (customData != null) {
                            customData.putBoolean(NBT_KEY, value);
                        }

                        context.getSource().sendSuccess(() ->
                            Component.literal("§7distanceScaling を " + (value ? "§a有効" : "§c無効") + " §7にしました"),
                            true
                        );

                        if (value) {
                            context.getSource().sendSuccess(() ->
                                Component.literal("§7スポーン地点から " + BLOCKS_PER_LEVEL + " ブロックごとにMobが強化されます (最大Lv" + MAX_LEVEL + ")"),
                                false
                            );
                        }

                        return 1;
                    })
                )
            )
        );
    }

    // ============================
    // サーバー起動時に復元
    // ============================
    @SubscribeEvent
    public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        net.minecraft.nbt.CompoundTag customData = event.getServer().getWorldData().getCustomBossEvents();
        if (customData != null && customData.contains(NBT_KEY)) {
            enabled = customData.getBoolean(NBT_KEY);
            System.out.println("距離スケーリング復元: " + (enabled ? "有効" : "無効"));
        }
    }

    // ============================
    // Mobスポーン時: 距離に応じたステータス強化
    // ============================
    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!enabled) {
            return;
        }
        if (event.getEntity() == null || !(event.getEntity() instanceof Monster monster)) {
            return;
        }
        if (monster.level().isClientSide) {
            return;
        }

        UUID id = monster.getUUID();
        if (distanceScaled.contains(id)) {
            return;
        }

        // ワールドスポーン地点からの距離を計算
        int spawnX = monster.level().getLevelData().getXSpawn();
        int spawnZ = monster.level().getLevelData().getZSpawn();
        BlockPos spawnPos = new BlockPos(spawnX, 0, spawnZ);
        BlockPos mobPos = monster.blockPosition();

        double dx = mobPos.getX() - spawnPos.getX();
        double dz = mobPos.getZ() - spawnPos.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        int level = Math.min((int)(distance / BLOCKS_PER_LEVEL), MAX_LEVEL);
        if (level <= 0) {
            return;
        }

        // --- ステータス強化の適用 ---
        applyDistanceScaling(monster, level);
        distanceScaled.add(id);
    }

    private static void applyDistanceScaling(Monster monster, int level) {
        // HP倍率: 1.0 + level × 0.15 (最大 4.0x)
        if (monster.getAttribute(Attributes.MAX_HEALTH) != null) {
            double hpMultiplier = 1.0 + level * 0.15;
            double baseHp = monster.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
            double newHp = baseHp * hpMultiplier;
            monster.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newHp);
            monster.setHealth((float) newHp);
        }

        // 攻撃力倍率: 1.0 + level × 0.10 (最大 3.0x)
        if (monster.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            double atkMultiplier = 1.0 + level * 0.10;
            double baseAtk = monster.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
            monster.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAtk * atkMultiplier);
        }

        // 防御力加算: level × 1.0 (最大 +20)
        if (monster.getAttribute(Attributes.ARMOR) != null) {
            double baseArmor = monster.getAttribute(Attributes.ARMOR).getBaseValue();
            monster.getAttribute(Attributes.ARMOR).setBaseValue(baseArmor + level * 1.0);
        }

        // 移動速度倍率: 1.0 + level × 0.02 (最大 1.4x)
        if (monster.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            double spdMultiplier = 1.0 + level * 0.02;
            double baseSpd = monster.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
            monster.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpd * spdMultiplier);
        }

        // 追跡範囲加算: level × 2.0 (最大 +40)
        if (monster.getAttribute(Attributes.FOLLOW_RANGE) != null) {
            double baseRange = monster.getAttribute(Attributes.FOLLOW_RANGE).getBaseValue();
            monster.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(baseRange + level * 2.0);
        }

        // ノックバック耐性加算: level × 0.03 (最大 +0.6)
        if (monster.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
            double baseKb = monster.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getBaseValue();
            double newKb = Math.min(1.0, baseKb + level * 0.03);
            monster.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(newKb);
        }

        // レベルが高いとカスタム名で距離レベルを表示（level 5以上）
        if (level >= 5) {
            String color;
            if (level >= 15) {
                color = "§4"; // 暗赤 (15+)
            } else if (level >= 10) {
                color = "§c"; // 赤 (10-14)
            } else {
                color = "§e"; // 黄 (5-9)
            }

            // Lv表示は AILevelRenderLayer が独自レンダーで行うため、ここでは setCustomName しない。
            // ブロック越しに見えるバニラネームプレートを使わない設計。
        }
    }

    // ============================
    // メモリクリーンアップ
    // ============================
    @SubscribeEvent
    public static void onMobDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof Monster) {
            distanceScaled.remove(event.getEntity().getUUID());
        }
    }

    // ============================
    // ユーティリティ
    // ============================
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 指定座標の距離レベルを取得（UI表示用）
     */
    public static int getDistanceLevel(Level world, BlockPos pos) {
        int spawnX = world.getLevelData().getXSpawn();
        int spawnZ = world.getLevelData().getZSpawn();
        BlockPos spawnPos = new BlockPos(spawnX, 0, spawnZ);
        double dx = pos.getX() - spawnPos.getX();
        double dz = pos.getZ() - spawnPos.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        return Math.min((int)(distance / BLOCKS_PER_LEVEL), MAX_LEVEL);
    }

    /**
     * 指定レベルのスケーリング情報テキストを生成（InfoCommand用）
     */
    public static String getScalingInfo(int level) {
        if (level <= 0) return "§7強化なし（スポーン地点付近）";
        return String.format(
            "§eLv%d §7(HP×%.1f, ATK×%.1f, DEF+%d, SPD×%.2f)",
            level,
            1.0 + level * 0.15,
            1.0 + level * 0.10,
            level,
            1.0 + level * 0.02
        );
    }
}
