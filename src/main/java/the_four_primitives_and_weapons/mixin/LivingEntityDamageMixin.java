package the_four_primitives_and_weapons.mixin;

import the_four_primitives_and_weapons.damage.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * LivingEntityのダメージ処理にフックするMixin
 * NBTタグから属性情報を読み取り、属性ダメージを適用します
 */
@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {

    /**
     * ダメージを受ける際に属性ダメージを適用
     * 攻撃者が持っているアイテムのNBTタグから属性を読み取る
     */
    @ModifyVariable(
        method = "hurt",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private float applyElementalDamage(float originalDamage, DamageSource source) {
        LivingEntity target = (LivingEntity) (Object) this;
        boolean hasExplicitElementalSource = source instanceof IElementalDamageSource elementalSource
                && elementalSource.getElementType() != ElementType.NONE;

        // 攻撃元のエンティティを取得
        if (!hasExplicitElementalSource && source.getEntity() instanceof LivingEntity attacker) {
            // 攻撃者が持っているメインハンドのアイテムを取得
            ItemStack weapon = attacker.getMainHandItem();

            // アイテムに属性が設定されているかチェック
            if (ElementalDamageUtils.hasElement(weapon)) {
                ElementType elementType = ElementalDamageUtils.getEffectiveElementType(weapon);
                int elementLevel = ElementalDamageUtils.getEffectiveElementLevel(weapon);

                // bookスロットの魔導書でカウンター属性を持っていれば無効化
                if (ElementalDamageUtils.isElementNullifiedByBook(target, elementType)) {
                    return originalDamage;
                }

                float modifiedDamage = originalDamage;

                // 属性に応じてダメージを計算
                switch (elementType) {
                    case ICE:
                        modifiedDamage = IceElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                        break;
                    case ELECTRIC:
                        modifiedDamage = ElectricElementDamageHandler.calculateDamage(target, originalDamage, elementLevel, source);
                        break;
                    case CORROSION:
                        modifiedDamage = CorrosionElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                        break;
                    case HOLY:
                        modifiedDamage = HolyElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                        break;
                    default:
                        break;
                }

                return modifiedDamage;
            }
        }

        // DamageSourceに属性が設定されている場合もチェック（Mixin経由）
        if (source instanceof IElementalDamageSource) {
            IElementalDamageSource elementalSource = (IElementalDamageSource) source;
            ElementType elementType = elementalSource.getElementType();
            int elementLevel = elementalSource.getElementLevel();

            // bookスロットの魔導書でカウンター属性を持っていれば無効化
            if (ElementalDamageUtils.isElementNullifiedByBook(target, elementType)) {
                return originalDamage;
            }

            float modifiedDamage = originalDamage;

            switch (elementType) {
                case ICE:
                    modifiedDamage = IceElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                    break;
                case ELECTRIC:
                    modifiedDamage = ElectricElementDamageHandler.calculateDamage(target, originalDamage, elementLevel, source);
                    break;
                case CORROSION:
                    modifiedDamage = CorrosionElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                    break;
                case HOLY:
                    modifiedDamage = HolyElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                    break;
                case SOUL:
                    modifiedDamage = SoulElementDamageHandler.calculateDamage(
                            source.getEntity() instanceof net.minecraft.world.entity.LivingEntity soulAttacker
                                    ? soulAttacker : null,
                            target, originalDamage, elementLevel);
                    break;
                case MIASMA:
                    modifiedDamage = MiasmaElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                    break;
                case BLOOD:
                    modifiedDamage = BloodElementDamageHandler.calculateDamage(
                            source.getEntity() instanceof net.minecraft.world.entity.LivingEntity bloodAttacker
                                    ? bloodAttacker : null,
                            target, originalDamage, elementLevel);
                    break;
                case SOUL_FIRE:
                    modifiedDamage = SoulFireElementDamageHandler.calculateDamage(target, originalDamage, elementLevel);
                    break;
                default:
                    break;
            }

            return modifiedDamage;
        }

        return originalDamage;
    }
}
