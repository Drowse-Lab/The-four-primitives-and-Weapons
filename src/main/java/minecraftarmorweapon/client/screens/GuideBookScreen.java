package minecraftarmorweapon.client.screens;

import minecraftarmorweapon.entity.AngelTrioEntity;
import minecraftarmorweapon.entity.BlackholeEntity;
import minecraftarmorweapon.entity.CommonSoldierEntity;
import minecraftarmorweapon.entity.EliteSoldierEntity;
import minecraftarmorweapon.entity.HeroicTierEntity;
import minecraftarmorweapon.entity.SingularityEntity;
import minecraftarmorweapon.init.CustomEntityInit;
import minecraftarmorweapon.init.MinecraftArmorWeaponModEntities;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * モッド内容を画像付きで紹介するガイドブック画面。
 *   ページ毎にタイトル / 本文 / 右側に "イラスト" (アイテムアイコンや mob プレビュー)
 *   左右ボタンでページ送り。ESC で閉じる。
 */
public class GuideBookScreen extends Screen {

    private static final int BOOK_W = 300;
    private static final int BOOK_H = 200;

    private int bx, by;
    private int pageIndex = 0;
    private final List<Page> pages = new ArrayList<>();
    private Button prev, next;

    public GuideBookScreen() {
        super(Component.literal("Guide Book"));
    }

    @Override
    protected void init() {
        super.init();
        this.bx = (this.width - BOOK_W) / 2;
        this.by = (this.height - BOOK_H) / 2;

        if (pages.isEmpty()) buildPages();

        prev = Button.builder(Component.literal("◀"),
            b -> { if (pageIndex > 0) pageIndex--; updateButtons(); })
            .bounds(bx + 10, by + BOOK_H - 28, 30, 20).build();
        next = Button.builder(Component.literal("▶"),
            b -> { if (pageIndex < pages.size() - 1) pageIndex++; updateButtons(); })
            .bounds(bx + BOOK_W - 40, by + BOOK_H - 28, 30, 20).build();
        addRenderableWidget(prev);
        addRenderableWidget(next);
        addRenderableWidget(Button.builder(Component.literal("閉じる"),
            b -> onClose())
            .bounds(bx + (BOOK_W - 60) / 2, by + BOOK_H - 28, 60, 20).build());
        updateButtons();
    }

