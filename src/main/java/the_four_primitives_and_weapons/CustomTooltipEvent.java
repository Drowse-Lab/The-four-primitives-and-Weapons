// package the_four_primitives_and_weapons.event;

// import net.minecraft.ChatFormatting;
// import net.minecraft.network.chat.Component;
// import net.minecraft.world.item.ItemStack;
// import net.minecraftforge.api.distmarker.Dist;
// import net.minecraftforge.event.entity.player.ItemTooltipEvent;
// import net.minecraftforge.eventbus.api.SubscribeEvent;
// import net.minecraftforge.fml.common.Mod;

// @Mod.EventBusSubscriber(modid = "minecraf_tarmor_weapon", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
// public class CustomTooltipEvent {

//     @SubscribeEvent
//     public static void onItemTooltip(ItemTooltipEvent event) {
//         ItemStack stack = event.getItemStack();

//         if (stack.hasTag()) {
//             String feynTag = stack.getTag().getString("Feyn");
//             if ("cursed".equals(feynTag)) {
//                 event.getToolTip().add(Component.literal("妖").withStyle(ChatFormatting.LIGHT_PURPLE));
//             } else if ("sigiled".equals(feynTag)) {
//                 event.getToolTip().add(Component.literal("封").withStyle(ChatFormatting.BLUE));
//             }
//         }
//     }
// }
