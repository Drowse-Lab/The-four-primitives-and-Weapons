package minecraftarmorweapon.damage;

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
        }
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

        ElementType type = getElementType(weapon);

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
            default:
                return baseDmg;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Curios bookスロット関連
    // ─────────────────────────────────────────────────────────────

    /**
     * Curiosのbookスロットにある魔導書のレベルを取得
     */
    public static int getBookSlotLevel(Player player) {
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
     * Curiosのbookスロットにある魔導書の属性を取得
     */
    public static ElementType getBookSlotElement(Player player) {
        try {
            java.util.concurrent.atomic.AtomicReference<ElementType> result =
                    new java.util.concurrent.atomic.AtomicReference<>(ElementType.NONE);

            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
                handler.getStacksHandler("book").ifPresent(stacksHandler -> {
                    for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            String itemName = stack.getItem().getClass().getSimpleName();
                            switch (itemName) {
                                case "FireballItem":    result.set(ElementType.FIRE);      return;
                                case "ThunderboltItem": result.set(ElementType.THUNDER);   return;
                                case "BubbleshotItem":  result.set(ElementType.WATER);     return;
                                case "StormItem":
                                case "WindStepItem":    result.set(ElementType.WIND);      return;
                                case "DarknessItem":    result.set(ElementType.DARK);      return;
                                case "IceBookItem":     result.set(ElementType.ICE);       return;
                                case "ElectricBookItem":result.set(ElementType.ELECTRIC);  return;
                                case "CorrosionBookItem":result.set(ElementType.CORROSION);return;
                                case "HolyBookItem":    result.set(ElementType.HOLY);      return;
                                case "ErrorBookItem":   result.set(ElementType.ERROR);     return;
                                case "MiasmaBookItem":  result.set(ElementType.MIASMA);    return;
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

        ElementType bookElement = getBookSlotElement(player);
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
     * bookスロットにStormItem（キメラ魔導書）が装備されているかチェック
     */
    private static boolean isStormBookEquipped(Player player) {
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
