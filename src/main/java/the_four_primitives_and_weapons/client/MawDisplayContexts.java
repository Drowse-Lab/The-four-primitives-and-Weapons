package the_four_primitives_and_weapons.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

/**
 * MAW 専用カスタム ItemDisplayContext 登録。
 *
 * - SAYA_BACK: Curios "back" スロットに saya を装備した時の表示
 * - SAYA_BELT: Curios "belt" スロットに saya を装備した時の表示
 *
 * これらは saya item JSON の "display" セクションに
 * "the_four_primitives_and_weapons:back" / "...:belt" キーで JSON 値が
 * 書かれていれば、その rotation/translation/scale がレンダリングに使われる。
 *
 * Blockbench plugin (sb_worn_display.js v3.0+) でこれらの値を視覚編集可能。
 *
 * フォールバック: 各 context の第3引数で FIXED context を指定。JSON にカスタム
 * キー定義が無いモデルは fixed context の transform を使う (= ほぼ原寸表示)。
 */
public class MawDisplayContexts {

    public static final ItemDisplayContext SAYA_BACK = ItemDisplayContext.create(
        "SAYA_BACK",
        new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "back"),
        ItemDisplayContext.FIXED
    );

    public static final ItemDisplayContext SAYA_BELT = ItemDisplayContext.create(
        "SAYA_BELT",
        new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "belt"),
        ItemDisplayContext.FIXED
    );

    /** 強制的にこのクラスを class load して static フィールドを初期化させる。
     *  MAW の client setup から 1 度呼ぶ。 */
    public static void init() {
        // SAYA_BACK / SAYA_BELT に touch するだけで static initializer が走る
        if (SAYA_BACK == null || SAYA_BELT == null) {
            throw new IllegalStateException("MawDisplayContexts init failed");
        }
    }
}
