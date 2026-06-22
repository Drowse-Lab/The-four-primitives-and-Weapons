package the_four_primitives_and_weapons.item.rarity;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * シンプル化レアリティ解放テーブル向けの JEI 表示用レシピデータ。
 *
 * 3 つのモードを表現する:
 *   - BOOK_ELEMENT : 中央 = 魔導書 + 触媒 = level table 素材
 *   - UNBREAKABLE  : 中央 = SwordItem + 触媒 = 特定組み合わせ
 *   - RARITY       : 中央 = 武器 + 触媒 = 任意 ( 通常素材 )
 *
 * 各スロット ( center / cat0 / cat1 / output ) は表示候補 list を持つ。
 * 表示時は JEI 側がローテーションで切り替える。
 */
public class RarityForgeNewRecipe {

    public enum Kind { BOOK_ELEMENT, UNBREAKABLE, RARITY }

    private final Kind kind;
    private final List<ItemStack> centerCandidates;
    private final List<ItemStack> cat0Candidates;
    private final List<ItemStack> cat1Candidates;
    private final List<ItemStack> outputCandidates;
    private final String description;

    public RarityForgeNewRecipe(Kind kind,
                                List<ItemStack> centerCandidates,
                                List<ItemStack> cat0Candidates,
                                List<ItemStack> cat1Candidates,
                                List<ItemStack> outputCandidates,
                                String description) {
        this.kind = kind;
        this.centerCandidates = centerCandidates == null ? List.of() : new ArrayList<>(centerCandidates);
        this.cat0Candidates   = cat0Candidates   == null ? List.of() : new ArrayList<>(cat0Candidates);
        this.cat1Candidates   = cat1Candidates   == null ? List.of() : new ArrayList<>(cat1Candidates);
        this.outputCandidates = outputCandidates == null ? List.of() : new ArrayList<>(outputCandidates);
        this.description = description == null ? "" : description;
    }

    public Kind getKind() { return kind; }
    public List<ItemStack> getCenterCandidates() { return Collections.unmodifiableList(centerCandidates); }
    public List<ItemStack> getCat0Candidates() { return Collections.unmodifiableList(cat0Candidates); }
    public List<ItemStack> getCat1Candidates() { return Collections.unmodifiableList(cat1Candidates); }
    public List<ItemStack> getOutputCandidates() { return Collections.unmodifiableList(outputCandidates); }
    public String getDescription() { return description; }
}
