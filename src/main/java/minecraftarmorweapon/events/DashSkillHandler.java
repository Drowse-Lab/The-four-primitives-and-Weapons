package minecraftarmorweapon.events;

import minecraftarmorweapon.util.DamageCalculator;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.client.Minecraft;

import java.util.*;

/**
 * ダッシュ専用スキルのtick処理ハンドラ。
 * 突進斬り: WASD方向に直線的に走り抜けてダメージ
 * 跳ね斬り: WASD方向に跳躍して遠くに飛ぶ、ダメージ増加バフ
 * シャドーステップ: 5tick無敵、自由に高速移動
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class DashSkillHandler {

    // === 突進斬り (Dash Rush) ===
    private static final Map<UUID, DashRushState> dashRushStates = new HashMap<>();

    static class DashRushState {
        int remainingTicks = 10;
        float baseDamage = 8.0f;
        Set<Integer> hitEntities = new HashSet<>();
    }

    // === シャドーステップ (Shadow Step) ===
    private static final Map<UUID, ShadowStepState> shadowStepStates = new HashMap<>();

    static class ShadowStepState {
        int remainingTicks = 60; // 3秒間
        String weaponClass;
    }

    // === WASD入力から移動方向を計算 ===

    /**
     * プレイヤーのWASD入力に基づく移動方向（水平）を返す。
     * 入力がない場合は視線方向の水平成分を使用。
     */
    private static Vec3 getMovementDirection(Player player) {
        float forward = player.zza;  // W/S
        float strafe = player.xxa;   // A/D

        if (forward == 0 && strafe == 0) {
            // 入力なし → 視線方向（水平成分）
            Vec3 look = player.getLookAngle();
            Vec3 horizontal = new Vec3(look.x, 0, look.z);
            return horizontal.length() > 0.001 ? horizontal.normalize() : new Vec3(0, 0, 1);
        }

        float yawRad = player.getYRot() * ((float) Math.PI / 180F);
        float sinYaw = (float) Math.sin(yawRad);
        float cosYaw = (float) Math.cos(yawRad);

        // 視線のyawに基づいてWASD入力をワールド座標に変換
        double moveX = strafe * cosYaw - forward * sinYaw;
        double moveZ = strafe * sinYaw + forward * cosYaw;

        Vec3 dir = new Vec3(moveX, 0, moveZ);
        return dir.length() > 0.001 ? dir.normalize() : new Vec3(0, 0, 1);
    }

    // === 起動メソッド（MotionExecutorから呼ばれる） ===

    /**
     * 突進斬り: WASD方向に即座に直線移動、通過した場所の敵にダメージ
     */
    public static void activateDashRush(Player player) {
        DashRushState state = new DashRushState();
        dashRushStates.put(player.getUUID(), state);

        // WASD方向に直線移動
        Vec3 moveDir = getMovementDirection(player);
        player.setDeltaMovement(moveDir.scale(2.5).add(0, 0.1, 0));
        player.hurtMarked = true; // サーバー→クライアント速度同期

        // 開始音
        Level world = player.level();
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.4f);

        if (!world.isClientSide) {
            player.displayClientMessage(Component.literal("\u00A7c\u7A81\u9032\u65AC\u308A\uFF01"), true);
        }
    }

    /**
     * 跳ね斬り: WASD方向に跳躍し、勢いで遠くに飛ぶ。着地までダメージ増加
     */
    public static void activateLeapSlash(Player player) {
        // WASD方向に跳躍（上方向 + 水平方向の勢い）
        Vec3 moveDir = getMovementDirection(player);
        player.setDeltaMovement(moveDir.scale(1.5).add(0, 0.7, 0));
        player.hurtMarked = true; // サーバー→クライアント速度同期

        // ダメージ増加バフ（Strength II, 40tick = 2秒間）
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, false, true));

        // エフェクトと音
        Level world = player.level();
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.6f);

        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            Vec3 pos = player.position();
            serverWorld.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    pos.x, pos.y + 1, pos.z, 10, 0.5, 0.5, 0.5, 0.1);
            player.displayClientMessage(
                    Component.literal("\u00A7e\u8DF3\u306D\u659C\u308A\uFF01 \u00A77\u653B\u6483\u3067\u30C0\u30E1\u30FC\u30B8\u5897\u52A0\uFF01"), true);
        }
    }

    /**
     * シャドーステップ: 3秒間無敵＋透明＋超高速移動、黒い靄だけが見える
     */
    public static void activateShadowStep(Player player) {
        ShadowStepState state = new ShadowStepState();
        state.weaponClass = player.getMainHandItem().isEmpty() ? ""
                : player.getMainHandItem().getItem().getClass().getSimpleName();
        shadowStepStates.put(player.getUUID(), state);

        // 初期移動（WASD方向に超高速移動）
        Vec3 moveDir = getMovementDirection(player);
        player.setDeltaMovement(moveDir.scale(3.0));
        player.hurtMarked = true;

        // 透明化
        player.setInvisible(true);

        // 開始エフェクト
        Level world = player.level();
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 2.0f);

        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            Vec3 pos = player.position();
            // 黒い靄の開始エフェクト
            serverWorld.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pos.x, pos.y + 1, pos.z, 20, 0.6, 0.6, 0.6, 0.05);
            serverWorld.sendParticles(ParticleTypes.SQUID_INK,
                    pos.x, pos.y + 1, pos.z, 10, 0.4, 0.4, 0.4, 0.02);
            player.displayClientMessage(Component.literal("\u00A78\u30B7\u30E3\u30C9\u30FC\u30B9\u30C6\u30C3\u30D7..."), true);
        }
    }

    // === Tick処理 ===

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        UUID id = player.getUUID();

        // 突進斬り処理
        DashRushState dashRush = dashRushStates.get(id);
        if (dashRush != null) {
            tickDashRush(player, dashRush);
            dashRush.remainingTicks--;
            if (dashRush.remainingTicks <= 0) {
                dashRushStates.remove(id);
            }
        }

        // シャドーステップ処理
        ShadowStepState shadowStep = shadowStepStates.get(id);
        if (shadowStep != null) {
            if (player.level().isClientSide) {
                checkShadowStepCancelClient(player, shadowStep);
            } else {
                tickShadowStepServer(player, shadowStep);
            }
            shadowStep.remainingTicks--;
            if (shadowStep.remainingTicks <= 0) {
                endShadowStep(player, id);
            }
        }
    }

    private static void tickDashRush(Player player, DashRushState state) {
        if (player.level().isClientSide) return;

        ServerLevel serverWorld = (ServerLevel) player.level();
        Vec3 pos = player.position();

        // プレイヤー周辺の敵にダメージ（同じ敵に2回当たらない）
        AABB hitBox = new AABB(
                pos.x - 1.5, pos.y, pos.z - 1.5,
                pos.x + 1.5, pos.y + 2, pos.z + 1.5);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e != player && !state.hitEntities.contains(e.getId()));

        for (LivingEntity target : targets) {
            state.hitEntities.add(target.getId());
            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            DamageCalculator.dealDamage(player, target, state.baseDamage, weapon);

            // ノックバック
            Vec3 knockback = target.position().subtract(pos).normalize().scale(0.5);
            target.setDeltaMovement(knockback.x, 0.3, knockback.z);
        }

        // 軌跡パーティクル
        serverWorld.sendParticles(ParticleTypes.SWEEP_ATTACK,
                pos.x, pos.y + 1, pos.z, 2, 0.5, 0.3, 0.5, 0);
        serverWorld.sendParticles(ParticleTypes.CLOUD,
                pos.x, pos.y + 0.1, pos.z, 3, 0.3, 0, 0.3, 0.01);
    }

    private static void tickShadowStepServer(Player player, ShadowStepState state) {
        ServerLevel serverWorld = (ServerLevel) player.level();
        Vec3 pos = player.position();

        // WASD入力に応じて超高速移動
        float forward = player.zza;
        float strafe = player.xxa;
        if (forward != 0 || strafe != 0) {
            Vec3 moveDir = getMovementDirection(player);
            player.setDeltaMovement(moveDir.scale(3.0));
            player.hurtMarked = true;
        }

        // 武器変更チェック（サーバー側）
        String currentWeaponClass = player.getMainHandItem().isEmpty() ? ""
                : player.getMainHandItem().getItem().getClass().getSimpleName();
        if (!currentWeaponClass.equals(state.weaponClass)) {
            endShadowStep(player, player.getUUID());
            return;
        }

        // 黒い靄のパーティクル（位置に残る煙のみ）
        serverWorld.sendParticles(ParticleTypes.LARGE_SMOKE,
                pos.x, pos.y + 0.5, pos.z, 8, 0.4, 0.6, 0.4, 0.02);
        serverWorld.sendParticles(ParticleTypes.SQUID_INK,
                pos.x, pos.y + 1, pos.z, 4, 0.3, 0.4, 0.3, 0.01);
    }

    @OnlyIn(Dist.CLIENT)
    private static void checkShadowStepCancelClient(Player player, ShadowStepState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) return;

        // 攻撃ボタンが押されたら解除
        if (mc.options.keyAttack.isDown()) {
            endShadowStep(player, player.getUUID());
        }
    }

    private static void endShadowStep(Player player, UUID id) {
        if (!shadowStepStates.containsKey(id)) return;
        shadowStepStates.remove(id);
        player.setInvisible(false);

        if (!player.level().isClientSide) {
            ServerLevel serverWorld = (ServerLevel) player.level();
            Vec3 pos = player.position();
            serverWorld.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pos.x, pos.y + 1, pos.z, 15, 0.5, 0.5, 0.5, 0.05);
            player.level().playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.5f);
            player.displayClientMessage(Component.literal("\u00A77\u30B7\u30E3\u30C9\u30FC\u30B9\u30C6\u30C3\u30D7\u7D42\u4E86"), true);
        }
    }

    // === ダメージ無効化（シャドーステップ中） ===

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (shadowStepStates.containsKey(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    // === 公開API ===

    public static boolean isInShadowStep(Player player) {
        return shadowStepStates.containsKey(player.getUUID());
    }

    public static boolean isInDashRush(Player player) {
        return dashRushStates.containsKey(player.getUUID());
    }

    /** いずれかのダッシュスキルが実行中かどうか */
    public static boolean isAnyDashSkillActive(Player player) {
        UUID id = player.getUUID();
        return dashRushStates.containsKey(id) || shadowStepStates.containsKey(id);
    }
}
