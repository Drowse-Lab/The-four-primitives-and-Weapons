package minecraftarmorweapon.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * ガイドブック — 操作方法・ナイフ・Mobを画像付きで紹介。
 *   右クリックでクライアント側の GuideBookScreen を開く。
 */
public class GuideBookItem extends Item {

    public GuideBookItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            openScreen();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    private static void openScreen() {
        net.minecraft.client.Minecraft.getInstance().setScreen(
            new minecraftarmorweapon.client.screens.GuideBookScreen());
    }
}
