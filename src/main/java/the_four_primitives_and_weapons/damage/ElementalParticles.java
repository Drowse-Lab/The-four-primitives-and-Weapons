package the_four_primitives_and_weapons.damage;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import org.joml.Vector3f;

/**
 * 属性ごとの「その属性らしいパーティクル」を任意座標に出す共通ユーティリティ。
 *
 * <p>攻撃ヒット時 ({@link the_four_primitives_and_weapons.ElementalDamageEvent}) と
 * 通常攻撃のスイング時 ({@code ChargedAttackHandler.performNormalAttack}) の両方から呼ばれ、
 * 「攻撃に載っている属性のパーティクルが攻撃に付く」表現を一元化する。</p>
 */
public final class ElementalParticles {

    private ElementalParticles() {}

    /**
     * 属性 {@code type} のパーティクルを (x,y,z) に出す。 spread は控えめ。
     *
     * <p>「属性らしい themed 粒子 (flame / snowflake / soul 等) を多め、 dust (色付き粉) を控えめ」
     * に配分する。 dust が多いと攻撃が砂っぽく見えるため、 themed を主役にする。</p>
     */
    public static void spawn(ServerLevel sl, ElementType type, double x, double y, double z, int count) {
        if (sl == null || type == null || type == ElementType.NONE) return;
        int n = Math.max(1, count);
        int themed = n + Math.max(1, n / 2);   // 属性らしい粒子は多め (約 1.5 倍)
        int dust = Math.max(1, n / 2);          // dust は控えめ (約半分)
        int dustSub = Math.max(1, dust / 2);

        switch (type) {
            case ICE:
                sl.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, themed, 0.25, 0.3, 0.25, 0.02);
                break;
            case ELECTRIC:
            case THUNDER:
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, themed, 0.32, 0.32, 0.32, 0.06);
                break;
            case FIRE:
                sl.sendParticles(ParticleTypes.FLAME, x, y, z, themed, 0.22, 0.28, 0.22, 0.02);
                break;
            case WATER:
                sl.sendParticles(ParticleTypes.SPLASH, x, y, z, themed, 0.3, 0.3, 0.3, 0.05);
                break;
            case WIND:
                sl.sendParticles(ParticleTypes.CLOUD, x, y, z, themed, 0.35, 0.2, 0.35, 0.03);
                break;
            case HOLY:
                sl.sendParticles(ParticleTypes.END_ROD, x, y, z, themed, 0.25, 0.3, 0.25, 0.02);
                break;
            case DARK:
                sl.sendParticles(ParticleTypes.SMOKE, x, y, z, themed, 0.25, 0.3, 0.25, 0.01);
                sl.sendParticles(ParticleTypes.SQUID_INK, x, y, z, Math.max(1, n / 3), 0.2, 0.25, 0.2, 0.01);
                break;
            case SOUL:
                // 魂: 青白い魂の霊気 (SOUL) + 青みの強い SCULK_SOUL でしっかり見えるように。
                sl.sendParticles(ParticleTypes.SOUL, x, y, z, themed, 0.25, 0.3, 0.25, 0.02);
                sl.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, Math.max(2, n / 2), 0.2, 0.28, 0.2, 0.015);
                break;
            case SOUL_FIRE:
                // 燐火: 青い魂炎 (SOUL_FIRE_FLAME) を主役に、魂の霊気 (SOUL) を添える。
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, themed, 0.22, 0.28, 0.22, 0.02);
                sl.sendParticles(ParticleTypes.SOUL, x, y, z, Math.max(2, n / 2), 0.2, 0.28, 0.2, 0.02);
                break;
            case ERASURE:
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, themed, 0.3, 0.3, 0.3, 0.04);
                break;
            case BLOOD:
                // 血: themed の代替として大粒の赤ダスト少量 + 落下する赤(lava)を少し。
                sl.sendParticles(new DustParticleOptions(new Vector3f(0.55f, 0.0f, 0.02f), 1.2f),
                        x, y, z, dust, 0.25, 0.3, 0.25, 0.01);
                sl.sendParticles(ParticleTypes.FALLING_LAVA, x, y, z, dustSub, 0.2, 0.2, 0.2, 0.0);
                break;
            case CORROSION:
                // 侵食: 赤紫(マゼンタ)。 dust は減らし、桜の花びらでマゼンタのアクセントを足す。
                sl.sendParticles(new DustParticleOptions(new Vector3f(0.75f, 0.1f, 0.55f), 1.25f),
                        x, y, z, dust, 0.28, 0.3, 0.28, 0.03);
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, x, y, z, Math.max(1, n / 3), 0.25, 0.28, 0.25, 0.0);
                break;
            case MIASMA:
                // 瘴気: 魔女の渦(themed)を主役に、紫 dust は控えめ。
                sl.sendParticles(ParticleTypes.WITCH, x, y, z, themed, 0.28, 0.32, 0.28, 0.02);
                sl.sendParticles(new DustParticleOptions(new Vector3f(0.45f, 0.1f, 0.7f), 1.4f),
                        x, y, z, dust, 0.3, 0.32, 0.3, 0.015);
                break;
            default:
                break;
        }
    }
}
