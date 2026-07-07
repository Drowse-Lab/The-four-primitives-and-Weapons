package the_four_primitives_and_weapons.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;

/**
 * Iron's Spells 'n Spellbooks (irons_spellbooks) 連携。
 *
 * ロードされていれば:
 *   - 本 MOD の Mana 消費を Iron's Spellbooks の PlayerMagicData.setMana/getMana に転送
 *   - 本 MOD の Mana HUD 描画を抑制 (二重バーを避ける)
 *
 * ロードされていなければ: 既存の Attribute ベース ManaHelper のまま動作。
 *
 * 結合は **リフレクションのみ** で行うため、開発時の compile 依存は不要。
 * Iron's Spellbooks の API が変わっても本 MOD はロード失敗しない (getMana が -1 を返す)。
 */
public final class SpellbooksCompat {
    public static final String MOD_ID = "irons_spellbooks";

    private static Boolean loaded;
    private static Method mGetPlayerMagicData;
    private static Method mGetMana;
    private static Method mSetMana;

    // スペルコンテナ ( アイテムに付与されたスペル ) 検出用。 API が無ければ null のまま。
    private static Boolean spellContainerReady;
    private static Method mSpellContainerGet;   // static ISpellContainer get(ItemStack)
    private static Method mSpellContainerIsEmpty; // boolean isEmpty()

    // 能動詠唱 ( 右クリックで付与スペルを発動 ) 用。
    private static Boolean castReflectReady;
    private static Method mContainerGetActive;   // ISpellContainer.getActiveSpells() : List<SpellSlot>
    private static Method mSlotGetSpell;         // SpellSlot.getSpell() : AbstractSpell
    private static Method mSlotGetLevel;         // SpellSlot.getLevel() : int
    private static Method mAttemptInitiateCast;  // AbstractSpell.attemptInitiateCast(...)
    private static Object castSourceSword;       // CastSource.SWORD
    private static String slotMainhand;          // SpellSelectionManager.MAINHAND
    private static String slotOffhand;           // SpellSelectionManager.OFFHAND

    private SpellbooksCompat() {}

    public static boolean isLoaded() {
        if (loaded == null) {
            boolean present = ModList.get().isLoaded(MOD_ID);
            if (present) present = initReflection();
            loaded = present;
        }
        return loaded;
    }

    private static boolean initReflection() {
        try {
            // Iron's Spellbooks 3.15.6 の実 API:
            //   io.redspace.ironsspellbooks.api.magic.MagicData
            //     static MagicData getPlayerMagicData(LivingEntity)
            //     float  getMana()
            //     void   setMana(float)
            Class<?> cls = Class.forName(
                "io.redspace.ironsspellbooks.api.magic.MagicData");
            mGetPlayerMagicData = cls.getMethod("getPlayerMagicData", LivingEntity.class);
            mGetMana = cls.getMethod("getMana");
            mSetMana = cls.getMethod("setMana", float.class);
            return true;
        } catch (Throwable t) {
            // クラス/メソッドが見つからなければ無効化 (バージョン不整合でも落ちない)
            return false;
        }
    }

    /** @return Iron's 管理下の Mana、未ロードや失敗時は -1 */
    public static double getMana(Player p) {
        if (!isLoaded()) return -1;
        try {
            Object data = mGetPlayerMagicData.invoke(null, p);
            if (data == null) return -1;
            return ((Number) mGetMana.invoke(data)).doubleValue();
        } catch (Throwable t) {
            return -1;
        }
    }

    /** Iron's 側の Mana を直接設定。未ロードなら何もしない。 */
    public static void setMana(Player p, double value) {
        if (!isLoaded()) return;
        try {
            Object data = mGetPlayerMagicData.invoke(null, p);
            if (data == null) return;
            float clamped = (float) Math.max(0.0, value);
            mSetMana.invoke(data, clamped);
        } catch (Throwable ignored) {}
    }

    // ─── Attribute フォワーディング ( max_mana / mana_regen ) ─────────────
    //   Iron's Spellbooks の attribute を ResourceLocation 経由で引いて値を返す。
    //   compile-time dep なしで動くように ForgeRegistries.ATTRIBUTES を走査する。

    private static final ResourceLocation MAX_MANA_ATTR_ID    =
            new ResourceLocation(MOD_ID, "max_mana");
    private static final ResourceLocation MANA_REGEN_ATTR_ID  =
            new ResourceLocation(MOD_ID, "mana_regen");

