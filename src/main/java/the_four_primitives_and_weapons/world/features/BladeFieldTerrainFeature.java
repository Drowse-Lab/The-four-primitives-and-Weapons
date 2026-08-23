package the_four_primitives_and_weapons.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import the_four_primitives_and_weapons.init.BladeCrystalInit;

/** 洞窟を埋め、緩い丘だけの荒れた土の剣原へチャンク地形を整形する。 */
public class BladeFieldTerrainFeature extends Feature<NoneFeatureConfiguration> {
    public BladeFieldTerrainFeature(Codec<NoneFeatureConfiguration> codec) { super(codec); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        int baseX = context.origin().getX() & ~15;
        int baseZ = context.origin().getZ() & ~15;
        int minY = context.level().getMinBuildHeight();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = baseX + dx, z = baseZ + dz;
            int normalSurfaceY = surfaceHeight(x, z);
            boolean waterBiome = isBiome(context.level(), x, normalSurfaceY, z, "blade_field_water");
            double marsh = waterBiome ? valueNoise(x, z, 13, 0x0A2E5E11L) : -1.0D;
            boolean pool = waterBiome && marsh > -0.18D;
            // 湿原の水底は67～68、水のない畦は69。水面を常にY=69へそろえて滝状の流出を防ぐ。
            int surfaceY = waterBiome ? (pool ? 67 + (marsh > 0.48D ? 1 : 0) : 69) : normalSurfaceY;
            int oldTop = context.level().getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            boolean corrosion = isCorrosion(context.level(), x, surfaceY, z);

            // 地下を完全に詰めるため、ノイズ洞窟も峡谷も残らない。
            for (int y = minY + 1; y <= surfaceY - 4; y++)
                context.level().setBlock(cursor.set(x, y, z), Blocks.STONE.defaultBlockState(), 2);
            for (int y = surfaceY - 3; y < surfaceY; y++)
                context.level().setBlock(cursor.set(x, y, z),
                        (corrosion ? Blocks.CALCITE : Blocks.DIRT).defaultBlockState(), 2);
            context.level().setBlock(cursor.set(x, surfaceY, z), surfaceBlock(context.level(), x, surfaceY, z).defaultBlockState(), 2);

            // 元のオーバーワールド地形が丘より高い場合は削って空にする。
            for (int y = surfaceY + 1; y <= oldTop + 1; y++)
                context.level().setBlock(cursor.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);

            // 水地帯は尾瀬のような浅い湿原。泥の窪地へ一段だけ水を張り、所々に水草を置く。
            if (waterBiome) {
                if (pool) {
                    context.level().setBlock(cursor.set(x, surfaceY, z), Blocks.MUD.defaultBlockState(), 2);
                    for (int wy = surfaceY + 1; wy <= 69; wy++)
                        context.level().setBlock(cursor.set(x, wy, z), Blocks.WATER.defaultBlockState(), 2);
                    // 約3%だけ。連続したスイレンの絨毯にしない。
                    if (hash(x, z, 0x51A7E11DL) > 0.94D)
                        context.level().setBlock(cursor.set(x, 70, z), Blocks.LILY_PAD.defaultBlockState(), 2);
                } else if (marsh < -0.63D) {
                    context.level().setBlock(cursor.set(x, surfaceY, z), Blocks.MOSS_BLOCK.defaultBlockState(), 2);
                    context.level().setBlock(cursor.set(x, surfaceY + 1, z), Blocks.FERN.defaultBlockState(), 2);
                } else if (marsh < -0.42D) {
                    context.level().setBlock(cursor.set(x, surfaceY, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    context.level().setBlock(cursor.set(x, surfaceY + 1, z), Blocks.GRASS.defaultBlockState(), 2);
                }
            }
        }
        // 高さマップの更新順に依存させず、整形済みの座標へ密集気味に16～27本生成する。
        RandomSource random = context.random();
        // 侵食地帯では複数色の巨大結晶が地中から突き出す。地表を結晶ブロックで敷き詰めない。
        for (int i = 0; i < 2; i++) {
            int x = baseX + random.nextInt(16), z = baseZ + random.nextInt(16);
            int y = terrainHeight(context.level(), x, z);
            if (!isCorrosion(context.level(), x, y, z)) continue;
            Block[] colors = {BladeCrystalInit.VIOLET.get(), BladeCrystalInit.CYAN.get(),
                    BladeCrystalInit.AMBER.get(), BladeCrystalInit.CRIMSON.get()};
            Block crystal = colors[random.nextInt(colors.length)];
            int height = 14 + random.nextInt(13);
            for (int h = 0; h < height; h++) {
                double taper = 1.0D - h / (double)height;
                int radius = taper > 0.72D ? 3 : taper > 0.38D ? 2 : taper > 0.12D ? 1 : 0;
                for (int ox = -radius; ox <= radius; ox++) for (int oz = -radius; oz <= radius; oz++)
                    if (ox * ox + oz * oz <= radius * radius)
                        context.level().setBlock(cursor.set(x + ox, y + h, z + oz), crystal.defaultBlockState(), 2);
            }
            // 根元から鋭角に伸びる、色違いの副結晶。先端ほど細くして棘状にする。
            for (int branch = 0; branch < 4 + random.nextInt(4); branch++) {
                int sx = random.nextBoolean() ? 1 : -1, sz = random.nextBoolean() ? 1 : -1;
                int length = 6 + random.nextInt(8);
                Block branchCrystal = colors[random.nextInt(colors.length)];
                for (int n = 1; n <= length; n++) {
                    int bx = x + sx * n / 2, bz = z + sz * n / 2, by = y + n;
                    context.level().setBlock(cursor.set(bx, by, bz), branchCrystal.defaultBlockState(), 2);
                    if (n < length / 2)
                        context.level().setBlock(cursor.set(bx + sx, by, bz), branchCrystal.defaultBlockState(), 2);
                }
            }
        }
        int count = 16 + random.nextInt(12);
        for (int i = 0; i < count; i++) {
            int x = baseX + random.nextInt(16), z = baseZ + random.nextInt(16);
            BladeFieldFeature.placeWeapon(context.level(), random, new BlockPos(x, terrainHeight(context.level(), x, z) + 1, z));
        }
        return true;
    }

    /** 大きな丘と小さな起伏を滑らかな値ノイズで合成。規則的な波模様にはならない。 */
    /** 転移処理も同じ確定地表高を参照する。 */
    public static int surfaceHeight(int x, int z) {
        double broad = valueNoise(x, z, 112, 0x4B1D5EEDL);
        double local = valueNoise(x, z, 43, 0x71A9C3D2L);
        double detail = valueNoise(x, z, 21, 0x19E3779BL);
        return 68 + (int)Math.round(broad * 7.0D + local * 3.5D + detail * 1.2D);
    }

    private static int terrainHeight(net.minecraft.world.level.WorldGenLevel level, int x, int z) {
        int normal = surfaceHeight(x, z);
        if (!isBiome(level, x, normal, z, "blade_field_water")) return normal;
        double marsh = valueNoise(x, z, 13, 0x0A2E5E11L);
        return marsh > -0.18D ? 67 + (marsh > 0.48D ? 1 : 0) : 69;
    }

    /** 荒れた土主体。低頻度の砂利・根付いた土・ポドゾルで地表の単調さを崩す。 */
    private static Block surfaceBlock(net.minecraft.world.level.WorldGenLevel level, int x, int y, int z) {
        double patch = valueNoise(x, z, 18, 0x63D2A4F1L);
        String biome = level.getBiome(new BlockPos(x, y, z)).unwrapKey()
                .map(k -> k.location().getPath()).orElse("blade_field");
        switch (biome) {
            case "blade_field_fire": return patch > 0.45D ? Blocks.MAGMA_BLOCK : Blocks.NETHERRACK;
            case "blade_field_ice": return patch > 0.25D ? Blocks.SNOW_BLOCK : Blocks.PACKED_ICE;
            case "blade_field_thunder": return patch > 0.15D ? Blocks.GRAVEL : Blocks.STONE;
            case "blade_field_water": return patch > 0.20D ? Blocks.CLAY : Blocks.MUD;
            case "blade_field_blood": return patch > 0.42D ? BladeCrystalInit.COAGULATED_BLOOD.get()
                    : patch > -0.72D ? BladeCrystalInit.BLOOD_SOAKED_EARTH.get() : Blocks.SOUL_SOIL;
            case "blade_field_wind": return patch > 0.10D ? Blocks.SAND : Blocks.GRAVEL;
            case "blade_field_corrosion": return patch > -0.68D ? BladeCrystalInit.CORRODED_EARTH.get()
                    : Blocks.CALCITE;
        }
        if (patch > 0.62D) return Blocks.GRAVEL;
        if (patch < -0.70D) return Blocks.PODZOL;
        if (patch < -0.48D) return Blocks.ROOTED_DIRT;
        return Blocks.COARSE_DIRT;
    }

    private static boolean isCorrosion(net.minecraft.world.level.WorldGenLevel level, int x, int y, int z) {
        return isBiome(level, x, y, z, "blade_field_corrosion");
    }

    private static boolean isBiome(net.minecraft.world.level.WorldGenLevel level, int x, int y, int z, String path) {
        return level.getBiome(new BlockPos(x, y, z)).unwrapKey()
                .map(key -> key.location().getPath().equals(path)).orElse(false);
    }

    private static double valueNoise(int x, int z, int scale, long salt) {
        int x0 = Math.floorDiv(x, scale), z0 = Math.floorDiv(z, scale);
        double fx = Math.floorMod(x, scale) / (double)scale;
        double fz = Math.floorMod(z, scale) / (double)scale;
        fx = fx * fx * (3.0D - 2.0D * fx);
        fz = fz * fz * (3.0D - 2.0D * fz);
        double a = lerp(hash(x0, z0, salt), hash(x0 + 1, z0, salt), fx);
        double b = lerp(hash(x0, z0 + 1, salt), hash(x0 + 1, z0 + 1, salt), fx);
        return lerp(a, b, fz);
    }

    private static double hash(int x, int z, long salt) {
        long n = salt ^ (x * 341873128712L) ^ (z * 132897987541L);
        n = (n ^ (n >>> 30)) * 0xBF58476D1CE4E5B9L;
        n = (n ^ (n >>> 27)) * 0x94D049BB133111EBL;
        n ^= n >>> 31;
        return ((n >>> 11) * 0x1.0p-53) * 2.0D - 1.0D;
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}
