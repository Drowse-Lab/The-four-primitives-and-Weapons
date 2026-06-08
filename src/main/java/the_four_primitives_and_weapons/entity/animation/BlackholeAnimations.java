package the_four_primitives_and_weapons.entity.animation;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * Blackhole エンティティのアニメーション定義 (vanilla AnimationDefinition 版).
 *
 * 元 GeckoLib の blackhole.animation.json から手動変換:
 *   - idle: animation_length 0.4583s, looping
 *     bone と bone2 にそれぞれ回転キーフレーム
 *
 * 元の easeInQuart は vanilla には無いので CATMULLROM (キャットマルロム) で近似。
 * 単純な区間は LINEAR にしてある。
 */
public class BlackholeAnimations {

    public static final AnimationDefinition IDLE = AnimationDefinition.Builder
        .withLength(0.4583f).looping()
        .addAnimation("bone", new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0f,    KeyframeAnimations.degreeVec(0f, 0f, 0f),
                AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.0833f, KeyframeAnimations.degreeVec(-17.5f, -12.5f, 0f),
                AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.125f,  KeyframeAnimations.degreeVec(-17.33947f, -5.64209f, -2.17833f),
                AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.1667f, KeyframeAnimations.degreeVec(-18.31602f, 20.90171f, -10.63941f),
                AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.25f,   KeyframeAnimations.degreeVec(-58.11711f, 57.49713f, -32.72101f),
                AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.3333f, KeyframeAnimations.degreeVec(-11.37576f, 73.17179f, 17.72699f),
                AnimationChannel.Interpolations.CATMULLROM),
            new Keyframe(0.4167f, KeyframeAnimations.degreeVec(-84.41449f, 44.08218f, -57.25065f),
                AnimationChannel.Interpolations.CATMULLROM)
        ))
        .addAnimation("bone2", new AnimationChannel(
            AnimationChannel.Targets.ROTATION,
            new Keyframe(0.0f,    KeyframeAnimations.degreeVec(0f, 0f, 0f),
                AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.0833f, KeyframeAnimations.degreeVec(-23.30696f, 9.30727f, 20.57639f),
                AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.1667f, KeyframeAnimations.degreeVec(-28.16584f, 34.19019f, 7.81656f),
                AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.25f,   KeyframeAnimations.degreeVec(-62.72778f, 56.63332f, -15.52983f),
                AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.3333f, KeyframeAnimations.degreeVec(-127.20399f, 78.11269f, -87.85698f),
                AnimationChannel.Interpolations.LINEAR),
            new Keyframe(0.4167f, KeyframeAnimations.degreeVec(-98.0902f, 27.74541f, -55.03545f),
                AnimationChannel.Interpolations.LINEAR)
        ))
        .build();

    private BlackholeAnimations() {}
}
