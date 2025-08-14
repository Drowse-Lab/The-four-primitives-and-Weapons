package minecraftarmorweapon.event;

import minecraftarmorweapon.command.CustomDifficultyCommand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class CustomDifficultyEventHandler {
    
    // プレイヤーが受けるダメージを難易度に応じて調整（シンプル版）
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player) {
            float multiplier = CustomDifficultyCommand.getDamageMultiplier();
            
            // ダメージ倍率を適用
            event.setAmount(event.getAmount() * multiplier);
        }
    }
}