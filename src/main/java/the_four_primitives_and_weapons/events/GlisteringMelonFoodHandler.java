package the_four_primitives_and_weapons.events;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 煌めくスイカ ( Glistering Melon Slice ) はバニラでは食べられないので、 食料として使えるようにする。
 * 右クリックで即時に食べる ( 満腹時は不可 )。
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class GlisteringMelonFoodHandler {

    private static final int NUTRITION = 4;        // 回復する満腹度 ( スイカ片の倍 )
    private static final float SATURATION = 0.6f;  // 隠し満腹度係数

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (stack.getItem() != Items.GLISTERING_MELON_SLICE) return;
        if (!player.canEat(false)) return; // 満腹なら食べられない

        if (!player.level().isClientSide) {
            player.getFoodData().eat(NUTRITION, SATURATION);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8f, 1.0f);
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.copy()),
                        player.getX(), player.getEyeY() - 0.2, player.getZ(), 8, 0.1, 0.1, 0.1, 0.05);
            }
            stack.shrink(1);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