    private static AttributeInstance lookupAttr(Player p, ResourceLocation id) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(id);
        return attr == null ? null : p.getAttribute(attr);
    }

    /** @return Iron's 管理下の最大 Mana、未ロード / attribute 無しは -1 */
    public static double getMaxMana(Player p) {
        if (!isLoaded()) return -1;
        AttributeInstance ai = lookupAttr(p, MAX_MANA_ATTR_ID);
        return ai == null ? -1 : ai.getValue();
    }

    /** @return Iron's 管理下のマナ回復速度 ( 1秒あたり )、未ロード / attribute 無しは -1 */
    public static double getManaRegenPerSec(Player p) {
        if (!isLoaded()) return -1;
        AttributeInstance ai = lookupAttr(p, MANA_REGEN_ATTR_ID);
        return ai == null ? -1 : ai.getValue();
    }

    // ─── スペル付与判定 ( 右クリックのスペル優先に使用 ) ──────────────────

    private static boolean initSpellContainerReflection() {
        try {
            // Iron's Spellbooks: io.redspace.ironsspellbooks.api.spells.ISpellContainer
            //   static ISpellContainer get(ItemStack)
            //   boolean isEmpty()
            Class<?> cls = Class.forName(
                "io.redspace.ironsspellbooks.api.spells.ISpellContainer");
            mSpellContainerGet = cls.getMethod("get", ItemStack.class);
            mSpellContainerIsEmpty = cls.getMethod("isEmpty");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * アイテムに Iron's Spellbooks のスペルが付与 ( 登録 ) されているか。
     * 未ロード / API 不一致 / 付与なし の場合は false ( = スペル優先しない )。
     */
    public static boolean hasImbuedSpell(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isLoaded()) return false;
        if (spellContainerReady == null) spellContainerReady = initSpellContainerReflection();
        if (!spellContainerReady) return false;
        try {
            Object container = mSpellContainerGet.invoke(null, stack);
            if (container == null) return false;
            Object empty = mSpellContainerIsEmpty.invoke(container);
            return (empty instanceof Boolean) && !((Boolean) empty);
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean initCastReflection() {
        try {
            if (spellContainerReady == null) spellContainerReady = initSpellContainerReflection();
            if (!spellContainerReady) return false;
            Class<?> containerCls = Class.forName("io.redspace.ironsspellbooks.api.spells.ISpellContainer");
            mContainerGetActive = containerCls.getMethod("getActiveSpells");
            Class<?> slotCls = Class.forName("io.redspace.ironsspellbooks.api.spells.SpellSlot");
            mSlotGetSpell = slotCls.getMethod("getSpell");
            mSlotGetLevel = slotCls.getMethod("getLevel");
            Class<?> spellCls = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
            Class<?> castSourceCls = Class.forName("io.redspace.ironsspellbooks.api.spells.CastSource");
            mAttemptInitiateCast = spellCls.getMethod("attemptInitiateCast",
                ItemStack.class, int.class, Level.class, Player.class, castSourceCls, boolean.class, String.class);
            castSourceSword = castSourceCls.getField("SWORD").get(null);
            Class<?> selCls = Class.forName("io.redspace.ironsspellbooks.api.magic.SpellSelectionManager");
            slotMainhand = (String) selCls.getField("MAINHAND").get(null);
            slotOffhand = (String) selCls.getField("OFFHAND").get(null);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * アイテムに付与された Iron's スペルを ( CastSource.SWORD として ) 発動する。
     * Iron's の詠唱アイテムと同様に、 client / server 両サイドで呼ぶこと ( attemptInitiateCast が
     * サイドを内部で処理する )。
     *
     * @return 詠唱を開始できたら true。 未ロード / API 不一致 / スペル無し / マナ不足等は false。
     */
    public static boolean castImbuedSpell(Player player, ItemStack stack, InteractionHand hand) {
        if (player == null || stack == null || stack.isEmpty() || !isLoaded()) return false;
        if (castReflectReady == null) castReflectReady = initCastReflection();
        if (!castReflectReady) return false;
        try {
            Object container = mSpellContainerGet.invoke(null, stack);
            if (container == null) return false;
            if (Boolean.TRUE.equals(mSpellContainerIsEmpty.invoke(container))) return false;
            java.util.List<?> active = (java.util.List<?>) mContainerGetActive.invoke(container);
            if (active == null || active.isEmpty()) return false;
            Object slot = active.get(0);
            Object spell = mSlotGetSpell.invoke(slot);
            if (spell == null) return false;
            int level = ((Number) mSlotGetLevel.invoke(slot)).intValue();
            String slotStr = (hand == InteractionHand.OFF_HAND) ? slotOffhand : slotMainhand;
            Object result = mAttemptInitiateCast.invoke(
                spell, stack, level, player.level(), player, castSourceSword, true, slotStr);
            return Boolean.TRUE.equals(result);
        } catch (Throwable t) {
            return false;
        }
    }
}
