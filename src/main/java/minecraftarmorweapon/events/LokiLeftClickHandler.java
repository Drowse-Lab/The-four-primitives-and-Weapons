package minecraftarmorweapon.events;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import minecraftarmorweapon.item.LokiTheTricksterItem;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class LokiLeftClickHandler {

    // 左クリック空振り時 - 通常攻撃をキャンセル
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        // Loki the Tricksterを持っている場合、通常攻撃をキャンセル（チャージシステムを使用）
        if (mainHand.getItem() instanceof LokiTheTricksterItem && !player.isShiftKeyDown()) {
            // このイベントはキャンセルできないが、フラグとして記録
            // 実際のキャンセルはAttackEntityEventで行う
        }
    }

    // ブロック左クリック時 - ブロック破壊をキャンセル
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        // Loki the Tricksterを持っている場合、ブロック破壊をキャンセル
        if (mainHand.getItem() instanceof LokiTheTricksterItem && !player.isShiftKeyDown()) {
            event.setCanceled(true);
        }
    }

    // エンティティ攻撃時 - 通常攻撃をキャンセル
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);

        // Loki the Tricksterを持っている場合、通常攻撃をキャンセル（チャージシステムを使用）
        if (mainHand.getItem() instanceof LokiTheTricksterItem && !player.isShiftKeyDown()) {
            event.setCanceled(true);
        }
    }
}
