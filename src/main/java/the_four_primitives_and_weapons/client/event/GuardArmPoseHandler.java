package the_four_primitives_and_weapons.client.event;

import the_four_primitives_and_weapons.client.GuardClientState;
import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModMobEffects;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ガード中のプレイヤーの腕ポーズを盾のようなBLOCKポーズに変更する
 * RenderLivingEvent.PreはsetModelProperties()の後、setupAnim()の前に発火するため
 * ここで腕ポーズを上書きすれば正しく反映される
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons", value = Dist.CLIENT)
public class GuardArmPoseHandler {

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
        if (!(event.getRenderer().getModel() instanceof PlayerModel<?> model)) return;

        boolean isGuarding = GuardClientState.isGuarding(player.getId());

        // GUARDエフェクトもチェック（旧システムとの互換性）
        if (!isGuarding) {
            try {
                isGuarding = player.hasEffect(TheFourPrimitivesAndWeaponsModMobEffects.GUARD.get());
            } catch (Exception ignored) {
            }
        }

        if (isGuarding) {
            model.rightArmPose = HumanoidModel.ArmPose.BLOCK;
        }
    }
}
