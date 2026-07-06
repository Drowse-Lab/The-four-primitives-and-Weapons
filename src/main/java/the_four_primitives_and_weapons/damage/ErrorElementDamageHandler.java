package the_four_primitives_and_weapons.damage;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModDamageTypes;

import java.util.UUID;

/**
 * ERROR / 消滅属性。
 *
 * 攻撃側では普通の攻撃として扱い、追加ダメージや防御貫通は行わない。
 * 防御側が ERROR を持っている場合、遠距離攻撃・属性 DamageSource・属性武器/本を
 * 持つ他者からの攻撃を消滅させる。
 */
public class ErrorElementDamageHandler {

    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel) {
        spawnEraseParticles(target, Math.max(1, elementLevel));
        return originalDamage;
    }

    public static boolean shouldNullifyIncoming(Player defender, DamageSource source) {
        if (defender == null || source == null || !hasErrorElement(defender)) return false;
        if (source.getEntity() == defender) return false;

        if (isRangedAttack(source)) return true;

        ElementType sourceElement = getSourceElement(source);
        if (sourceElement != ElementType.NONE) return true;

        if (source.getEntity() instanceof LivingEntity attacker && attacker != defender) {
            return hasAnyAttackElement(attacker);
        }

        return false;
    }

    public static void spawnNullifyEffect(Player defender) {
        spawnEraseParticles(defender, 3);
    }

    public static boolean hasPendingHurt(UUID uuid) {
        return false;
    }

    public static boolean hasPendingEffect(UUID uuid) {
        return false;
    }

    public static float applyArmorPenetration(LivingEntity target, float postDamage) {
        return postDamage;
    }

    public static float applyEffectPenetration(LivingEntity target, float postDamage) {
        return postDamage;
    }

    private static boolean hasErrorElement(Player player) {
        if (ElementalDamageUtils.getElementType(player.getMainHandItem()) == ElementType.ERROR) return true;
        if (ElementalDamageUtils.getElementType(player.getOffhandItem()) == ElementType.ERROR) return true;
        return ElementalDamageUtils.getBookSlotInfo(player).type == ElementType.ERROR;
    }

    private static boolean hasAnyAttackElement(LivingEntity attacker) {
        if (hasAttackElement(attacker.getMainHandItem())) return true;
        if (hasAttackElement(attacker.getOffhandItem())) return true;
        if (attacker instanceof Player player) {
            ElementType bookType = ElementalDamageUtils.getBookSlotInfo(player).type;
            return bookType != ElementType.NONE;
        }
        return false;
    }

    private static boolean hasAttackElement(ItemStack stack) {
        ElementType type = ElementalDamageUtils.getElementType(stack);
        return type != ElementType.NONE;
    }

    private static boolean isRangedAttack(DamageSource source) {
        Entity direct = source.getDirectEntity();
        Entity owner = source.getEntity();
        return source.is(DamageTypeTags.IS_PROJECTILE)
                || source.is(DamageTypes.ARROW)
                || source.is(DamageTypes.TRIDENT)
                || source.is(DamageTypes.THROWN)
                || source.is(DamageTypes.MOB_PROJECTILE)
                || direct instanceof Projectile
                || (direct != null && direct != owner && !(direct instanceof LivingEntity));
    }

    private static ElementType getSourceElement(DamageSource source) {
        if (source instanceof IElementalDamageSource elementalSource) {
            ElementType type = elementalSource.getElementType();
            if (type != null && type != ElementType.NONE) return type;
        }

        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.HOLY)) return ElementType.HOLY;
        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.ICE)) return ElementType.ICE;
        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.ELECTRIC)) return ElementType.ELECTRIC;
        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.THUNDER)) return ElementType.THUNDER;
        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.CORROSION)) return ElementType.CORROSION;
        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.DARK)) return ElementType.DARK;
        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.MIASMA)) return ElementType.MIASMA;
        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.FIRE)) return ElementType.FIRE;
        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.WATER)) return ElementType.WATER;
        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.WIND)) return ElementType.WIND;
        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.ERROR)) return ElementType.ERROR;
        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.BLOOD)) return ElementType.BLOOD;
        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.DARK_DOT)) return ElementType.DARK;
        if (source.is(TheFourPrimitivesAndWeaponsModDamageTypes.BLOOD_DOT)) return ElementType.BLOOD;

        return ElementType.NONE;
    }

    private static void spawnEraseParticles(LivingEntity target, int level) {
        if (target == null || target.level().isClientSide) return;
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        int count = Math.min(18, 6 + level);
        double y = target.getY() + target.getBbHeight() * 0.5;
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                target.getX(), y, target.getZ(),
                count, 0.35, 0.45, 0.35, 0.04);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                target.getX(), y, target.getZ(),
                Math.max(2, count / 3), 0.25, 0.3, 0.25, 0.08);
    }
}
