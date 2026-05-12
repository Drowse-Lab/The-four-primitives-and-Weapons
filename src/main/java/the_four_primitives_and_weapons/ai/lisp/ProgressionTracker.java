package the_four_primitives_and_weapons.ai.lisp;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ワールド全体の進行度を追跡し、Mob基礎AIレベルへの加算ボーナスを計算する。
 *
 * ボーナス加算イベント:
 *   - 初めてネザー入り        : +10
 *   - 初めてエンド入り        : +20
 *   - ネザーに入る(都度)      : +1
 *   - エンドに入る(都度)      : +2
 *   - ダイヤモンド獲得(5個毎) : +1
 *
 * AngelTierDodgeHandler は「mob実Lv + 進行度ボーナス」を実効Lvとして回避率を算出する。
 * オーバーワールドでは進行度ボーナスを半減（序盤の難易度を抑制）。
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class ProgressionTracker {

    private static final String DATA_NAME = "the_four_primitives_and_weapons_progression";

    private static ProgressionData cached;

    private static ProgressionData getData() {
        if (cached != null) return cached;
        return new ProgressionData();
    }

    public static void bindWorld(ServerLevel overworld) {
        cached = overworld.getDataStorage().computeIfAbsent(
            ProgressionData::load, ProgressionData::new, DATA_NAME);
    }

    /** 現在の基礎AIレベル加算ボーナス */
    public static int getBaseLevelBonus() {
        return getData().baseLevelBonus;
    }

    /** ディメンション別の回避率上限 */
    public static float getDodgeCapForDimension(Level level) {
        if (level == null) return 0.20f;
        ResourceKey<Level> dim = level.dimension();
        if (dim == Level.NETHER) return 0.25f;
        if (dim == Level.END) return 0.30f;
        return 0.20f;
    }

    /** オーバーワールドでは進行度ボーナスを半減 */
    public static float getEffectiveBonus(Level level) {
        int bonus = getBaseLevelBonus();
        if (level != null && level.dimension() == Level.OVERWORLD) {
            return bonus * 0.5f;
        }
        return bonus;
    }

    private static void addBonus(int amount, String reason) {
        ProgressionData data = getData();
        data.baseLevelBonus += amount;
        data.setDirty();
    }

    // === イベントフック ===

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceKey<Level> to = event.getTo();
        ProgressionData data = getData();

        if (to == Level.NETHER) {
            if (!data.hasEnteredNether) {
                data.hasEnteredNether = true;
                addBonus(10, "first_nether");
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§5§l[進行度] §7初めてのネザー。Mob基礎レベル +10"));
            } else {
                addBonus(1, "nether_reentry");
            }
        } else if (to == Level.END) {
            if (!data.hasEnteredEnd) {
                data.hasEnteredEnd = true;
                addBonus(20, "first_end");
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§5§l[進行度] §7初めてのエンド。Mob基礎レベル +20"));
            } else {
                addBonus(2, "end_reentry");
            }
        }
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        ItemStack stack = event.getStack();
        if (stack == null || stack.isEmpty()) return;
        if (stack.getItem() != Items.DIAMOND) return;

        ProgressionData data = getData();
        int before = data.diamondsCollected;
        data.diamondsCollected += stack.getCount();
        data.setDirty();

        // 5個ごとに +1
        int beforeTier = before / 5;
        int afterTier = data.diamondsCollected / 5;
        int delta = afterTier - beforeTier;
        if (delta > 0) {
            addBonus(delta, "diamond_" + data.diamondsCollected);
        }
    }

    /** /test コマンド等から現在の進行度を確認するAPI */
    public static String describe() {
        ProgressionData d = getData();
        return String.format(
            "bonus=%d, diamonds=%d, nether=%s, end=%s",
            d.baseLevelBonus, d.diamondsCollected,
            d.hasEnteredNether, d.hasEnteredEnd);
    }

    // === SavedData ===

    public static class ProgressionData extends SavedData {
        public int baseLevelBonus = 0;
        public int diamondsCollected = 0;
        public boolean hasEnteredNether = false;
        public boolean hasEnteredEnd = false;

        public ProgressionData() {}

        public static ProgressionData load(CompoundTag tag) {
            ProgressionData d = new ProgressionData();
            d.baseLevelBonus = tag.getInt("baseLevelBonus");
            d.diamondsCollected = tag.getInt("diamondsCollected");
            d.hasEnteredNether = tag.getBoolean("hasEnteredNether");
            d.hasEnteredEnd = tag.getBoolean("hasEnteredEnd");
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("baseLevelBonus", baseLevelBonus);
            tag.putInt("diamondsCollected", diamondsCollected);
            tag.putBoolean("hasEnteredNether", hasEnteredNether);
            tag.putBoolean("hasEnteredEnd", hasEnteredEnd);
            return tag;
        }
    }
}
