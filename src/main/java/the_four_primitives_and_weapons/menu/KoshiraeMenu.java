package the_four_primitives_and_weapons.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import the_four_primitives_and_weapons.init.KoshiraeInit;
import the_four_primitives_and_weapons.util.KoshiraeFittings;

import java.util.ArrayList;
import java.util.List;

/**
 * 拵え台のメニュー ( 石切台風 )。 入力スロットに 刀/鞘 を入れると、 見た目の候補が出て、
 * クリックで選ぶと結果スロットに「見た目を替えた同じアイテム」が出る ( 素材消費なし )。
 */
public class KoshiraeMenu extends AbstractContainerMenu {

	public static final int INPUT = 0;
	public static final int RESULT = 1;
	public static final int DYE = 2;

	private final ContainerLevelAccess access;
	private final Slot inputSlot;
	private final Slot resultSlot;
	private final Slot dyeSlot;
	private final SimpleContainer container = new SimpleContainer(1);
	private final SimpleContainer resultContainer = new SimpleContainer(1);
	private final SimpleContainer dyeContainer = new SimpleContainer(1);
	private final DataSlot selected = DataSlot.standalone();
	private ItemStack lastInput = ItemStack.EMPTY;
	private ItemStack lastDye = ItemStack.EMPTY;

	public KoshiraeMenu(int id, Inventory inv) {
		this(id, inv, ContainerLevelAccess.NULL);
	}

	public KoshiraeMenu(int id, Inventory inv, ContainerLevelAccess access) {
		super(KoshiraeInit.MENU.get(), id);
		this.access = access;
		// 注: Slot の第2引数は「コンテナ内のindex」。 入力/結果は別コンテナ(各サイズ1)なので両方 0。
		// ( メニュー上のスロット番号は addSlot の順 = INPUT(0), RESULT(1) )
		this.inputSlot = this.addSlot(new Slot(this.container, 0, 20, 33));
		this.resultSlot = this.addSlot(new Slot(this.resultContainer, 0, 143, 33) {
			@Override public boolean mayPlace(ItemStack s) { return false; }
			@Override public void onTake(Player p, ItemStack taken) {
				inputSlot.remove(1);
				setupResult();
				super.onTake(p, taken);
			}
		});
		// 染料スロット ( メニュー番号 DYE=2 )。 染料を入れると 候補が「部位ごとの色変更」になる。
		this.dyeSlot = this.addSlot(new Slot(this.dyeContainer, 0, 20, 55) {
			@Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof net.minecraft.world.item.DyeItem; }
		});
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 9; j++)
				this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
		for (int k = 0; k < 9; k++)
			this.addSlot(new Slot(inv, k, 8 + k * 18, 142));

		this.container.addListener(c -> this.slotsChanged(c));
		this.dyeContainer.addListener(c -> this.slotsChanged(c));
		this.addDataSlot(this.selected);
	}

	public int getSelected() { return this.selected.get(); }
	public List<ItemStack> getCandidates() {
		// クライアント表示用: 入力(+染料)から都度計算 ( 同期不要 )
		return KoshiraeFittings.candidatesFor(this.inputSlot.getItem(), this.dyeSlot.getItem());
	}
	public boolean hasInput() { return this.inputSlot.hasItem(); }

	@Override
	public boolean clickMenuButton(Player player, int id) {
		List<ItemStack> list = KoshiraeFittings.candidatesFor(this.inputSlot.getItem(), this.dyeSlot.getItem());
		if (id >= 0 && id < list.size()) {
			this.selected.set(id);
			setupResult();
		}
		return true;
	}

	@Override
	public void slotsChanged(Container c) {
		ItemStack in = this.inputSlot.getItem();
		ItemStack dye = this.dyeSlot.getItem();
		if (!ItemStack.matches(in, this.lastInput) || !ItemStack.matches(dye, this.lastDye)) {
			this.lastInput = in.copy();
			this.lastDye = dye.copy();
			this.selected.set(-1);
			this.resultSlot.set(ItemStack.EMPTY);
			broadcastChanges();
		}
	}

	private void setupResult() {
		int i = this.selected.get();
		List<ItemStack> list = KoshiraeFittings.candidatesFor(this.inputSlot.getItem(), this.dyeSlot.getItem());
		if (i >= 0 && i < list.size()) {
			ItemStack r = list.get(i).copy();
			r.resetHoverName(); // 候補のラベル名(「柄を染める」等)は結果に残さない
			this.resultSlot.set(r);
		} else {
			this.resultSlot.set(ItemStack.EMPTY);
		}
		broadcastChanges();
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, KoshiraeInit.BLOCK.get());
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.access.execute((lvl, pos) -> {
			this.clearContainer(player, this.container);
			this.clearContainer(player, this.dyeContainer);
		});
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		ItemStack ret = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack s = slot.getItem();
			ret = s.copy();
			int invStart = 3;               // プレイヤーインベントリの開始スロット ( INPUT/RESULT/DYE の次 )
			int invEnd = this.slots.size();
			if (index == INPUT || index == RESULT || index == DYE) {
				if (!this.moveItemStackTo(s, invStart, invEnd, true)) return ItemStack.EMPTY;
				slot.onQuickCraft(s, ret);
			} else if (s.getItem() instanceof net.minecraft.world.item.DyeItem) {
				// プレイヤー → 染料スロットへ
				if (!this.moveItemStackTo(s, DYE, DYE + 1, false)) return ItemStack.EMPTY;
			} else {
				// プレイヤー → 入力スロットへ
				if (!this.moveItemStackTo(s, INPUT, INPUT + 1, false)) return ItemStack.EMPTY;
			}
			if (s.isEmpty()) slot.set(ItemStack.EMPTY);
			else slot.setChanged();
			if (s.getCount() == ret.getCount()) return ItemStack.EMPTY;
			slot.onTake(player, s);
			broadcastChanges();
		}
		return ret;
	}
}
