package the_four_primitives_and_weapons.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * レイピア（細剣） — 突きの速度がごっついはやい剣
 *
 * 特性:
 *   - 攻撃速度: 0.0f → 毎秒4回攻撃（バニラ剣の2.5倍、ゲーム内最高速）
 *   - ダメージはやや低め（速度とのトレードオフ）
 *   - 命中ごとに魔法ダメージ（防具貫通）を追加
 *   - 命中時にクリットパーティクル＋突き音
 */
public class RapierItem extends SwordItem {

    public RapierItem() {
        super(new Tier() {
            public int getUses()                 { return 0; }
            public float getSpeed()              { return 4f; }
            public float getAttackDamageBonus()  { return 2f; }
            public int getLevel()                { return 1; }
            public int getEnchantmentValue()     { return 14; }
            public Ingredient getRepairIngredient() { return Ingredient.of(); }
        }, 2, 0.0f, new Item.Properties());
        // attackSpeedModifier=0.0f → 4.0攻撃/秒（ゲーム最高速）
        // バニラ剣=-2.4f (1.6/秒) の約2.5倍
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide && attacker instanceof Player player) {
            // 鎧貫通: 魔法ダメージを追加（防具・耐性無視）
            float pierceDamage = (this.getDamage() + 2) * 0.25f;
            target.hurt(target.damageSources().magic(), pierceDamage);

            // 突きパーティクル
            if (attacker.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    8, 0.2, 0.3, 0.2, 0.05);
            }

            // 突き音
            attacker.level().playSound(null,
                attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.6f, 1.8f);
        }
        return super.hurtEnemy(stack, target, attacker);
    }
}
