package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.item.MaterializedPouchItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.function.Supplier;

/**
 * インベントリ上のフローティング「結晶ポーチ」 パネルでスロットをクリックした結果を反映する。
 *
 * カーソルはサバイバル ( サーバー権威 ) とクリエイティブ ( クライアント権威 ) で扱いが異なり、
 * サーバー側 {@code menu.getCarried()} がクリエイティブで空になる。 そのためスワップ計算は
 * クライアントで行い、 <b>結果 ( 新しいスロット中身 + 新しいカーソル ) を送って</b>
 * サーバーはポーチ NBT を永続化 ＆ カーソルを同期する。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class PouchSlotClickMessage {

    public final int pouchInvSlot;
    public final int slotIndex;
    public final ItemStack newSlot;   // 操作後のポーチスロット中身
    public final ItemStack newCursor; // 操作後のカーソル

    public PouchSlotClickMessage(int pouchInvSlot, int slotIndex, ItemStack newSlot, ItemStack newCursor) {
        this.pouchInvSlot = pouchInvSlot;
        this.slotIndex = slotIndex;
        this.newSlot = newSlot;
        this.newCursor = newCursor;
    }

    public PouchSlotClickMessage(FriendlyByteBuf buf) {
        this.pouchInvSlot = buf.readInt();
        this.slotIndex = buf.readInt();
        this.newSlot = readFullItem(buf);
        this.newCursor = readFullItem(buf);
    }

    public static void buffer(PouchSlotClickMessage m, FriendlyByteBuf buf) {
        buf.writeInt(m.pouchInvSlot);
        buf.writeInt(m.slotIndex);
        // 完全な NBT ( エンチャント / カスタムタグ等 ) を保つため ItemStack.save 経由で送る。
        // buf.writeItem(stack) は limitedTag=true で耐久なしアイテムのタグを落とすので使わない。
        writeFullItem(buf, m.newSlot);
        writeFullItem(buf, m.newCursor);
    }

    /** ItemStack を完全な NBT 付きで書き込む ( 空は Empty フラグのみ )。 */
    private static void writeFullItem(FriendlyByteBuf buf, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            buf.writeBoolean(false);
            return;
        }
        buf.writeBoolean(true);
        buf.writeNbt(stack.save(new CompoundTag()));
    }

    /** {@link #writeFullItem} で書いた ItemStack を読み戻す。 */
    private static ItemStack readFullItem(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) return ItemStack.EMPTY;
        CompoundTag tag = buf.readNbt();
        return tag == null ? ItemStack.EMPTY : ItemStack.of(tag);
    }

    public static void handler(PouchSlotClickMessage m, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (m.slotIndex < 0 || m.slotIndex >= MaterializedPouchItem.SLOTS) return;

            // ポーチ解決
            ItemStack pouch = ItemStack.EMPTY;
            if (m.pouchInvSlot >= 0 && m.pouchInvSlot < player.getInventory().getContainerSize()
                    && player.getInventory().getItem(m.pouchInvSlot).getItem() instanceof MaterializedPouchItem) {
                pouch = player.getInventory().getItem(m.pouchInvSlot);
            } else {
                pouch = MaterializedPouchItem.findFirst(player);
            }
            if (pouch.isEmpty()) return;

            ItemStack[] lo = MaterializedPouchItem.getLoadout(pouch);
            ItemStack current = lo[m.slotIndex];

            // 検証: スロットへ「新規に物を入れる」 場合は 収納可 + 非ロック を要求
            boolean inserting = !m.newSlot.isEmpty()
                    && !ItemStack.matches(m.newSlot, current);
            if (inserting) {
                if (MaterializedPouchItem.isInsertLocked(player)) return;
                if (!MaterializedPouchItem.canStore(m.newSlot, m.slotIndex)) return;
            }

            // 武器スロットは具現化版を通常版へ戻して収納 ( クライアントと一致させる )
            lo[m.slotIndex] = (!m.newSlot.isEmpty() && MaterializedPouchItem.isWeaponSlot(m.slotIndex))
                    ? MaterializedPouchItem.normalizeForWeaponSlot(m.newSlot)
                    : m.newSlot.copy();
            MaterializedPouchItem.setLoadout(pouch, lo);

            // 収納時は「結晶化」マゼンタ演出 + 音（他の収納経路と統一）
            if (inserting) {
                MaterializedPouchItem.crystalPuff(player);
                player.playSound(net.minecraft.sounds.SoundEvents.BUNDLE_INSERT, 0.8f, 1.0f);
            }

            // カーソルをサーバー側にも反映 ( サバイバルの権威同期用 ) + クライアントへ送る
            AbstractContainerMenu menu = player.containerMenu;
            menu.setCarried(m.newCursor.copy());
            menu.broadcastChanges();
            player.connection.send(new ClientboundContainerSetSlotPacket(
                -1, menu.incrementStateId(), -1, m.newCursor.copy()));
        });
        ctx.setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(
            PouchSlotClickMessage.class, PouchSlotClickMessage::buffer, PouchSlotClickMessage::new, PouchSlotClickMessage::handler);
    }
}
