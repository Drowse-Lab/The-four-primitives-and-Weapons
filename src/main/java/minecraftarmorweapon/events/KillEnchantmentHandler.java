package minecraftarmorweapon.events;

import minecraftarmorweapon.util.VersionHelper;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import minecraftarmorweapon.init.MinecraftArmorWeaponModEnchantments;

@Mod.EventBusSubscriber
public class KillEnchantmentHandler {
    
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        try {
            if (event.getEntity() == null || event.getSource() == null)
                return;

            Entity attacker = event.getSource().getEntity();
            Entity directAttacker = event.getSource().getDirectEntity();
            LivingEntity target = event.getEntity();

            if (target.level().isClientSide())
                return;

            boolean hasKillEnchant = false;

            // 直接攻撃の場合
            if (attacker instanceof LivingEntity livingAttacker) {
                ItemStack weapon = livingAttacker.getMainHandItem();
                int killLevel = EnchantmentHelper.getItemEnchantmentLevel(
                    MinecraftArmorWeaponModEnchantments.KILL.get(), weapon);

                if (killLevel > 0) {
                    hasKillEnchant = true;
                }

                // NBTタグでのkillフラグもチェック
                if (attacker.getPersistentData().getBoolean("minecraft_armor_weapon:killentity")) {
                    hasKillEnchant = true;
                }
            }

            // 飛び道具（矢）の場合
            if (directAttacker instanceof AbstractArrow arrow) {
                // 矢を発射したエンティティを取得
                if (arrow.getOwner() instanceof LivingEntity shooter) {
                    // 射手の持っている弓をチェック
                    ItemStack mainHand = shooter.getMainHandItem();
                    ItemStack offHand = shooter.getOffhandItem();

                    // メインハンドの弓をチェック
                    if (mainHand.getItem() instanceof BowItem || mainHand.getItem() instanceof CrossbowItem) {
                        int killLevel = EnchantmentHelper.getItemEnchantmentLevel(
                            MinecraftArmorWeaponModEnchantments.KILL.get(), mainHand);
                        if (killLevel > 0) {
                            hasKillEnchant = true;
                        }
                    }

                    // オフハンドの弓をチェック
                    if (offHand.getItem() instanceof BowItem || offHand.getItem() instanceof CrossbowItem) {
                        int killLevel = EnchantmentHelper.getItemEnchantmentLevel(
                            MinecraftArmorWeaponModEnchantments.KILL.get(), offHand);
                        if (killLevel > 0) {
                            hasKillEnchant = true;
                        }
                    }

                    // 射手のNBTタグもチェック
                    if (shooter.getPersistentData().getBoolean("minecraft_armor_weapon:killentity")) {
                        hasKillEnchant = true;
                    }
                }
            }

            // Killエンチャントが有効な場合、確率で即死させる
            if (hasKillEnchant) {
                // 82.4%の確率で即死
                if (Math.random() < 0.824) {
                    // killコマンドを実行
                    if (target.getServer() != null) {
                        target.getServer().getCommands().performPrefixedCommand(
                            new CommandSourceStack(
                                CommandSource.NULL,
                                target.position(),
                                target.getRotationVector(),
                                VersionHelper.getLevel(target) instanceof ServerLevel ? (ServerLevel) VersionHelper.getLevel(target) : null,
                                4,
                                target.getName().getString(),
                                target.getDisplayName(),
                                target.level().getServer(),
                                target
                            ),
                            "kill @s"
                        );
                    }

                    // setHealth(0f)で確実に倒す
                    // これは現在の体力のみを0にし、最大体力は変更しないので安全
                    target.setHealth(0f);
                }
            }
        } catch (Throwable e) {
            System.err.println("[KillEnchantment] Error in onLivingHurt: " + e.getMessage());
            e.printStackTrace();
        }
    }
}