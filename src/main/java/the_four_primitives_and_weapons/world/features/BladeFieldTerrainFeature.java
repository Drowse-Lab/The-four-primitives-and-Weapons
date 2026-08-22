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
            int surfaceY = surfaceHeight(x, z);
            int oldTop = context.level().getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

            // 地下を完全に詰めるため、ノイズ洞窟も峡谷も残らない。
            for (int y = minY + 1; y <= surfaceY - 4; y++)
                context.level().setBlock(cursor.set(x, y, z), Blocks.STONE.defaultBlockState(), 2);
            for (int y = surfaceY - 3; y < surfaceY; y++)
                context.level().setBlock(cursor.set(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
            context.level().setBlock(cursor.set(x, surfaceY, z), surfaceBlock(x, z).defaultBlockState(), 2);

            // 元のオーバーワールド地形が丘より高い場合は削って空にする。
            for (int y = surfaceY + 1; y <= oldTop + 1; y++)
                context.level().setBlock(cursor.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        // 高さマップの更新順に依存させず、整形済みの座標へ密集気味に16～27本生成する。
        RandomSource random = context.random();
        int count = 16 + random.nextInt(12);
        for (int i = 0; i < count; i++) {
            int x = baseX + random.nextInt(16), z = baseZ + random.nextInt(16);
            BladeFieldFeature.placeWeapon(context.level(), random, new BlockPos(x, surfaceHeight(x, z) + 1, z));
        }
        return true;
    }

    /** 大きな丘と小さな起伏を滑らかな値ノイズで合成。規則的な波模様にはならない。 */
    private static int surfaceHeight(int x, int z) {
        double broad = valueNoise(x, z, 112, 0x4B1D5EEDL);
        double local = valueNoise(x, z, 43, 0x71A9C3D2L);
        double detail = valueNoise(x, z, 21, 0x19E3779BL);
        return 68 + (int)Math.round(broad * 7.0D + local * 3.5D + detail * 1.2D);
    }

    /** 荒れた土主体。低頻度の砂利・根付いた土・ポドゾルで地表の単調さを崩す。 */
    private static Block surfaceBlock(int x, int z) {
        double patch = valueNoise(x, z, 18, 0x63D2A4F1L);
        if (patch > 0.62D) return Blocks.GRAVEL;
        if (patch < -0.70D) return Blocks.PODZOL;
        if (patch < -0.48D) return Blocks.ROOTED_DIRT;
        return Blocks.COARSE_DIRT;
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
