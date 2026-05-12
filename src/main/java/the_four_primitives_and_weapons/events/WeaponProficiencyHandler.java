package the_four_primitives_and_weapons.events;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.skill.PlayerSkillData;
import the_four_primitives_and_weapons.skill.PlayerSkillData.WeaponProficiency;
import the_four_primitives_and_weapons.skill.WeaponTypeRegistry;

import java.util.UUID;

/**
 * 武器適正システム
 * - 得意武器タイプと一致しない武器は攻撃速度が低下（クールタイム増加）
 * - 攻撃速度バーが最大でない状態で攻撃するとダメージ減少（バニラ仕様を活用）
 * - 得意武器なら攻撃速度にボーナス
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class WeaponProficiencyHandler {

    private static final UUID PROFICIENCY_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final String MODIFIER_NAME = "Weapon Proficiency";

    // 適正外の攻撃速度ペナルティ
    private static final double NON_PROFICIENT_SPEED_PENALTY = -1.0;
    // 適正武器の攻撃速度ボーナス
    private static final double PROFICIENT_SPEED_BONUS = 0.4;
    // ダメージ減少の閾値（この%以下のチャージだとダメージ大幅減）
    private static final float LOW_CHARGE_THRESHOLD = 0.5f;
    private static final float LOW_CHARGE_DAMAGE_MULT = 0.3f;

    /**
     * 毎tick: 持っている武器の適正をチェックして攻撃速度を調整
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // 5 tick おき (0.25秒) で十分 — 攻撃速度モディファイアは即時反映される必要なし
        if (event.player.tickCount % 5 != 0) return;
        Player player = event.player;
        if (player.level().isClientSide()) return;

        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) return;

        ItemStack mainHand = player.getMainHandItem();

        // 武器を持っていない場合はモディファイア削除
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof SwordItem)) {
            removeProficiencyModifier(attackSpeed);
            return;
        }

        // プレイヤーの得意武器タイプを取得
        PlayerSkillData.SkillStorage skillData = PlayerSkillData.getSkillData(player);
        if (skillData == null) {
            removeProficiencyModifier(attackSpeed);
            return;
        }

        WeaponProficiency proficiency = skillData.getWeaponProficiency();
        if (proficiency == WeaponProficiency.NONE) {
            // 得意武器未設定 → ペナルティなし
            removeProficiencyModifier(attackSpeed);
            return;
        }

        // 武器のタイプを取得
        String weaponTypeId = getWeaponTypeId(mainHand);
        boolean isProficient = matchesProficiency(proficiency, weaponTypeId);

        // モディファイアを適用
        double targetValue = isProficient ? PROFICIENT_SPEED_BONUS : NON_PROFICIENT_SPEED_PENALTY;
        applyProficiencyModifier(attackSpeed, targetValue);
    }

    /**
     * 攻撃時: チャージ不足ならダメージ追加減少
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof SwordItem)) return;

        float chargeScale = player.getAttackStrengthScale(0.5f);

        // チャージが50%以下の場合、さらにダメージ減少（バニラの減少に加えて）
        if (chargeScale < LOW_CHARGE_THRESHOLD) {
            // PersistentDataで一時的にダメージ倍率を保存（LivingHurtEventで適用）
            player.getPersistentData().putFloat("ProficiencyDamageMult", LOW_CHARGE_DAMAGE_MULT);
        } else if (chargeScale < 1.0f) {
            // 50-100%: バニラのダメージ減少のみ（追加ペナルティなし）
            player.getPersistentData().remove("ProficiencyDamageMult");
        } else {
            // 100%チャージ: フルダメージ
            player.getPersistentData().remove("ProficiencyDamageMult");
        }
    }

    /**
     * ダメージ適用時: チャージ不足ペナルティの追加減少
     */
    @SubscribeEvent
    public static void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        if (player.getPersistentData().contains("ProficiencyDamageMult")) {
            float mult = player.getPersistentData().getFloat("ProficiencyDamageMult");
            event.setAmount(event.getAmount() * mult);
            player.getPersistentData().remove("ProficiencyDamageMult");
        }
    }

    // === ヘルパー ===

    private static String getWeaponTypeId(ItemStack stack) {
        WeaponTypeRegistry.WeaponTypeData type = WeaponTypeRegistry.getTypeForItem(stack);
        return type != null ? type.getId() : null;
    }

    private static boolean matchesProficiency(WeaponProficiency proficiency, String weaponTypeId) {
        if (weaponTypeId == null) return false;
        return proficiency.getId().equals(weaponTypeId);
    }

    private static void applyProficiencyModifier(AttributeInstance attr, double value) {
        AttributeModifier existing = attr.getModifier(PROFICIENCY_MODIFIER_UUID);
        if (existing != null) {
            if (Math.abs(existing.getAmount() - value) < 0.001) return; // 変更不要
            attr.removeModifier(PROFICIENCY_MODIFIER_UUID);
        }
        attr.addTransientModifier(new AttributeModifier(
                PROFICIENCY_MODIFIER_UUID, MODIFIER_NAME, value,
                AttributeModifier.Operation.ADDITION));
    }

    private static void removeProficiencyModifier(AttributeInstance attr) {
        if (attr.getModifier(PROFICIENCY_MODIFIER_UUID) != null) {
            attr.removeModifier(PROFICIENCY_MODIFIER_UUID);
        }
    }
}
