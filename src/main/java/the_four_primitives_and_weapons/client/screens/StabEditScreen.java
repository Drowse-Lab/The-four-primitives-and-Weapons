package the_four_primitives_and_weapons.client.screens;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.network.StabEditMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 突き刺さった武器/杭の編集GUI ( /stabedit でカーソル先の対象に対して開く )。
 * 左: 向き・傾き・高さ・判定 のボタン微調整。 右: 球をドラッグで自由回転 ( 向き＋傾き )。
 */
public class StabEditScreen extends Screen {

	private final int entityId;

	// 右側の回転トラックボール ( 球 )
	private int sphereX, sphereY, sphereR;

	public StabEditScreen(int entityId) {
		super(Component.literal("§d武器の編集"));
		this.entityId = entityId;
	}

	/** クライアントで GUI を開く ( パケットハンドラから呼ばれる )。 */
	public static void open(int entityId) {
		Minecraft.getInstance().setScreen(new StabEditScreen(entityId));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		int bx = cx - 170;             // ボタン列 ( 左 )
		int y = this.height / 2 - 46;

		addRenderableWidget(Button.builder(Component.literal("§e向き ←"), b -> send(0, -15f)).bounds(bx, y, 78, 20).build());
		addRenderableWidget(Button.builder(Component.literal("§e向き →"), b -> send(0, 15f)).bounds(bx + 82, y, 78, 20).build());
		y += 24;
		addRenderableWidget(Button.builder(Component.literal("§b傾き －"), b -> send(1, -5f)).bounds(bx, y, 78, 20).build());
		addRenderableWidget(Button.builder(Component.literal("§b傾き ＋"), b -> send(1, 5f)).bounds(bx + 82, y, 78, 20).build());
		y += 24;
		addRenderableWidget(Button.builder(Component.literal("§a高さ ↓"), b -> send(2, -0.1f)).bounds(bx, y, 78, 20).build());
		addRenderableWidget(Button.builder(Component.literal("§a高さ ↑"), b -> send(2, 0.1f)).bounds(bx + 82, y, 78, 20).build());
		y += 24;
		addRenderableWidget(Button.builder(Component.literal("§dロール ↺"), b -> send(4, -15f)).bounds(bx, y, 78, 20).build());
		addRenderableWidget(Button.builder(Component.literal("§dロール ↻"), b -> send(4, 15f)).bounds(bx + 82, y, 78, 20).build());
		y += 24;
		addRenderableWidget(Button.builder(Component.literal("§6判定 －"), b -> send(3, -0.1f)).bounds(bx, y, 78, 20).build());
		addRenderableWidget(Button.builder(Component.literal("§6判定 ＋"), b -> send(3, 0.1f)).bounds(bx + 82, y, 78, 20).build());
		y += 28;
		addRenderableWidget(Button.builder(Component.literal("閉じる"), b -> onClose()).bounds(bx + 40, y, 80, 20).build());

		// 右の回転球
		this.sphereR = 52;
		this.sphereX = cx + 110;
		this.sphereY = this.height / 2 - 8;
	}

	private void send(int mode, float delta) {
		TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new StabEditMessage(entityId, mode, delta));
	}

	/**
	 * 球をドラッグして自由回転。
	 *   左ドラッグ        : 横 = 向き(yaw) / 縦 = 傾き(pitch) ( 360° どこでも )
	 *   Shift + 左ドラッグ : 横 = ロール(roll)
	 *   右ドラッグ        : 上下 = 高さ
	 */
	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (super.mouseDragged(mx, my, button, dx, dy)) return true;
		if (button == 0) {
			if (hasShiftDown()) {
				if (dx != 0) send(4, (float) (dx * 2.0));    // ロール
			} else {
				if (dx != 0) send(0, (float) (dx * 2.0));    // 向き
				if (dy != 0) send(1, (float) (-dy * 1.5));   // 傾き
			}
			return true;
		}
		if (button == 1) {
			if (dy != 0) send(2, (float) (-dy * 0.02));      // 高さ ( 右ドラッグ )
			return true;
		}
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false; // 開いたまま対象の変化を見たい
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(g);
		g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFF);
		drawTrackball(g);
		g.drawCenteredString(this.font, Component.literal("§b左ドラッグ:回転"), sphereX, sphereY + sphereR + 6, 0xFFFFFF);
		g.drawCenteredString(this.font, Component.literal("§7Shift:ロール / 右:高さ"), sphereX, sphereY + sphereR + 17, 0xFFFFFF);
		super.render(g, mouseX, mouseY, partialTick);
	}

	/** 回転トラックボールの球を 3 つの円 ( 正面・横・縦 ) で描く。 */
	private void drawTrackball(GuiGraphics g) {
		int seg = 48;
		int main = 0xFF66CCFF;   // 正面の円
		int sub = 0xFF3A8FCC;    // 横・縦の楕円
		for (int i = 0; i < seg; i++) {
			double a = (Math.PI * 2 * i) / seg;
			double c = Math.cos(a), s = Math.sin(a);
			dot(g, sphereX + (int) (c * sphereR), sphereY + (int) (s * sphereR), main);             // 正面円
			dot(g, sphereX + (int) (c * sphereR), sphereY + (int) (s * sphereR * 0.35), sub);        // 横楕円
			dot(g, sphereX + (int) (c * sphereR * 0.35), sphereY + (int) (s * sphereR), sub);        // 縦楕円
		}
	}

	private static void dot(GuiGraphics g, int x, int y, int color) {
		g.fill(x - 1, y - 1, x + 1, y + 1, color);
	}
}
