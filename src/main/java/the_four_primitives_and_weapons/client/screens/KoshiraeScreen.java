package the_four_primitives_and_weapons.client.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import the_four_primitives_and_weapons.menu.KoshiraeMenu;

import java.util.List;

/**
 * 拵え台のGUI ( 石切台風 )。 入力スロットに 刀/鞘 を入れると、 見た目候補が並ぶ。
 * クリックで選ぶと結果スロットに「見た目を替えた同じアイテム」が出る。
 */
public class KoshiraeScreen extends AbstractContainerScreen<KoshiraeMenu> {

	// 候補グリッド ( GUI ローカル座標 )
	private static final int GRID_X = 52, GRID_Y = 17;
	private static final int COLS = 4, ROWS = 3, CELL = 18;

	private float scrollOffs;
	private int startRow;

	public KoshiraeScreen(KoshiraeMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		this.imageWidth = 176;
		this.imageHeight = 166;
		this.titleLabelY = 4;
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
		int x = this.leftPos, y = this.topPos;
		// パネル
		g.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
		g.fill(x, y, x + imageWidth, y + 1, 0xFFFFFFFF);
		g.fill(x, y, x + 1, y + imageHeight, 0xFFFFFFFF);
		g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF555555);
		g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);
		// スロット枠
		slotBg(g, x + 20, y + 33);
		slotBg(g, x + 143, y + 33);
		// 矢印
		g.drawString(this.font, "→", x + 126, y + 38, 0x404040, false);
		// 候補エリア背景
		g.fill(x + GRID_X - 1, y + GRID_Y - 1, x + GRID_X + COLS * CELL + 1, y + GRID_Y + ROWS * CELL + 1, 0xFF8B8B8B);

		// 候補ボタン
		List<ItemStack> cands = this.menu.getCandidates();
		int rows = (cands.size() + COLS - 1) / COLS;
		this.startRow = (int) (this.scrollOffs * Math.max(0, rows - ROWS) + 0.5);
		int sel = this.menu.getSelected();
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				int idx = (this.startRow + r) * COLS + c;
				if (idx >= cands.size()) continue;
				int bx = x + GRID_X + c * CELL, by = y + GRID_Y + r * CELL;
				boolean hover = mx >= bx && mx < bx + CELL && my >= by && my < by + CELL;
				int bg = (idx == sel) ? 0xFF3B6FB0 : (hover ? 0xFF6E6E6E : 0xFF4A4A4A);
				g.fill(bx, by, bx + CELL, by + CELL, bg);
				g.renderItem(cands.get(idx), bx + 1, by + 1);
			}
		}
	}

	private void slotBg(GuiGraphics g, int sx, int sy) {
		g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
		g.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		List<ItemStack> cands = this.menu.getCandidates();
		int rows = (cands.size() + COLS - 1) / COLS;
		this.startRow = (int) (this.scrollOffs * Math.max(0, rows - ROWS) + 0.5);
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				int idx = (this.startRow + r) * COLS + c;
				if (idx >= cands.size()) continue;
				int bx = this.leftPos + GRID_X + c * CELL, by = this.topPos + GRID_Y + r * CELL;
				if (mx >= bx && mx < bx + CELL && my >= by && my < by + CELL) {
					if (this.minecraft != null && this.minecraft.gameMode != null) {
						this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, idx);
					}
					this.menu.clickMenuButton(this.minecraft.player, idx);
					return true;
				}
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double delta) {
		List<ItemStack> cands = this.menu.getCandidates();
		int rows = (cands.size() + COLS - 1) / COLS;
		int max = Math.max(0, rows - ROWS);
		if (max > 0) {
			this.scrollOffs = (float) Math.max(0.0, Math.min(1.0, this.scrollOffs - delta / max));
		}
		return true;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partial) {
		super.render(g, mx, my, partial);
		// 候補のツールチップ
		List<ItemStack> cands = this.menu.getCandidates();
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) {
				int idx = (this.startRow + r) * COLS + c;
				if (idx >= cands.size()) continue;
				int bx = this.leftPos + GRID_X + c * CELL, by = this.topPos + GRID_Y + r * CELL;
				if (mx >= bx && mx < bx + CELL && my >= by && my < by + CELL) {
					g.renderTooltip(this.font, cands.get(idx), mx, my);
				}
			}
		}
		this.renderTooltip(g, mx, my);
	}
}
