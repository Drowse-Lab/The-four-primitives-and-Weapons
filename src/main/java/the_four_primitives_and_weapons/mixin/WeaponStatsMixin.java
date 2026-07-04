package the_four_primitives_and_weapons.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import the_four_primitives_and_weapons.skill.WeaponStatsRegistry;
import the_four_primitives_and_weapons.skill.WeaponStatsRegistry.WeaponStats;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.common.ForgeMod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ItemStack.class)
public abstract class WeaponStatsMixin {

    /** リーチ ( ENTITY_REACH ) 用の固定UUID。 */
    private static final UUID TFPW_REACH_UUID = UUID.fromString("6b2f7d1a-8c3e-4a11-9f2b-2f4e7a1c9d33");
    /** バニラ ATTACK_DAMAGE_UUID と同値 ( protected なので複製 )。 */
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    /** バニラ ATTACK_SPEED_UUID と同値。 */
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    /**
     * getMaxDamage() を上書き: JSONで定義された耐久値を返す
     */
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void onGetMaxDamage(CallbackInfoReturnable<Integer> cir) {
        if (!WeaponStatsRegistry.isLoaded()) return;
        ItemStack self = (ItemStack)(Object)this;
        int durability = WeaponStatsRegistry.getDurability(self);
        if (durability >= 0) {
            cir.setReturnValue(durability);
        }
    }

    /**
     * getEnchantmentValue() の戻り値を上書き: JSONで定義されたエンチャント適性を返す
     * (Item.getEnchantmentValue が ItemStack 経由で呼ばれるパスをカバー)
     */
    @Inject(method = "isEnchantable", at = @At("RETURN"), cancellable = true)
    private void onIsEnchantable(CallbackInfoReturnable<Boolean> cir) {
        if (!WeaponStatsRegistry.isLoaded()) return;
        ItemStack self = (ItemStack)(Object)this;
        int enchant = WeaponStatsRegistry.getEnchantability(self);
        if (enchant > 0) {
            cir.setReturnValue(true);
        }
    }

    /**
     * getAttributeModifiers() を上書き: JSONで定義された 攻撃力 / 攻撃速度 / 近接リーチ を
     * メインハンドの属性に反映する ( 該当キーが定義された武器のみ )。
     */
    @Inject(method = "getAttributeModifiers", at = @At("RETURN"), cancellable = true)
    private void tfpw$applyStats(EquipmentSlot slot, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        if (slot != EquipmentSlot.MAINHAND) return;
        if (!WeaponStatsRegistry.isLoaded()) return;
        ItemStack self = (ItemStack)(Object)this;
        WeaponStats st = WeaponStatsRegistry.getStats(self);
        if (st == null) return;
        boolean hasDmg = !Float.isNaN(st.attackDamage);
        boolean hasSpd = !Float.isNaN(st.attackSpeed);
        boolean hasReach = !Float.isNaN(st.attackRange);
        if (!hasDmg && !hasSpd && !hasReach) return;

        // アイテム側に AttributeModifiers ( item attribute ) が付いている場合、 バニラはNBTの分だけを返す。
        // その属性は item attribute を優先し、 NBTに含まれない属性だけ JSON で補う。
        // → 例: forge:entity_reach だけNBTで指定すれば、 攻撃範囲はそれ・ダメージ/速度はJSONのまま。
        boolean hasNbt = self.hasTag() && self.getTag().contains("AttributeModifiers", 9);
        Attribute reach = ForgeMod.ENTITY_REACH.get();
        Multimap<Attribute, AttributeModifier> out = HashMultimap.create(cir.getReturnValue());

        if (hasDmg && !(hasNbt && out.containsKey(Attributes.ATTACK_DAMAGE))) {
            out.removeAll(Attributes.ATTACK_DAMAGE);
            // バニラは「基礎1 + modifier」で総攻撃力になる。 総攻撃力 = attackDamage にする。
            out.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                    ATTACK_DAMAGE_UUID, "Weapon modifier", st.attackDamage - 1.0, AttributeModifier.Operation.ADDITION));
        }
        if (hasSpd && !(hasNbt && out.containsKey(Attributes.ATTACK_SPEED))) {
            out.removeAll(Attributes.ATTACK_SPEED);
            out.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                    ATTACK_SPEED_UUID, "Weapon modifier", st.attackSpeed, AttributeModifier.Operation.ADDITION));
        }
        if (hasReach && !(hasNbt && out.containsKey(reach))) {
            out.removeAll(reach);
            out.put(reach, new AttributeModifier(
                    TFPW_REACH_UUID, "Weapon reach", st.attackRange, AttributeModifier.Operation.ADDITION));
        }
        cir.setReturnValue(out);
    }
}
