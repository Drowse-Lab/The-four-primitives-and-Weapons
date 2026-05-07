package minecraftarmorweapon.events;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.nbt.CompoundTag;

import minecraftarmorweapon.init.MinecraftArmorWeaponModItems;
import minecraftarmorweapon.init.MinecraftArmorWeaponModMobEffects;

@Mod.EventBusSubscriber(modid = "minecraft_armor_weapon")
public class SheathEffectHandler {
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // 全インベントリスキャンを毎 tick から 10 tick (0.5秒) おきに変更
        if (event.player.tickCount % 10 != 0) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        // OldKatanaを手に持っているかチェック
        boolean hasOldKatanaInHand = player.getMainHandItem().getItem() == MinecraftArmorWeaponModItems.OLD_KATANA.get() ||
                                      player.getOffhandItem().getItem() == MinecraftArmorWeaponModItems.OLD_KATANA.get();
        
        // 手に持っている場合は処理をスキップ（IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedureが処理）
        if (hasOldKatanaInHand) {
            return;
        }
        
        // プレイヤーのインベントリをチェック
        boolean hasOldKatanaSheathed = false;
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            
            // 鞘アイテムかチェック
            if (stack.getItem() == MinecraftArmorWeaponModItems.SAYA.get()) {
                if (stack.hasTag()) {
                    CompoundTag nbt = stack.getTag();
                    if (nbt.contains("StoredKatana")) {
                        CompoundTag storedKatana = nbt.getCompound("StoredKatana");
                        String katanaId = storedKatana.getString("id");
                        
                        // OldKatanaが納刀されているかチェック
                        if (katanaId.equals("minecraft_armor_weapon:old_katana")) {
                            hasOldKatanaSheathed = true;
                            break;
                        }
                    }
                }
            }
        }
        
        // オフハンドもチェック
        ItemStack offhandStack = player.getOffhandItem();
        if (!hasOldKatanaSheathed && offhandStack.getItem() == MinecraftArmorWeaponModItems.SAYA.get()) {
            if (offhandStack.hasTag()) {
                CompoundTag nbt = offhandStack.getTag();
                if (nbt.contains("StoredKatana")) {
                    CompoundTag storedKatana = nbt.getCompound("StoredKatana");
                    String katanaId = storedKatana.getString("id");
                    
                    if (katanaId.equals("minecraft_armor_weapon:old_katana")) {
                        hasOldKatanaSheathed = true;
                    }
                }
            }
        }
        
        // 納刀中の場合、SliceGuard効果を付与
        if (hasOldKatanaSheathed) {
            player.addEffect(new MobEffectInstance(
                MinecraftArmorWeaponModMobEffects.SLICE_GUARD.get(), 
                60, // 3秒間（余裕を持たせる）
                1,  // レベル2（元のレベルと同じ）
                true, 
                false
            ));
        }
    }
}