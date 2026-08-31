package the_four_primitives_and_weapons.damage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 属性ダメージ用のユーティリティクラス
 * NBTタグから属性情報を読み書きする
 */
public class ElementalDamageUtils {

    // NBTタグのキー
    private static final String ELEMENT_TYPE_KEY  = "ElementType";
    private static final String ELEMENT_LEVEL_KEY = "ElementLevel";
    private static final String ELEMENT_TYPE_2_KEY  = "ElementType2";
    private static final String ELEMENT_LEVEL_2_KEY = "ElementLevel2";
    private static final String ELEMENT_KIND_KEY    = "ElementDamageKind";

    /**
     * アイテムに属性を設定
     * @param stack       アイテムスタック
     * @param elementType 属性タイプ
     * @param level       属性レベル
     */
    public static void setElement(ItemStack stack, ElementType elementType, int level) {
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(ELEMENT_TYPE_KEY, elementType.getName());
        tag.putInt(ELEMENT_LEVEL_KEY, level);
        tag.remove(ELEMENT_TYPE_2_KEY);
        tag.remove(ELEMENT_LEVEL_2_KEY);
        // Magical Katana 特殊技解放 — CORROSION Lv>=12 セット時に自動 unlock
        // ( MagicalKatanaCrystalHandler を直接参照しないため key 文字列を直書き )
        if (elementType == ElementType.CORROSION && level >= 12) {
            tag.putBoolean("MagicalKatanaUnlocked", true);
        }
    }

