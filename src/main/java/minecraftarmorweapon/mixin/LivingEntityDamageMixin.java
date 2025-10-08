package minecraftarmorweapon.mixin;

import minecraftarmorweapon.damage.*;
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

        // 攻撃元のエンティティを取得
        if (source.getEntity() instanceof LivingEntity attacker) {
            // 攻撃者が持っているメインハンドのアイテムを取得
            ItemStack weapon = attacker.getMainHandItem();

            // デバッグログ
            System.out.println("[ElementalDamage] Checking weapon: " + weapon.getDisplayName().getString());
            System.out.println("[ElementalDamage] Has element: " + ElementalDamageUtils.hasElement(weapon));

            // アイテムに属性が設定されているかチェック
            if (ElementalDamageUtils.hasElement(weapon)) {
                ElementType elementType = ElementalDamageUtils.getElementType(weapon);
                int elementLevel = ElementalDamageUtils.getElementLevel(weapon);

                System.out.println("[ElementalDamage] Element: " + elementType + " Level: " + elementLevel);
                System.out.println("[ElementalDamage] Original damage: " + originalDamage);

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

                System.out.println("[ElementalDamage] Modified damage: " + modifiedDamage);
                return modifiedDamage;
            }
        }

        // DamageSourceに属性が設定されている場合もチェック（Mixin経由）
        if (source instanceof IElementalDamageSource) {
            IElementalDamageSource elementalSource = (IElementalDamageSource) source;
            ElementType elementType = elementalSource.getElementType();
            int elementLevel = elementalSource.getElementLevel();

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
                default:
                    break;
            }

            return modifiedDamage;
        }

        return originalDamage;
    }
}
