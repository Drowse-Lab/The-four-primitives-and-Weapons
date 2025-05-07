package minecraftarmorweapon.item;

import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;

import minecraftarmorweapon.world.inventory.GuideBookGuiMenu; // カスタムGUIメニュー
import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

import javax.annotation.Nullable;

import io.netty.buffer.Unpooled;

public class GuideBookItem extends Item {
    public GuideBookItem() {
        super(new Item.Properties()
                .tab(MinecraftArmorWeaponModTabs.TAB_MAGIC_BOOKS) // ゲーム内のカスタムタブ
                .stacksTo(1) // スタック数を1に制限
                .rarity(Rarity.RARE)); // レアリティを設定
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // サーバーサイドでのみGUIを開く
        if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Guide Book"); // GUIのタイトル
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                    // メニューのデータを送信
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    buf.writeItem(itemstack); // アイテムデータを送信
                    return new GuideBookGuiMenu(id, inventory, buf); // カスタムGUIメニュー
                }
            }, buf -> {
                buf.writeItem(itemstack); // アイテムデータを送信
            });
        }

        return InteractionResultHolder.success(itemstack);
    }
}
