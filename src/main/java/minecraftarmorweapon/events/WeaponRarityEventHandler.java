package minecraftarmorweapon.events;

import minecraftarmorweapon.item.rarity.WeaponRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * レアリティに応じた攻撃力ボーナスとツールチップ表示
 */
@Mod.EventBusSubscriber
public class WeaponRarityEventHandler {

    /**
     * 武器のAttributeModifierにレアリティボーナスを追加
     */
    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.MAINHAND) return;

        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof SwordItem)) return;

        WeaponRarity rarity = WeaponRarity.getFromStack(stack);
        double catalystBonus = WeaponRarity.getCatalystBonus(stack);
        double totalBonus = (rarity != null ? rarity.getAttackBonus() : 0) + catalystBonus;

        // レアリティ＋触媒ボーナスを1つのModifierとして追加
        if (totalBonus > 0) {
            event.addModifier(Attributes.ATTACK_DAMAGE,
                    WeaponRarity.createCombinedDamageModifier(totalBonus));
        }
    }

    /**
     * ツールチップにレアリティ情報を表示
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof SwordItem)) return;

        WeaponRarity rarity = WeaponRarity.getFromStack(stack);
        if (rarity == null) return;

        // レアリティ名を1行目の後に挿入
        event.getToolTip().add(1, Component.literal(rarity.getColoredName()));

        // 合算攻撃力ボーナス表示
        double catalystBonus = WeaponRarity.getCatalystBonus(stack);
        double totalBonus = rarity.getAttackBonus() + catalystBonus;
        int nextIdx = 2;
        if (totalBonus > 0) {
            event.getToolTip().add(nextIdx++,
                    Component.literal("\u00A77Bonus: \u00A7a+" + (int) totalBonus + " Attack Damage"));
        }

        // 禁忌レアリティの特殊能力表示
        if (rarity == WeaponRarity.FORBIDDEN) {
            event.getToolTip().add(nextIdx,
                    Component.literal("\u00A74Forbidden Power: \u00A7cProjectile Reflection"));
        }
    }
}
