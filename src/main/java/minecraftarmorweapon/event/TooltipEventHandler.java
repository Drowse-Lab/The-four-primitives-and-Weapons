// package minecraftarmorweapon.event;

// import net.minecraft.world.item.ItemStack;
// import net.minecraftforge.event.entity.player.ItemTooltipEvent;
// import net.minecraftforge.eventbus.api.SubscribeEvent;
// import net.minecraftforge.fml.common.Mod;

// import net.minecraft.network.chat.Component;

// @Mod.EventBusSubscriber
// public class TooltipEventHandler {

//     @SubscribeEvent
//     public static void onTooltipDisplay(ItemTooltipEvent event) {
//         if (!event.getEntity().isCreative()) {
//             return; // クリエイティブモードでない場合は表示しない
//         }

//         if (!event.getEntity().isShiftKeyDown()) {
//             return; // Shiftキーを押していない場合は表示しない
//         }

//         ItemStack stack = event.getItemStack();
//         if (stack.isEmpty() || stack.getTag() == null) {
//             return; // 空のアイテムやNBTデータがない場合は処理しない
//         }

//         event.getToolTip().add(Component.literal("NBT Data: {"));

//         stack.getTag().getAllKeys().forEach(key -> {
//             String value = stack.getTag().get(key).toString();
//             event.getToolTip().add(Component.literal("  \"" + key + "\": " + value));
//         });

//         event.getToolTip().add(Component.literal("}"));
//     }
// }
