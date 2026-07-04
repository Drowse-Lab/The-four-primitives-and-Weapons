package the_four_primitives_and_weapons.client;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.skill.WeaponStatsRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * 「When in Main Hand:」の 攻撃力/攻撃速度 と同じ緑スタイルで「攻撃範囲」を表示する。
 * 併せて、 自動で出る Entity Reach / Block Reach の属性行を消して重複を防ぐ。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, value = Dist.CLIENT)
public class WeaponRangeTooltip {

    /** 素の近接攻撃リーチ ( 表示上の基礎値 )。 */
    private static final double BASE_REACH = 3.0;

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        // このMODのアイテムのみ対象
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !id.getNamespace().equals(TheFourPrimitivesAndWeaponsMod.MODID)) return;

        List<Component> tip = event.getToolTip();

        // 自動追加された Reach 系の行を除去 ( 重複防止 )。
        String entityReachName = I18n.get(ForgeMod.ENTITY_REACH.get().getDescriptionId());
        String blockReachName = I18n.get(ForgeMod.BLOCK_REACH.get().getDescriptionId());
        tip.removeIf(c -> {
            String s = c.getString();
            return s.contains(entityReachName) || s.contains(blockReachName);
        });

        // 攻撃速度の行を探す ( = 近接武器のとき )。 無ければ範囲は表示しない。
        String atkSpeedName = I18n.get(Attributes.ATTACK_SPEED.getDescriptionId());
        int idx = -1;
        for (int i = 0; i < tip.size(); i++) {
            if (tip.get(i).getString().contains(atkSpeedName)) { idx = i; break; }
        }
        if (idx < 0) return;

        double range = BASE_REACH + WeaponStatsRegistry.attackRangeBonus(stack);
        Component line = Component.translatable(
                "attribute.modifier.equals.0",
                ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(range),
                Component.translatable("attribute.name.the_four_primitives_and_weapons.attack_range"))
                .withStyle(ChatFormatting.DARK_GREEN);
        tip.add(idx + 1, line);
    }
}
