package minecraftarmorweapon.event;

import minecraftarmorweapon.command.CustomDifficultyCommand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class CustomDifficultyEventHandler {

    // Mobからプレイヤーが受けるダメージを難易度に応じて調整
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player && event.getSource().getEntity() instanceof Mob) {
            float multiplier = CustomDifficultyCommand.getDamageMultiplier();

            // Mobからのダメージ倍率を適用
            event.setAmount(event.getAmount() * multiplier);
        }
    }
}