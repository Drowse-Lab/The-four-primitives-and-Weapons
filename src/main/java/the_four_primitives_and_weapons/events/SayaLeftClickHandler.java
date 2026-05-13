package the_four_primitives_and_weapons.events;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import the_four_primitives_and_weapons.item.LokiTheTricksterItem;
import the_four_primitives_and_weapons.network.BattouFromCurioPacket;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class SayaLeftClickHandler {
    
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // メインハンドに鞘を持っている場合
        if (isSaya(mainHand)) {
            performBattou(player, mainHand, InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND);
        }
        // オフハンドに鞘を持っている場合
        else if (isSaya(offHand)) {
            performBattou(player, offHand, InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND);
        }
        // 手に鞘がない＋メインハンドが空 → Curiosスロットから抜刀（サーバーにパケット送信）
        else if (mainHand.isEmpty()) {
            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new BattouFromCurioPacket());
        }
    }
    
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.isCanceled()) return;
        
        Player player = event.getEntity();
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // メインハンドに鞘を持っている場合
        if (isSaya(mainHand)) {
            performBattou(player, mainHand, InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND);
            event.setCanceled(true); // ブロック破壊をキャンセル
        }
        // オフハンドに鞘を持っている場合
        else if (isSaya(offHand)) {
            performBattou(player, offHand, InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND);
            event.setCanceled(true); // ブロック破壊をキャンセル
        }
    }
    
    // Mobへの攻撃時でも抜刀できるようにする（優先度を高くして先に処理）
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        
        // メインハンドに鞘を持っている場合
        if (isSaya(mainHand)) {
            // 抜刀処理を実行
            if (performBattou(player, mainHand, InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND)) {
                // 抜刀に成功したら、元の攻撃をキャンセル
                event.setCanceled(true);
            }
        }
        // オフハンドに鞘を持っている場合
        else if (isSaya(offHand)) {
            if (performBattou(player, offHand, InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND)) {
                event.setCanceled(true);
            }
        }
    }
    
    private static boolean performBattou(Player player, ItemStack sheathStack, InteractionHand sheathHand, InteractionHand otherHand) {
        CompoundTag tag = sheathStack.getOrCreateTag();

        // 鞘に武器が入っているか (StoredKatana / StoredSword / StoredRapier)
        String storedKey = the_four_primitives_and_weapons.util.CuriosScabbardHelper.findStoredKey(sheathStack);
        if (storedKey == null) return false;

        ItemStack otherHandItem = player.getItemInHand(otherHand);
        // 反対の手が空の場合のみ抜刀
        if (!otherHandItem.isEmpty()) return false;

        // 保存された武器の情報から武器を生成
        ItemStack weaponStack = ItemStack.of(tag.getCompound(storedKey));
        if (weaponStack.isEmpty()) return false;

        // 鞘から武器の情報を削除 (空の鞘にする)
        tag.remove(storedKey);

        // 封印鞘 / 霊刀スタイル等の特殊 NBT に応じた predicate 値を維持
        // (見た目本体は SayaModelWrapper が NBT を読み解いて差し替えるので
        //  ここで余分な CustomModelData は書かない)

        // タグをItemStackに適用
        sheathStack.setTag(tag);

        // まず鞘を片付ける (sheathHand に空鞘を再配置) — 次の setItemInHand と順番が逆だと
        // sheathHand と otherHand が同じだった場合に武器が消える事故が起きるので注意
        player.setItemInHand(sheathHand, sheathStack);

        // 反対の手に武器を配置
        player.setItemInHand(otherHand, weaponStack);

        // 抜刀音を再生
        player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.0F);

        // オフハンドに鞘を持っていて、かつ Loki the Trickster を抜刀した場合、モード切り替え
        if (sheathHand == InteractionHand.OFF_HAND && weaponStack.getItem() instanceof LokiTheTricksterItem) {
            LokiTheTricksterItem.toggleMode(weaponStack);
            String newMode = LokiTheTricksterItem.getMode(weaponStack);
            String modeName = newMode.equals("disarm") ? "Disarm" : "Decoy";
            player.displayClientMessage(Component.literal("§b[抜刀] Loki Mode: " + modeName), true);
        }

        return true;
    }

    private static boolean isSaya(ItemStack stack) {
        return the_four_primitives_and_weapons.util.CuriosScabbardHelper.isScabbard(stack);
    }
}