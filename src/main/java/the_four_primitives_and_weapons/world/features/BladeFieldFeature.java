package the_four_primitives_and_weapons.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.WorldGenLevel;
import the_four_primitives_and_weapons.entity.StabbedWeaponEntity;
import the_four_primitives_and_weapons.util.KatanaFittings;
import the_four_primitives_and_weapons.events.StabWeaponHandler;
import the_four_primitives_and_weapons.damage.ElementType;
import the_four_primitives_and_weapons.damage.ElementalDamageUtils;
import net.minecraftforge.registries.ForgeRegistries;

/** 既存の「戦地設営の杭」と同じ刺さった武器エンティティを剣の原へ生成する。 */
public class BladeFieldFeature extends Feature<NoneFeatureConfiguration> {
    private static final String MOD = "the_four_primitives_and_weapons:";
    private static final String[] COMMON_WEAPONS = {
            "minecraft:stone_sword", "minecraft:iron_sword", "minecraft:golden_sword",
            MOD + "stone_katana", MOD + "iron_katana", MOD + "gold_katana",
            MOD + "iron_tyokuto", MOD + "gold_tyokuto",
            MOD + "stone_rapier", MOD + "iron_rapier", MOD + "gold_rapier",
            MOD + "warabitetou", MOD + "ninjatou"
    };
    private static final String[] RARE_WEAPONS = {
            "minecraft:diamond_sword", "minecraft:netherite_sword",
            MOD + "diamond_katana", MOD + "netherite_katana",
            MOD + "diamond_tyokuto", MOD + "netherite_tyokuto",
            MOD + "diamond_rapier", MOD + "netherite_rapier",
            MOD + "diamond_dagger", MOD + "netherite_dagger",
            MOD + "prototype_katana"
    };
    /** weapon_stats.json で durability:0 の武器。通常抽選には絶対に混ぜない。 */
    private static final String[] TIMELESS_WEAPONS = {
            MOD + "wither_katana", MOD + "darkness_katana", MOD + "rivers_of_blood",
            MOD + "replica_sword_of_light", MOD + "sword_of_night", MOD + "luna"
    };