    private void updateButtons() {
        prev.active = pageIndex > 0;
        next.active = pageIndex < pages.size() - 1;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderBackground(g);
        g.fill(bx, by, bx + BOOK_W, by + BOOK_H, 0xF0181818);
        g.fill(bx, by, bx + BOOK_W, by + 1, 0xFFFFD700);
        g.fill(bx, by + BOOK_H - 1, bx + BOOK_W, by + BOOK_H, 0xFFFFD700);
        g.fill(bx, by, bx + 1, by + BOOK_H, 0xFFFFD700);
        g.fill(bx + BOOK_W - 1, by, bx + BOOK_W, by + BOOK_H, 0xFFFFD700);

        Page p = pages.get(pageIndex);
        Minecraft mc = Minecraft.getInstance();

        // タイトル (翻訳キー解決して現在ロケールで描画)
        String titleText = tr(p.titleKey);
        g.drawString(mc.font, titleText, bx + 16, by + 10, 0xFFFFD700, false);
        // ページ番号
        String pg = (pageIndex + 1) + "/" + pages.size();
        g.drawString(mc.font, pg, bx + BOOK_W - mc.font.width(pg) - 12, by + 10, 0xFF808080, false);
        // 区切り線
        g.fill(bx + 10, by + 24, bx + BOOK_W - 10, by + 25, 0xFF605020);

        // 本文 (左側 170px 幅で word-wrap)。\n で段落区切り。
        int tx = bx + 14;
        int ty = by + 32;
        int textW = 170;
        int line = 0;
        String body = tr(p.bodyKey);
        for (String paragraph : body.split("\n")) {
            var wrapped = mc.font.getSplitter().splitLines(paragraph, textW, net.minecraft.network.chat.Style.EMPTY);
            for (var s : wrapped) {
                g.drawString(mc.font, s.getString(), tx, ty + line * 10, 0xFFE8E8E8, false);
                line++;
            }
        }

        // 右側イラスト領域
        int ix = bx + BOOK_W - 100;
        int iy = by + 40;
        if (p.illustration != null) p.illustration.run(g, ix, iy);

        super.render(g, mouseX, mouseY, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // --- Pages ---------------------------------------------------------

    private static final String KEY_PREFIX = "guidebook.minecraft_armor_weapon.";

    /** 翻訳キーから現在ロケールのテキストを取得。表示直前に呼ぶのでロケール切替にも追従する。 */
    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private void buildPages() {
        pages.add(new Page(KEY_PREFIX + "welcome.title", KEY_PREFIX + "welcome.body",
            (g, x, y) -> drawItemIcon(g, new ItemStack(CustomEntityInit.KNIFE_LAUNCHER.get()), x, y, 4.0f)
        ));
        pages.add(new Page(KEY_PREFIX + "launcher.title", KEY_PREFIX + "launcher.body",
            (g, x, y) -> drawItemIcon(g, new ItemStack(CustomEntityInit.KNIFE_LAUNCHER.get()), x, y, 4.0f)
        ));
        pages.add(new Page(KEY_PREFIX + "normal_knife.title", KEY_PREFIX + "normal_knife.body",
            (g, x, y) -> drawItemIcon(g, new ItemStack(CustomEntityInit.THROWING_KNIFE.get()), x, y, 4.0f)
        ));
        pages.add(new Page(KEY_PREFIX + "stun_knife.title", KEY_PREFIX + "stun_knife.body",
            (g, x, y) -> drawItemIcon(g, new ItemStack(CustomEntityInit.STUN_KNIFE.get()), x, y, 4.0f)
        ));
        pages.add(new Page(KEY_PREFIX + "screw_knife.title", KEY_PREFIX + "screw_knife.body",
            (g, x, y) -> drawItemIcon(g, new ItemStack(CustomEntityInit.SCREW_KNIFE.get()), x, y, 4.0f)
        ));
        pages.add(new Page(KEY_PREFIX + "mana.title", KEY_PREFIX + "mana.body", null));
        pages.add(new Page(KEY_PREFIX + "spellbooks_compat.title", KEY_PREFIX + "spellbooks_compat.body", null));
        pages.add(new Page(KEY_PREFIX + "katana_saya.title", KEY_PREFIX + "katana_saya.body",
            (g, x, y) -> drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.SAYA.get()), x, y, 4.0f)
        ));
        pages.add(new Page(KEY_PREFIX + "parry_shield.title", KEY_PREFIX + "parry_shield.body",
            (g, x, y) -> drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.NIGU_SHIELD.get()), x, y, 4.0f)
        ));
        pages.add(new Page(KEY_PREFIX + "test_bow.title", KEY_PREFIX + "test_bow.body",
            (g, x, y) -> drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.TEST_BOW.get()), x, y, 4.0f)
        ));
        pages.add(new Page(KEY_PREFIX + "magic_books.title", KEY_PREFIX + "magic_books.body",
            (g, x, y) -> {
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.FIREBALL.get()),    x,      y,      2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.THUNDERBOLT.get()), x + 40, y,      2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.STORM.get()),       x,      y + 40, 2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.WIND_STEP.get()),   x + 40, y + 40, 2.8f);
            }
        ));
        pages.add(new Page(KEY_PREFIX + "dragon_armor.title", KEY_PREFIX + "dragon_armor.body",
            (g, x, y) -> {
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.DRAGON_ARMOR_HELMET.get()),     x,      y,      2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.DRAGON_ARMOR_CHESTPLATE.get()), x + 40, y,      2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.DRAGON_ARMOR_LEGGINGS.get()),   x,      y + 40, 2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.DRAGON_ARMOR_BOOTS.get()),      x + 40, y + 40, 2.8f);
            }
        ));
        pages.add(new Page(KEY_PREFIX + "common_soldier.title", KEY_PREFIX + "common_soldier.body",
            (g, x, y) -> drawEntity(g, () -> new CommonSoldierEntity(
                CustomEntityInit.COMMON_SOLDIER.get(), Minecraft.getInstance().level), x, y, 36)
        ));
        pages.add(new Page(KEY_PREFIX + "elite_soldier.title", KEY_PREFIX + "elite_soldier.body",
            (g, x, y) -> drawEntity(g, () -> new EliteSoldierEntity(
                CustomEntityInit.ELITE_SOLDIER.get(), Minecraft.getInstance().level), x, y, 36)
        ));
        pages.add(new Page(KEY_PREFIX + "singularity.title", KEY_PREFIX + "singularity.body",
            (g, x, y) -> drawEntity(g, () -> new SingularityEntity(
                CustomEntityInit.SINGULARITY.get(), Minecraft.getInstance().level), x, y, 28)
        ));
        pages.add(new Page(KEY_PREFIX + "heroic_tier.title", KEY_PREFIX + "heroic_tier.body",
            (g, x, y) -> drawEntity(g, () -> new HeroicTierEntity(
                CustomEntityInit.HEROIC_TIER.get(), Minecraft.getInstance().level), x, y, 24)
        ));
        pages.add(new Page(KEY_PREFIX + "angel_trio.title", KEY_PREFIX + "angel_trio.body",
            (g, x, y) -> drawEntity(g, () -> new AngelTrioEntity(
                CustomEntityInit.ANGEL_SERIOUS.get(), Minecraft.getInstance().level), x, y, 28)
        ));
        pages.add(new Page(KEY_PREFIX + "blackhole.title", KEY_PREFIX + "blackhole.body",
            (g, x, y) -> drawEntity(g, () -> new BlackholeEntity(
                MinecraftArmorWeaponModEntities.BLACKHOLE.get(), Minecraft.getInstance().level), x, y, 20)
        ));
        // === 敵強化システム ===
        pages.add(new Page(KEY_PREFIX + "mob_traits.title", KEY_PREFIX + "mob_traits.body", null));
        pages.add(new Page(KEY_PREFIX + "distance_scaling.title", KEY_PREFIX + "distance_scaling.body", null));
        // === 武器カテゴリ ===
        pages.add(new Page(KEY_PREFIX + "katanas.title", KEY_PREFIX + "katanas.body",
            (g, x, y) -> {
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.IRON_KATANA.get()),     x,      y,      2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.GOLD_KATANA.get()),     x + 40, y,      2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.NETHERITE_KATANA.get()),x,      y + 40, 2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.MAGICAL_KATANA.get()),  x + 40, y + 40, 2.8f);
            }
        ));
        pages.add(new Page(KEY_PREFIX + "swords.title", KEY_PREFIX + "swords.body",
            (g, x, y) -> {
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get()),  x,      y,      2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.RIVERS_OF_BLOOD.get()), x + 40, y,      2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.PROTOTYPE_KATANA.get()),x,      y + 40, 2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.KURIKARAKEN.get()),     x + 40, y + 40, 2.8f);
            }
        ));
        pages.add(new Page(KEY_PREFIX + "misc_weapons.title", KEY_PREFIX + "misc_weapons.body",
            (g, x, y) -> {
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.HAMMER.get()), x,      y,      2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.SCYTHE.get()), x + 40, y,      2.8f);
                drawItemIcon(g, new ItemStack(MinecraftArmorWeaponModItems.RAPIER.get()), x,      y + 40, 2.8f);
            }
        ));
    }

    private static void drawItemIcon(GuiGraphics g, ItemStack stack, int x, int y, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    private static void drawEntity(GuiGraphics g, Supplier<LivingEntity> factory, int x, int y, int size) {
        try {
            LivingEntity e = factory.get();
            if (e == null) return;
            // 1.20.1 Forge: renderEntityInInventoryFollowsMouse(GuiGraphics, x, y, scale, mouseX, mouseY, LivingEntity)
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 35, y + 60, size, 0f, 0f, e);
        } catch (Throwable ignored) {
            // mob 生成に失敗 (level=null 等) したら描画スキップ
        }
    }

    /** レンダラ関数のインタフェース (GuiGraphics + 位置で描画) */
    @FunctionalInterface
    private interface Illustration {
        void run(GuiGraphics g, int x, int y);
    }

    private static class Page {
        final String titleKey;
        final String bodyKey;
        final Illustration illustration;
        Page(String tk, String bk, Illustration i) { titleKey = tk; bodyKey = bk; illustration = i; }
    }
}
