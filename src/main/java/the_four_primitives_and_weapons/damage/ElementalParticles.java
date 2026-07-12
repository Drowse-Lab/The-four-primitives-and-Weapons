package the_four_primitives_and_weapons.damage;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import org.joml.Vector3f;

/**
 * 属性ごとの「その属性らしいパーティクル」を任意座標に出す共通ユーティリティ。
 *
 * <p>攻撃ヒット時 ({@link the_four_primitives_and_weapons.ElementalDamageEvent}) と
 * 通常攻撃のスイング時、 スキル発動時 ({@code MotionExecutor}) から呼ばれ、
 * 「攻撃に載っている属性のパーティクルが攻撃に付く」表現を一元化する。</p>
 *
 * <p>表現方針:
 * <ul>
 *   <li>炎 / 魂 / 燐火 … 専用パーティクル ( FLAME / SOUL / SOUL_FIRE_FLAME ) が
 *       その属性のアイデンティティなので themed 粒子のみで表現する。</li>
 *   <li>それ以外の属性 … <b>色を変えた dust</b> が主役。
 *       氷 / 電気 / 雷 / 水 / 血 だけは質感を出すため、専用パーティクル
 *       ( 雪片 / 電光 / 水しぶき / レッドストーンブロックの破片 ) を少量アクセントに重ねる。</li>
 * </ul></p>
 */
public final class ElementalParticles {

    /** ヒット/スイング時の既定の散布幅 ( 横 )。 */
    private static final double DEFAULT_SPREAD_XZ = 0.25;
    /** ヒット/スイング時の既定の散布幅 ( 縦 )。 */
    private static final double DEFAULT_SPREAD_Y = 0.30;

    /** dust の粒サイズ。 */
    private static final float DUST_SIZE = 1.3f;

    /** 血属性のアクセント: レッドストーンブロックの破壊パーティクル ( 赤い破片 )。 */
    private static final BlockParticleOption BLOOD_CHUNK =
            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState());

    private ElementalParticles() {}

    /**
     * 専用パーティクルで表現する属性か。
     * ( 炎 / 魂 / 燐火 のみ。 それ以外は色付き dust で表現する )
     */
    public static boolean usesThemedParticles(ElementType type) {
        return type == ElementType.FIRE
            || type == ElementType.SOUL
            || type == ElementType.SOUL_FIRE;
    }

    /** 属性の色 ( dust 用 )。 {@code NONE} は null。 */
    public static Vector3f colorOf(ElementType type) {
        if (type == null) return null;
        switch (type) {
            case ICE:       return new Vector3f(0.55f, 0.85f, 1.00f);  // 淡い水色
            case ELECTRIC:  return new Vector3f(1.00f, 0.95f, 0.40f);  // 黄色い電光
            case THUNDER:   return new Vector3f(0.80f, 0.90f, 1.00f);  // 青白い雷光
            case WATER:     return new Vector3f(0.20f, 0.50f, 1.00f);  // 青
            case WIND:      return new Vector3f(0.75f, 1.00f, 0.85f);  // 淡い緑白
            case HOLY:      return new Vector3f(1.00f, 0.97f, 0.72f);  // 金白
            case DARK:      return new Vector3f(0.16f, 0.10f, 0.22f);  // 黒紫
            case ERASURE:   return new Vector3f(0.38f, 0.06f, 0.55f);  // 消滅の濃紫
            case BLOOD:     return new Vector3f(0.55f, 0.00f, 0.02f);  // 血の赤
            case CORROSION: return new Vector3f(0.75f, 0.10f, 0.55f);  // 侵食の赤紫
            case MIASMA:    return new Vector3f(0.45f, 0.10f, 0.70f);  // 瘴気の紫
            case FIRE:      return new Vector3f(1.00f, 0.55f, 0.15f);  // ( themed だが色も持つ )
            case SOUL:      return new Vector3f(0.45f, 0.90f, 0.95f);
            case SOUL_FIRE: return new Vector3f(0.30f, 0.80f, 1.00f);
            default:        return null;                                // NONE
        }
    }

    /** 属性色の dust。 {@code NONE} は null。 */
    public static DustParticleOptions dustOf(ElementType type) {
        Vector3f color = colorOf(type);
        return color == null ? null : new DustParticleOptions(color, DUST_SIZE);
    }

    /**
     * 属性 {@code type} のパーティクルを (x,y,z) に出す。 spread は控えめ。
     */
    public static void spawn(ServerLevel sl, ElementType type, double x, double y, double z, int count) {
        emit(sl, type, x, y, z, count, DEFAULT_SPREAD_XZ, DEFAULT_SPREAD_Y);
    }

    /**
     * 散布幅を指定して出す。 斬撃の弧に沿って属性色を広く撒くとき ( スキル ) に使う。
     *
     * @param spreadXZ 横方向の散布幅 ( 弧の広がりに合わせて大きめに )
     * @param spreadY  縦方向の散布幅 ( 斬撃の軌跡なので控えめに )
     */
    public static void spawnWide(ServerLevel sl, ElementType type, double x, double y, double z,
                                 int count, double spreadXZ, double spreadY) {
        emit(sl, type, x, y, z, count, spreadXZ, spreadY);
    }

    private static void emit(ServerLevel sl, ElementType type, double x, double y, double z,
                             int count, double dxz, double dy) {
        if (sl == null || type == null || type == ElementType.NONE) return;
        int n = Math.max(1, count);
        int main = n + Math.max(1, n / 2);     // 主役の粒子は多め (約 1.5 倍)
        int sub = Math.max(2, n / 2);          // 添えの粒子

        // 炎 / 魂 / 燐火 以外は「色を変えた dust」が主役。
        // ただし 氷 / 電気 / 雷 / 水 / 血 は dust だけだと質感が出ないので、
        // 専用パーティクルをアクセントとして少量重ねる。
        if (!usesThemedParticles(type)) {
            sl.sendParticles(dustOf(type), x, y, z, main, dxz, dy, dxz, 0.01);
            switch (type) {
                case ICE:
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, sub, dxz, dy, dxz, 0.02);
                    break;
                case ELECTRIC:
                case THUNDER:
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, sub, dxz, dy, dxz, 0.06);
                    break;
                case WATER:
                    sl.sendParticles(ParticleTypes.SPLASH, x, y, z, sub, dxz, dy, dxz, 0.05);
                    break;
                case BLOOD:
                    sl.sendParticles(BLOOD_CHUNK, x, y, z, sub, dxz, dy, dxz, 0.05);
                    break;
                default:
                    break;
            }
            return;
        }

        switch (type) {
            case FIRE:
                sl.sendParticles(ParticleTypes.FLAME, x, y, z, main, dxz, dy, dxz, 0.02);
                break;
            case SOUL:
                // 魂: 青白い魂の霊気 (SOUL) + 青みの強い SCULK_SOUL でしっかり見えるように。
                sl.sendParticles(ParticleTypes.SOUL, x, y, z, main, dxz, dy, dxz, 0.02);
                sl.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, sub, dxz, dy, dxz, 0.015);
                break;
            case SOUL_FIRE:
                // 燐火: 青い魂炎 (SOUL_FIRE_FLAME) を主役に、魂の霊気 (SOUL) を添える。
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, main, dxz, dy, dxz, 0.02);
                sl.sendParticles(ParticleTypes.SOUL, x, y, z, sub, dxz, dy, dxz, 0.02);
                break;
            default:
                break;
        }
    }
}
