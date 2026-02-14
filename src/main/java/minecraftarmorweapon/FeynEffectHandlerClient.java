package minecraftarmorweapon;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FeynEffectHandlerClient {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        ItemStack weapon = player.getMainHandItem();
        if (!weapon.hasTag()) return;

        CompoundTag tag = weapon.getTag();
        String feyn = tag.getString("Feyn");

        if ("sancted".equals(feyn)) {
            LivingEntity target = event.getEntity();
            if (target.getMobType() == MobType.UNDEAD) {
                event.setAmount(event.getAmount() + 4.0F);
            }
        }

        if ("cursed".equals(feyn)) {
            event.setAmount(event.getAmount() + 4.0F);
        }
    }

    // @SubscribeEvent
    // public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
    //     Player player = event.player;
    //     if (player == null || player.level().isClientSide) return;

    //     ItemStack item = player.getMainHandItem();
    //     if (!item.isDamageableItem() || !item.hasTag()) return;

    //     CompoundTag tag = item.getTag();
    //     if (!"cursed".equals(tag.getString("Feyn"))) return;

    //     int currentDamage = item.getDamageValue();
    //     int foodLevel = player.getFoodData().getFoodLevel();

    //     // 耐久が減っていて、満腹度が十分なら回復
    //     if (currentDamage > 0 && foodLevel > 8) {
    //         player.getFoodData().setFoodLevel(foodLevel - 1); // 満腹度を1消費
    //         item.setDamageValue(Math.max(currentDamage - 5, 0)); // 耐久を5回復（0未満にならないように）
    //     }
    // }
}
