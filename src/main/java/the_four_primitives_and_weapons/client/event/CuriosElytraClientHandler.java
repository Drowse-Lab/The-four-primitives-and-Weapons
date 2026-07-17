package the_four_primitives_and_weapons.client.event;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.network.CuriosElytraTakeoffPacket;
import the_four_primitives_and_weapons.util.CuriosElytraHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Curios elytra スロット離陸のクライアント側入力検知 (ElytraSlot mod 相当機能)。
 *
 * <p>バニラは {@code LocalPlayer#aiStep} でチェストのエリトラのみ見て
 * START_FALL_FLYING を送るため、Curios スロットのエリトラでは離陸できない。
 * ここで空中でのジャンプキー rising edge を検知し ( {@code MultiJumpClientHandler}
 * と同方式 )、条件を満たせば予測として {@code startFallFlying()} を実行しつつ
 * サーバーへ {@link CuriosElytraTakeoffPacket} を送る。</p>
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, value = Dist.CLIENT)
public class CuriosElytraClientHandler {

    private static boolean wasJumpHeld = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) {
            wasJumpHeld = false;
            return;
        }

        boolean jumpDown = mc.options.keyJump.isDown();

        // rising edge: 押し始め + 空中 + 滑空していない
        if (jumpDown && !wasJumpHeld && canTakeoff(player)) {
            // クライアント予測: 即座に滑空姿勢へ (サーバー同期で確定)
            player.startFallFlying();
            TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new CuriosElytraTakeoffPacket());
        }

        wasJumpHeld = jumpDown;
    }

    /** バニラ {@code Player#tryToStartFallFlying} + aiStep の離陸条件 ( Curios 版 )。 */
    private static boolean canTakeoff(LocalPlayer player) {
        if (player.onGround() || player.isFallFlying() || player.isInWater()
                || player.isPassenger() || player.onClimbable()
                || player.getAbilities().flying
                || player.hasEffect(MobEffects.LEVITATION)) {
            return false;
        }
        // チェストのエリトラで飛べるならバニラが処理する
        if (player.getItemBySlot(EquipmentSlot.CHEST).canElytraFly(player)) {
            return false;
        }
        return !CuriosElytraHelper.findFlyableElytra(player).isEmpty();
    }
}
