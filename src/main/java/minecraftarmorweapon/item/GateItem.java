package minecraftarmorweapon.item;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Gate剣
 *
 * 元データパック: poof.mcfunction / Custom:197
 *
 * 機能:
 *   - 右クリック（use）で追尾型装甲スタンドプロジェクタイル（fr1）を発射
 *   - fr1は敵に近づくとTNTを召喚して自爆
 *   - 発射時にResistance付与・サウンド再生・パーティクル
 *   - Knockback 10 相当の吹き飛ばし
 */
public class GateItem extends SwordItem {

    public static final int CUSTOM_ID = 197;

    public GateItem() {
        super(Tiers.GOLD, 3, -2.4f,
                new Properties()
                        .stacksTo(1)
                        .fireResistant()
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Gate")
                .setStyle(Style.EMPTY
                        .withColor(net.minecraft.ChatFormatting.GOLD)
                        .withItalic(false)));
    }

    /** アイテムがGate剣かどうか判定するユーティリティ */
    public static boolean isGateSword(ItemStack stack) {
        return stack.getItem() instanceof GateItem;
    }
}
