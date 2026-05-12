
package the_four_primitives_and_weapons.events;

import the_four_primitives_and_weapons.item.ReitouItem;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

/**
 * 霊刀の怨念回復ハンドラー
 * 他の武器で敵を攻撃すると、インベントリ内の霊刀の耐久度が回復する
 */
@Mod.EventBusSubscriber
public class ReitouDurabilityHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        try {
            if (event.getSource() == null || event.getSource().getEntity() == null)
                return;

            Entity attacker = event.getSource().getEntity();
            if (!(attacker instanceof Player player))
                return;

            if (player.level().isClientSide())
                return;

            // メインハンドが霊刀以外の武器であるかチェック
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.isEmpty() || mainHand.getItem() instanceof ReitouItem)
                return;

            // インベントリ内の霊刀を探して耐久度を回復
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() instanceof ReitouItem && stack.isDamaged()) {
                    // 怨念の力で耐久度を回復（攻撃ごとに2ポイント）
                    int repairAmount = 2;
                    stack.setDamageValue(Math.max(0, stack.getDamageValue() - repairAmount));
                    break; // 一度に回復するのは1本だけ
                }
            }
        } catch (Throwable e) {
            System.err.println("[ReitouDurabilityHandler] Error: " + e.getMessage());
        }
    }
}
