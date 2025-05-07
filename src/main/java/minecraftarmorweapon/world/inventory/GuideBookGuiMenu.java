// package minecraftarmorweapon.world.inventory;

// import net.minecraft.world.inventory.AbstractContainerMenu;
// import net.minecraft.world.entity.player.Player;
// import net.minecraft.world.entity.player.Inventory;
// import net.minecraft.network.FriendlyByteBuf;

// public class GuideBookGuiMenu extends AbstractContainerMenu {
//     private final String guideBookData;

//     public GuideBookGuiMenu(int id, Inventory playerInventory, FriendlyByteBuf data) {
//         super(null, id); // 必要に応じてメニュータイプを設定
//         this.guideBookData = data.readUtf(); // JSONデータを受け取る
//     }

//     public String getGuideBookData() {
//         return guideBookData;
//     }

//     @Override
//     public boolean stillValid(Player player) {
//         return true; // GUIを開いている間有効
//     }
// }
