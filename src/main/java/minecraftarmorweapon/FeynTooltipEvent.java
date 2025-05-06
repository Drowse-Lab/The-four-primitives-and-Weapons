package minecraftarmorweapon;

import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.nbt.CompoundTag;

import net.minecraftforge.client.event.RenderTooltipEvent;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon", value = Dist.CLIENT)
public class FeynTooltipEvent {
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        // PoseStack poseStack = event.getPoseStack(); // テキトウに残しておこう

        if (stack.hasTag() && stack.getTag().contains("Feyn")) {
            String feyn = stack.getTag().getString("Feyn");

            if ("cursed".equals(feyn)) {
                event.getToolTip().add(Component.translatable("tooltip.minecraft_armor_weapon.feyn.cursed")
                        .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#BB44CC")).withItalic(false)));
            } else if ("sigiled".equals(feyn)) {
                event.getToolTip().add(Component.translatable("tooltip.minecraft_armor_weapon.feyn.sigiled")
                        .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#3366FF")).withItalic(false)));
            } else if ("sancted".equals(feyn)) {
                event.getToolTip().add(Component.translatable("tooltip.minecraft_armor_weapon.feyn.sancted")
                        .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFDDAA")).withItalic(false)));
            }
        }
    }
//背景を透過
    // @SubscribeEvent
    // public static void onTooltipColor(RenderTooltipEvent.Color event) {
    //     ItemStack stack = event.getItemStack();
    //     if (stack != null && stack.hasTag()) {
    //         CompoundTag tag = stack.getTag(); // 正しいインポートを確認
    //         if (tag.contains("Feyn") && "sancted".equals(tag.getString("Feyn"))) {
    //             // 色をカスタマイズ（RGB値）
    //             event.setBackground(0xFFAAAA); // 明るいピンク背景
    //             event.setBorderStart(0xFFD700); // ゴールド枠線（始まり）
    //             event.setBorderEnd(0xFF8C00);   // ゴールド枠線（終わり）
    //         }
    //     }
    // }
}
