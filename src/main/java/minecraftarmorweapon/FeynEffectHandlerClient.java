package minecraftarmorweapon;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

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
public class FeynEffectHandlerClient {

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
//     @SubscribeEvent
//     public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
//         Player player = event.player;
    
//         if (player == null || player.level.isClientSide) return; // サーバー側のみ
    
//         ItemStack item = player.getMainHandItem();
    
//     if (item != null && item.isDamageableItem() && item.hasTag()) {
//         CompoundTag tag = item.getTag();
//         System.out.println("Item Tag: " + tag.getString("Feyn"));

//         if ("cursed".equals(tag.getString("Feyn"))) {
//             int currentDamage = item.getDamageValue();
//             int maxDamage = item.getMaxDamage();
//             int foodLevel = player.getFoodData().getFoodLevel();

//             // System.out.println("Current Damage: " + currentDamage + ", Max Damage: " + maxDamage);
//             // System.out.println("Food Level: " + foodLevel);

//         // 耐久が最大値の場合は回復処理をスキップ
//             if (currentDamage == 0) {
//             // System.out.println("Item is already at full durability. No repair needed.");
//             return;
//             }

//             // if (currentDamage >= maxDamage * 0.8 && foodLevel > 2) { // 80%以上のダメージ
//             if (foodLevel > 8) { // 満腹度が8以上
//                 player.getFoodData().setFoodLevel(foodLevel - 1);    // 空腹ゲージを消費
//                 item.setDamageValue(currentDamage - 5);            // 耐久を回復
//                 System.out.println("Item repaired. New Damage Value: " + item.getDamageValue());
//             } // else {
//             // System.out.println("Repair condition not met. Current Damage: " + currentDamage + ", Food Level: " + foodLevel);
//             // }
//         }
// }
    
// }


}