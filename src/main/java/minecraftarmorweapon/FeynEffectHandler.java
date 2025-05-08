package minecraftarmorweapon;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.ItemStack;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FeynEffectHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        ItemStack weapon = player.getMainHandItem();

        if (weapon.hasTag()) {
            CompoundTag tag = weapon.getTag();
            if ("sancted".equals(tag.getString("Feyn"))) {
                LivingEntity target = event.getEntity();
                if (target.getMobType() == MobType.UNDEAD) {
                    event.setAmount(event.getAmount() + 4.0F); // 追加ダメージ（調整可能）
                }
            }
        }
    }
    // 満腹度を消費して耐久を回復
 @SubscribeEvent
public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    Player player = event.player;

    if (player == null || player.level.isClientSide) return; // サーバー側のみ

    ItemStack item = player.getMainHandItem();
    if (item != null && item.isDamageableItem() && item.hasTag()) {
        CompoundTag tag = item.getTag();
        if ("cursed".equals(tag.getString("Feyn"))) {
            int currentDamage = item.getDamageValue();
            int maxDamage = item.getMaxDamage();

            // 空腹ゲージを取得
            int foodLevel = player.getFoodData().getFoodLevel();

            // 条件を空腹ゲージが2以上であることに変更
            if (currentDamage >= maxDamage - 1 && foodLevel > 2) {
                player.getFoodData().setFoodLevel(foodLevel - 2); // 空腹ゲージを消費
                item.setDamageValue(maxDamage - 5);              // 耐久を回復
            }
        }
    }
}
    
}

    

