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
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = baseX + dx, z = baseZ + dz;
            int normalSurfaceY = surfaceHeight(x, z);
            String biome = biomePath(context.level(), x, normalSurfaceY, z);
            boolean waterBiome = biome.equals("blade_field_water");
            double marsh = waterBiome ? valueNoise(x, z, 13, 0x0A2E5E11L) : -1.0D;
            boolean pool = isWaterPool(context.level(), x, z, marsh);
            // 湿原の水底は67～68、水のない畦は69。水面を常にY=69へそろえて滝状の流出を防ぐ。
            int surfaceY = waterBiome ? (pool ? 67 + (marsh > 0.48D ? 1 : 0) : 69) : normalSurfaceY;
            int oldTop = context.level().getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            boolean corrosion = biome.equals("blade_field_corrosion");

            // 地表を支える範囲だけを埋める。全高度の走査はチャンク生成負荷が非常に大きい。
            for (int y = surfaceY - 16; y <= surfaceY - 4; y++) {
                cursor.set(x, y, z);
                if (!context.level().getBlockState(cursor).is(Blocks.STONE))
                    context.level().setBlock(cursor, Blocks.STONE.defaultBlockState(), 2);
            }
            Block fill = corrosion ? Blocks.CALCITE : Blocks.DIRT;
            for (int y = surfaceY - 3; y < surfaceY; y++) {
                cursor.set(x, y, z);
                if (!context.level().getBlockState(cursor).is(fill))
                    context.level().setBlock(cursor, fill.defaultBlockState(), 2);
            }
            Block surface = surfaceBlock(biome, x, z);
            cursor.set(x, surfaceY, z);
            if (!context.level().getBlockState(cursor).is(surface))
                context.level().setBlock(cursor, surface.defaultBlockState(), 2);

            // 元のオーバーワールド地形が丘より高い場合は削って空にする。
            for (int y = surfaceY + 1; y <= oldTop + 1; y++) {
                cursor.set(x, y, z);
                if (!context.level().getBlockState(cursor).isAir())
                    context.level().setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
            }

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
        // 高さマップの更新順に依存させず、整形済みの座標へ生成する。
        RandomSource random = context.random();
        // 大型結晶群は低頻度にし、生成時の大量ブロック更新を抑える。
        if (random.nextInt(8) == 0) {
            int x = baseX + 4 + random.nextInt(8), z = baseZ + 4 + random.nextInt(8);
            int y = terrainHeight(context.level(), x, z);
            if (isCorrosion(context.level(), x, y, z)) {
                Block[] colors = {BladeCrystalInit.VIOLET.get(), BladeCrystalInit.CYAN.get(),
                        BladeCrystalInit.AMBER.get(), BladeCrystalInit.CRIMSON.get()};
                Block[] glass = {Blocks.PURPLE_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS,
                        Blocks.ORANGE_STAINED_GLASS, Blocks.RED_STAINED_GLASS};
            // 中央の主晶柱。根元4～5ブロック幅から段階的に細くなり、最後は1ブロックの切先になる。
            int mainColor = random.nextInt(colors.length);
            placeCrystalSpike(context.level(), cursor, random, x, y, z, 0, 0,
                    26 + random.nextInt(15), 3, colors[mainColor], glass[mainColor]);

            // 周囲へ大小の晶柱を束状に生やし、アメジストクラスター風の輪郭を作る。
            int satellites = 4 + random.nextInt(4);
            for (int spike = 0; spike < satellites; spike++) {
                double angle = Math.PI * 2.0D * spike / satellites + random.nextDouble() * 0.45D;
                int distance = 2 + random.nextInt(6);
                int sx = x + (int)Math.round(Math.cos(angle) * distance);
                int sz = z + (int)Math.round(Math.sin(angle) * distance);
                int leanX = Integer.signum(sx - x), leanZ = Integer.signum(sz - z);
                int color = random.nextInt(colors.length);
                placeCrystalSpike(context.level(), cursor, random, sx, terrainHeight(context.level(), sx, sz), sz,
                        leanX, leanZ, 9 + random.nextInt(15), 1 + random.nextInt(2), colors[color], glass[color]);
            }
            }
        }
        // 武器は永続エンティティなので、チャンクごとの数を抑えて生成後の負荷も軽減する。
        int count = 4 + random.nextInt(4);
        for (int i = 0; i < count; i++) {
            int x = baseX + random.nextInt(16), z = baseZ + random.nextInt(16);
            BladeFieldFeature.placeWeapon(context.level(), random, new BlockPos(x, terrainHeight(context.level(), x, z) + 1, z));
        }
        return true;
    }

    /** ブロックだけで作るアメジスト晶柱。中心を少しずつずらし、根元から先端へ段階的に絞る。 */
    private static void placeCrystalSpike(net.minecraft.world.level.WorldGenLevel level, BlockPos.MutableBlockPos cursor,
                                          RandomSource random, int baseX, int baseY, int baseZ,
                                          int leanX, int leanZ, int height, int baseRadius,
                                          Block crystal, Block glass) {
        for (int h = 0; h < height; h++) {
            double progress = h / (double)Math.max(1, height - 1);
            int radius = progress < 0.20D ? baseRadius
                    : progress < 0.72D ? Math.max(1, baseRadius - 1)
                    : progress < 0.90D ? 1 : 0;
            int cx = baseX + leanX * h / 5;
            int cz = baseZ + leanZ * h / 5;
            for (int ox = -radius; ox <= radius; ox++) for (int oz = -radius; oz <= radius; oz++) {
                // 角を落とした八角形断面。立方体の塊ではなく結晶柱らしく見せる。
                if (Math.abs(ox) + Math.abs(oz) > radius * 2 - (radius > 1 ? 1 : 0)) continue;
                boolean exposed = Math.abs(ox) == radius || Math.abs(oz) == radius;
                Block material = exposed && h > 1 && random.nextInt(9) == 0 ? glass : crystal;
                level.setBlock(cursor.set(cx + ox, baseY + h, cz + oz), material.defaultBlockState(), 2);
            }
        }
        // 一番上を必ず単独ブロックにして、平らな頭を残さない。
        int tipX = baseX + leanX * height / 5;
        int tipZ = baseZ + leanZ * height / 5;
        level.setBlock(cursor.set(tipX, baseY + height, tipZ), crystal.defaultBlockState(), 2);
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
        return isWaterPool(level, x, z, marsh) ? 67 + (marsh > 0.48D ? 1 : 0) : 69;
    }

    /** 水域の外周を最低1ブロックの水バイオーム陸地で囲み、境界崩壊を防ぐ。 */
    private static boolean isWaterPool(net.minecraft.world.level.WorldGenLevel level, int x, int z, double marsh) {
        if (marsh <= -0.18D) return false;
        int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] side : sides) {
            int nx = x + side[0], nz = z + side[1], ny = surfaceHeight(nx, nz);
            if (!isBiome(level, nx, ny, nz, "blade_field_water")) return false;
        }
        return true;
    }

    /** 荒れた土主体。低頻度の砂利・根付いた土・ポドゾルで地表の単調さを崩す。 */
    private static Block surfaceBlock(String biome, int x, int z) {
        double patch = valueNoise(x, z, 18, 0x63D2A4F1L);
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
        return biomePath(level, x, y, z).equals(path);
    }

    private static String biomePath(net.minecraft.world.level.WorldGenLevel level, int x, int y, int z) {
        return level.getBiome(new BlockPos(x, y, z)).unwrapKey()
                .map(key -> key.location().getPath()).orElse("blade_field");
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
