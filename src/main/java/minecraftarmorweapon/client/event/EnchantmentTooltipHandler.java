package minecraftarmorweapon.client.event;

import minecraftarmorweapon.MinecraftArmorWeaponMod;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * 全アイテムの Tooltip にエンチャントの効果説明を追加。
 *
 * 翻訳キー命名規則: {@code <enchantment.descriptionId>.desc}
 *   例: {@code enchantment.minecraft_armor_weapon.multi_jump.desc}
 *       → ja_jp.json で「空中ジャンプ追加。Hookshot 装着で浮遊回復ペース UP」 等を定義
 *
 * 翻訳キーが存在しないエンチャは何も追加しない (vanilla エンチャ含め、必要なものだけ書ける)。
 * tooltip にレベル固有の数値を入れたい場合は {@code %d} などを翻訳テキスト内に書き、
 * I18n のフォーマット引数でレベルを渡す。
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, value = Dist.CLIENT)
public class EnchantmentTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        if (enchants.isEmpty()) return;

        for (var entry : enchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int level = entry.getValue();
            String descKey = ench.getDescriptionId() + ".desc";
            if (!I18n.exists(descKey)) continue;
            event.getToolTip().add(
                Component.translatable(descKey, level).withStyle(ChatFormatting.DARK_GRAY)
            );
        }
    }
}
