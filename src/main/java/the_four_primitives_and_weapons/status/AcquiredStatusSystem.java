package the_four_primitives_and_weapons.status;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * 後天的ステータスシステム（儀式/クエストによる強化）
 *
 * 強化項目と上限:
 *   HP        +20HP固定（1回のみ）
 *   攻撃力    +1/回、上限5回（最大+5）
 *   ジャンプ力 +0.5ブロック相当/回、上限5回（最大+2.5）
 *
 * NBT構造:
 *   AcquiredHpGranted:   boolean（HP付与済みか）
 *   AcquiredAttackCount: int（攻撃力儀式回数 0〜5）
 *   AcquiredJumpCount:   int（ジャンプ力儀式回数 0〜5）
 */
public class AcquiredStatusSystem {

    // NBTキー
    private static final String NBT_HP_GRANTED     = "AcquiredHpGranted";
    private static final String NBT_ATTACK_COUNT   = "AcquiredAttackCount";
    private static final String NBT_JUMP_COUNT     = "AcquiredJumpCount";

    // 上限
    private static final int   MAX_RITUAL_COUNT    = 5;

    // HP強化量
    private static final float HP_BONUS            = 20.0f;

    // 攻撃力強化量/回
    private static final double ATTACK_BONUS_PER   = 1.0;

    // ジャンプ力強化量/回（Minecraftのjump_strength単位）
    // vanilla は 0.42 ≒ 1ブロック相当、0.5ブロック ≒ +0.21
    private static final double JUMP_BONUS_PER     = 0.21;

    // AttributeModifier UUID
    private static final UUID ATTACK_UUID =
            UUID.fromString("b1c2d3e4-f5a6-7890-abcd-ef1234567890");
    private static final UUID JUMP_UUID =
            UUID.fromString("c2d3e4f5-a6b7-8901-bcde-f12345678901");

    // ────────────────────────────────────────────────────────────────
    // HP強化（最大+20、1回のみ）
    // ────────────────────────────────────────────────────────────────

    /**
     * HP強化儀式を実行する。
     * @return true = 成功、false = 既に付与済み
     */
    public static boolean ritualHp(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (tag.getBoolean(NBT_HP_GRANTED)) return false;

        AttributeInstance maxHp = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp == null) return false;

        maxHp.addPermanentModifier(new AttributeModifier(
                UUID.fromString("a0b1c2d3-e4f5-6789-abcd-ef0123456789"),
                "acquired_hp_bonus",
                HP_BONUS,
                AttributeModifier.Operation.ADDITION
        ));
        // 現在HPも最大まで回復
        entity.setHealth(entity.getMaxHealth());

        tag.putBoolean(NBT_HP_GRANTED, true);
        return true;
    }

    public static boolean hasHpRitual(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(NBT_HP_GRANTED);
    }

    // ────────────────────────────────────────────────────────────────
    // 攻撃力強化（+1/回、上限5回）
    // ────────────────────────────────────────────────────────────────

    /**
     * 攻撃力強化儀式を実行する。
     * @return true = 成功、false = 上限到達
     */
    public static boolean ritualAttack(LivingEntity entity) {
        CompoundTag tag   = entity.getPersistentData();
        int         count = tag.getInt(NBT_ATTACK_COUNT);
        if (count >= MAX_RITUAL_COUNT) return false;

        AttributeInstance atk = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk == null) return false;

        // 既存のModifierを一旦削除して累積値で再付与
        if (atk.getModifier(ATTACK_UUID) != null) {
            atk.removeModifier(ATTACK_UUID);
        }
        int newCount = count + 1;
        atk.addPermanentModifier(new AttributeModifier(
                ATTACK_UUID,
                "acquired_attack_bonus",
                ATTACK_BONUS_PER * newCount,
                AttributeModifier.Operation.ADDITION
        ));

        tag.putInt(NBT_ATTACK_COUNT, newCount);
        return true;
    }

    public static int getAttackRitualCount(LivingEntity entity) {
        return entity.getPersistentData().getInt(NBT_ATTACK_COUNT);
    }

    // ────────────────────────────────────────────────────────────────
    // ジャンプ力強化（+0.5ブロック相当/回、上限5回）
    // ────────────────────────────────────────────────────────────────

    /**
     * ジャンプ力強化儀式を実行する。
     * @return true = 成功、false = 上限到達
     */
    public static boolean ritualJump(LivingEntity entity) {
        CompoundTag tag   = entity.getPersistentData();
        int         count = tag.getInt(NBT_JUMP_COUNT);
        if (count >= MAX_RITUAL_COUNT) return false;

        AttributeInstance jump = entity.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump == null) return false;

        if (jump.getModifier(JUMP_UUID) != null) {
            jump.removeModifier(JUMP_UUID);
        }
        int newCount = count + 1;
        jump.addPermanentModifier(new AttributeModifier(
                JUMP_UUID,
                "acquired_jump_bonus",
                JUMP_BONUS_PER * newCount,
                AttributeModifier.Operation.ADDITION
        ));

        tag.putInt(NBT_JUMP_COUNT, newCount);
        return true;
    }

    public static int getJumpRitualCount(LivingEntity entity) {
        return entity.getPersistentData().getInt(NBT_JUMP_COUNT);
    }

    // ────────────────────────────────────────────────────────────────
    // ロード時にAttributeを再適用（サーバー再起動後の復元）
    // ────────────────────────────────────────────────────────────────

    /**
     * エンティティのロード時に呼び出す。
     * NBTに保存された後天的ステータスをAttributeに再適用する。
     */
    public static void restoreOnLoad(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();

        // HP
        if (tag.getBoolean(NBT_HP_GRANTED)) {
            AttributeInstance maxHp = entity.getAttribute(Attributes.MAX_HEALTH);
            if (maxHp != null && maxHp.getModifier(
                    UUID.fromString("a0b1c2d3-e4f5-6789-abcd-ef0123456789")) == null) {
                maxHp.addPermanentModifier(new AttributeModifier(
                        UUID.fromString("a0b1c2d3-e4f5-6789-abcd-ef0123456789"),
                        "acquired_hp_bonus", HP_BONUS,
                        AttributeModifier.Operation.ADDITION));
            }
        }

        // 攻撃力
        int atkCount = tag.getInt(NBT_ATTACK_COUNT);
        if (atkCount > 0) {
            AttributeInstance atk = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null && atk.getModifier(ATTACK_UUID) == null) {
                atk.addPermanentModifier(new AttributeModifier(
                        ATTACK_UUID, "acquired_attack_bonus",
                        ATTACK_BONUS_PER * atkCount,
                        AttributeModifier.Operation.ADDITION));
            }
        }

        // ジャンプ力
        int jumpCount = tag.getInt(NBT_JUMP_COUNT);
        if (jumpCount > 0) {
            AttributeInstance jump = entity.getAttribute(Attributes.JUMP_STRENGTH);
            if (jump != null && jump.getModifier(JUMP_UUID) == null) {
                jump.addPermanentModifier(new AttributeModifier(
                        JUMP_UUID, "acquired_jump_bonus",
                        JUMP_BONUS_PER * jumpCount,
                        AttributeModifier.Operation.ADDITION));
            }
        }
    }
}
