package the_four_primitives_and_weapons.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;

import the_four_primitives_and_weapons.damage.SpecialDebuffHandler;
import the_four_primitives_and_weapons.skill.PlayerSkillData;
import the_four_primitives_and_weapons.skill.PlayerSkillData.AttackSlot;
import the_four_primitives_and_weapons.skill.MotionExecutor;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import the_four_primitives_and_weapons.procedures.SwordOfNightTpProcedure;
import the_four_primitives_and_weapons.procedures.SwordOfNightShotProcedure;
import the_four_primitives_and_weapons.procedures.SwordOfNightChargingGlowProcedure;
import the_four_primitives_and_weapons.procedures.MagicKatanaSpecialChargeProcedure;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModEnchantments;
import the_four_primitives_and_weapons.network.AttackPacket;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.util.DamageCalculator;
import the_four_primitives_and_weapons.skill.ElectricDischargeBurstSkill;
import the_four_primitives_and_weapons.skill.ElectricBeamSkill;
import the_four_primitives_and_weapons.skill.ElectricSlashSkill;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import the_four_primitives_and_weapons.damage.ElementType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class ChargedAttackHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChargedAttackHandler.class);

    private static final Map<UUID, ChargeData> playerChargeData = new HashMap<>();
    private static final int MAX_CHARGE_TIME = 60; // 3秒 (20 ticks/秒 × 3)
    private static final int MIN_CHARGE_TIME = 20; // 最小チャージ時間 1秒
    
    private static class ChargeData {
        boolean isCharging = false;
        int chargeTime = 0;
        ItemStack chargingItem = ItemStack.EMPTY;
        long lastAttackTime = 0;
        boolean wasLeftClickPressed = false;
        int clickReleaseTimer = 0;
        int comboCounter = 0; // 連撃カウンター
        boolean isFallingCharge = false; // 落下中のチャージ
        int fallTime = 0; // 落下時間
        int chargeCooldown = 0; // チャージ攻撃後のクールダウン
        boolean maxChargeNotified = false; // 最大チャージ到達の「ピン」を鳴らしたか ( 1 回だけ )

        void reset() {
            isCharging = false;
            chargeTime = 0;
            chargingItem = ItemStack.EMPTY;
            isFallingCharge = false;
            fallTime = 0;
            maxChargeNotified = false; // 次のチャージで再び鳴らせるように
            // クールダウンは維持
        }

        void resetCombo() {
            comboCounter = 0;
        }
    }
    
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Sword of Nightの発光効果を管理
        SwordOfNightChargingGlowProcedure.managGlowTicks(entity);
    }
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        UUID playerId = player.getUUID();
        ChargeData data = playerChargeData.computeIfAbsent(playerId, k -> new ChargeData());

        // チャージクールダウンのカウントダウン
        if (data.chargeCooldown > 0) {
            data.chargeCooldown--;
        }

        // クライアント側で左クリックの状態を検出
        if (player.level().isClientSide) {
            checkMouseInput(player, data);
        }

        // チャージ中の処理
        if (data.isCharging && !data.chargingItem.isEmpty()) {
            data.chargeTime++;
            
            // チャージエフェクト
            if (data.chargeTime % 10 == 0) {
                displayChargeEffect(player, data.chargeTime);
            }
            
            // Sword of Nightの場合、ターゲットを発光させる
            String itemName = data.chargingItem.getItem().getClass().getSimpleName();
            if (itemName.equals("SwordOfNightItem") && !player.level().isClientSide) {
                SwordOfNightChargingGlowProcedure.execute(
                    player.level(), 
                    player.getX(), 
                    player.getY(), 
                    player.getZ(), 
                    player, 
                    true // チャージ中
                );
            }
            
            // 最大チャージ到達
            if (data.chargeTime >= MAX_CHARGE_TIME) {
                // player.displayClientMessage(Component.literal("§e最大チャージ！"), true);
                if (!player.level().isClientSide) {
                    ((ServerLevel) player.level()).sendParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1, player.getZ(),
                        10, 0.5, 0.5, 0.5, 0.1
                    );
                }
                // 最大まで貯まった「瞬間」に 1 回だけ「ピン」と鳴らす
                if (!data.maxChargeNotified) {
                    data.maxChargeNotified = true;
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0f, 2.0f);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 2.0f);
                }
            }
        }

    }

    private static void checkMouseInput(Player player, ChargeData data) {
        // クライアント側でのみ実行
        if (!player.level().isClientSide) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) return;

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

        // 鞘を持っていて刀が納刀されている場合の落下中抜刀攻撃
        boolean hasSheathWithKatana = (isSaya(mainHand) && hasStoredKatana(mainHand)) || 
                                      (isSaya(offHand) && hasStoredKatana(offHand));
        
        // 落下中かチェック
        boolean isFalling = !player.onGround() && player.getDeltaMovement().y < -0.1;
        
        if (isFalling && hasSheathWithKatana) {
            boolean isLeftClickHeld = mc.options.keyAttack.isDown();
            
            // 落下中に左クリック長押し開始
            if (isLeftClickHeld && !data.isFallingCharge) {
                data.isFallingCharge = true;
                data.fallTime = 0;
                data.chargingItem = mainHand.copy();
            }
            // 落下中のチャージ継続
            else if (isLeftClickHeld && data.isFallingCharge) {
                data.fallTime++;
                
                // 落下チャージエフェクト
                if (data.fallTime % 5 == 0) {
                    // displayFallingChargeEffect(player, data.fallTime); // 一時的にコメントアウト
                }
            }
            // 落下チャージ解除または着地
            else if (!isLeftClickHeld && data.isFallingCharge) {
                // 落下攻撃をサーバーに送信
                float fallPower = Math.min((float) data.fallTime / 40.0f, 2.0f); // 最大2倍
                TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new AttackPacket(2, fallPower));
                data.reset();
            }
            
            data.wasLeftClickPressed = isLeftClickHeld;
            return;
        }

        // 通常の武器を持っている場合
        if (isWeapon(mainHand)) {
            boolean isLeftClickHeld = mc.options.keyAttack.isDown();
            
            // 左クリックが押された瞬間を検出（通常攻撃）— チャージ中は送らない
            if (isLeftClickHeld && !data.wasLeftClickPressed && !data.isCharging) {
                data.clickReleaseTimer = 0;
                // サーバーに攻撃パケットを送信
                TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new AttackPacket(0, 0));
            }

            // チャージ開始（左クリック長押し）- クールダウン中は開始しない。
            // 閾値を上げて「コンボの押しっぱなし」で誤ってチャージ攻撃(slam_down等)が
            // 出るのを防ぐ ( 5→10 tick = 0.5秒明確に押し続けた時だけチャージ開始 )。
            if (isLeftClickHeld && !data.isCharging && data.clickReleaseTimer > 10) {
                if (data.chargeCooldown <= 0) {
                    data.isCharging = true;
                    data.chargeTime = 0;
                    data.chargingItem = mainHand.copy();
                } else if (data.clickReleaseTimer == 6) { // 一度だけ表示
                    // クールダウン中のメッセージ
                    player.displayClientMessage(
                        Component.literal(String.format("§cチャージクールダウン (%.1f秒)", data.chargeCooldown / 20.0f)),
                        true
                    );
                }
            }
            // チャージ解除
            else if (!isLeftClickHeld && data.isCharging) {
                releaseChargedAttack(player, data);
            }
            // 左クリックが離された時はタイマーをリセット
            else if (!isLeftClickHeld && data.wasLeftClickPressed) {
                data.clickReleaseTimer = 0;
            }

            // 左クリック押し続けている時間をカウント
            if (isLeftClickHeld) {
                data.clickReleaseTimer++;
            }
            
            data.wasLeftClickPressed = isLeftClickHeld;
        } else {
            data.reset();
        }
    }
    
    private static void releaseChargedAttack(Player player, ChargeData data) {
        // Sword of Nightの場合、発光を解除
        String itemName = data.chargingItem.getItem().getClass().getSimpleName();
        if (itemName.equals("SwordOfNightItem") && !player.level().isClientSide) {
            SwordOfNightChargingGlowProcedure.execute(
                player.level(),
                player.getX(),
                player.getY(), 
                player.getZ(),
                player,
                false // チャージ解除
            );
        }
        
        if (data.chargeTime >= MIN_CHARGE_TIME) {
            float chargePercent = Math.min((float) data.chargeTime / MAX_CHARGE_TIME, 1.0f);
            // サーバーに攻撃パケットを送信
            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new AttackPacket(1, chargePercent));

            // チャージ攻撃後のクールダウンを設定（チャージ率に応じて長くなる）
            data.chargeCooldown = 20 + (int)(chargePercent * 20); // 1秒～2秒
        }
        data.reset();
    }
    
    public static void performChargedAttack(Player player, float chargePercent) {
        // サーバー側でクールダウン状態をチェック
        UUID playerId = player.getUUID();
        ChargeData data = playerChargeData.get(playerId);
        boolean isCooldown = data != null && data.chargeCooldown > 0;
        performChargedAttack(player, chargePercent, isCooldown);
    }

    public static void performChargedAttack(Player player, float chargePercent, boolean isCooldown) {
        Level world = player.level();
        Vec3 playerPos = player.position();
        Vec3 lookVec = the_four_primitives_and_weapons.skill.MotionExecutor.horizontalLook(player);

        // プレイヤーのスキルデータを取得
        PlayerSkillData.SkillStorage skillData = PlayerSkillData.getSkillData(player);

        // 固有スキルのチェック
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        String itemName = mainHand.getItem().getClass().getSimpleName();

        // 禁忌レアリティ: ため攻撃時に飛び道具反射
        the_four_primitives_and_weapons.item.rarity.WeaponRarity rarity =
                the_four_primitives_and_weapons.item.rarity.WeaponRarity.getFromStack(mainHand);
        if (rarity == the_four_primitives_and_weapons.item.rarity.WeaponRarity.FORBIDDEN) {
            ForbiddenRarityHandler.reflectNearbyProjectiles(player);
        }

        // 倶利伽羅 + 雷属性の固有スキル
        if (chargePercent >= 0.5f && ElementalDamageUtils.getElementType(mainHand) == ElementType.ELECTRIC) {
            if (itemName.equals("KurikarakenswordItem") || itemName.equals("KaminariKurikarakenSwordItem")) {
                ElectricDischargeBurstSkill.fire(player);
                return;
            }
            if (itemName.equals("KurikarakenItem") || itemName.equals("KaminariKurikarakenTyokutouItem")) {
                ElectricBeamSkill.fire(player);
                return;
            }
            if (itemName.equals("KurikarakenutigatanaItem") || itemName.equals("KaminariKurikarakenUtigatanaItem")) {
                ElectricSlashSkill.fire(player);
                return;
            }
        }

        // 倶利伽羅の固有スキル（雷属性が付いていない場合でも発動）
        if (chargePercent >= 0.5f) {
            if (itemName.equals("KaminariKurikarakenSwordItem") || itemName.equals("KurikarakenswordItem")) {
                ElectricDischargeBurstSkill.fire(player);
                return;
            }
            if (itemName.equals("KaminariKurikarakenTyokutouItem") || itemName.equals("KurikarakenItem")) {
                ElectricBeamSkill.fire(player);
                return;
            }
            if (itemName.equals("KaminariKurikarakenUtigatanaItem") || itemName.equals("KurikarakenutigatanaItem")) {
                ElectricSlashSkill.fire(player);
                return;
            }
        }

        // Magic Katana special charged attacks
        if ((itemName.equals("MagischesFeenKatanaItem") || itemName.equals("MagicalKatanaItem")) && chargePercent >= 0.5f) {
            MagicKatanaSpecialChargeProcedure.execute(
                world, playerPos.x, playerPos.y, playerPos.z, player, chargePercent
            );
            return;
        }

        // Luna専用のチャージ攻撃 (曲線ビーム)
        // 曲線ビーム本体は TyokutouThrustAttackProcedure.executeChargedThrust 内の
        // isLunaItem(heldItem) 分岐に実装されているので、performChargedThrust 経由で呼ぶ。
        // 旧コードは存在しない performLunaChargedAttack を参照していたため
        // コメントアウトされていた (= Luna チャージ技が出なくなっていた) のを修正。
        if (itemName.equals("LunaItem")) {
            performChargedThrust(player, world, lookVec, playerPos, chargePercent, isCooldown);
            return;
        }

        // ReplicaSwordOfLightの固有スキル（ガード）
        if (itemName.equals("ReplicaSwordOfLightItem") && skillData.isUniqueSkillEnabled("ReplicaSwordOfLight")) {
            player.addEffect(new MobEffectInstance(TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get(), 100, 0));
        }

        // SwordOfNightの固有スキル（テレポート攻撃）
        if (itemName.equals("SwordOfNightItem") && skillData.isUniqueSkillEnabled("SwordOfNight")) {
            SwordOfNightTpProcedure.execute(world, player.getX(), player.getY(), player.getZ(), player);
            return;
        }

        // CHARGEDスロットに設定されたモーションを実行
        String motionId = skillData.getMotionForWeapon(AttackSlot.CHARGED, player.getMainHandItem());
        MotionExecutor.executeMotion(motionId, player, chargePercent);
    }
    
    private static void performChargedThrust(Player player, Level world, Vec3 lookVec, Vec3 playerPos, float chargePercent) {
        performChargedThrust(player, world, lookVec, playerPos, chargePercent, false);
    }

    private static void performChargedThrust(Player player, Level world, Vec3 lookVec, Vec3 playerPos, float chargePercent, boolean isCooldown) {
        // Luna専用の強化突き攻撃処理
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        String itemName = mainHand.getItem().getClass().getSimpleName();

        // weapon_stats.json に "thrust" を持つ武器は JSON駆動の突き連撃 ( ダガー等・短reach )。
        the_four_primitives_and_weapons.skill.WeaponStatsRegistry.WeaponStats stats =
                the_four_primitives_and_weapons.skill.WeaponStatsRegistry.getStats(mainHand);
        if (stats != null && stats.thrust != null) {
            the_four_primitives_and_weapons.procedures.JsonThrustProcedure.execute(player, chargePercent, stats.thrust);
            return;
        }

        if (the_four_primitives_and_weapons.procedures.TyokutouThrustAttackProcedure.isStraightSword(mainHand)) {
            // 直刀のチャージ強化突進攻撃を実行（チャージ率に応じて威力増加）
            the_four_primitives_and_weapons.procedures.TyokutouThrustAttackProcedure.executeChargedThrust(
                world, player.getX(), player.getY(), player.getZ(), player, chargePercent, isCooldown
            );
            return;
        }

        float baseDamage = 15.0f * (1.0f + chargePercent);

        // 竹を破壊する範囲を設定
        breakBambooInPath(world, playerPos, lookVec, 6.0);
        double range = Math.max(1.0, 6.0f + chargePercent * 2.0f
                + the_four_primitives_and_weapons.skill.WeaponStatsRegistry.attackRangeBonus(mainHand));
        
        // 貫通突きエフェクト
        if (!world.isClientSide) {
            ServerLevel serverWorld = (ServerLevel) world;
            
            // 巨大な突きエフェクト
            for (double d = 0; d <= range; d += 0.3) {
                serverWorld.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    playerPos.x + lookVec.x * d,
                    playerPos.y + 1,
                    playerPos.z + lookVec.z * d,
                    5, 0.2, 0.2, 0.2, 0.05
                );
                
                if (chargePercent >= 1.0f) {
                    serverWorld.sendParticles(
                        ParticleTypes.END_ROD,
                        playerPos.x + lookVec.x * d,
                        playerPos.y + 1,
                        playerPos.z + lookVec.z * d,
                        2, 0.1, 0.1, 0.1, 0
                    );
                }
            }
        }
        
        // 貫通攻撃（直線上の全ての敵）
        Vec3 endPos = playerPos.add(lookVec.scale(range));
        AABB searchArea = new AABB(playerPos, endPos).inflate(1.0);
        
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
            entity -> {
                if (entity == player) return false;
                Vec3 toEntity = entity.position().subtract(playerPos);
                double dot = lookVec.dot(toEntity.normalize());
                return dot > 0.8 && toEntity.length() <= range;
            });
        
        for (LivingEntity target : targets) {
            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            float actualDamage = DamageCalculator.dealDamage(player, target, baseDamage, weapon);
            
            // 貫通による吹き飛ばし
            target.setDeltaMovement(lookVec.scale(2.0 * chargePercent).add(0, 0.5, 0));
            
            if (chargePercent >= 1.0f) {
                // 最大チャージで出血効果
                target.setSecondsOnFire(5);
            }
        }
        
        world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
            SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.0f);
        //player.displayClientMessage(Component.literal("§c貫通突き！"), true);
    }
    
    public static void performNormalAttack(Player player) {
        Level world = player.level();

        // プレイヤーのスキルデータを取得
        PlayerSkillData.SkillStorage skillData = PlayerSkillData.getSkillData(player);

        // 固有スキルのチェック
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        String itemName = mainHand.getItem().getClass().getSimpleName();

        // 禁忌レアリティ: 通常攻撃時に飛び道具反射
        the_four_primitives_and_weapons.item.rarity.WeaponRarity normalRarity =
                the_four_primitives_and_weapons.item.rarity.WeaponRarity.getFromStack(mainHand);
        if (normalRarity == the_four_primitives_and_weapons.item.rarity.WeaponRarity.FORBIDDEN) {
            ForbiddenRarityHandler.reflectNearbyProjectiles(player);
        }

        if (itemName.equals("SwordOfNightItem") && skillData.isUniqueSkillEnabled("SwordOfNight")) {
            // Sword of Night Effectがアクティブな間はショットを撃たない
            if (!player.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.SWORD_OF_NIGHT_EFFECT.get())) {
                // SwordOfNightの通常攻撃（ショット）
                SwordOfNightShotProcedure.execute(world, player.getX(), player.getY(), player.getZ(), player);
            }
            return;
        }

        // Lunaの固有スキル
        if (itemName.equals("LunaItem") && skillData.isUniqueSkillEnabled("Luna")) {
            the_four_primitives_and_weapons.procedures.LunaenteiteigaaitemuwoZhentutaShiProcedure.execute(world, player.getX(), player.getY(), player.getZ(), player);
            return;
        }

        // コンボカウンターを取得
        UUID playerId = player.getUUID();
        ChargeData data = playerChargeData.computeIfAbsent(playerId, k -> new ChargeData());

        // コンボタイムアウト（20tick = 1秒 攻撃しないとリセット）
        long now = world.getGameTime();
        if (now - data.lastAttackTime > 20) {
            data.resetCombo();
        }
        data.lastAttackTime = now;

        // コンボ段階に応じたスロットを決定
        int combo = data.comboCounter % 3;
        AttackSlot slot;
        if (combo == 0) {
            slot = AttackSlot.FIRST_HIT;
        } else if (combo == 1) {
            slot = AttackSlot.SECOND_HIT;
        } else {
            slot = AttackSlot.THIRD_HIT;
        }

        // スロットに設定されたモーションを実行
        String motionId = skillData.getMotionForWeapon(slot, player.getMainHandItem());
        MotionExecutor.executeMotion(motionId, player, 0.0f);

        // コンボカウンターを増やす
        data.comboCounter++;
    }
    
    private static void displayChargeEffect(Player player, int chargeTime) {
        if (player.level().isClientSide) return;
        
        ServerLevel world = (ServerLevel) player.level();
        float chargePercent = Math.min((float) chargeTime / MAX_CHARGE_TIME, 1.0f);
        
        // チャージレベルに応じたパーティクル
        if (chargePercent < 0.33f) {
            world.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1, player.getZ(),
                5, 0.3, 0.3, 0.3, 0.01);
        } else if (chargePercent < 0.66f) {
            world.sendParticles(ParticleTypes.FLAME,
                player.getX(), player.getY() + 1, player.getZ(),
                5, 0.3, 0.3, 0.3, 0.01);
        } else {
            world.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1, player.getZ(),
                8, 0.3, 0.3, 0.3, 0.02);
        }
    }
    
    private static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // 他MOD（TACZ等の銃MOD）のアイテムは除外
        String className = stack.getItem().getClass().getName();
        if (className.contains("tacz") || className.contains("cgm")) return false;

        // SwordItemまたはカタナ系アイテムかチェック
        if (stack.getItem() instanceof SwordItem) return true;
        // トライデントも左クリック通常攻撃・コンボ・チャージの対象
        if (stack.getItem() instanceof net.minecraft.world.item.TridentItem) return true;

        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.contains("Katana") || itemName.contains("Sword") ||
               itemName.contains("Blade") || itemName.contains("katana");
    }
    
    // プレイヤーの実際の攻撃力を計算
    // @deprecated Use DamageCalculator.calculateDamage instead
    @Deprecated
    private static float calculateActualDamage(Player player, LivingEntity target, float baseDamage) {
        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        float damage = baseDamage;
        
        // 武器の基本攻撃力を取得
        if (weapon.getItem() instanceof SwordItem swordItem) {
            // ソードの基本ダメージを追加
            damage += swordItem.getDamage();
        }
        
        // プレイヤーの攻撃力属性を取得
        double attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        damage += (float)attackDamage;
        
        // 攻撃力上昇エフェクト
        if (player.hasEffect(MobEffects.DAMAGE_BOOST)) {
            int amplifier = player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier();
            damage += damage * (0.3f * (amplifier + 1));
        }
        
        // 弱体化エフェクト
        if (player.hasEffect(MobEffects.WEAKNESS)) {
            int amplifier = player.getEffect(MobEffects.WEAKNESS).getAmplifier();
            damage -= damage * (0.2f * (amplifier + 1));
        }
        
        // シャープネスエンチャント
        int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, weapon);
        if (sharpnessLevel > 0) {
            damage += 0.5f * sharpnessLevel + 0.5f;
        }
        
        // アンデッド特攻
        if (target.getMobType() == MobType.UNDEAD) {
            int smiteLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, weapon);
            if (smiteLevel > 0) {
                damage += 2.5f * smiteLevel;
            }
        }
        
        // 虫特攻
        if (target.getMobType() == MobType.ARTHROPOD) {
            int baneLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BANE_OF_ARTHROPODS, weapon);
            if (baneLevel > 0) {
                damage += 2.5f * baneLevel;
                // スローネス → attribute modifier ベース (牛乳で消えない / モヤ無し)
                int duration = 20 + (int)(Math.random() * 10 * baneLevel);
                SpecialDebuffHandler.applySlowness(target, duration, 3);
            }
        }
        
        // クリティカルダメージの計算（ランダムで発生）
        if (Math.random() < 0.1) { // 10%の確率でクリティカル
            damage *= 1.5f;
            
            // クリティカルエフェクト
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1);
            }
            
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        
        return damage;
    }
    
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        UUID playerId = player.getUUID();
        ChargeData data = playerChargeData.get(playerId);
        
        // チャージ中はブロック破壊をキャンセル
        if (data != null && data.isCharging) {
            event.setCanceled(true);
        }
    }
    
    // 武器特殊効果を適用するヘルパーメソッド
    // @deprecated Use DamageCalculator.applyWeaponEffects instead
    @Deprecated
    private static void applyWeaponEffects(Player player, LivingEntity target, float damage) {
        Level world = player.level();
        ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
        String weaponName = weapon.getItem().getClass().getSimpleName();
        
        // RiversOfBloodの吸血効果
        if (weaponName.equals("RiversOfBloodItem")) {
            // ターゲットが呪われているかチェック
            boolean isCursed = target.hasEffect(MobEffects.WITHER) || 
                               (target.getPersistentData().contains("Feyn") && 
                                "cursed".equals(target.getPersistentData().getString("Feyn")));
            
            float healAmount = isCursed ? damage * 0.5f : damage * 0.2f;
            player.heal(healAmount);
            
            if (isCursed) {
                // 呪われた敵への追加効果 — Wither は DoT (カスタムダメージ) に、 Weakness は attribute modifier に
                target.hurt(target.damageSources().magic(), damage * 0.3f);
                SpecialDebuffHandler.applyWither(target, 100, 0.5f);
                SpecialDebuffHandler.applyWeakness(target, 200, 1);
            }
            
            // 血のエフェクト
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.1);
            }
            
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.2f);
        }
        
        // WitherKatanaのウィザー効果
        if (weaponName.equals("WitherKatanaItem")) {
            // ターゲットが呪われているかチェック
            boolean isCursed = target.getPersistentData().contains("Feyn") && 
                               "cursed".equals(target.getPersistentData().getString("Feyn"));
            
            if (isCursed) {
                // 呪われた敵には強化されたウィザー効果
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));
                target.hurt(target.damageSources().wither(), damage * 0.5f);
                
                // 闇のオーラエフェクト
                if (world instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SOUL,
                        target.getX(), target.getY() + 1, target.getZ(),
                        15, 0.5, 0.5, 0.5, 0.05);
                }
            } else {
                // 通常のウィザー効果
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            }
            
            // ウィザーサウンド
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.5f, 1.0f);
        }
        
        // Killエンチャントの効果
        if (EnchantmentHelper.getItemEnchantmentLevel(TheFourPrimitivesAndWeaponsModEnchantments.KILL.get(), weapon) > 0) {
            // 即死判定
            if (Math.random() < 0.824) { // 82.4%の確率
                target.hurt(target.damageSources().magic(), target.getMaxHealth() * 2);
                
                // 即死エフェクト
                if (world instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE,
                        target.getX(), target.getY() + 1, target.getZ(),
                        20, 0.5, 0.5, 0.5, 0.1);
                }
                
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 2.0f);
            }
        }
    }
    
    // 攻撃経路上の竹を破壊する
    private static void breakBambooInPath(Level world, Vec3 startPos, Vec3 direction, double range) {
        if (world.isClientSide) return;
        
        // 攻撃経路に沿って竹をチェック
        for (double d = 0; d <= range; d += 0.5) {
            Vec3 checkPos = startPos.add(direction.scale(d));
            
            // 上下左右も含めて範囲をチェック
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos pos = new BlockPos(
                            (int)(checkPos.x + dx),
                            (int)(checkPos.y + dy),
                            (int)(checkPos.z + dz)
                        );
                        
                        BlockState state = world.getBlockState(pos);
                        
                        // 竹または竹の苗をチェック
                        if (state.getBlock() == Blocks.BAMBOO || 
                            state.getBlock() == Blocks.BAMBOO_SAPLING) {
                            // 竹を破壊（ドロップあり）
                            world.destroyBlock(pos, true);
                        }
                    }
                }
            }
        }
    }
    
    // 円形範囲の竹を破壊する
    private static void breakBambooInRadius(Level world, Vec3 centerPos, double radius) {
        if (world.isClientSide) return;
        
        BlockPos center = new BlockPos((int)centerPos.x, (int)centerPos.y, (int)centerPos.z);
        int radiusInt = (int) Math.ceil(radius);
        
        // 円形範囲内の竹をチェック
        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -radiusInt; z <= radiusInt; z++) {
                    if (x * x + z * z <= radius * radius) {
                        BlockPos pos = center.offset(x, y, z);
                        BlockState state = world.getBlockState(pos);
                        
                        // 竹または竹の苗をチェック
                        if (state.getBlock() == Blocks.BAMBOO || 
                            state.getBlock() == Blocks.BAMBOO_SAPLING) {
                            // 竹を破壊（ドロップあり）
                            world.destroyBlock(pos, true);
                        }
                    }
                }
            }
        }
    }

    // private static void displayFallingChargeEffect(Player player, int fallTime) {
    //     if (player.level().isClientSide) return;
    //
    //     ServerLevel serverWorld = (ServerLevel) player.level();
    //     double radius = Math.min(fallTime / 20.0, 2.0);
    //
    //     // 落下中の円形エフェクト
    //     for (int i = 0; i < 360; i += 30) {
    //         double angle = Math.toRadians(i);
    //         serverWorld.sendParticles(
    //             ParticleTypes.ELECTRIC_SPARK,
    //             player.getX() + Math.cos(angle) * radius,
    //             player.getY(),
    //             player.getZ() + Math.sin(angle) * radius,
    //             1, 0, 0.1, 0, 0.01
    //         );
    //     }
    // }

    // 落下攻撃の実行（特定アイテム専用のため一時的にコメントアウト）
    // public static void performFallingAttack(Player player, float fallPower) {
    //     Level world = player.level();
    //     Vec3 playerPos = player.position();

    //     // 抜刀処理（鞘から刀を抜く）
    //     ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
    //     ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
    //     ItemStack sheathStack = null;
    //     InteractionHand sheathHand = null;
    //
    //     if (isSaya(mainHand) && hasStoredKatana(mainHand)) {
    //         sheathStack = mainHand;
    //         sheathHand = InteractionHand.MAIN_HAND;
    //     } else if (isSaya(offHand) && hasStoredKatana(offHand)) {
    //         sheathStack = offHand;
    //         sheathHand = InteractionHand.OFF_HAND;
    //     }
    //
    //     if (sheathStack != null) {
    //         // 抜刀
    //         CompoundTag tag = sheathStack.getOrCreateTag();
    //
    //         // StoredKatanaまたはStoredSwordをチェック
    //         String storedKey = tag.contains("StoredKatana") ? "StoredKatana" : "StoredSword";
    //         ItemStack katanaStack = ItemStack.of(tag.getCompound(storedKey));
    //
    //         // 反対の手に刀を配置
    //         InteractionHand katanaHand = sheathHand == InteractionHand.MAIN_HAND ?
    //                                      InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    //         player.setItemInHand(katanaHand, katanaStack);
    //
    //         // 鞘から刀を削除
    //         tag.remove(storedKey);
    //         tag.putInt("CustomModelData", 0);
    //         sheathStack.setTag(tag);
    //         player.setItemInHand(sheathHand, sheathStack);
    //     }
    //
    //     // 大ダメージの範囲攻撃
    //     double range = 5.0 * (1.0 + fallPower);
    //     float baseDamage = 20.0f * (1.0f + fallPower * 2.0f);
    //
    //     // 地面衝撃エフェクト
    //     if (!world.isClientSide) {
    //         ServerLevel serverWorld = (ServerLevel) world;
    //
    //         // 衝撃波エフェクト
    //         for (int ring = 0; ring < 3; ring++) {
    //             double r = range * (ring + 1) / 3.0;
    //             for (int i = 0; i < 360; i += 10) {
    //                 double angle = Math.toRadians(i);
    //                 serverWorld.sendParticles(
    //                     ParticleTypes.EXPLOSION,
    //                     playerPos.x + Math.cos(angle) * r,
    //                     playerPos.y + 0.1,
    //                     playerPos.z + Math.sin(angle) * r,
    //                     1, 0, 0, 0, 0
    //                 );
    //             }
    //         }
    //
    //         // 縦の衝撃エフェクト
    //         for (int i = 0; i < 20; i++) {
    //             serverWorld.sendParticles(
    //                 ParticleTypes.CLOUD,
    //                 playerPos.x, playerPos.y + i * 0.2, playerPos.z,
    //                 5, 0.3, 0, 0.3, 0.1
    //             );
    //         }
    //     }
    //
    //     // 範囲内の全ての敵にダメージ
    //     AABB searchArea = new AABB(
    //         playerPos.x - range, playerPos.y - 2, playerPos.z - range,
    //         playerPos.x + range, playerPos.y + 4, playerPos.z + range
    //     );
    //
    //     List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
    //         entity -> entity != player && entity.distanceTo(player) <= range);
    //
    //     for (LivingEntity target : targets) {
    //         float actualDamage = calculateActualDamage(player, target, baseDamage);
    //         target.hurt(player.damageSources().playerAttack(player), actualDamage);
    //
    //         // 武器特殊効果を適用
    //         ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
    //         if (weapon.isEmpty()) {
    //             weapon = player.getItemInHand(InteractionHand.OFF_HAND);
    //         }
    //         applyWeaponEffects(player, target, actualDamage);
    //
    //         // 強烈な吹き飛ばし
    //         Vec3 knockback = target.position().subtract(playerPos).normalize();
    //         target.setDeltaMovement(
    //             knockback.x * (1.5 + fallPower),
    //             0.5 + fallPower * 0.5,
    //             knockback.z * (1.5 + fallPower)
    //         );
    //
    //         // スタン効果
    //         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
    //                                               (int)(40 * fallPower), 2));
    //     }
    //
    //     // 地震音
    //     world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
    //         SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.5f);
    //     world.playSound(null, playerPos.x, playerPos.y, playerPos.z,
    //         SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, 0.8f);

    //     player.displayClientMessage(
    //         Component.literal(fallPower >= 1.5f ? "§c§l落下斬撃！！" : "§c落下斬撃！"),
    //         true
    //     );
    // }
    
    private static boolean isSaya(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemName = stack.getItem().getClass().getSimpleName();
        return itemName.equals("SayaItem") || itemName.equals("TyokutouSayaItem");
    }

    private static boolean hasStoredKatana(ItemStack stack) {
        return stack.hasTag() && (stack.getTag().contains("StoredKatana") || stack.getTag().contains("StoredSword"));
    }


}
