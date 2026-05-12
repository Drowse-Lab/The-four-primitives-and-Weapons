package the_four_primitives_and_weapons.damage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性専用の持続ダメージ（DoT）システム。
 * Wither effectもwither DamageSourceも使わず、
 * 炎ダメージと同じtickベースで独自に直接ダメージを与える。
 *
 * 使い方:
 *   ElementalDoTHandler.apply(target, duration, dmgPerTick, ElementType.CORROSION);
 */
public class ElementalDoTHandler {

    /**
     * 持続ダメージのデータ。
     * duration と dmgPerTick はレベルに応じて呼び出し元で計算済みの値を受け取る。
     */
    private static class DoTEntry {
        int       remainingTick;
        float     dmgPerTick;
        ElementType element;

        DoTEntry(int remainingTick, float dmgPerTick, ElementType element) {
            this.remainingTick = remainingTick;
            this.dmgPerTick    = dmgPerTick;
            this.element       = element;
        }
    }

    // entityUUID → DoTEntry
    private static final Map<UUID, DoTEntry> dotMap = new ConcurrentHashMap<>();

    // ────────────────────────────────────────────────────────────────
    // 独自 DamageSource キー
    // ────────────────────────────────────────────────────────────────

    /**
     * 属性ごとの独自 DamageSource を生成する。
     * vanilla の DamageType を流用しつつ、メッセージIDで属性を区別する。
     *
     * ※ 1.20.1 では DamageType は data-driven なので、
     *   resources/data/the_four_primitives_and_weapons/damage_type/ に
     *   corrosion_dot.json / dark_dot.json を用意してください。
     *   （fallback として magic を使います）
     */
    public static DamageSource createDoTSource(LivingEntity target, ElementType element) {
        try {
            String typeId = "the_four_primitives_and_weapons:" + element.getName() + "_dot";
            ResourceKey<DamageType> key = ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation(typeId)
            );
            return new DamageSource(
                    target.level().registryAccess()
                          .registryOrThrow(Registries.DAMAGE_TYPE)
                          .getHolderOrThrow(key)
            );
        } catch (Exception e) {
            // data-driven DamageType が未登録の場合は magic にフォールバック
            return target.damageSources().magic();
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 公開API
    // ────────────────────────────────────────────────────────────────

    /**
     * 持続ダメージを付与する。
     * 既存のDoTがある場合は残り時間が長い方を採用し、ダメージは加算する。
     *
     * @param target     対象エンティティ
     * @param duration   持続tick数  (例: 60 = 3秒)
     * @param dmgPerTick 1tickあたりのダメージ (例: 0.5f)
     * @param element    属性タイプ（DamageSource の識別に使用）
     */
    public static void apply(LivingEntity target, int duration,
                             float dmgPerTick, ElementType element) {
        UUID id = target.getUUID();
        DoTEntry existing = dotMap.get(id);

        if (existing != null && existing.element == element) {
            // 残り時間は長い方を優先、ダメージは加算
            existing.remainingTick = Math.max(existing.remainingTick, duration);
            existing.dmgPerTick   += dmgPerTick;
        } else {
            dotMap.put(id, new DoTEntry(duration, dmgPerTick, element));
        }
    }

    /**
     * 対象エンティティのDoTを即時解除する。
     */
    public static void clear(LivingEntity target) {
        dotMap.remove(target.getUUID());
    }

    /**
     * 対象がDoT中かどうかを返す。
     */
    public static boolean isActive(LivingEntity target) {
        return dotMap.containsKey(target.getUUID());
    }

    // ────────────────────────────────────────────────────────────────
    // TickHandler
    // ────────────────────────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
    public static class DoTTickHandler {

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (event.getServer() == null) return;

            dotMap.entrySet().removeIf(entry -> {
                DoTEntry dot = entry.getValue();

                // tick消費
                dot.remainingTick--;
                if (dot.remainingTick <= 0) return true; // 終了 → 削除

                // エンティティを検索
                LivingEntity target = null;
                for (ServerLevel level : event.getServer().getAllLevels()) {
                    net.minecraft.world.entity.Entity e = level.getEntity(entry.getKey());
                    if (e instanceof LivingEntity living) {
                        target = living;
                        break;
                    }
                }
                if (target == null || !target.isAlive()) return true;

                // 独自 DamageSource で直接ダメージ
                DamageSource source = createDoTSource(target, dot.element);
                target.hurt(source, dot.dmgPerTick);

                return false;
            });
        }
    }
}
