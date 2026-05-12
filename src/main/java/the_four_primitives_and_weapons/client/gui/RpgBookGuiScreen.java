package the_four_primitives_and_weapons.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Button;

import the_four_primitives_and_weapons.world.inventory.RpgBookGuiMenu;

import the_four_primitives_and_weapons.network.RpgBookGuiButtonMessage;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class RpgBookGuiScreen extends AbstractContainerScreen<RpgBookGuiMenu> {
	private final static HashMap<String, Object> guistate = RpgBookGuiMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	Button button_bogged_outer;
	Button button_magic_swordsman;
	Button button_ninja;
	Button button_vampire;
	Button button_nigu;
	Button button_chuzume;
	ImageButton imagebutton_texture1;
	ImageButton imagebutton_tapmimit;

	public RpgBookGuiScreen(RpgBookGuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 170;
		this.imageHeight = 195;
	}

	@Override
	public void render(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(ms);
		super.render(ms, mouseX, mouseY, partialTicks);
		this.renderTooltip(ms, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics ms, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		ms.blit(new ResourceLocation("the_four_primitives_and_weapons:textures/screens/img_0974.png"), this.leftPos + -57, this.topPos + -6, 0, 0, 320, 213, 320, 213);

		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	public void containerTick() {
		super.containerTick();
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		button_bogged_outer = Button.builder(Component.translatable("gui.the_four_primitives_and_weapons.rpg_book_gui.button_bogged_outer"), e -> {
			if (true) {
				TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new RpgBookGuiButtonMessage(0, x, y, z));
				RpgBookGuiButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}).bounds(this.leftPos + 11, this.topPos + 63, 87, 20).build();
		guistate.put("button:button_bogged_outer", button_bogged_outer);
		this.addRenderableWidget(button_bogged_outer);
		button_magic_swordsman = Button.builder(Component.translatable("gui.the_four_primitives_and_weapons.rpg_book_gui.button_magic_swordsman"), e -> {
			if (true) {
				TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new RpgBookGuiButtonMessage(1, x, y, z));
				RpgBookGuiButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}).bounds(this.leftPos + -4, this.topPos + 92, 103, 20).build();
		guistate.put("button:button_magic_swordsman", button_magic_swordsman);
		this.addRenderableWidget(button_magic_swordsman);
		button_ninja = Button.builder(Component.translatable("gui.the_four_primitives_and_weapons.rpg_book_gui.button_ninja"), e -> {
			if (true) {
				TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new RpgBookGuiButtonMessage(2, x, y, z));
				RpgBookGuiButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		}).bounds(this.leftPos + 47, this.topPos + 6, 51, 20).build();
		guistate.put("button:button_ninja", button_ninja);
		this.addRenderableWidget(button_ninja);
		button_vampire = Button.builder(Component.translatable("gui.the_four_primitives_and_weapons.rpg_book_gui.button_vampire"), e -> {
			if (true) {
				TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new RpgBookGuiButtonMessage(3, x, y, z));
				RpgBookGuiButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		}).bounds(this.leftPos + 37, this.topPos + 34, 61, 20).build();
		guistate.put("button:button_vampire", button_vampire);
		this.addRenderableWidget(button_vampire);
		button_nigu = Button.builder(Component.translatable("gui.the_four_primitives_and_weapons.rpg_book_gui.button_nigu"), e -> {
			if (true) {
				TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new RpgBookGuiButtonMessage(4, x, y, z));
				RpgBookGuiButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		}).bounds(this.leftPos + 52, this.topPos + 119, 46, 20).build();
		guistate.put("button:button_nigu", button_nigu);
		this.addRenderableWidget(button_nigu);
		button_chuzume = Button.builder(Component.translatable("gui.the_four_primitives_and_weapons.rpg_book_gui.button_chuzume"), e -> {
			if (true) {
				TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new RpgBookGuiButtonMessage(5, x, y, z));
				RpgBookGuiButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		}).bounds(this.leftPos + 37, this.topPos + 148, 61, 20).build();
		guistate.put("button:button_chuzume", button_chuzume);
		this.addRenderableWidget(button_chuzume);
		imagebutton_texture1 = new ImageButton(this.leftPos + -32, this.topPos + 8, 16, 16, 0, 0, 16, new ResourceLocation("the_four_primitives_and_weapons:textures/screens/atlas/imagebutton_texture1.png"), 16, 32, e -> {
			if (true) {
				TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new RpgBookGuiButtonMessage(6, x, y, z));
				RpgBookGuiButtonMessage.handleButtonAction(entity, 6, x, y, z);
			}
		});
		guistate.put("button:imagebutton_texture1", imagebutton_texture1);
		this.addRenderableWidget(imagebutton_texture1);
		imagebutton_tapmimit = new ImageButton(this.leftPos + -47, this.topPos + 180, 16, 16, 0, 0, 16, new ResourceLocation("the_four_primitives_and_weapons:textures/screens/atlas/imagebutton_tapmimit.png"), 16, 32, e -> {
			if (true) {
				TheFourPrimitivesAndWeaponsMod.PACKET_HANDLER.sendToServer(new RpgBookGuiButtonMessage(7, x, y, z));
				RpgBookGuiButtonMessage.handleButtonAction(entity, 7, x, y, z);
			}
		});
		guistate.put("button:imagebutton_tapmimit", imagebutton_tapmimit);
		this.addRenderableWidget(imagebutton_tapmimit);
	}
}
