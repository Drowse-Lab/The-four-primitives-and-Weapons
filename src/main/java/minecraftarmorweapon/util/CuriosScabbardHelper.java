package minecraftarmorweapon.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import minecraftarmorweapon.item.SayaItem;
import minecraftarmorweapon.item.TyokutoSayaItem;
import minecraftarmorweapon.item.SwordSayaItem;
import minecraftarmorweapon.events.DodgeAndBattouHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Curiosスロット内の鞘へのアクセスを提供するユーティリティ
 */
public class CuriosScabbardHelper {

    private static final String[] SCABBARD_SLOTS = {"belt", "back"};

    /**
     * Curiosスロットから最初に見つかった鞘の情報を返す。
     * 納刀可能な空の鞘、または抜刀可能な刀入り鞘を検索する。
     */
    @Nullable
    public static ScabbardSlotInfo findScabbardInCurios(Player player) {
        var ref = new Object() { ScabbardSlotInfo result = null; };
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            for (String slotId : SCABBARD_SLOTS) {
                handler.getStacksHandler(slotId).ifPresent(stacksHandler -> {
                    if (ref.result != null) return;
                    for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        if (isScabbard(stack)) {
                            ref.result = new ScabbardSlotInfo(stack, slotId, i, stacksHandler);
                            return;
                        }
                    }
                });
                if (ref.result != null) break;
            }
        });
        return ref.result;
    }

    /**
     * Curiosスロットから刀が入っている鞘を検索する（抜刀用）
     */
    @Nullable
    public static ScabbardSlotInfo findLoadedScabbardInCurios(Player player) {
        var ref = new Object() { ScabbardSlotInfo result = null; };
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            for (String slotId : SCABBARD_SLOTS) {
                handler.getStacksHandler(slotId).ifPresent(stacksHandler -> {
                    if (ref.result != null) return;
                    for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        if (isScabbard(stack) && hasStoredWeapon(stack)) {
                            ref.result = new ScabbardSlotInfo(stack, slotId, i, stacksHandler);
                            return;
                        }
                    }
                });
                if (ref.result != null) break;
            }
        });
        return ref.result;
    }

    /**
     * Curiosスロットから空の鞘を検索する（納刀用）
     */
    @Nullable
    public static ScabbardSlotInfo findEmptyScabbardInCurios(Player player) {
        var ref = new Object() { ScabbardSlotInfo result = null; };
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            for (String slotId : SCABBARD_SLOTS) {
                handler.getStacksHandler(slotId).ifPresent(stacksHandler -> {
                    if (ref.result != null) return;
                    for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        if (isScabbard(stack) && !hasStoredWeapon(stack)) {
                            ref.result = new ScabbardSlotInfo(stack, slotId, i, stacksHandler);
                            return;
                        }
                    }
                });
                if (ref.result != null) break;
            }
        });
        return ref.result;
    }

    /**
     * Curiosスロットの鞘に納刀する
     */
    public static boolean sheathIntoCurioSlot(Player player, ItemStack weaponStack, InteractionHand weaponHand) {
        if (!DodgeAndBattouHandler.isWeapon(weaponStack)) return false;

        ScabbardSlotInfo info = findEmptyScabbardInCurios(player);
        if (info == null) return false;

        ItemStack sheathStack = info.stack;
        boolean isTyokutoSaya = sheathStack.getItem() instanceof TyokutoSayaItem;
        boolean isSwordSaya = sheathStack.getItem() instanceof SwordSayaItem;

        // 直刀鞘の場合、直刀のみ納刀可能
        if (isTyokutoSaya) {
            if (!minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.isStraightSword(weaponStack)) {
                player.displayClientMessage(Component.literal("§cこの鞘には直刀のみ納刀可能です"), true);
                return false;
            }
        } else if (isSwordSaya) {
            // 剣の鞘の場合、登録済みのvanilla剣のみ納刀可能
            if (!SwordSayaItem.canSheathe(weaponStack)) {
                player.displayClientMessage(Component.literal("§cこの鞘にはこの武器を納刀できません"), true);
                return false;
            }
        } else {
            // 通常の鞘には直刀・剣の鞘対象武器は不可
            if (minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.isStraightSword(weaponStack)) {
                player.displayClientMessage(Component.literal("§c直刀は専用の鞘が必要です"), true);
                return false;
            }
            if (SwordSayaItem.canSheathe(weaponStack)) {
                player.displayClientMessage(Component.literal("§cこの剣は専用の鞘が必要です"), true);
                return false;
            }
        }

        CompoundTag sheathTag = sheathStack.getOrCreateTag();
        String storageKey = (isTyokutoSaya || isSwordSaya) ? "StoredSword" : "StoredKatana";

        // 武器のNBTデータを保存
        CompoundTag weaponData = weaponStack.save(new CompoundTag());
        sheathTag.put(storageKey, weaponData);

        // 鞘の見た目を更新
        int modelData;
        if (isTyokutoSaya) {
            modelData = TyokutoSayaItem.getSwordModelData(weaponStack);
        } else if (isSwordSaya) {
            modelData = SwordSayaItem.getSwordModelData(weaponStack);
        } else {
            modelData = getWeaponModelDataForSaya(weaponStack);
        }
        sheathTag.putInt("CustomModelData", modelData);
        sheathStack.setTag(sheathTag);

        // Curiosスロットを更新
        info.stacksHandler.getStacks().setStackInSlot(info.slotIndex, sheathStack);

        // 武器を削除
        player.setItemInHand(weaponHand, ItemStack.EMPTY);

        // 納刀音
        player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.8F);
        player.displayClientMessage(Component.literal("§7納刀"), true);

        return true;
    }

    /**
     * Curiosスロットの鞘から抜刀する
     */
    public static boolean battouFromCurioSlot(Player player, InteractionHand targetHand) {
        ScabbardSlotInfo info = findLoadedScabbardInCurios(player);
        if (info == null) return false;

        // ターゲットの手が空いていることを確認
        if (!player.getItemInHand(targetHand).isEmpty()) return false;

        ItemStack sheathStack = info.stack;
        CompoundTag tag = sheathStack.getOrCreateTag();

        // StoredKatanaまたはStoredSwordをチェック
        String storedKey = tag.contains("StoredKatana") ? "StoredKatana" :
                           tag.contains("StoredSword") ? "StoredSword" : null;
        if (storedKey == null) return false;

        // 刀を生成
        ItemStack weaponStack = ItemStack.of(tag.getCompound(storedKey));
        if (weaponStack.isEmpty()) return false;

        // 手に刀を配置
        player.setItemInHand(targetHand, weaponStack);

        // 鞘を空にする
        tag.remove(storedKey);
        tag.putInt("CustomModelData", 0);
        sheathStack.setTag(tag);

        // Curiosスロットを更新
        info.stacksHandler.getStacks().setStackInSlot(info.slotIndex, sheathStack);

        // 抜刀音
        player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.0F);

        return true;
    }

    /**
     * アイテムが鞘かどうか判定
     */
    public static boolean isScabbard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof SayaItem
            || stack.getItem() instanceof TyokutoSayaItem
            || stack.getItem() instanceof SwordSayaItem;
    }

    /**
     * 鞘に武器が入っているか判定
     */
    public static boolean hasStoredWeapon(ItemStack stack) {
        if (!stack.hasTag()) return false;
        CompoundTag tag = stack.getTag();
        return tag.contains("StoredKatana") || tag.contains("StoredSword");
    }

    /**
     * 鞘から格納された武器を取り出す（NBTからItemStackを復元）
     */
    public static ItemStack extractWeaponFromScabbard(ItemStack scabbardStack) {
        if (!scabbardStack.hasTag()) return ItemStack.EMPTY;
        CompoundTag tag = scabbardStack.getTag();
        String storedKey = tag.contains("StoredKatana") ? "StoredKatana" :
                           tag.contains("StoredSword") ? "StoredSword" : null;
        if (storedKey == null) return ItemStack.EMPTY;
        return ItemStack.of(tag.getCompound(storedKey));
    }

    /**
     * Curiosスロット + インベントリから武器入りの鞘を全て列挙する
     */
    public static List<DrawableWeaponInfo> findAllLoadedScabbards(Player player) {
        List<DrawableWeaponInfo> results = new ArrayList<>();

        // 1. 手持ちの鞘 (MAIN_HAND / OFF_HAND) — 最優先
        for (int h = 0; h < 2; h++) {
            InteractionHand hand = h == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack handStack = player.getItemInHand(hand);
            if (isScabbard(handStack) && hasStoredWeapon(handStack)) {
                ItemStack weapon = extractWeaponFromScabbard(handStack);
                if (!weapon.isEmpty()) {
                    results.add(new DrawableWeaponInfo(
                        handStack, weapon, ScabbardLocation.HAND, null, h));
                }
            }
        }

        // 2. Curiosスロット (belt → back の順)
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            for (String slotId : SCABBARD_SLOTS) {
                handler.getStacksHandler(slotId).ifPresent(stacksHandler -> {
                    for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        if (isScabbard(stack) && hasStoredWeapon(stack)) {
                            ItemStack weapon = extractWeaponFromScabbard(stack);
                            if (!weapon.isEmpty()) {
                                results.add(new DrawableWeaponInfo(
                                    stack, weapon, ScabbardLocation.CURIOS, slotId, i));
                            }
                        }
                    }
                });
            }
        });

        // 3. インベントリ (ホットバー + メインインベントリ)
        //    手持ちスロット（選択中ホットバー・オフハンド=40）は既にチェック済みなのでスキップ
        int selectedSlot = player.getInventory().selected;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (i == selectedSlot || i == 40) continue;
            ItemStack stack = player.getInventory().getItem(i);
            if (isScabbard(stack) && hasStoredWeapon(stack)) {
                ItemStack weapon = extractWeaponFromScabbard(stack);
                if (!weapon.isEmpty()) {
                    results.add(new DrawableWeaponInfo(
                        stack, weapon, ScabbardLocation.INVENTORY, null, i));
                }
            }
        }

        // 4. CoverUp (Bluepurge復元)
        CompoundTag entityData = player.getPersistentData();
        if (entityData.contains("CoverUp")) {
            CompoundTag savedItemTag = entityData.getCompound("CoverUp");
            ItemStack coverUpItem = ItemStack.of(savedItemTag);
            if (!coverUpItem.isEmpty()) {
                results.add(new DrawableWeaponInfo(
                    ItemStack.EMPTY, coverUpItem, ScabbardLocation.COVERUP, null, 0));
            }
        }

        return results;
    }

    /**
     * 手/Curios/インベントリから空の鞘を全て列挙する（納刀ホイール用）
     */
    public static List<DrawableWeaponInfo> findAllEmptyScabbards(Player player) {
        List<DrawableWeaponInfo> results = new ArrayList<>();

        // 1. 手持ちの鞘 (MAIN_HAND / OFF_HAND) — 空の鞘のみ
        for (int h = 0; h < 2; h++) {
            InteractionHand hand = h == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack handStack = player.getItemInHand(hand);
            if (isScabbard(handStack) && !hasStoredWeapon(handStack)) {
                results.add(new DrawableWeaponInfo(
                    handStack, ItemStack.EMPTY, ScabbardLocation.HAND, null, h));
            }
        }

        // 2. Curiosスロット (belt → back の順)
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            for (String slotId : SCABBARD_SLOTS) {
                handler.getStacksHandler(slotId).ifPresent(stacksHandler -> {
                    for (int i = 0; i < stacksHandler.getStacks().getSlots(); i++) {
                        ItemStack stack = stacksHandler.getStacks().getStackInSlot(i);
                        if (isScabbard(stack) && !hasStoredWeapon(stack)) {
                            results.add(new DrawableWeaponInfo(
                                stack, ItemStack.EMPTY, ScabbardLocation.CURIOS, slotId, i));
                        }
                    }
                });
            }
        });

        // 3. インベントリ (手持ちスロットはスキップ)
        int selectedSlot = player.getInventory().selected;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (i == selectedSlot || i == 40) continue;
            ItemStack stack = player.getInventory().getItem(i);
            if (isScabbard(stack) && !hasStoredWeapon(stack)) {
                results.add(new DrawableWeaponInfo(
                    stack, ItemStack.EMPTY, ScabbardLocation.INVENTORY, null, i));
            }
        }

        return results;
    }

    /**
     * 武器と鞘の互換性をチェック（直刀⇔直刀鞘、通常武器⇔通常鞘）。
     * 登録外の武器 (model data 0) は納刀不可。
     */
    public static boolean isCompatible(ItemStack weaponStack, ItemStack scabbardStack) {
        boolean isTyokutoSaya = scabbardStack.getItem() instanceof TyokutoSayaItem;
        boolean isSwordSaya = scabbardStack.getItem() instanceof SwordSayaItem;
        boolean isStraightSword = minecraftarmorweapon.procedures.TyokutouThrustAttackProcedure.isStraightSword(weaponStack);
        boolean isSwordSayaTarget = SwordSayaItem.canSheathe(weaponStack);
        if (isTyokutoSaya) {
            // 直刀鞘: 直刀 AND 登録済み (getSwordModelData != 0)
            return isStraightSword && TyokutoSayaItem.getSwordModelData(weaponStack) != 0;
        } else if (isSwordSaya) {
            // 剣鞘: SwordSayaItem.canSheathe で既に登録チェック済み
            return isSwordSayaTarget;
        } else {
            // 通常鞘: 通常武器 AND 登録済み (getWeaponModelDataForSaya != 0)
            if (isStraightSword || isSwordSayaTarget) return false;
            return getWeaponModelDataForSaya(weaponStack) != 0;
        }
    }

    /**
     * 通常 saya に登録されているアイテムか (UI フィルタ・デバッグ用)。
     */
    public static boolean isRegisteredForSaya(ItemStack weaponStack) {
        return getWeaponModelDataForSaya(weaponStack) != 0;
    }

    /**
     * 直刀 saya に登録されているアイテムか。
     */
    public static boolean isRegisteredForTyokutoSaya(ItemStack weaponStack) {
        return TyokutoSayaItem.getSwordModelData(weaponStack) != 0;
    }

    /**
     * 鞘の武器データをクリアする（抜刀後の処理用）
     */
    public static void clearWeaponFromScabbard(ItemStack scabbard) {
        CompoundTag tag = scabbard.getOrCreateTag();
        if (tag.contains("StoredKatana")) tag.remove("StoredKatana");
        if (tag.contains("StoredSword")) tag.remove("StoredSword");
        tag.putInt("CustomModelData", 0);
        scabbard.setTag(tag);
    }

    /**
     * 通常の鞘(SayaItem)用のCustomModelDataを取得。
     * data/&lt;namespace&gt;/maw_saya/*.json の "katana" セクションから読み込まれる。
     */
    public static int getWeaponModelDataForSaya(ItemStack weapon) {
        return SayaRegistry.getModelData(SayaRegistry.SayaType.KATANA, weapon);
    }

    /**
     * Curiosスロット内の鞘情報を保持するレコード
     */
    public static class ScabbardSlotInfo {
        public final ItemStack stack;
        public final String slotId;
        public final int slotIndex;
        public final ICurioStacksHandler stacksHandler;

        public ScabbardSlotInfo(ItemStack stack, String slotId, int slotIndex, ICurioStacksHandler stacksHandler) {
            this.stack = stack;
            this.slotId = slotId;
            this.slotIndex = slotIndex;
            this.stacksHandler = stacksHandler;
        }
    }

    /**
     * 鞘の場所を示すenum
     */
    public enum ScabbardLocation {
        CURIOS,
        INVENTORY,
        COVERUP,
        HAND
    }

    /**
     * 抜刀可能な武器の情報
     */
    public static class DrawableWeaponInfo {
        public final ItemStack scabbardStack;
        public final ItemStack weaponStack;
        public final ScabbardLocation location;
        @Nullable
        public final String curioSlotId;
        public final int slotIndex;

        public DrawableWeaponInfo(ItemStack scabbardStack, ItemStack weaponStack,
                                  ScabbardLocation location, @Nullable String curioSlotId, int slotIndex) {
            this.scabbardStack = scabbardStack;
            this.weaponStack = weaponStack;
            this.location = location;
            this.curioSlotId = curioSlotId;
            this.slotIndex = slotIndex;
        }
    }
}
