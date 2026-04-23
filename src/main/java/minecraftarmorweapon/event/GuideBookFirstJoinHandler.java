package minecraftarmorweapon.event;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.init.CustomEntityInit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ワールドに初めてログインしたプレイヤーにガイドブック + ナイフホルダー + 投げナイフ16本を支給する。
 * プレイヤー永続データ (getPersistentData().getCompound("PlayerPersisted"))
 * に受け取り済みフラグを立てて重複配布を防ぐ。
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID)
public class GuideBookFirstJoinHandler {

    private static final String TAG_BOOK = "MAW_GuideBookReceived";

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(ServerPlayer.PERSISTED_NBT_TAG);

        // 配布するのはガイドブックのみ。
        // ナイフホルダーと投げナイフはプレイヤーがクラフトして入手する。
        if (persisted.getBoolean(TAG_BOOK)) return;
        giveOrDrop(player, new ItemStack(CustomEntityInit.GUIDE_BOOK.get()));
        persisted.putBoolean(TAG_BOOK, true);
        data.put(ServerPlayer.PERSISTED_NBT_TAG, persisted);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            ItemEntity drop = new ItemEntity(player.level(),
                player.getX(), player.getY(), player.getZ(), stack);
            drop.setDefaultPickUpDelay();
            player.level().addFreshEntity(drop);
        }
    }
}
