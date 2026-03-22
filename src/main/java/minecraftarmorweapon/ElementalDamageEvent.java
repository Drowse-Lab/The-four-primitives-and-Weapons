package minecraftarmorweapon;

import minecraftarmorweapon.util.VersionHelper;

import minecraftarmorweapon.damage.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

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

        // 武器に属性が設定されているかチェック
        if (!ElementalDamageUtils.hasElement(weapon)) {
            return;
        }

        // 属性情報を取得
        ElementType elementType = ElementalDamageUtils.getElementType(weapon);
        int elementLevel = ElementalDamageUtils.getElementLevel(weapon);

        // ターゲットとオリジナルダメージを取得
        LivingEntity target = event.getEntity();
        float originalDamage = event.getAmount();

        // 属性に応じてダメージを計算
        float modifiedDamage = originalDamage;

        // bookスロットの魔導書でカウンター属性を持っていれば無効化
        if (ElementalDamageUtils.isElementNullifiedByBook(target, elementType)) {
            if (target instanceof Player p) {
                p.displayClientMessage(Component.literal("§b魔導書が" + elementType.getName() + "属性を無効化した！"), true);
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.5f);
            }
            return; // 属性ダメージ倍率を適用しない（通常ダメージのまま）
        }

        // 古いダメージタグをクリア
        clearDamageTags(target);

        switch (elementType) {
            case ICE:
                modifiedDamage = IceElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                target.addTag(TAG_ICE_DAMAGE);
                break;
            case ELECTRIC:
                // 電気属性の伝染処理は後で行う（再帰を避けるため）
                modifiedDamage = ElectricElementDamageHandler.calculateDamage(target, originalDamage, elementLevel, event.getSource());
                // 水中での伝染ダメージを処理
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
            default:
                break;
        }

        // ダメージを変更
        if (modifiedDamage != originalDamage) {
            event.setAmount(modifiedDamage);
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
}
