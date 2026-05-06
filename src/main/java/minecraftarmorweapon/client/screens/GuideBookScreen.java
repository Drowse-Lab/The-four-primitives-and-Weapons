package minecraftarmorweapon.client.screens;

import minecraftarmorweapon.init.CustomEntityInit;
import minecraftarmorweapon.init.KnifeExtrasRegistrar;
import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * モッド内容を画像付きで紹介するガイドブック画面。
 *
 * 構成:
 *   page 1 — このmodについて
 *   page 2 — スキルについて
 *   page 3 — スキルの変更方法
 *   page 4+ — 武器を ABC 順 (item id 昇順) に各 2 ページずつ
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

        String titleText = tr(p.titleKey);
        g.drawString(mc.font, titleText, bx + 16, by + 10, 0xFFFFD700, false);
        String pg = (pageIndex + 1) + "/" + pages.size();
        g.drawString(mc.font, pg, bx + BOOK_W - mc.font.width(pg) - 12, by + 10, 0xFF808080, false);
        g.fill(bx + 10, by + 24, bx + BOOK_W - 10, by + 25, 0xFF605020);

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

        int ix = bx + BOOK_W - 100;
        int iy = by + 40;
        if (p.illustration != null) p.illustration.run(g, ix, iy);

        super.render(g, mouseX, mouseY, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static final String K = "guidebook.minecraft_armor_weapon.";

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }

    private void buildPages() {
        // === Intro 3 ページ ===
        pages.add(new Page(K + "about_mod.title", K + "about_mod.body",
            (g, x, y) -> drawItemIcon(g, new ItemStack(CustomEntityInit.GUIDE_BOOK.get()), x, y, 4.0f)));
        pages.add(new Page(K + "about_skills.title", K + "about_skills.body", null));
        pages.add(new Page(K + "skill_change.title", K + "skill_change.body", null));

        // === 武器ページ — ABC 順. 各武器 2 ページ. ===
        for (WeaponEntry w : WEAPONS) {
            pages.add(new Page(K + w.id + ".title", K + w.id + ".body",
                (g, x, y) -> drawItemIcon(g, w.icon.get(), x, y, 4.0f)));
            pages.add(new Page(K + w.id + ".title2", K + w.id + ".body2",
                (g, x, y) -> drawItemIcon(g, w.icon.get(), x, y, 4.0f)));
        }

        // === 弓スキルページ — 各スキル 1 ページ ===
        for (BowSkillEntry s : BOW_SKILLS) {
            pages.add(new Page(K + s.id + ".title", K + s.id + ".body",
                (g, x, y) -> drawItemIcon(g, s.icon.get(), x, y, 4.0f)));
        }
    }

    private record BowSkillEntry(String id, Supplier<ItemStack> icon) {}

    /** 弓/クロスボウのスキル説明ページ. アイコンはホイールと同じ. */
    private static final List<BowSkillEntry> BOW_SKILLS = List.of(
        new BowSkillEntry("bow_power_shot", () -> new ItemStack(Items.SPECTRAL_ARROW)),
        new BowSkillEntry("bow_explosive",  () -> new ItemStack(Items.TNT)),
        new BowSkillEntry("bow_pierce",     () -> new ItemStack(Items.ARROW)),
        new BowSkillEntry("bow_rapid_fire", () -> new ItemStack(Items.FEATHER)),
        new BowSkillEntry("bow_homing",     () -> new ItemStack(Items.COMPASS)),
        new BowSkillEntry("bow_arrow_rain", () -> new ItemStack(Items.WATER_BUCKET)),
        new BowSkillEntry("bow_quick_draw", () -> new ItemStack(Items.SUGAR)),
        new BowSkillEntry("bow_heavy_blow", () -> new ItemStack(Items.IRON_INGOT)),
        new BowSkillEntry("bow_wind",       () -> new ItemStack(Items.PHANTOM_MEMBRANE))
    );

    /** 武器 1 つを (item id, アイコン Supplier) で表す. ABC 順に並べる. */
    private record WeaponEntry(String id, Supplier<ItemStack> icon) {}

    /**
     * ABC 順 (item id 昇順) の武器リスト.
     * 新しい武器を追加するときはここに 1 行追加 + lang ファイルに 4 エントリ
     * (`<id>.title`, `<id>.body`, `<id>.title2`, `<id>.body2`) を追加.
     */
    private static final List<WeaponEntry> WEAPONS = List.of(
        new WeaponEntry("achromatic_shield",     () -> new ItemStack(MinecraftArmorWeaponModItems.ACHROMATIC_SHIELD.get())),
        new WeaponEntry("anti_gravity_bracelet", () -> new ItemStack(KnifeExtrasRegistrar.ANTI_GRAVITY_BRACELET.get())),
        new WeaponEntry("bluepurge",             () -> new ItemStack(MinecraftArmorWeaponModItems.BLUEPURGE.get())),
        new WeaponEntry("bow",                   () -> new ItemStack(Items.BOW)),
        new WeaponEntry("crossbow",              () -> new ItemStack(Items.CROSSBOW)),
        new WeaponEntry("convergent_gate",       () -> new ItemStack(MinecraftArmorWeaponModItems.CONVERGENT_GATE.get())),
        new WeaponEntry("darkness_katana",       () -> new ItemStack(MinecraftArmorWeaponModItems.DARKNESS_KATANA.get())),
        new WeaponEntry("explosive_throwing_knife", () -> new ItemStack(KnifeExtrasRegistrar.EXPLOSIVE_THROWING_KNIFE.get())),
        new WeaponEntry("gate",                  () -> new ItemStack(MinecraftArmorWeaponModItems.GATE.get())),
        new WeaponEntry("gold_katana",           () -> new ItemStack(MinecraftArmorWeaponModItems.GOLD_KATANA.get())),
        new WeaponEntry("gold_tyokuto",          () -> new ItemStack(MinecraftArmorWeaponModItems.GOLD_TYOKUTO.get())),
        new WeaponEntry("hammer",                () -> new ItemStack(MinecraftArmorWeaponModItems.HAMMER.get())),
        new WeaponEntry("immortal_core",         () -> new ItemStack(MinecraftArmorWeaponModItems.IMMORTAL_CORE.get())),
        new WeaponEntry("iron_katana",           () -> new ItemStack(MinecraftArmorWeaponModItems.IRON_KATANA.get())),
        new WeaponEntry("iron_tyokuto",          () -> new ItemStack(MinecraftArmorWeaponModItems.IRON_TYOKUTO.get())),
        new WeaponEntry("katana_tobu",           () -> new ItemStack(MinecraftArmorWeaponModItems.KATANA_TOBU.get())),
        new WeaponEntry("knife_launcher",        () -> new ItemStack(CustomEntityInit.KNIFE_LAUNCHER.get())),
        new WeaponEntry("kurikaraken",           () -> new ItemStack(MinecraftArmorWeaponModItems.KURIKARAKEN.get())),
        new WeaponEntry("loki_the_trickster",    () -> new ItemStack(MinecraftArmorWeaponModItems.LOKI_THE_TRICKSTER.get())),
        new WeaponEntry("luna",                  () -> new ItemStack(MinecraftArmorWeaponModItems.LUNA.get())),
        new WeaponEntry("machete",               () -> new ItemStack(MinecraftArmorWeaponModItems.MACHETE.get())),
        new WeaponEntry("magical_katana",        () -> new ItemStack(MinecraftArmorWeaponModItems.MAGICAL_KATANA.get())),
        new WeaponEntry("magisches_feen_katana", () -> new ItemStack(MinecraftArmorWeaponModItems.MAGISCHES_FEEN_KATANA.get())),
        new WeaponEntry("mana_potion",           () -> new ItemStack(CustomEntityInit.MANA_POTION.get())),
        new WeaponEntry("netherite_katana",      () -> new ItemStack(MinecraftArmorWeaponModItems.NETHERITE_KATANA.get())),
        new WeaponEntry("nigu_shield",           () -> new ItemStack(MinecraftArmorWeaponModItems.NIGU_SHIELD.get())),
        new WeaponEntry("ninjatou",              () -> new ItemStack(MinecraftArmorWeaponModItems.NINJATOU.get())),
        new WeaponEntry("ofuda",                 () -> new ItemStack(MinecraftArmorWeaponModItems.OFUDA.get())),
        new WeaponEntry("recross_hookshot_long", () -> new ItemStack(CustomEntityInit.RECROSS_HOOKSHOT_LONG.get())),
        new WeaponEntry("recross_hookshot_short",() -> new ItemStack(CustomEntityInit.RECROSS_HOOKSHOT_SHORT.get())),
        new WeaponEntry("replica_sword_of_light",() -> new ItemStack(MinecraftArmorWeaponModItems.REPLICA_SWORD_OF_LIGHT.get())),
        new WeaponEntry("rivers_of_blood",       () -> new ItemStack(MinecraftArmorWeaponModItems.RIVERS_OF_BLOOD.get())),
        new WeaponEntry("scythe",                () -> new ItemStack(MinecraftArmorWeaponModItems.SCYTHE.get())),
        new WeaponEntry("small_sword",           () -> new ItemStack(MinecraftArmorWeaponModItems.SMALL_SWORD.get())),
        new WeaponEntry("stone_katana",          () -> new ItemStack(MinecraftArmorWeaponModItems.STONE_KATANA.get())),
        new WeaponEntry("sword_of_night",        () -> new ItemStack(MinecraftArmorWeaponModItems.SWORD_OF_NIGHT.get())),
        new WeaponEntry("throwing_knife",        () -> new ItemStack(CustomEntityInit.THROWING_KNIFE.get())),
        new WeaponEntry("undead_army_banish",    () -> new ItemStack(CustomEntityInit.UNDEAD_ARMY_BANISH.get())),
        new WeaponEntry("warabitetou",           () -> new ItemStack(MinecraftArmorWeaponModItems.WARABITETOU.get())),
        new WeaponEntry("wither_katana",         () -> new ItemStack(MinecraftArmorWeaponModItems.WITHER_KATANA.get()))
    );

    private static void drawItemIcon(GuiGraphics g, ItemStack stack, int x, int y, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

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
