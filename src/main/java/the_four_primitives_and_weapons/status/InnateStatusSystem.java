package the_four_primitives_and_weapons.status;

import the_four_primitives_and_weapons.damage.ElementType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * 先天的ステータスシステム
 *
 * スポーン時にランダムで以下のどちらかが決まる:
 *   - 属性耐性（特定属性のダメージを軽減）
 *   - 属性得意（特定属性でのダメージを増幅）
 *
 * NBTタグで保存・復元する（エンティティが再ロードされても保持）。
 *
 * NBT構造:
 *   InnateType: "resistance" | "affinity"
 *   InnateElement: 属性名 (例: "ice", "fire" ...)
 *   InnateValue: float (耐性なら軽減率 0.0〜1.0、得意なら倍率ボーナス)
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class InnateStatusSystem {

    private static final String NBT_TYPE    = "InnateType";
    private static final String NBT_ELEMENT = "InnateElement";
    private static final String NBT_VALUE   = "InnateValue";

    // 耐性の軽減率（例: 0.3 = 30%軽減）
    private static final float RESISTANCE_RATE = 0.30f;
    // 得意の追加倍率（例: 0.3 = +30%ダメージ）
    private static final float AFFINITY_BONUS  = 0.30f;

    // 先天的ステータスを付与する対象から除外する属性
    private static final ElementType[] EXCLUDED = {
            ElementType.NONE, ElementType.ERASURE
    };

    public enum InnateType {
        RESISTANCE, // 耐性（軽減）
        AFFINITY    // 得意（強化）
    }

    // ────────────────────────────────────────────────────────────────
    // スポーン時に先天的ステータスをランダム付与
    // ────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        // プレイヤーには付与しない
        if (entity instanceof net.minecraft.world.entity.player.Player) return;

        CompoundTag tag = entity.getPersistentData();
        // 既にセット済みならスキップ（リロード時の再付与防止）
        if (tag.contains(NBT_TYPE)) return;

        assignRandom(entity);
    }

    // ────────────────────────────────────────────────────────────────
    // 公開API
    // ────────────────────────────────────────────────────────────────

    /**
     * ランダムな先天的ステータスを付与する。
     */
    public static void assignRandom(LivingEntity entity) {
        Random rng         = new Random();
        ElementType element = randomElement(rng);
        InnateType  type    = rng.nextBoolean() ? InnateType.RESISTANCE : InnateType.AFFINITY;
        float       value   = type == InnateType.RESISTANCE ? RESISTANCE_RATE : AFFINITY_BONUS;

        CompoundTag tag = entity.getPersistentData();
        tag.putString(NBT_TYPE,    type.name());
        tag.putString(NBT_ELEMENT, element.getName());
        tag.putFloat (NBT_VALUE,   value);
    }

    /** 先天的ステータスを持っているか */
    public static boolean hasInnate(LivingEntity entity) {
        return entity.getPersistentData().contains(NBT_TYPE);
    }

    /** 先天的ステータスのタイプを返す */
    public static InnateType getInnateType(LivingEntity entity) {
        String raw = entity.getPersistentData().getString(NBT_TYPE);
        try { return InnateType.valueOf(raw); }
        catch (Exception e) { return InnateType.RESISTANCE; }
    }

    /** 先天的ステータスの属性を返す */
    public static ElementType getInnateElement(LivingEntity entity) {
        String raw = entity.getPersistentData().getString(NBT_ELEMENT);
        return ElementType.fromString(raw);
    }

    /** 先天的ステータスの値を返す（耐性率 or 得意ボーナス） */
    public static float getInnateValue(LivingEntity entity) {
        return entity.getPersistentData().getFloat(NBT_VALUE);
    }

    /**
     * 属性ダメージに先天的ステータスを適用して返す。
     * ElementalDamageUtils.applyElementalDamage() の戻り値に掛ける。
     *
     * @param target   攻撃対象
     * @param element  攻撃属性
     * @param damage   属性ダメージ計算後の値
     * @return 先天的補正後のダメージ
     */
    public static float applyInnate(LivingEntity target, ElementType element, float damage) {
        if (!hasInnate(target)) return damage;
        if (getInnateElement(target) != element) return damage;

        float value = getInnateValue(target);

        return switch (getInnateType(target)) {
            case RESISTANCE -> damage * (1.0f - value); // 軽減
            case AFFINITY   -> damage * (1.0f + value); // 増幅（弱点）
        };
    }

    // ────────────────────────────────────────────────────────────────

    private static ElementType randomElement(Random rng) {
        ElementType[] all = ElementType.values();
        ElementType picked;
        do {
            picked = all[rng.nextInt(all.length)];
        } while (isExcluded(picked));
        return picked;
    }

    private static boolean isExcluded(ElementType type) {
        for (ElementType ex : EXCLUDED) {
            if (ex == type) return true;
        }
        return false;
    }
}
