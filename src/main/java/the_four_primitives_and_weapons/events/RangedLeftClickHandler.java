package the_four_primitives_and_weapons.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.network.DodgeRequestPacket;

/**
 * 遠距離武器（弓/クロスボウ, トライデント除く）を持っている時:
 * - 左クリック押下 → 回避を発動
 * - 右クリックは vanilla のまま（弓の引き絞り/発射）
 *
 * <p>投げナイフは除く。 短剣と同じ近接技 ( weapon_types の "throwing" ) を
 * 左クリックで出すようになったため、 ここで回避も出すと 1 回の左クリックで
 * 回避と技が同時に走ってしまう。 右クリック＝投擲 はそのまま。</p>
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RangedLeftClickHandler {

    private static boolean prevAttackDown = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        if (mc.screen != null) {
            prevAttackDown = false;
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        // 投げナイフは近接技側 ( ChargedAttackHandler ) が左クリックを使うので対象外。
        boolean isRanged = DodgeAndBattouHandler.isRangedWeapon(mainHand)
                && !(mainHand.getItem() instanceof the_four_primitives_and_weapons.item.ThrowingKnifeItem);
        boolean nowDown = mc.options.keyAttack.isDown();

        if (!isRanged) {
            prevAttackDown = nowDown;
            return;
        }

        // 押下の立ち上がりで回避発動（連打での連続発動を防ぐ）
        if (nowDown && !prevAttackDown) {
            if (DodgeAndBattouHandler.performDodge(player)) {
                TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(
                        new DodgeRequestPacket(player.zza, player.xxa));
            }
        }

        prevAttackDown = nowDown;
    }
}