    /**
     * アイテムから属性タイプを取得
     * @param stack アイテムスタック
     * @return 属性タイプ（属性がない場合はNONE）
     */
    public static ElementType getElementType(ItemStack stack) {
        if (stack.isEmpty()) return ElementType.NONE;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ELEMENT_TYPE_KEY)) return ElementType.NONE;
        return ElementType.fromString(tag.getString(ELEMENT_TYPE_KEY));
    }

    /**
     * アイテムから属性レベルを取得
     * @param stack アイテムスタック
     * @return 属性レベル（属性がない場合は0）
     */
    public static int getElementLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ELEMENT_LEVEL_KEY)) return 0;
        return tag.getInt(ELEMENT_LEVEL_KEY);
    }

    /**
     * アイテムに属性ダメージの種類 ( 物理 / 魔法 / 蓄積 ) を設定する。
     * 属性そのもの ( {@link ElementType} ) とは独立なので、どの属性にも付けられる。
     *
     * @param stack アイテムスタック
     * @param kind  ダメージ種類。 {@link ElementDamageKind#PHYSICAL} は既定値なのでタグを消す
     */
    public static void setElementKind(ItemStack stack, ElementDamageKind kind) {
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        if (kind == null || kind == ElementDamageKind.PHYSICAL) {
            tag.remove(ELEMENT_KIND_KEY);
            return;
        }
        tag.putString(ELEMENT_KIND_KEY, kind.getName());
    }

    /**
     * アイテムの属性ダメージ種類を取得。
     * 未設定のアイテムは従来どおり {@link ElementDamageKind#PHYSICAL} 扱い。
     */
    public static ElementDamageKind getElementKind(ItemStack stack) {
        if (stack.isEmpty()) return ElementDamageKind.PHYSICAL;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ELEMENT_KIND_KEY)) return ElementDamageKind.PHYSICAL;
        return ElementDamageKind.fromString(tag.getString(ELEMENT_KIND_KEY));
    }

    public static void setElementPair(ItemStack stack, ElementType primary, int primaryLevel,
                                      ElementType secondary, int secondaryLevel) {
        if (stack.isEmpty()) return;
        if (primary == null || secondary == null
                || primary == ElementType.NONE || secondary == ElementType.NONE) {
            removeElement(stack);
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(ELEMENT_TYPE_KEY, primary.getName());
        tag.putInt(ELEMENT_LEVEL_KEY, Math.max(1, primaryLevel));
        tag.putString(ELEMENT_TYPE_2_KEY, secondary.getName());
        tag.putInt(ELEMENT_LEVEL_2_KEY, Math.max(1, secondaryLevel));
    }

    public static ElementType getSecondaryElementType(ItemStack stack) {
        if (stack.isEmpty()) return ElementType.NONE;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ELEMENT_TYPE_2_KEY)) return ElementType.NONE;
        return ElementType.fromString(tag.getString(ELEMENT_TYPE_2_KEY));
    }

    public static int getSecondaryElementLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ELEMENT_LEVEL_2_KEY)) return 0;
        return tag.getInt(ELEMENT_LEVEL_2_KEY);
    }

    public static boolean hasSecondaryElement(ItemStack stack) {
        return getSecondaryElementType(stack) != ElementType.NONE;
    }

    public static boolean isOneToOneFireSoul(ItemStack stack) {
        ElementType primary = getElementType(stack);
        ElementType secondary = getSecondaryElementType(stack);
        int primaryLevel = getElementLevel(stack);
        int secondaryLevel = getSecondaryElementLevel(stack);
        return isFireSoulPair(primary, secondary) && primaryLevel > 0 && primaryLevel == secondaryLevel;
    }

    public static ElementType getEffectiveElementType(ItemStack stack) {
        if (isOneToOneFireSoul(stack)) return ElementType.SOUL_FIRE;
        return getElementType(stack);
    }

    public static int getEffectiveElementLevel(ItemStack stack) {
        if (isOneToOneFireSoul(stack)) return getElementLevel(stack);
        return getElementLevel(stack);
    }

    /**
     * プレイヤーの攻撃に実際に載る属性 ( 武器 → 無ければ魔導書スロット )。
     *
     * <p>{@link the_four_primitives_and_weapons.ElementalDamageEvent} のダメージ側と
     * 同じ優先順位 ( 武器優先、武器が無属性なら本 ) なので、
     * 「本だけで攻撃したときもその属性のパーティクルが出る」ようになる。</p>
     */
    public static ElementType getAttackElementType(net.minecraft.world.entity.player.Player player) {
        if (player == null) return ElementType.NONE;
        ElementType weaponType = getEffectiveElementType(player.getMainHandItem());
        if (weaponType != ElementType.NONE) return weaponType;
        BookSlotInfo info = getBookSlotInfo(player);
        return info != null ? info.type : ElementType.NONE;
    }

    private static boolean isFireSoulPair(ElementType left, ElementType right) {
        return (left == ElementType.FIRE && right == ElementType.SOUL)
                || (left == ElementType.SOUL && right == ElementType.FIRE);
    }

    /**
     * アイテムに属性があるかチェック
     * @param stack アイテムスタック
     * @return 属性がある場合true
     */
    public static boolean hasElement(ItemStack stack) {
        return getElementType(stack) != ElementType.NONE;
    }

    /**
     * アイテムから属性を削除
     * @param stack アイテムスタック
     */
    public static void removeElement(ItemStack stack) {
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(ELEMENT_TYPE_KEY);
            tag.remove(ELEMENT_LEVEL_KEY);
            tag.remove(ELEMENT_TYPE_2_KEY);
            tag.remove(ELEMENT_LEVEL_2_KEY);
            tag.remove(ELEMENT_KIND_KEY);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 属性レベルによるダメージスケーリング
    // ─────────────────────────────────────────────────────────────

    /** 属性が足した分の、レベル1超過1あたりの伸び率。 */
    private static final float LEVEL_GROWTH_PER_LEVEL = 0.03f;
    /** 伸び率の上限 ( +60% )。 */
    private static final float LEVEL_GROWTH_MAX       = 0.60f;
    /** 倍率がほぼ等倍の属性でもレベルで伸びるようにする下限。 素ダメージに対する割合 / Lv。 */
    private static final float LEVEL_FLOOR_PER_LEVEL  = 0.06f;
    /** 下限の上限 ( 素ダメージの 150% )。 */
    private static final float LEVEL_FLOOR_MAX        = 1.50f;

    /**
     * 属性が足したダメージをレベルで底上げする。
     *
     * <p>属性ごとのハンドラは、氷や魂のようにレベルで伸びるものと、
     * 炎 / 水 / 侵食 / 闇 / 瘴気 / 電気のように<b>倍率が固定</b>のものが混在していた。
     * ここで共通の底上げを掛けることで、どの属性でもレベルを上げた分だけダメージが増える。</p>
     *
     * <ul>
     *   <li>既に伸びる属性 … 属性分を Lv で最大 +60% まで増幅</li>
     *   <li>倍率固定の属性 … 素ダメージ × 6%/Lv ( 上限 150% ) を最低保証</li>
     * </ul>
     *
     * <p>Lv1 では何も変わらない ( 従来どおり )。 属性側が意図的にダメージを<b>下げている</b>場合
     * ( 血属性の対アンデッド 0.9x など ) は底上げしない。</p>
     *
     * @param elementBonus 属性処理が足した分 ( 最終ダメージ − 素ダメージ )
     * @param baseDamage   属性が乗る前の素ダメージ
     * @param level        属性レベル
     * @return レベルで底上げした「属性が足す分」
     */
    public static float scaleElementBonusByLevel(float elementBonus, float baseDamage, int level) {
        if (level <= 1 || elementBonus < 0.0f || baseDamage <= 0.0f) return elementBonus;

        int overLevel = level - 1;
        float grown = elementBonus
                * (1.0f + Math.min(LEVEL_GROWTH_MAX, LEVEL_GROWTH_PER_LEVEL * overLevel));
        float floor = baseDamage
                * Math.min(LEVEL_FLOOR_MAX, LEVEL_FLOOR_PER_LEVEL * overLevel);
        return Math.max(grown, floor);
    }

    // ─────────────────────────────────────────────────────────────
    // 属性ダメージ dispatch
    // ─────────────────────────────────────────────────────────────

    /**
     * 武器の属性に応じたダメージ処理を実行する。
     * LivingEntityDamageMixin から呼び出す。
     *
     * @param attacker 攻撃者
     * @param target   攻撃対象
     * @param weapon   使用武器
     * @param baseDmg  基礎ダメージ
     * @return 属性込みの最終ダメージ
     */
    public static float applyElementalDamage(LivingEntity attacker,
                                             LivingEntity target,
                                             ItemStack weapon,
                                             float baseDmg) {

        if (!hasElement(weapon)) return baseDmg;

        ElementType type = getEffectiveElementType(weapon);

        switch (type) {
            case ICE:
                return IceElementDamageHandler.handleIceDamage(attacker, target, weapon, baseDmg);
            case ELECTRIC:
                return ElectricElementDamageHandler.handleElectricDamage(attacker, target, weapon, baseDmg);
            case CORROSION:
                return CorrosionElementDamageHandler.handleCorrosionDamage(attacker, target, weapon, baseDmg);
            case HOLY:
                return HolyElementDamageHandler.handleHolyDamage(attacker, target, weapon, baseDmg);
            case FIRE:
                return FireElementDamageHandler.handleFireDamage(attacker, target, weapon, baseDmg);
            case WATER:
                return WaterElementDamageHandler.handleWaterDamage(attacker, target, weapon, baseDmg);
            case WIND:
                return WindElementDamageHandler.handleWindDamage(attacker, target, weapon, baseDmg);
            case THUNDER:
                return ThunderElementDamageHandler.handleThunderDamage(attacker, target, weapon, baseDmg);
            case DARK:
                return DarkElementDamageHandler.handleDarkDamage(attacker, target, weapon, baseDmg);
            case MIASMA:
                return MiasmaElementDamageHandler.handleMiasmaDamage(attacker, target, weapon, baseDmg);
            case BLOOD:
                return BloodElementDamageHandler.handleBloodDamage(attacker, target, weapon, baseDmg);
            case SOUL:
                return SoulElementDamageHandler.handleSoulDamage(attacker, target, weapon, baseDmg);
            case SOUL_FIRE:
                return SoulFireElementDamageHandler.handleSoulFireDamage(attacker, target, weapon, baseDmg);
            default:
                return baseDmg;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Curios bookスロット関連
    // ─────────────────────────────────────────────────────────────

    /**
     * アイテムのクラス名から魔導書の属性を判定
     */
    public static ElementType getBookElementFromItemStack(ItemStack stack) {
        return getBookElementFromItem(stack);
    }

    private static ElementType getBookElementFromItem(ItemStack stack) {
        if (stack.isEmpty()) return ElementType.NONE;
        String itemName = stack.getItem().getClass().getSimpleName();
        switch (itemName) {
            case "FireballItem":    return ElementType.FIRE;
            case "ThunderboltItem": return ElementType.THUNDER;
            case "BubbleshotItem":  return ElementType.WATER;
            case "StormItem":
            case "WindStepItem":    return ElementType.WIND;
            case "DarknessItem":    return ElementType.DARK;
            case "IceBookItem":     return ElementType.ICE;
            case "ElectricBookItem":return ElementType.ELECTRIC;
            case "CorrosionBookItem":return ElementType.CORROSION;
            case "HolyBookItem":    return ElementType.HOLY;
            case "ErasureBookItem":   return ElementType.ERASURE;
            case "MiasmaBookItem":  return ElementType.MIASMA;
            case "SoulBookItem":    return ElementType.SOUL;
            case "SoulFireBookItem": return ElementType.SOUL_FIRE;
            default:                return ElementType.NONE;
        }
    }

    /** 書スロット情報を 1 回の Curios 走査でまとめて取得するための値オブジェクト */
    public static final class BookSlotInfo {
        public final ElementType type;
        public final int level;
        /** 魔導書に設定された属性ダメージ種類 ( 未設定なら物理 )。 */
        public final ElementDamageKind kind;
        public static final BookSlotInfo NONE = new BookSlotInfo(ElementType.NONE, 0);
        public BookSlotInfo(ElementType t, int l) { this(t, l, ElementDamageKind.PHYSICAL); }
        public BookSlotInfo(ElementType t, int l, ElementDamageKind k) {
            this.type = t; this.level = l; this.kind = k;
        }
    }

    /**
     * メインハンド→オフハンド→Curios "book" スロットを 1 回だけ走査して
     * 最初に見つかった魔導書の (属性, レベル) を返す。
     * 以前の getBookSlotElement + getBookSlotLevel は Curios 走査を 2 回していたので
     * これ 1 本に統合して LivingHurtEvent ごとの負荷を半減する。
     */
    public static BookSlotInfo getBookSlotInfo(Player player) {
        // メインハンド
        ItemStack main = player.getMainHandItem();
        ElementType mType = getBookElementFromItem(main);
        if (mType != ElementType.NONE) {
            // クラスで book と判定できた場合、 NBT level が無くても最低 Lv1 として扱う
            // (= creative tab で入手 + 普通の武器で殴っても通常攻撃に属性が乗る)
            return new BookSlotInfo(mType, Math.max(getElementLevel(main), 1), getElementKind(main));
        }
        // オフハンド
        ItemStack off = player.getOffhandItem();
        ElementType oType = getBookElementFromItem(off);
        if (oType != ElementType.NONE) {
            return new BookSlotInfo(oType, Math.max(getElementLevel(off), 1), getElementKind(off));
        }
        // Curios の book スロット (1 回だけ走査)
        try {
            java.util.concurrent.atomic.AtomicReference<BookSlotInfo> result =
                new java.util.concurrent.atomic.AtomicReference<>(BookSlotInfo.NONE);
            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
                handler.getStacksHandler("book").ifPresent(stacksHandler -> {
                    var stacks = stacksHandler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        ItemStack s = stacks.getStackInSlot(i);
                        if (s.isEmpty()) continue;
                        ElementType t = getBookElementFromItem(s);
                        if (t != ElementType.NONE) {
                            result.set(new BookSlotInfo(t, Math.max(getElementLevel(s), 1), getElementKind(s)));
                            return;
                        }
                    }
                });
            });
            return result.get();
        } catch (Exception e) {
            return BookSlotInfo.NONE;
        }
    }

    /**
     * Curiosのbookスロット・メインハンド・オフハンドにある魔導書のレベルを取得
     */
    public static int getBookSlotLevel(Player player) {
        // メインハンドをチェック
        ItemStack mainHand = player.getMainHandItem();
        if (getBookElementFromItem(mainHand) != ElementType.NONE) {
            int lv = getElementLevel(mainHand);
            if (lv > 0) return lv;
        }

        // オフハンドをチェック
        ItemStack offHand = player.getOffhandItem();
        if (getBookElementFromItem(offHand) != ElementType.NONE) {
            int lv = getElementLevel(offHand);
            if (lv > 0) return lv;
        }

        // Curiosのbookスロットをチェック
        try {
            java.util.concurrent.atomic.AtomicInteger result =
                    new java.util.concurrent.atomic.AtomicInteger(0);

            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
                handler.getStacksHandler("book").ifPresent(stacksHandler -> {
                    for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            int lv = getElementLevel(stack);
                            if (lv > 0) {
                                result.set(lv);
                                return;
                            }
                        }
                    }
                });
            });

            return result.get();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Curiosのbookスロット・メインハンド・オフハンドにある魔導書の属性を取得
     */
    public static ElementType getBookSlotElement(Player player) {
        // メインハンドをチェック
        ElementType mainElement = getBookElementFromItem(player.getMainHandItem());
        if (mainElement != ElementType.NONE) return mainElement;

        // オフハンドをチェック
        ElementType offElement = getBookElementFromItem(player.getOffhandItem());
        if (offElement != ElementType.NONE) return offElement;

        // Curiosのbookスロットをチェック
        try {
            java.util.concurrent.atomic.AtomicReference<ElementType> result =
                    new java.util.concurrent.atomic.AtomicReference<>(ElementType.NONE);

            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
                handler.getStacksHandler("book").ifPresent(stacksHandler -> {
                    for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            ElementType type = getBookElementFromItem(stack);
                            if (type != ElementType.NONE) {
                                result.set(type);
                                return;
                            }
                        }
                    }
                });
            });

            return result.get();
        } catch (Exception e) {
            return ElementType.NONE;
        }
    }

    /**
     * ターゲットがbookスロットの魔導書で属性ダメージを無効化できるかチェック
     * @param target        ダメージを受けるエンティティ
     * @param damageElement 受ける属性ダメージのタイプ
     * @return true = 無効化できる
     */
    public static boolean isElementNullifiedByBook(LivingEntity target, ElementType damageElement) {
        if (damageElement == ElementType.NONE) return false;
        if (!(target instanceof Player player)) return false;

        // Curios 走査 1 回で element/level をまとめて取得 (以前は 2 関数で別走査)
        ElementType bookElement = getBookSlotInfo(player).type;
        if (bookElement == ElementType.NONE) return false;

        // 通常のカウンターチェック
        ElementType counterElement = damageElement.getCounterElement();
        if (bookElement == counterElement) return true;

        // StormはWIND+WATER+THUNDERのキメラ: 複数属性のカウンターをまとめて防ぐ
        if (bookElement == ElementType.WIND && isStormBookEquipped(player)) {
            switch (damageElement) {
                case ELECTRIC:
                case THUNDER:
                case FIRE:
                case SOUL_FIRE:
                case CORROSION:
                case WATER:
                    return true;
                default:
                    break;
            }
        }

        return false;
    }

    /**
     * bookスロット・メインハンド・オフハンドにStormItem（キメラ魔導書）が装備されているかチェック
     */
    private static boolean isStormBookEquipped(Player player) {
        // メインハンド・オフハンドをチェック
        if (player.getMainHandItem().getItem().getClass().getSimpleName().equals("StormItem")) return true;
        if (player.getOffhandItem().getItem().getClass().getSimpleName().equals("StormItem")) return true;

        // Curiosのbookスロットをチェック
        try {
            java.util.concurrent.atomic.AtomicBoolean result =
                    new java.util.concurrent.atomic.AtomicBoolean(false);

            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
                handler.getStacksHandler("book").ifPresent(stacksHandler -> {
                    for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty()
                                && stack.getItem().getClass().getSimpleName().equals("StormItem")) {
                            result.set(true);
                            return;
                        }
                    }
                });
            });

            return result.get();
        } catch (Exception e) {
            return false;
        }
    }
}
