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
    /** ブロック操作距離 ( BLOCK_REACH ) 用の固定UUID。 */
    private static final UUID TFPW_BLOCK_REACH_UUID = UUID.fromString("7c30ae2b-9d4f-4b22-8e3c-3a5f8b2d0e44");
    /** バニラ ATTACK_DAMAGE_UUID と同値 ( protected なので複製 )。 */
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    /** 本MOD attribute の名前空間プレフィックス。 */
    private static final String TFPW_ATTRIBUTE_PREFIX = "the_four_primitives_and_weapons:";
    /** バニラ ATTACK_SPEED_UUID と同値。 */
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    /**
     * AttributeModifiers の Name は内部識別用に過ぎないため、AttributeName があれば省略可能にする。
     * バニラが modifier を読む前に、未指定の Name を attribute ID から補完する。
     */
    @Inject(method = "getAttributeModifiers", at = @At("HEAD"), require = 0)
    private void tfpw$fillMissingAttributeModifierNames(EquipmentSlot slot,
                                                        CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (!self.hasTag() || !self.getTag().contains("AttributeModifiers", net.minecraft.nbt.Tag.TAG_LIST)) return;
        net.minecraft.nbt.ListTag modifiers = self.getTag().getList(
                "AttributeModifiers", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < modifiers.size(); i++) {
            net.minecraft.nbt.CompoundTag modifier = modifiers.getCompound(i);
            String attributeName = modifier.getString("AttributeName");
            if (attributeName.isEmpty()) continue;
            if (attributeName.startsWith(TFPW_ATTRIBUTE_PREFIX)) {
                // 本MODのattributeは短縮名 (例: evasion / entity_reach) で固定する
                String shortName = attributeName.substring(TFPW_ATTRIBUTE_PREFIX.length());
                if (!shortName.equals(modifier.getString("Name"))) {
                    modifier.putString("Name", shortName);
                }
            } else if (modifier.getString("Name").isEmpty()) {
                modifier.putString("Name", attributeName);
            }
        }
    }

    /**
     * getMaxDamage() を上書き: JSONで定義された耐久値を返す
     */
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true, require = 0)
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
    @Inject(method = "isEnchantable", at = @At("RETURN"), cancellable = true, require = 0)
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
    @Inject(method = "getAttributeModifiers", at = @At("RETURN"), cancellable = true, require = 0)
    private void tfpw$applyStats(EquipmentSlot slot, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        // 何があってもゲームをクラッシュさせない ( 例外時は元の戻り値のまま )。
        try {
            if (slot != EquipmentSlot.MAINHAND || !WeaponStatsRegistry.isLoaded()) return;
            ItemStack self = (ItemStack) (Object) this;
            WeaponStats st = WeaponStatsRegistry.getStats(self);
            if (st == null) return;
            boolean hasDmg = !Float.isNaN(st.attackDamage);
            boolean hasSpd = !Float.isNaN(st.attackSpeed);
            boolean hasReach = !Float.isNaN(st.attackRange);
            if (!hasDmg && !hasSpd && !hasReach) return;

            Multimap<Attribute, AttributeModifier> orig = cir.getReturnValue();
            if (orig == null) return;

            // アイテム側に AttributeModifiers ( item attribute ) が付いていれば その属性は優先し、
            // NBTに含まれない属性だけ JSON で補う ( 例: reach だけNBT指定→ダメージ/速度はJSONのまま )。
            boolean hasNbt = self.hasTag() && self.getTag().contains("AttributeModifiers", 9);
            Attribute reach = hasReach ? ForgeMod.ENTITY_REACH.get() : null;
            Attribute blockReach = (hasReach && st.attackRange < 0) ? ForgeMod.BLOCK_REACH.get() : null;

            boolean applyDmg = hasDmg && !(hasNbt && orig.containsKey(Attributes.ATTACK_DAMAGE));
            boolean applySpd = hasSpd && !(hasNbt && orig.containsKey(Attributes.ATTACK_SPEED));
            boolean applyReach = reach != null && !(hasNbt && orig.containsKey(reach));
            boolean applyBlock = blockReach != null && !(hasNbt && orig.containsKey(blockReach));
            // 変更が無いなら Map を確保せず即return ( ラグ削減 )。
            if (!applyDmg && !applySpd && !applyReach && !applyBlock) return;

            Multimap<Attribute, AttributeModifier> out = HashMultimap.create(orig);
            if (applyDmg) {
                out.removeAll(Attributes.ATTACK_DAMAGE);
                // バニラは「基礎1 + modifier」で総攻撃力。 総攻撃力 = attackDamage。
                out.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                        ATTACK_DAMAGE_UUID, "Weapon modifier", st.attackDamage - 1.0, AttributeModifier.Operation.ADDITION));
            }
            if (applySpd) {
                out.removeAll(Attributes.ATTACK_SPEED);
                out.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                        ATTACK_SPEED_UUID, "Weapon modifier", st.attackSpeed, AttributeModifier.Operation.ADDITION));
            }
            if (applyReach) {
                out.removeAll(reach);
                out.put(reach, new AttributeModifier(
                        TFPW_REACH_UUID, "Weapon reach", st.attackRange, AttributeModifier.Operation.ADDITION));
            }
            if (applyBlock) {
                out.removeAll(blockReach);
                out.put(blockReach, new AttributeModifier(
                        TFPW_BLOCK_REACH_UUID, "Weapon block reach", st.attackRange, AttributeModifier.Operation.ADDITION));
            }
            cir.setReturnValue(out);
        } catch (Throwable ignored) {
            // no-op: 属性上書きは失敗しても無視 ( クラッシュ防止 )
        }
    }
}
