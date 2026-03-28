package minecraftarmorweapon.damage;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ThunderElementDamageHandler {

    // 基礎倍率（電気属性と同じ）
    private static final float BASE_MULTIPLIER        = 1.2f;
    private static final float WATER_MULTIPLIER       = 1.5f;
    private static final float CONDUCTOR_BONUS        = 0.3f;

    // AOE範囲 (ブロック)
    private static final double AOE_RADIUS            = 3.0;

    /**
     * 雷属性ダメージを計算して返す。
     * 電気属性と同仕様：水中でAOE、導体装備時にボーナス。
     *
     * @param attacker 攻撃者
     * @param target   攻撃対象
     * @param weapon   使用武器
     * @param baseDmg  基礎ダメージ
     * @return 属性込みの最終ダメージ
     */
    public static float handleThunderDamage(LivingEntity attacker,
                                            LivingEntity target,
                                            ItemStack weapon,
                                            float baseDmg) {

        float multiplier = BASE_MULTIPLIER;

        // 水中ボーナス
        boolean inWater = target.isInWater();
        if (inWater) {
            multiplier = WATER_MULTIPLIER;

            // AOE: 水中の周囲エンティティにもダメージ
            Level level = target.level();
            List<LivingEntity> nearby = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(
                            target.getX() - AOE_RADIUS, target.getY() - AOE_RADIUS, target.getZ() - AOE_RADIUS,
                            target.getX() + AOE_RADIUS, target.getY() + AOE_RADIUS, target.getZ() + AOE_RADIUS
                    )
            );
            for (LivingEntity nearby_entity : nearby) {
                if (nearby_entity != target && nearby_entity != attacker && nearby_entity.isInWater()) {
                    nearby_entity.hurt(
                            level.damageSources().lightningBolt(),
                            baseDmg * multiplier * 0.5f
                    );
                }
            }
        }

        // 導体装備ボーナス（鉄・金・チェーン装備を導体とみなす）
        int conductorCount = countConductorArmor(target);
        multiplier += CONDUCTOR_BONUS * conductorCount;

        return baseDmg * multiplier;
    }

    /**
     * レベル指定で雷属性ダメージ計算（魔導書経由用）
     */
    public static float calculateDamage(LivingEntity attacker, LivingEntity target, float baseDmg, int level) {
        float multiplier = BASE_MULTIPLIER;
        if (target.isInWater()) {
            multiplier = WATER_MULTIPLIER;
            net.minecraft.world.level.Level world = target.level();
            List<LivingEntity> nearby = world.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(
                            target.getX() - AOE_RADIUS, target.getY() - AOE_RADIUS, target.getZ() - AOE_RADIUS,
                            target.getX() + AOE_RADIUS, target.getY() + AOE_RADIUS, target.getZ() + AOE_RADIUS
                    )
            );
            for (LivingEntity nearby_entity : nearby) {
                if (nearby_entity != target && nearby_entity != attacker && nearby_entity.isInWater()) {
                    nearby_entity.hurt(world.damageSources().lightningBolt(), baseDmg * multiplier * 0.5f);
                }
            }
        }
        multiplier += CONDUCTOR_BONUS * countConductorArmor(target);
        return baseDmg * multiplier;
    }

    /**
     * 対象の装備スロットにある導体アイテム（鉄・金・チェーン）の数を返す。
     */
    private static int countConductorArmor(LivingEntity entity) {
        int count = 0;
        for (ItemStack armor : entity.getArmorSlots()) {
            if (armor.isEmpty()) continue;
            String id = armor.getItem().toString();
            if (id.contains("iron") || id.contains("gold") || id.contains("chain")) {
                count++;
            }
        }
        return count;
    }
}
