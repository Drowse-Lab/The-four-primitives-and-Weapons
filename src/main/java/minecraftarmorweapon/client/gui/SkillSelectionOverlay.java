package minecraftarmorweapon.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

import minecraftarmorweapon.skill.PlayerSkillData;
import minecraftarmorweapon.skill.PlayerSkillData.WeaponType;
import minecraftarmorweapon.skill.PlayerSkillData.SkillType;
import minecraftarmorweapon.network.SkillSelectionPacket;
import minecraftarmorweapon.MinecraftArmorWeaponMod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID, value = Dist.CLIENT)
public class SkillSelectionOverlay {
    
    private static final ResourceLocation SKILL_GUI_TEXTURE = new ResourceLocation(MinecraftArmorWeaponMod.MODID, "textures/gui/skill_selection.png");
    private static boolean isExpanded = false;
    private static List<AbstractWidget> skillWidgets = new ArrayList<>();
    
    @SubscribeEvent
    public static void onInventoryOpen(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventoryScreen)) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        
        // 知恵の書の上にスキル選択ボタンを追加
        int x = inventoryScreen.getGuiLeft() + 130;
        int y = inventoryScreen.getGuiTop() - 22;
        
        // メインのスキル選択ボタン
        Button skillButton = new Button(x, y, 20, 20, 
            Component.literal("⚔"), 
            button -> toggleSkillMenu(inventoryScreen)) {
            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                
                // ボタンの背景
                int textureY = this.isHoveredOrFocused() ? 20 : 0;
                blit(poseStack, this.x, this.y, 0, textureY, this.width, this.height);
                
                // アイコンまたはテキスト
                drawCenteredString(poseStack, mc.font, this.getMessage(), 
                    this.x + this.width / 2, this.y + (this.height - 8) / 2, 
                    this.isHoveredOrFocused() ? 0xFFFFFF : 0xA0A0A0);
                
                RenderSystem.disableBlend();
            }
        };
        
        event.addListener(skillButton);
        
        // 展開時のスキル選択UI
        if (isExpanded) {
            addSkillSelectionWidgets(inventoryScreen, event);
        }
    }
    
    private static void toggleSkillMenu(InventoryScreen screen) {
        isExpanded = !isExpanded;
        screen.init(Minecraft.getInstance(), screen.width, screen.height);
    }
    
    private static void addSkillSelectionWidgets(InventoryScreen screen, ScreenEvent.Init.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        PlayerSkillData.SkillStorage skillData = PlayerSkillData.getSkillData(player);
        
        // GUI位置を調整（インベントリの右側に配置）
        int baseX = screen.getGuiLeft() + 180;
        int baseY = screen.getGuiTop() + 20;
        
        // 武器タイプ選択ボタン
        int typeY = baseY;
        for (WeaponType type : WeaponType.values()) {
            Button typeButton = new Button(baseX, typeY, 80, 20,
                Component.literal(type.getDisplayName()),
                button -> {
                    skillData.setSelectedWeaponType(type);
                    MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                        new SkillSelectionPacket(type, null, null, null)
                    );
                    screen.init(mc, screen.width, screen.height);
                }) {
                @Override
                public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                    // 選択中の武器タイプをハイライト
                    boolean isSelected = skillData.getSelectedWeaponType() == type;
                    int color = isSelected ? 0x80FF80 : (this.isHoveredOrFocused() ? 0xFFFFFF : 0xA0A0A0);
                    
                    fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height,
                        isSelected ? 0x4000FF00 : 0x80000000);
                    drawCenteredString(poseStack, mc.font, this.getMessage(),
                        this.x + this.width / 2, this.y + (this.height - 8) / 2, color);
                }
            };
            event.addListener(typeButton);
            typeY += 25;
        }
        
        // 現在選択中の武器タイプに応じたスキル一覧
        WeaponType currentType = skillData.getSelectedWeaponType();
        int skillX = baseX + 100;
        int skillY = baseY;
        
        // スキルタイトル
        event.addListener(new Button(skillX, skillY, 150, 20,
            Component.literal("【" + currentType.getDisplayName() + "の技】"),
            button -> {}) {
            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                drawCenteredString(poseStack, mc.font, this.getMessage(),
                    this.x + this.width / 2, this.y + (this.height - 8) / 2, 0xFFD700);
            }
        });
        
        skillY += 25;
        
        // 各技の説明
        if (currentType == WeaponType.STRAIGHT_SWORD) {
            addSkillDescription(event, skillX, skillY, 
                "ダッシュ抜刀: 突きながら前進",
                "通常攻撃: 素早い突きの連打",
                "強化攻撃: 強力な突きを一撃");
        } else if (currentType == WeaponType.KATANA) {
            addSkillDescription(event, skillX, skillY,
                "ダッシュ抜刀: 居合斬り",
                "通常攻撃: 三段斬り（左上→右上→横）",
                "強化攻撃: 回転斬り（周囲攻撃）");
        }
        
        // 固有スキルのON/OFF（位置を調整）
        addUniqueSkillToggles(event, screen, skillX, skillY + 80);
    }
    
    private static void addSkillDescription(ScreenEvent.Init.Post event, int x, int y, 
                                           String dash, String normal, String charged) {
        Minecraft mc = Minecraft.getInstance();
        
        event.addListener(new Button(x, y, 200, 15,
            Component.literal("▸ " + dash),
            button -> {}) {
            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                drawString(poseStack, mc.font, this.getMessage(),
                    this.x, this.y + (this.height - 8) / 2, 0xCCCCCC);
            }
        });
        
        event.addListener(new Button(x, y + 20, 200, 15,
            Component.literal("▸ " + normal),
            button -> {}) {
            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                drawString(poseStack, mc.font, this.getMessage(),
                    this.x, this.y + (this.height - 8) / 2, 0xCCCCCC);
            }
        });
        
        event.addListener(new Button(x, y + 40, 200, 15,
            Component.literal("▸ " + charged),
            button -> {}) {
            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                drawString(poseStack, mc.font, this.getMessage(),
                    this.x, this.y + (this.height - 8) / 2, 0xCCCCCC);
            }
        });
    }
    
    private static void addUniqueSkillToggles(ScreenEvent.Init.Post event, Screen screen, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        PlayerSkillData.SkillStorage skillData = PlayerSkillData.getSkillData(player);
        ItemStack mainHand = player.getMainHandItem();
        
        // タイトル
        event.addListener(new Button(x, y, 100, 20,
            Component.literal("【固有スキル】"),
            button -> {}) {
            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                drawString(poseStack, mc.font, this.getMessage(),
                    this.x, this.y + (this.height - 8) / 2, 0xFFD700);
            }
        });
        
        y += 25;
        
        // 特定の武器の固有スキル
        if (mainHand.getItem().getClass().getSimpleName().equals("ReplicaSwordOfLightItem")) {
            addUniqueSkillToggle(event, x, y, "ReplicaSwordOfLight", 
                "SwordOfLight - ジャストガード", skillData, screen);
        } else if (mainHand.getItem().getClass().getSimpleName().equals("SwordOfNightItem")) {
            addUniqueSkillToggle(event, x, y, "SwordOfNight",
                "Sword Of Night - テレポート攻撃", skillData, screen);
            y += 25;
        } else if (mainHand.getItem().getClass().getSimpleName().equals("LunaItem")) {
            addUniqueSkillToggle(event, x, y, "Luna",
                "Luna - 突き (直刀のみ)", skillData, screen);
            y += 25;
        }
    }
    
    private static void addUniqueSkillToggle(ScreenEvent.Init.Post event, int x, int y, 
                                            String itemId, String skillName,
                                            PlayerSkillData.SkillStorage skillData, Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        
        Button toggleButton = new Button(x, y, 180, 20,
            Component.literal(skillName),
            button -> {
                // 現在の状態を取得してトグル
                boolean currentState = skillData.isUniqueSkillEnabled(itemId);
                boolean newState = !currentState;
                skillData.setUniqueSkillEnabled(itemId, newState);
                
                // サーバーに変更を送信
                MinecraftArmorWeaponMod.PACKET_HANDLER.sendToServer(
                    new SkillSelectionPacket(null, null, itemId, newState)
                );
                
                // 画面を再初期化して更新を反映
                screen.init(mc, screen.width, screen.height);
            }) {
            @Override
            public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
                boolean enabled = skillData.isUniqueSkillEnabled(itemId);
                int bgColor = enabled ? 0x6000FF00 : 0x60404040;
                int borderColor = enabled ? 0xFF00FF00 : 0xFF808080;
                int textColor = this.isHoveredOrFocused() ? 0xFFFFFF : (enabled ? 0x00FF00 : 0xCCCCCC);
                
                // 背景を描画
                fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, bgColor);
                // 枠線を描画
                fill(poseStack, this.x, this.y, this.x + this.width, this.y + 1, borderColor);
                fill(poseStack, this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, borderColor);
                fill(poseStack, this.x, this.y, this.x + 1, this.y + this.height, borderColor);
                fill(poseStack, this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, borderColor);
                
                // テキストを描画（ON/OFF状態を表示）
                String displayText = this.getMessage().getString() + " [" + (enabled ? "ON" : "OFF") + "]";
                drawCenteredString(poseStack, mc.font, Component.literal(displayText),
                    this.x + this.width / 2, this.y + (this.height - 8) / 2, textColor);
            }
        };
        
        event.addListener(toggleButton);
    }
}