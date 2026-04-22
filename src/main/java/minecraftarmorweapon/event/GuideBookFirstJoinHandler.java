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
 * ワールドに初めてログインしたプレイヤーにガイドブックを支給する。
 * プレイヤー永続データ (getPersistentData().getCompound("PlayerPersisted"))
 * に受け取り済みフラグを立てて重複配布を防ぐ。
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID)
public class GuideBookFirstJoinHandler {

    private static final String TAG_RECEIVED = "MAW_GuideBookReceived";

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Forge 推奨の永続タグ領域を使う
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(TAG_RECEIVED)) return;
        persisted.putBoolean(TAG_RECEIVED, true);
        data.put(ServerPlayer.PERSISTED_NBT_TAG, persisted);

        // インベントリに追加、入らなければ足元に落とす
        ItemStack book = new ItemStack(CustomEntityInit.GUIDE_BOOK.get());
        if (!player.getInventory().add(book)) {
            ItemEntity drop = new ItemEntity(player.level(),
                player.getX(), player.getY(), player.getZ(), book);
            drop.setDefaultPickUpDelay();
            player.level().addFreshEntity(drop);
        }
    }
}
