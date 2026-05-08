package minecraftarmorweapon.event;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.init.CustomEnchantmentInit;
import minecraftarmorweapon.item.RecrossHookshotItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi Jump エンチャのサーバー側ロジック。
 *
 *  - {@link #tryAirJump(ServerPlayer)} : クライアントから {@code MultiJumpPacket} で呼ばれる。
 *    残ジャンプがあれば player に上昇速度を与えてカウンタを 1 進める。
 *  - {@code onPlayerTick} : 着地時にカウンタリセット。
 *
 *  - {@link #getLevel(Player)} : Leggings と Hookshot (両手) を見て multi_jump レベルの最大値を返す。
 *    呼出側 (フックショットの浮遊燃料回復ロジック等) からも参照される。
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID)
public class MultiJumpHandler {

    /** vanilla のジャンプ初速 (block/tick). */
    private static final double JUMP_VELOCITY = 0.42;

    private static final Map<UUID, Integer> usedJumps = new ConcurrentHashMap<>();

    /** Leggings / 両手フックショットのうち最も高い multi_jump レベルを取得. */
    public static int getLevel(Player player) {
        var ench = CustomEnchantmentInit.MULTI_JUMP.get();
        int leg = EnchantmentHelper.getItemEnchantmentLevel(ench, player.getItemBySlot(EquipmentSlot.LEGS));
        int main = isHookshot(player.getMainHandItem())
            ? EnchantmentHelper.getItemEnchantmentLevel(ench, player.getMainHandItem()) : 0;
        int off = isHookshot(player.getOffhandItem())
            ? EnchantmentHelper.getItemEnchantmentLevel(ench, player.getOffhandItem()) : 0;
        return Math.max(leg, Math.max(main, off));
    }

    /** クライアント側でフックショットを所持し、その multi_jump レベルだけを返す. (浮遊燃料回復用) */
    public static int getHookshotLevel(Player player) {
        var ench = CustomEnchantmentInit.MULTI_JUMP.get();
        int main = isHookshot(player.getMainHandItem())
            ? EnchantmentHelper.getItemEnchantmentLevel(ench, player.getMainHandItem()) : 0;
        int off = isHookshot(player.getOffhandItem())
            ? EnchantmentHelper.getItemEnchantmentLevel(ench, player.getOffhandItem()) : 0;
        return Math.max(main, off);
    }

    /** クライアントから来たジャンプ要求を処理。残ジャンプ無しなら no-op。 */
    public static void tryAirJump(ServerPlayer player) {
        if (player.onGround()) return;
        int level = getLevel(player);
        if (level <= 0) return;
        int used = usedJumps.getOrDefault(player.getUUID(), 0);
        if (used >= level) return;

        Vec3 m = player.getDeltaMovement();
        player.setDeltaMovement(m.x, JUMP_VELOCITY, m.z);
        player.hurtMarked = true;
        player.fallDistance = 0f;
        usedJumps.put(player.getUUID(), used + 1);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    /** 着地でジャンプカウンタをリセット. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.onGround()) {
            usedJumps.remove(sp.getUUID());
        }
    }

    private static boolean isHookshot(ItemStack stack) {
        return stack.getItem() instanceof RecrossHookshotItem;
    }
}
