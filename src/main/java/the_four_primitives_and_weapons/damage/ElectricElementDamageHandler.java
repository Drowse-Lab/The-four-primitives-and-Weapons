package the_four_primitives_and_weapons.damage;

import the_four_primitives_and_weapons.util.VersionHelper;

import org.joml.Vector3f;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 電気/雷属性ダメージハンドラー
 * - 水中にいるとその周りにもダメージが入る
 * - 鉄防具などの導体を身につけているとダメージが増加
 * - configファイルにitemidを入れると導体として認識される
 */
public class ElectricElementDamageHandler {

    // 基礎ダメージ倍率
    private static final float BASE_DAMAGE_MULTIPLIER = 1.2f;
    // 水中でのダメージ倍率
    private static final float WATER_DAMAGE_MULTIPLIER = 1.5f;
    // 導体装備1つあたりのダメージ倍率
    private static final float CONDUCTOR_DAMAGE_MULTIPLIER = 0.3f;

    public static final TagKey<Item> CONDUCTIVE_ARMOR_TAG = TagKey.create(
            Registries.ITEM,
            new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "electric_conductive_armor"));

    // 導体となるアイテムのセット（デフォルト）
    private static final Set<String> CONDUCTOR_ITEMS = new HashSet<>();

    static {
        // デフォルトの導体アイテム（鉄装備など）
        CONDUCTOR_ITEMS.add("minecraft:iron_helmet");
        CONDUCTOR_ITEMS.add("minecraft:iron_chestplate");
        CONDUCTOR_ITEMS.add("minecraft:iron_leggings");
        CONDUCTOR_ITEMS.add("minecraft:iron_boots");
        CONDUCTOR_ITEMS.add("minecraft:chainmail_helmet");
        CONDUCTOR_ITEMS.add("minecraft:chainmail_chestplate");
        CONDUCTOR_ITEMS.add("minecraft:chainmail_leggings");
        CONDUCTOR_ITEMS.add("minecraft:chainmail_boots");
        CONDUCTOR_ITEMS.add("minecraft:golden_helmet");
        CONDUCTOR_ITEMS.add("minecraft:golden_chestplate");
        CONDUCTOR_ITEMS.add("minecraft:golden_leggings");
        CONDUCTOR_ITEMS.add("minecraft:golden_boots");
        CONDUCTOR_ITEMS.add("minecraft:iron_sword");
        CONDUCTOR_ITEMS.add("minecraft:iron_axe");
    }

    /**
     * Configから導体アイテムを追加
     * @param itemIds 追加する導体アイテムのID
     */
    public static void addConductorItems(List<String> itemIds) {
        CONDUCTOR_ITEMS.addAll(itemIds);
    }

    public static boolean isConductiveItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(CONDUCTIVE_ARMOR_TAG)) return true;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return false;
        if (CONDUCTOR_ITEMS.contains(itemId.toString())) return true;

        String path = itemId.getPath();
        return path.contains("iron")
                || path.contains("chainmail")
                || path.contains("chain")
                || path.contains("golden")
                || path.contains("gold")
                || path.contains("copper")
                || path.contains("steel")
                || path.contains("bronze")
                || path.contains("silver")
                || path.contains("electrum");
    }

    private static boolean isRegisteredConductorItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null) return false;
        return CONDUCTOR_ITEMS.contains(itemId.toString());
    }

    public static int countConductiveArmorPieces(LivingEntity entity) {
        int count = 0;
        for (ItemStack armor : entity.getArmorSlots()) {
            if (isConductiveItem(armor)) {
                count++;
            }
        }
        return count;
    }

    /**
     * エンティティが身につけている導体の数を取得
     */
    private static int getConductorCount(LivingEntity entity) {
        int count = 0;

        // 装備スロットをチェック
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (slot.getType() == EquipmentSlot.Type.ARMOR
                    ? isConductiveItem(stack)
                    : isRegisteredConductorItem(stack)) {
                count++;
            }
        }

        return count;
    }

    /**
     * 電気属性ダメージを計算
     * @param target ターゲットエンティティ
     * @param originalDamage 元のダメージ
     * @param elementLevel 属性レベル
     * @param source ダメージソース
     * @return 計算後のダメージ
     */
    public static float calculateDamage(LivingEntity target, float originalDamage, int elementLevel, DamageSource source) {
        float damageMultiplier = BASE_DAMAGE_MULTIPLIER;

        // 水中にいる場合
        if (target.isInWaterOrRain() || target.isInWaterRainOrBubble()) {
            damageMultiplier *= WATER_DAMAGE_MULTIPLIER;
            // 周囲へのダメージはElementalDamageEventで処理される
        }

        // 導体を身につけている場合
        int conductorCount = getConductorCount(target);
        if (conductorCount > 0) {
            damageMultiplier += (conductorCount * CONDUCTOR_DAMAGE_MULTIPLIER);
        }

        // 電気のパーティクルエフェクト（ターゲット本体）
        if (VersionHelper.getLevel(target) instanceof ServerLevel serverLevel) {
            // 電気スパーク
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                15, 0.3, 0.5, 0.3, 0.1);

            // 黄色い電気のdustパーティクル（稲妻色）
            DustParticleOptions yellowDust = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 0.3f), 1.0f);
            serverLevel.sendParticles(yellowDust,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                20, 0.3, 0.5, 0.3, 0.1);
        }

        return originalDamage * damageMultiplier;
    }

    /**
     * 電気属性ダメージを簡易適用する
     * @param target ターゲットエンティティ
     * @param damage ダメージ量
     * @param type 属性タイプ
     */
    public static void apply(LivingEntity target, double damage, ElementType type) {
        applyElectricDamage(target, (float) damage, null, 1);
    }

    /**
     * エンティティに電気属性ダメージを与える
     * @param target ターゲットエンティティ
     * @param damage ダメージ量
     * @param source ダメージソース元
     * @param level 属性レベル
     */
    public static void applyElectricDamage(LivingEntity target, float damage, LivingEntity source, int level) {
        // カスタム DamageType: the_four_primitives_and_weapons:electric
        DamageSource ds = ModDamageSources.ofElement(target.level(), ElementType.ELECTRIC, source);
        // ds が IElementalDamageSource を実装していない場合 ( magic フォールバック等 ) に
        //   ClassCastException で hurt() 前に落ちる ＝ ダメージが入らない / スキルが中断する
        //   のを防ぐため、 instanceof でガードする。
        if (ds instanceof IElementalDamageSource elementalSource) {
            elementalSource.setElementType(ElementType.ELECTRIC);
            elementalSource.setElementLevel(level);
        }

        // 直前の被弾による無敵時間で弾かれて「ダメージが入らない」 のを防ぐ
        //   ( 放電スキルは連続/同時ヒットしうるため )。
        target.invulnerableTime = 0;
        target.hurt(ds, damage);
    }

    public static float handleElectricDamage(LivingEntity attacker, LivingEntity target, ItemStack weapon, float baseDmg) {
        int level = ElementalDamageUtils.getElementLevel(weapon);
        DamageSource ds = ModDamageSources.ofElement(target.level(), ElementType.ELECTRIC, attacker);
        float damage = calculateDamage(target, baseDmg, level, ds);
        applyElectricDamage(target, damage - baseDmg, attacker, level);
        return damage;
    }
}
