package the_four_primitives_and_weapons.events;

import the_four_primitives_and_weapons.util.DamageCalculator;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.*;

/**
 * ダッシュ専用スキルのtick処理ハンドラ。
 * 突進斬り: WASD方向に直線的に走り抜けてダメージ
 * 跳ね斬り: WASD方向に跳躍して遠くに飛ぶ、ダメージ増加バフ
 * シャドーステップ: 5tick無敵、自由に高速移動
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class DashSkillHandler {

    // === 突進斬り (Dash Rush) ===
    private static final Map<UUID, DashRushState> dashRushStates = new HashMap<>();

    static class DashRushState {
        int remainingTicks = 10;
        float baseDamage = 8.0f;
        Set<Integer> hitEntities = new HashSet<>();
        ElementType element = ElementType.NONE;
    }

    // === シャドーステップ (Shadow Step) ===
    private static final Map<UUID, ShadowStepState> shadowStepStates = new HashMap<>();

    static class ShadowStepState {
        int remainingTicks = 100; // 5秒間
        String weaponClass;
        ElementType element = ElementType.NONE;
    }

    // === 跳ね斬りダメージバフ ===
    private static final Map<UUID, Integer> leapSlashBuffTimers = new HashMap<>();
    private static final float LEAP_SLASH_DAMAGE_MULTIPLIER = 1.5f;

    // === 最後に使用したダッシュスキルの属性と種類を記録 ===
    private static final Map<UUID, ElementType> lastDashElement = new HashMap<>();
    private static final Map<UUID, Boolean> lastDashWasShadowStep = new HashMap<>();

    // === 武器属性取得ヘルパー ===
    private static ElementType getWeaponElement(Player player) {
        // まず武器のNBTから属性を取得
        ElementType weaponElement = ElementalDamageUtils.getElementType(player.getMainHandItem());
        if (weaponElement != ElementType.NONE) {
            return weaponElement;
        }
        // 武器に属性がなければCuriosのbookスロットの魔導書から属性を取得
        return getBookSlotElement(player);
    }

    /**
     * Curiosのbookスロットにある魔導書から属性を判定する
     */
    private static ElementType getBookSlotElement(Player player) {
        try {
            java.util.concurrent.atomic.AtomicReference<ElementType> result =
                    new java.util.concurrent.atomic.AtomicReference<>(ElementType.NONE);
            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
                handler.getStacksHandler("book").ifPresent(stacksHandler -> {
                    for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            String itemName = stack.getItem().getClass().getSimpleName();
                            switch (itemName) {
                                case "FireballItem":
                                    result.set(ElementType.FIRE); return;
                                case "ThunderboltItem":
                                    result.set(ElementType.THUNDER); return;
                                case "BubbleshotItem":
                                    result.set(ElementType.WATER); return;
                                case "StormItem":
                                case "WindStepItem":
                                    result.set(ElementType.WIND); return;
                                case "DarknessItem":
                                    result.set(ElementType.DARK); return;
                            }
                        }
                    }
                });
            });
            return result.get();
        } catch (Exception e) {
            return ElementType.NONE;
        }
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
        state.element = getWeaponElement(player);
        lastDashElement.put(player.getUUID(), state.element);
        lastDashWasShadowStep.put(player.getUUID(), false);
        dashRushStates.put(player.getUUID(), state);

        // 視線方向に突進（Y成分も含む、真上で約4ブロック）
        Vec3 look = player.getLookAngle();
        Vec3 moveDir = getMovementDirection(player);
        double yComponent = Math.max(look.y * 0.85, -0.2);
        // WIND: 機動力UP（速度1.3倍）
        double speedScale = state.element == ElementType.WIND ? 3.25 : 2.5;
        player.setDeltaMovement(moveDir.scale(speedScale).add(0, yComponent + 0.1, 0));
        player.hurtMarked = true;

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
        // 真上を向いている場合は何もしない
        Vec3 lookCheck = player.getLookAngle();
        if (lookCheck.y > 0.7) {
            return;
        }

        // WASD入力がある場合のみ水平移動、なければその場で跳ねるだけ
        float forward = player.zza;
        float strafe = player.xxa;
        ElementType element = getWeaponElement(player);
        lastDashElement.put(player.getUUID(), element);
        lastDashWasShadowStep.put(player.getUUID(), false);
        // WIND: 機動力UP（速度1.3倍）
        double windMult = element == ElementType.WIND ? 1.3 : 1.0;
        if ((forward != 0 || strafe != 0) && player.isSprinting()) {
            // ダッシュ+WASD入力: 約7マス飛ぶ
            Vec3 moveDir = getMovementDirection(player);
            Vec3 look = player.getLookAngle();
            double yComponent = Math.max(look.y * 0.5, 0.15);
            player.setDeltaMovement(moveDir.scale(2.0 * windMult).add(0, yComponent + 0.55, 0));
        } else if (forward != 0 || strafe != 0) {
            // 歩き+WASD入力: 短めに飛ぶ
            Vec3 moveDir = getMovementDirection(player);
            Vec3 look = player.getLookAngle();
            double yComponent = Math.max(look.y * 0.5, 0.15);
            player.setDeltaMovement(moveDir.scale(1.0 * windMult).add(0, yComponent + 0.35, 0));
        } else {
            // その場ジャンプのみ
            player.setDeltaMovement(0, 0.5, 0);
        }
        player.hurtMarked = true;

        // 独自ダメージ増加バフ（40tick = 2秒間）
        leapSlashBuffTimers.put(player.getUUID(), 40);

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
        state.element = getWeaponElement(player);
        lastDashElement.put(player.getUUID(), state.element);
        lastDashWasShadowStep.put(player.getUUID(), true);
        // DARK: 効果時間延長（100tick→150tick）
        if (state.element == ElementType.DARK) {
            state.remainingTicks = 150;
        }
        shadowStepStates.put(player.getUUID(), state);

        // 初期移動 (WASD 入力があれば移動、なければその場に留まる)
        // ※ y は 0 以下に clamp して上方向への飛行を禁止 (見上げて空中飛行バグの対策)。
        //   下方向 (look.y < 0) は維持して降下は可能にする。
        // ※ 速度は 3.0 → 1.0 に短縮 (リクエスト: 距離をもっと短く)。
        float fwd = player.zza;
        float str = player.xxa;
        if (fwd != 0 || str != 0) {
            Vec3 look = player.getLookAngle();
            Vec3 moveDir = getMovementDirection(player);
            double yComponent = Math.max(Math.min(look.y * 0.5, 0.0), -0.2);
            player.setDeltaMovement(moveDir.scale(1.0).add(0, yComponent, 0));
        } else {
            player.setDeltaMovement(0, 0, 0);
        }
        player.hurtMarked = true;

        // レンダー側で完全非表示にする（setInvisibleは使わない）

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

        // クライアント側はキャンセルチェックのみ（タイマー管理はサーバーのみ）
        if (player.level().isClientSide) {
            ShadowStepState shadowStep = shadowStepStates.get(id);
            if (shadowStep != null) {
                checkShadowStepCancelClient(player, shadowStep);
            }
            return;
        }

        // === 以下サーバー側のみ ===

        // 突進斬り処理
        DashRushState dashRush = dashRushStates.get(id);
        if (dashRush != null) {
            tickDashRush(player, dashRush);
            dashRush.remainingTicks--;
            if (dashRush.remainingTicks <= 0) {
                dashRushStates.remove(id);
            }
        }

        // 跳ね斬りバフタイマー
        Integer leapTimer = leapSlashBuffTimers.get(id);
        if (leapTimer != null) {
            if (leapTimer <= 1) {
                leapSlashBuffTimers.remove(id);
            } else {
                leapSlashBuffTimers.put(id, leapTimer - 1);
            }
        }

        // シャドーステップ処理
        ShadowStepState shadowStep = shadowStepStates.get(id);
        if (shadowStep != null) {
            tickShadowStepServer(player, shadowStep);
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

        // プレイヤー周辺の敵にダメージ（同じ敵に2回当たらない）。 武器の attack_range で半径を伸縮。
        double reach = Math.max(0.5, 1.5 + the_four_primitives_and_weapons.skill.WeaponStatsRegistry
                .attackRangeBonus(player.getItemInHand(InteractionHand.MAIN_HAND)));
        AABB hitBox = new AABB(
                pos.x - reach, pos.y, pos.z - reach,
                pos.x + reach, pos.y + 2, pos.z + reach);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                e -> e != player && !state.hitEntities.contains(e.getId()));

        for (LivingEntity target : targets) {
            state.hitEntities.add(target.getId());
            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            DamageCalculator.dealDamage(player, target, state.baseDamage, weapon);

            // ノックバック（WATER: 強化ノックバック）
            double knockScale = state.element == ElementType.WATER ? 1.2 : 0.5;
            Vec3 knockback = target.position().subtract(pos).normalize().scale(knockScale);
            target.setDeltaMovement(knockback.x, 0.3, knockback.z);

            // FIRE: 引火
            if (state.element == ElementType.FIRE) {
                target.setSecondsOnFire(3);
            }
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

        // WASD 入力に応じて移動、入力なしはその場に留まる
        // ※ activateShadowStep と同じく、y は 0 以下に clamp して空中飛行を禁止。
        // ※ 速度 3.0 → 1.0 に短縮 (距離もっと短く)。
        float forward = player.zza;
        float strafe = player.xxa;
        if (forward != 0 || strafe != 0) {
            Vec3 look = player.getLookAngle();
            Vec3 moveDir = getMovementDirection(player);
            double yComp = Math.max(Math.min(look.y * 0.5, 0.0), -0.2);
            player.setDeltaMovement(moveDir.scale(1.0).add(0, yComp, 0));
            player.hurtMarked = true;
        } else {
            // 入力なし: 移動を停止してその場に留まる
            player.setDeltaMovement(0, Math.min(player.getDeltaMovement().y, 0), 0);
            player.hurtMarked = true;
        }

        // 武器変更チェック（サーバー側）
        String currentWeaponClass = player.getMainHandItem().isEmpty() ? ""
                : player.getMainHandItem().getItem().getClass().getSimpleName();
        if (!currentWeaponClass.equals(state.weaponClass)) {
            endShadowStep(player, player.getUUID());
            return;
        }

        // 1ブロック障害物の自動ジャンプ
        {
            Vec3 moveDir2 = getMovementDirection(player);
            // プレイヤーの幅を考慮して複数地点をチェック
            for (double dist = 0.6; dist <= 1.5; dist += 0.4) {
                double checkX = pos.x + moveDir2.x * dist;
                double checkZ = pos.z + moveDir2.z * dist;
                int bx = (int) Math.floor(checkX);
                int by = (int) Math.floor(pos.y + 0.01);
                int bz = (int) Math.floor(checkZ);
                BlockPos wallPos = new BlockPos(bx, by, bz);

                BlockState wallBlock = player.level().getBlockState(wallPos);
                BlockState aboveWall = player.level().getBlockState(wallPos.above());
                BlockState aboveWall2 = player.level().getBlockState(wallPos.above(2));

                // 進行方向に壁があり、上2マスが空いていればジャンプ
                if (!wallBlock.getCollisionShape(player.level(), wallPos).isEmpty()
                        && aboveWall.getCollisionShape(player.level(), wallPos.above()).isEmpty()
                        && aboveWall2.getCollisionShape(player.level(), wallPos.above(2)).isEmpty()) {
                    Vec3 vel = player.getDeltaMovement();
                    // まだジャンプ中でなければ
                    if (vel.y < 0.2) {
                        player.setDeltaMovement(vel.x, 0.55, vel.z);
                        player.hurtMarked = true;
                    }
                    break;
                }
            }
        }

        // FIRE/WATER: 通過した敵に効果を与える
        if (state.element == ElementType.FIRE || state.element == ElementType.WATER) {
            AABB areaBox = new AABB(
                    pos.x - 1.5, pos.y, pos.z - 1.5,
                    pos.x + 1.5, pos.y + 2, pos.z + 1.5);
            List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, areaBox,
                    e -> e != player);
            for (LivingEntity target : nearby) {
                if (state.element == ElementType.FIRE) {
                    target.setSecondsOnFire(3);
                } else {
                    // WATER: ノックバック
                    Vec3 knockback = target.position().subtract(pos).normalize().scale(1.0);
                    target.setDeltaMovement(knockback.x, 0.3, knockback.z);
                }
            }
        }

        // 影のパーティクル（黒い靄）
        serverWorld.sendParticles(ParticleTypes.LARGE_SMOKE,
                pos.x, pos.y + 0.5, pos.z, 8, 0.4, 0.6, 0.4, 0.02);
        serverWorld.sendParticles(ParticleTypes.SQUID_INK,
                pos.x, pos.y + 1, pos.z, 4, 0.3, 0.4, 0.3, 0.01);
        // 足元に影っぽいパーティクル
        serverWorld.sendParticles(ParticleTypes.ASH,
                pos.x, pos.y + 0.1, pos.z, 6, 0.5, 0, 0.5, 0.01);
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

    // === ダメージ処理（シャドーステップ無効化 + 跳ね斬りバフ） ===

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // シャドーステップ中はダメージ無効
        if (event.getEntity() instanceof Player player) {
            if (shadowStepStates.containsKey(player.getUUID())) {
                event.setCanceled(true);
                return;
            }
        }

        // 跳ね斬りバフ中の攻撃はダメージ増加
        if (event.getSource().getEntity() instanceof Player attacker) {
            if (leapSlashBuffTimers.containsKey(attacker.getUUID())) {
                event.setAmount(event.getAmount() * LEAP_SLASH_DAMAGE_MULTIPLIER);
            }
        }
    }

    /**
     * シャドーステップ中にプレイヤーが entity 攻撃をしたら即座に解除。
     * (クライアント側の checkShadowStepCancelClient は client map しか消さないため、
     *  サーバー側 map に残ったままだと shadow step が継続してしまう。
     *  AttackEntityEvent は両側で fire するので、 両側の map から remove できる。)
     */
    @SubscribeEvent
    public static void onAttackEntity(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player == null) return;
        if (shadowStepStates.containsKey(player.getUUID())) {
            // 攻撃自体は通す (シャドーステップから攻撃で抜ける感覚)
            endShadowStep(player, player.getUUID());
        }
    }

    // === プレイヤー完全非表示（シャドーステップ中） ===

    @Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons", value = Dist.CLIENT)
    public static class ShadowStepClientHandler {
        @SubscribeEvent
        public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
            if (event.getEntity() instanceof Player player) {
                if (isInShadowStep(player)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    // === 公開API ===

    public static boolean isInShadowStep(Player player) {
        return shadowStepStates.containsKey(player.getUUID());
    }

    public static boolean isInDashRush(Player player) {
        return dashRushStates.containsKey(player.getUUID());
    }

    public static boolean hasLeapSlashBuff(Player player) {
        return leapSlashBuffTimers.containsKey(player.getUUID());
    }

    /** いずれかのダッシュスキルが実行中かどうか */
    public static boolean isAnyDashSkillActive(Player player) {
        UUID id = player.getUUID();
        return dashRushStates.containsKey(id) || shadowStepStates.containsKey(id);
    }

    /** 最後に使用したダッシュスキルの属性を取得 */
    public static ElementType getLastDashElement(Player player) {
        return lastDashElement.getOrDefault(player.getUUID(), ElementType.NONE);
    }

    /** 最後に使用したダッシュスキルがシャドーステップだったか */
    public static boolean wasLastDashShadowStep(Player player) {
        return lastDashWasShadowStep.getOrDefault(player.getUUID(), false);
    }
}
