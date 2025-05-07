package minecraftarmorweapon.world.inventory;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.FriendlyByteBuf;

public class GuideBookGuiMenu extends AbstractContainerMenu {
    private final ItemStack itemStack;

    public GuideBookGuiMenu(int id, Inventory playerInventory, FriendlyByteBuf data) {
        super(null, id); // 必要なメニュータイプを設定
        this.itemStack = data.readItem(); // アイテムデータを読み取る
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // GUIが開いている間、常に有効
    }

    // 必要に応じてGUIの要素やデータ処理を追加
}
