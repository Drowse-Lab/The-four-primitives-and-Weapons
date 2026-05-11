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

        if (pages.isEmpty()) {
            buildPages();
            paginateOverflowingPages();
        }

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

        // ===== 本ぽい外観: 革表紙 + 中央綴じ目 + 羊皮紙の本文ページ =====
        // 革色の外枠 (背表紙)
        int leather = 0xFF5C3A1A;
        int leatherDark = 0xFF3D2511;
        int leatherLight = 0xFF8B5A2A;
        int parchment = 0xFFE8D9A8;       // 羊皮紙色
        int parchmentShade = 0xFFCFB87A;  // 影
        int ink = 0xFF2A1B0A;             // インク文字色
        int gold = 0xFFC9A24B;

        // 外側の革背景
        g.fill(bx - 4, by - 4, bx + BOOK_W + 4, by + BOOK_H + 4, leatherDark);
        g.fill(bx - 3, by - 3, bx + BOOK_W + 3, by + BOOK_H + 3, leather);
        // 革のハイライト (上)
        g.fill(bx - 3, by - 3, bx + BOOK_W + 3, by - 2, leatherLight);
        g.fill(bx - 3, by - 3, bx - 2, by + BOOK_H + 3, leatherLight);

        // 羊皮紙の本文ページ (内側)
        g.fill(bx, by, bx + BOOK_W, by + BOOK_H, parchment);
        // ページの影 (上端)
        g.fill(bx, by, bx + BOOK_W, by + 1, parchmentShade);
        // 金枠 (装飾)
        g.fill(bx, by, bx + BOOK_W, by + 1, gold);
        g.fill(bx, by + BOOK_H - 1, bx + BOOK_W, by + BOOK_H, gold);
        g.fill(bx, by, bx + 1, by + BOOK_H, gold);
        g.fill(bx + BOOK_W - 1, by, bx + BOOK_W, by + BOOK_H, gold);

        // 中央綴じ目 (本の真ん中)
        int spine = bx + BOOK_W / 2;
        g.fill(spine - 1, by + 4, spine + 1, by + BOOK_H - 4, leatherDark);

        Page p = pages.get(pageIndex);
        Minecraft mc = Minecraft.getInstance();

        // タイトル (黒インク) - 分割ページなら "(2/3)" 等を付与
        String titleText = tr(p.titleKey);
        if (p.partNumber != null && p.partTotal != null && p.partTotal > 1) {
            titleText = titleText + " (" + p.partNumber + "/" + p.partTotal + ")";
        }
        g.drawString(mc.font, titleText, bx + 16, by + 8, ink, false);
        String pg = (pageIndex + 1) + "/" + pages.size();
        g.drawString(mc.font, pg, bx + BOOK_W - mc.font.width(pg) - 12, by + 8, 0xFF5C4A2A, false);
        // 装飾下線
        g.fill(bx + 10, by + 22, bx + BOOK_W - 10, by + 23, gold);

        // 本文は左ページに収める (中央 spine とイラスト用の右ページを侵さない)
        int tx = bx + 14;
        int ty = by + 32;
        int spineX = bx + BOOK_W / 2;
        // 左ページ右端マージン: spine から 6px 離す
        int textW = spineX - tx - 6;
        // ボタン領域 (by + BOOK_H - 28) と被らないように、本文の最大行数を制限
        int textBottom = by + BOOK_H - 34;  // ボタン上端より少し上
        int maxLines = Math.max(1, (textBottom - ty) / 10);
        int line = 0;
        // 分割ページなら bodyOverride を優先 (既にラップ済み)、それ以外は bodyKey から取得して再ラップ
        String body = (p.bodyOverride != null) ? p.bodyOverride : tr(p.bodyKey);
        outer:
        for (String paragraph : body.split("\n")) {
            // bodyOverride はもう適切な幅でラップ済みなので分割不要だが、bodyKey から来た場合は再ラップ
            java.util.List<? extends net.minecraft.util.FormattedCharSequence> wrapped =
                (p.bodyOverride != null)
                    ? java.util.List.of(net.minecraft.util.FormattedCharSequence.forward(paragraph, net.minecraft.network.chat.Style.EMPTY))
                    : mc.font.getSplitter().splitLines(paragraph, textW, net.minecraft.network.chat.Style.EMPTY)
                        .stream().map(t -> net.minecraft.util.FormattedCharSequence.forward(t.getString(), net.minecraft.network.chat.Style.EMPTY))
                        .toList();
            for (var s : wrapped) {
                if (line >= maxLines) break outer;
                g.drawString(mc.font, s, tx, ty + line * 10, ink, false);
                line++;
            }
        }

        // イラストは右ページに配置
        int ix = spineX + 14;
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

    /**
     * 各 Page の本文を計測し、ボタンと被るほど長い場合は複数 Page に分割する。
     * 分割後の Page は bodyOverride に断片を保持し、(part/total) を表示する。
     */
    private void paginateOverflowingPages() {
        Minecraft mc = Minecraft.getInstance();
        int spineX = BOOK_W / 2;
        int textW = spineX - 14 - 6;
        // textBottom = BOOK_H - 34 (ボタンより少し上), ty = 32, 行高 10
        int maxLines = Math.max(1, (BOOK_H - 34 - 32) / 10);

        java.util.List<Page> result = new java.util.ArrayList<>();
        for (Page p : pages) {
            String body = tr(p.bodyKey);
            // 本文を全行にラップ
            java.util.List<String> allLines = new java.util.ArrayList<>();
            for (String para : body.split("\n")) {
                if (para.isEmpty()) { allLines.add(""); continue; }
                for (var s : mc.font.getSplitter().splitLines(para, textW, net.minecraft.network.chat.Style.EMPTY)) {
                    allLines.add(s.getString());
                }
            }
            if (allLines.size() <= maxLines) {
                result.add(p);
                continue;
            }
            // 分割
            int total = (allLines.size() + maxLines - 1) / maxLines;
            for (int part = 0; part < total; part++) {
                int from = part * maxLines;
                int to = Math.min(from + maxLines, allLines.size());
                StringBuilder sb = new StringBuilder();
                for (int j = from; j < to; j++) {
                    if (j > from) sb.append('\n');
                    sb.append(allLines.get(j));
                }
                Page np = new Page(p.titleKey, p.bodyKey,
                    part == 0 ? p.illustration : null);
                np.bodyOverride = sb.toString();
                np.partNumber = part + 1;
                np.partTotal = total;
                result.add(np);
            }
        }
        pages.clear();
        pages.addAll(result);
    }

    @FunctionalInterface
    private interface Illustration {
        void run(GuiGraphics g, int x, int y);
    }

    private static class Page {
        final String titleKey;
        final String bodyKey;
        final Illustration illustration;
        /** 非 null なら bodyKey の代わりに使用 (auto-paginate で分割した本文を保持)。 */
        String bodyOverride;
        /** 同タイトルの何ページ目か (1-based)。null なら表示しない。 */
        Integer partNumber;
        Integer partTotal;
        Page(String tk, String bk, Illustration i) { titleKey = tk; bodyKey = bk; illustration = i; }
    }
}