    public BladeFieldFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return placeWeapon(context.level(), context.random(), context.origin());
    }

    /** 地形整形後の確定した地表座標へ武器を置く。 */
    public static boolean placeWeapon(WorldGenLevel level, RandomSource random, BlockPos pos) {
        if (!level.getBlockState(pos).canBeReplaced() ||
                !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return false;
        // 耐久なし武器は高い丘の頂上、かつ1/64成功時だけ。通常・希少抽選から完全分離する。
        boolean timelessCondition = pos.getY() - 1 >= 75 && random.nextInt(64) == 0;
        String[] pool = timelessCondition ? TIMELESS_WEAPONS
                : (random.nextInt(12) == 0 ? RARE_WEAPONS : COMMON_WEAPONS);
        net.minecraft.world.item.Item selected = ForgeRegistries.ITEMS.getValue(new ResourceLocation(pool[random.nextInt(pool.length)]));
        if (selected == null) return false;
        ItemStack weapon = new ItemStack(selected);
        randomizeFittings(weapon, random);
        String biome = level.getBiome(pos).unwrapKey().map(k -> k.location().getPath()).orElse("blade_field");
        ElementType element = elementForBiome(biome);
        if (element != null) ElementalDamageUtils.setElement(weapon, element, 1 + random.nextInt(3));
        // 通常荒地は比較的状態が良い。属性地帯は長く晒された残り耐久1～8。
        if (weapon.isDamageableItem()) {
            int remaining = element == null
                    ? Math.max(1, (int)(weapon.getMaxDamage() * (0.25D + random.nextDouble() * 0.35D)))
                    : 1 + random.nextInt(Math.min(8, weapon.getMaxDamage()));
            weapon.setDamageValue(weapon.getMaxDamage() - remaining);
        }
        weapon.getOrCreateTag().putBoolean(StabWeaponHandler.TAG_NATURAL_BLADE_FIELD_WEAPON, true);
        StabbedWeaponEntity entity = new StabbedWeaponEntity(level.getLevel());
        entity.setItem(weapon);
        entity.setStabYaw(random.nextFloat() * 360.0F);
        entity.setRoll(-18.0F + random.nextFloat() * 36.0F);

        // 自然生成分はすべて地面へ刺す。空中の剣は次元移動の儀式専用。
        // 傾きは4～30度。寝かせすぎて鍔まで埋まる姿勢は作らない。
        entity.setTilt(4.0F + random.nextFloat() * 26.0F);
        entity.setScale(0.85F + random.nextFloat() * 0.3F);
        entity.setRadius(1.0F);
        // 刺さる深さをランダム化。0.03～0.20なら切先は浮かず、鍔は地表より上に残る。
        double embedHeight = 0.03D + random.nextDouble() * 0.17D;
        entity.moveTo(pos.getX() + 0.5D, pos.getY() + embedHeight, pos.getZ() + 0.5D, 0.0F, 0.0F);
        return level.addFreshEntity(entity);
    }

    private static ElementType elementForBiome(String biome) {
        return switch (biome) {
            case "blade_field_fire" -> ElementType.FIRE;
            case "blade_field_ice" -> ElementType.ICE;
            case "blade_field_thunder" -> ElementType.THUNDER;
            case "blade_field_water" -> ElementType.WATER;
            case "blade_field_blood" -> ElementType.BLOOD;
            case "blade_field_wind" -> ElementType.WIND;
            case "blade_field_corrosion" -> ElementType.CORROSION;
            default -> null;
        };
    }

    /** 拵え対応武器だけ、プレイヤーが拵え台で行えるのと同じ着色・意匠変更を適用する。 */
    private static void randomizeFittings(ItemStack weapon, RandomSource random) {
        if (!KatanaFittings.isFittingWeapon(weapon)) return;
        DyeColor[] dyes = DyeColor.values();
        KatanaFittings.setTsuka(weapon, KatanaFittings.dyeRgb(dyes[random.nextInt(dyes.length)]));
        KatanaFittings.setTsuba(weapon, KatanaFittings.dyeRgb(dyes[random.nextInt(dyes.length)]));
        KatanaFittings.setKashira(weapon, KatanaFittings.dyeRgb(dyes[random.nextInt(dyes.length)]));
        KatanaFittings.setFuchi(weapon, KatanaFittings.dyeRgb(dyes[random.nextInt(dyes.length)]));
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(weapon.getItem());
        String path = id == null ? "" : id.getPath();
        if (path.contains("rapier")) {
            // レイピアは刀用 tuka/tuba ではなく専用テクスチャ名を使う。
            if (random.nextBoolean()) KatanaFittings.setTsukaWrap(weapon, "grip_b");
            if (random.nextBoolean()) KatanaFittings.setTsubaStyle(weapon, "guard_b");
            if (random.nextBoolean()) KatanaFittings.setKashiraStyle(weapon, "pommel_b");
        } else if (!path.contains("tyokuto")) {
            // 直刀は現状、柄・鍔・頭の色だけを変える。刀用UVの柄巻きは混ぜない。
            if (random.nextBoolean())
                KatanaFittings.setTsukaWrap(weapon, KatanaFittings.WRAPS[random.nextInt(KatanaFittings.WRAPS.length)]);
            if (random.nextBoolean())
                KatanaFittings.setTsubaStyle(weapon, KatanaFittings.TSUBAS[random.nextInt(KatanaFittings.TSUBAS.length)]);
            if (random.nextBoolean())
                KatanaFittings.setKashiraStyle(weapon, KatanaFittings.KASHIRAS[random.nextInt(KatanaFittings.KASHIRAS.length)]);
            if (random.nextBoolean())
                KatanaFittings.setFuchiStyle(weapon, KatanaFittings.FUCHIS[random.nextInt(KatanaFittings.FUCHIS.length)]);
        }
    }
}
