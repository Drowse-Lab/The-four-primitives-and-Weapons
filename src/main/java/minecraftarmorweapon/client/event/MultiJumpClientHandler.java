package minecraftarmorweapon.client.event;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.event.MultiJumpHandler;
import minecraftarmorweapon.network.MultiJumpPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Multi Jump クライアント側入力検知。
 *
 * 毎 tick で {@code keyJump} の rising edge を見て、空中かつ multi_jump レベル > 0 なら
 * サーバーへ {@link MultiJumpPacket} を送る。残カウンタの判定はサーバー側で行うので、
 * クライアントは「ジャンプ要求」を投げるだけ。
 *
 * 着地ごとにフラグをリセットして、空中で押しっぱなしによる多段発火を防ぐ。
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, value = Dist.CLIENT)
public class MultiJumpClientHandler {

    private static boolean wasJumpHeld = false;
    private static boolean wasOnGround = true;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null) {
            wasJumpHeld = false;
            return;
        }

        boolean jumpDown = mc.options.keyJump.isDown();
        boolean onGround = player.onGround();

        // 着地でフラグリセット (次の rising edge を許可)
        if (onGround && !wasOnGround) {
            wasJumpHeld = false;
        }

        // rising edge: 押し始め + 空中 + multi_jump 装備
        if (jumpDown && !wasJumpHeld && !onGround) {
            int level = MultiJumpHandler.getLevel(player);
            if (level > 0) {
                MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(new MultiJumpPacket());
            }
        }

        wasJumpHeld = jumpDown;
        wasOnGround = onGround;
    }
}
