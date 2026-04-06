package minecraftarmorweapon;

import minecraftarmorweapon.util.VersionHelper;

import minecraftarmorweapon.damage.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 属性ダメージを適用するForgeイベントハンドラー
 * LivingHurtEventを使用してダメージを変更する
 */
@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class ElementalDamageEvent {

    // データパック連携用タグ名
    private static final String TAG_ICE_DAMAGE = "minecraft_armor_weapon.mh_rpgish.ice_damage";
    private static final String TAG_ELECTRIC_DAMAGE = "minecraft_armor_weapon.mh_rpgish.electric_damage";
    private static final String TAG_CORROSION_DAMAGE = "minecraft_armor_weapon.mh_rpgish.corrosion_damage";
    private static final String TAG_HOLY_DAMAGE = "minecraft_armor_weapon.mh_rpgish.holy_damage";
    private static final String TAG_ERROR_DAMAGE = "minecraft_armor_weapon.mh_rpgish.error_damage";

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 攻撃者を取得
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        // 攻撃者の武器を取得
        ItemStack weapon = attacker.getMainHandItem();

        // 属性情報を取得（武器と魔導書を比較し、レベルの高い方を採用）
        ElementType elementType = ElementType.NONE;
        int elementLevel = 0;
        boolean fromBook = false;

        // 武器の属性を取得
        ElementType weaponType = ElementalDamageUtils.getElementType(weapon);
        int weaponLevel = ElementalDamageUtils.getElementLevel(weapon);

        // 魔導書の属性を取得
        ElementType bookType = ElementType.NONE;
        int bookLevel = 0;
        if (attacker instanceof Player attackerPlayer) {
            bookType = ElementalDamageUtils.getBookSlotElement(attackerPlayer);
            bookLevel = ElementalDamageUtils.getBookSlotLevel(attackerPlayer);
        }

        // 同じ属性ならレベルの高い方+ボーナス、違う属性なら武器優先
        if (weaponType != ElementType.NONE && bookType != ElementType.NONE
                && weaponType == bookType) {
            // 同属性の相性ボーナス: 高い方のレベル + 低い方の半分(切り捨て、最低+1)
            elementType = weaponType;
            int highLevel = Math.max(weaponLevel, bookLevel);
            int lowLevel = Math.min(weaponLevel, bookLevel);
            elementLevel = highLevel + Math.max(lowLevel / 2, 1);
        } else if (weaponType != ElementType.NONE) {
            elementType = weaponType;
            elementLevel = weaponLevel;
        } else if (bookType != ElementType.NONE) {
            elementType = bookType;
            elementLevel = bookLevel;
            fromBook = true;
        } else {
            return;
        }

        if (elementType == ElementType.NONE || elementLevel <= 0) {
            return;
        }

        // デバッグ: 属性情報をチャットに表示
        if (attacker instanceof Player debugPlayer) {
            debugPlayer.displayClientMessage(
                Component.literal("§e[DEBUG] 属性=" + elementType.getName()
                    + " Lv=" + elementLevel
                    + " 武器=" + weaponType.getName() + "(" + weaponLevel + ")"
                    + " 魔導書=" + bookType.getName() + "(" + bookLevel + ")"),
                true);
        }

        // ターゲットとオリジナルダメージを取得
        LivingEntity target = event.getEntity();
        float originalDamage = event.getAmount();

        // 属性に応じてダメージを計算
        float modifiedDamage = originalDamage;

        // bookスロットの魔導書でカウンター属性を持っていれば無効化（防御側）
        if (ElementalDamageUtils.isElementNullifiedByBook(target, elementType)) {
            if (target instanceof Player p) {
                p.displayClientMessage(Component.literal("§b魔導書が" + elementType.getName() + "属性を無効化した！"), true);
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.5f);
            }
            return; // 属性ダメージ倍率を適用しない（通常ダメージのまま）
        }

        // === カウンター属性による弱体化ボーナス（攻撃側） ===
        // ターゲットの武器/魔導書属性を取得し、攻撃属性がカウンターなら追加ダメージ
        float counterBonus = getCounterBonus(target, elementType, elementLevel);

        // 古いダメージタグをクリア
        clearDamageTags(target);

        switch (elementType) {
            case ICE:
                modifiedDamage = IceElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                target.addTag(TAG_ICE_DAMAGE);
                break;
            case ELECTRIC:
                modifiedDamage = ElectricElementDamageHandler.calculateDamage(target, originalDamage, elementLevel, event.getSource());
                applyElectricChainDamage(target, attacker, originalDamage, elementLevel);
                target.addTag(TAG_ELECTRIC_DAMAGE);
                break;
            case CORROSION:
                modifiedDamage = CorrosionElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                target.addTag(TAG_CORROSION_DAMAGE);
                break;
            case HOLY:
                modifiedDamage = HolyElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                target.addTag(TAG_HOLY_DAMAGE);
                break;
            case ERROR:
                modifiedDamage = ErrorElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                target.addTag(TAG_ERROR_DAMAGE);
                break;
            case FIRE:
                modifiedDamage = FireElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                break;
            case WATER:
                modifiedDamage = WaterElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                break;
            case WIND:
                modifiedDamage = WindElementDamageHandler.calculateDamage(attacker, target, originalDamage, elementLevel);
                break;
            case THUNDER:
                modifiedDamage = ThunderElementDamageHandler.calculateDamage(attacker, target, originalDamage, elementLevel);
                break;
            case DARK:
                modifiedDamage = DarkElementDamageHandler.calculateDamage(attacker, target, originalDamage, elementLevel);
                break;
            default:
                break;
        }

        // カウンターボーナスを加算
        modifiedDamage += counterBonus;

        // ダメージを変更
        if (modifiedDamage != originalDamage) {
            event.setAmount(modifiedDamage);
        }
    }

    /**
     * [Phase 1] LivingDamageEvent HIGH: アーマー・エンチャント・耐性の貫通
     * バニラ＋他modのアーマー軽減後、Heart Stop等の他modエフェクトが動く前に実行
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamagePhase1(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!ErrorElementDamageHandler.hasPendingHurt(target.getUUID())) {
            return;
        }

        float result = ErrorElementDamageHandler.applyArmorPenetration(target, event.getAmount());
        if (result != event.getAmount()) {
            event.setAmount(result);
        }
    }

    /**
     * [Phase 2] LivingDamageEvent LOWEST: 他modエフェクト（Heart Stop等）の貫通
     * Heart Stop等がNORMAL優先度でダメージを0にした後に実行し、貫通分を復元
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamagePhase2(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!ErrorElementDamageHandler.hasPendingEffect(target.getUUID())) {
            return;
        }

        float result = ErrorElementDamageHandler.applyEffectPenetration(target, event.getAmount());
        if (result != event.getAmount()) {
            event.setAmount(result);
        }
    }

    /**
     * エンティティから属性ダメージタグをクリア
     */
    private static void clearDamageTags(LivingEntity entity) {
        entity.removeTag(TAG_ICE_DAMAGE);
        entity.removeTag(TAG_ELECTRIC_DAMAGE);
        entity.removeTag(TAG_CORROSION_DAMAGE);
        entity.removeTag(TAG_HOLY_DAMAGE);
        entity.removeTag(TAG_ERROR_DAMAGE);
    }

    /**
     * 電気属性の伝染ダメージを処理
     */
    private static void applyElectricChainDamage(LivingEntity target, LivingEntity attacker, float originalDamage, int elementLevel) {
        // 水中にいる場合のみ
        if (!target.isInWaterOrRain() && !target.isInWaterRainOrBubble()) {
            return;
        }

        // 周囲のエンティティを検索
        double radius = 5.0;
        AABB searchBox = new AABB(
            target.getX() - radius,
            target.getY() - radius,
            target.getZ() - radius,
            target.getX() + radius,
            target.getY() + radius,
            target.getZ() + radius
        );

        List<LivingEntity> nearbyEntities = target.level().getEntitiesOfClass(
            LivingEntity.class, searchBox,
            e -> e != target && e != attacker && (e.isInWaterOrRain() || e.isInWaterRainOrBubble())
        );

        for (LivingEntity nearby : nearbyEntities) {
            // 電気の光線が伝染するパーティクルエフェクト
            if (VersionHelper.getLevel(target) instanceof ServerLevel serverLevel) {
                Vec3 start = target.position().add(0, target.getBbHeight() / 2, 0);
                Vec3 end = nearby.position().add(0, nearby.getBbHeight() / 2, 0);
                Vec3 direction = end.subtract(start).normalize();

                double distance = start.distanceTo(end);
                int particles = (int)(distance * 5);

                for (int i = 0; i < particles; i++) {
                    double ratio = (double)i / particles;
                    Vec3 particlePos = start.add(direction.scale(distance * ratio));

                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // 周囲のエンティティには50%のダメージ（通常のDamageSourceで再帰を防ぐ）
            float chainDamage = originalDamage * 0.5f;
            nearby.hurt(nearby.damageSources().lightningBolt(), chainDamage);
        }
    }

    /**
     * 聖属性魔導書のデバフ自動解除（プレイヤーTickで処理）
     * Lv1: 200tickごと、Lv10: 毎tick常時解除
     * MobEffectCategory.HARMFULで判定するため他modのデバフも解除可能
     */
    @SubscribeEvent
    public static void onPlayerTickHolyCleanse(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide()) return;

        ElementType bookElement = ElementalDamageUtils.getBookSlotElement(player);
        if (bookElement != ElementType.HOLY) return;

        int level = ElementalDamageUtils.getBookSlotLevel(player);
        if (level <= 0) return;

        // 解除間隔：Lv10=毎tick、Lv1=200tick
        int interval = level >= 10 ? 1 : Math.max(5, 200 - (level - 1) * 22);

        if (player.tickCount % interval != 0) return;

        // HARMFUL（有害）カテゴリのエフェクトを全て除去（他mod含む）
        List<MobEffectInstance> debuffs = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                debuffs.add(effect);
            }
        }

        if (!debuffs.isEmpty()) {
            for (MobEffectInstance debuff : debuffs) {
                player.removeEffect(debuff.getEffect());
            }

            // 浄化エフェクト
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.WAX_ON,
                    player.getX(), player.getY() + 1, player.getZ(),
                    8, 0.3, 0.4, 0.3, 0.05);
            }
        }
    }

    /**
     * カウンター属性ボーナスダメージを計算
     * 攻撃属性がターゲットの属性のカウンターである場合、追加ダメージを与える
     * 例：攻撃=FIRE、ターゲット=ICE → ICEのカウンターはFIRE → 弱点ボーナス発動
     */
    private static float getCounterBonus(LivingEntity target, ElementType attackElement, int attackLevel) {
        // ターゲットの属性を取得（武器 → 魔導書の順）
        ElementType targetElement = ElementType.NONE;

        ItemStack targetWeapon = target.getMainHandItem();
        if (ElementalDamageUtils.hasElement(targetWeapon)) {
            targetElement = ElementalDamageUtils.getElementType(targetWeapon);
        } else if (target instanceof Player targetPlayer) {
            targetElement = ElementalDamageUtils.getBookSlotElement(targetPlayer);
        }

        if (targetElement == ElementType.NONE || targetElement == ElementType.ERROR) return 0f;

        // ターゲットの属性のカウンターが攻撃属性と一致 → 弱点攻撃
        if (targetElement.getCounterElement() == attackElement) {
            // 弱点ボーナス：基礎3ダメージ + レベル×1.0
            float bonus = 3.0f + attackLevel * 1.0f;

            return bonus;
        }

        return 0f;
    }
}
