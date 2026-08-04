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
 * フォールバック: 各 context の第3引数で THIRD_PERSON_RIGHT_HAND を指定。JSON に
 * カスタムキー定義が無いモデルは thirdperson_righthand の transform を使う。
 * （ScabbardCurioRenderer のハードコード値はこの context を前提に調整済み）
 */
public class MawDisplayContexts {

    // フォールバック: JSON に custom key 定義が無いモデルは THIRD_PERSON_RIGHT_HAND の
    // transform を使う。ScabbardCurioRenderer のハードコード値はこの context を前提に
    // 調整されているため、FIXED にすると向きがずれる。
    public static final ItemDisplayContext SAYA_BACK = ItemDisplayContext.create(
        "SAYA_BACK",
        new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "back"),
        ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
    );

    public static final ItemDisplayContext SAYA_BELT = ItemDisplayContext.create(
        "SAYA_BELT",
        new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "belt"),
        ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
    );

    /**
     * ナイフホルダー ( knife_launcher ) を腰に差している見た目
     * ( {@link the_four_primitives_and_weapons.client.renderer.KnifeHolsterLayer} )。
     *
     * <p>item JSON の "display" に "the_four_primitives_and_weapons:holster" を書けば
     * その rotation/translation/scale が使われる。 saya の :back / :belt と同じく
     * Blockbench の worn display プラグインから視覚編集できる。</p>
     *
     * <p>フォールバックは FIXED。 このキーを持たないモデルは従来どおり
     * FIXED の transform で描かれる ( レイヤー側の既定オフセットも FIXED 前提 )。</p>
     */
    public static final ItemDisplayContext HOLSTER = ItemDisplayContext.create(
        "MAW_HOLSTER",
        new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "holster"),
        ItemDisplayContext.FIXED
    );

    /** 強制的にこのクラスを class load して static フィールドを初期化させる。
     *  MAW の client setup から 1 度呼ぶ。 */
    public static void init() {
        // SAYA_BACK / SAYA_BELT / HOLSTER に touch するだけで static initializer が走る
        if (SAYA_BACK == null || SAYA_BELT == null || HOLSTER == null) {
            throw new IllegalStateException("MawDisplayContexts init failed");
        }
    }
}
