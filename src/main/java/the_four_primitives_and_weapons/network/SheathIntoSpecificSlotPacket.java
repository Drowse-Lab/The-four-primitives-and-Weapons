package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.util.CuriosScabbardHelper;
import the_four_primitives_and_weapons.util.CuriosScabbardHelper.ScabbardLocation;
import the_four_primitives_and_weapons.events.DodgeAndBattouHandler;
import the_four_primitives_and_weapons.item.TyokutoSayaItem;
import the_four_primitives_and_weapons.item.SwordSayaItem;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Supplier;

/**
 * クライアント→サーバー: 特定のスロットの鞘に納刀するパケット
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SheathIntoSpecificSlotPacket {

    private final int locationType;
    private final String curioSlotId;
    private final int slotIndex;
    private final int weaponHandIndex; // 0=MAIN_HAND, 1=OFF_HAND

    public SheathIntoSpecificSlotPacket(ScabbardLocation location, String curioSlotId,
                                         int slotIndex, int weaponHandIndex) {
        this.locationType = location.ordinal();
        this.curioSlotId = curioSlotId != null ? curioSlotId : "";
        this.slotIndex = slotIndex;
        this.weaponHandIndex = weaponHandIndex;
    }

    public SheathIntoSpecificSlotPacket(FriendlyByteBuf buffer) {
        this.locationType = buffer.readInt();
        this.curioSlotId = buffer.readUtf();
        this.slotIndex = buffer.readInt();
        this.weaponHandIndex = buffer.readInt();
    }

    public static void buffer(SheathIntoSpecificSlotPacket message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.locationType);
        buffer.writeUtf(message.curioSlotId);
        buffer.writeInt(message.slotIndex);
        buffer.writeInt(message.weaponHandIndex);
    }

    public static void handler(SheathIntoSpecificSlotPacket message,
                                Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Player player = context.getSender();
            if (player == null) return;
            if (!player.level().hasChunkAt(player.blockPosition())) return;

            InteractionHand weaponHand = message.weaponHandIndex == 0
                ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack weaponStack = player.getItemInHand(weaponHand);

            // 武器の検証
            if (!DodgeAndBattouHandler.isWeapon(weaponStack)
                || DodgeAndBattouHandler.isSaya(weaponStack)) return;

            ScabbardLocation location = ScabbardLocation.values()[message.locationType];

            switch (location) {
                case CURIOS:
                    sheathIntoCurios(player, weaponStack, weaponHand,
                                     message.curioSlotId, message.slotIndex);
                    break;
                case HAND:
                    sheathIntoHand(player, weaponStack, weaponHand, message.slotIndex);
                    break;
                case INVENTORY:
                    sheathIntoInventory(player, weaponStack, weaponHand, message.slotIndex);
                    break;
                default:
                    break;
            }
        });
        context.setPacketHandled(true);
    }

    private static void sheathIntoCurios(Player player, ItemStack weaponStack,
                                          InteractionHand weaponHand,
                                          String curioSlotId, int slotIndex) {
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            handler.getStacksHandler(curioSlotId).ifPresent(stacksHandler -> {
                if (slotIndex >= stacksHandler.getStacks().getSlots()) return;
                ItemStack scabbard = stacksHandler.getStacks().getStackInSlot(slotIndex);
                if (!CuriosScabbardHelper.isScabbard(scabbard)
                    || CuriosScabbardHelper.hasStoredWeapon(scabbard)) return;
                if (!CuriosScabbardHelper.isCompatible(weaponStack, scabbard)) return;

                performSheathing(player, weaponStack, scabbard, weaponHand);
                stacksHandler.getStacks().setStackInSlot(slotIndex, scabbard);
            });
        });
    }

    private static void sheathIntoHand(Player player, ItemStack weaponStack,
                                        InteractionHand weaponHand, int handIndex) {
        InteractionHand scabbardHand = handIndex == 0
            ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (scabbardHand == weaponHand) return;

        ItemStack scabbard = player.getItemInHand(scabbardHand);
        if (!CuriosScabbardHelper.isScabbard(scabbard)
            || CuriosScabbardHelper.hasStoredWeapon(scabbard)) return;
        if (!CuriosScabbardHelper.isCompatible(weaponStack, scabbard)) return;

        performSheathing(player, weaponStack, scabbard, weaponHand);
        player.setItemInHand(scabbardHand, scabbard);
    }

    private static void sheathIntoInventory(Player player, ItemStack weaponStack,
                                             InteractionHand weaponHand, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= player.getInventory().getContainerSize()) return;
        ItemStack scabbard = player.getInventory().getItem(slotIndex);
        if (!CuriosScabbardHelper.isScabbard(scabbard)
            || CuriosScabbardHelper.hasStoredWeapon(scabbard)) return;
        if (!CuriosScabbardHelper.isCompatible(weaponStack, scabbard)) return;

        performSheathing(player, weaponStack, scabbard, weaponHand);
        player.getInventory().setItem(slotIndex, scabbard);
    }

    private static void performSheathing(Player player, ItemStack weaponStack,
                                          ItemStack scabbard, InteractionHand weaponHand) {
        // 木刀など 練習用武器は 納刀できない
        if (the_four_primitives_and_weapons.util.KatanaFittings.isPracticeWeapon(weaponStack)) {
            player.displayClientMessage(Component.literal("§cこの武器は納刀できません"), true);
            return;
        }

        CompoundTag sheathTag = scabbard.getOrCreateTag();
        String storageKey = CuriosScabbardHelper.storageKeyFor(scabbard);

        CompoundTag weaponData = weaponStack.save(new CompoundTag());
        sheathTag.put(storageKey, weaponData);

        // 鞘の見た目は SayaModelWrapper が NBT (storageKey) を読んで動的に解決する。
        scabbard.setTag(sheathTag);

        player.setItemInHand(weaponHand, ItemStack.EMPTY);
        player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.8F);
        player.displayClientMessage(Component.literal("§7納刀"), true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(
            SheathIntoSpecificSlotPacket.class,
            SheathIntoSpecificSlotPacket::buffer,
            SheathIntoSpecificSlotPacket::new,
            SheathIntoSpecificSlotPacket::handler
        );
    }
}
