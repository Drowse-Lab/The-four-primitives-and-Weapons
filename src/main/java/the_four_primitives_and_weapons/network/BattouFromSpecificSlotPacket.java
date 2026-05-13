package the_four_primitives_and_weapons.network;

import the_four_primitives_and_weapons.util.CuriosScabbardHelper;
import the_four_primitives_and_weapons.util.CuriosScabbardHelper.ScabbardLocation;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Supplier;

/**
 * クライアント→サーバー: 特定のスロットの鞘から抜刀するパケット
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class BattouFromSpecificSlotPacket {

    private final int locationType;
    private final String curioSlotId;
    private final int slotIndex;

    public BattouFromSpecificSlotPacket(ScabbardLocation location, String curioSlotId, int slotIndex) {
        this.locationType = location.ordinal();
        this.curioSlotId = curioSlotId != null ? curioSlotId : "";
        this.slotIndex = slotIndex;
    }

    public BattouFromSpecificSlotPacket(FriendlyByteBuf buffer) {
        this.locationType = buffer.readInt();
        this.curioSlotId = buffer.readUtf();
        this.slotIndex = buffer.readInt();
    }

    public static void buffer(BattouFromSpecificSlotPacket message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.locationType);
        buffer.writeUtf(message.curioSlotId);
        buffer.writeInt(message.slotIndex);
    }

    public static void handler(BattouFromSpecificSlotPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Player player = context.getSender();
            if (player == null) return;
            if (!player.level().hasChunkAt(player.blockPosition())) return;

            ScabbardLocation location = ScabbardLocation.values()[message.locationType];

            // メインハンドが空でない場合は抜刀しない（HAND+MAIN_HANDの場合は鞘自体を持っているのでOK）
            if (location == ScabbardLocation.HAND && message.slotIndex == 0) {
                // メインハンドに鞘を持っている場合はそのまま抜刀
            } else {
                ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (!mainHand.isEmpty() && location != ScabbardLocation.HAND) {
                    // メインハンドにアイテムがある場合は抜刀しない
                    return;
                }
            }

            switch (location) {
                case CURIOS:
                    drawFromCurios(player, message.curioSlotId, message.slotIndex);
                    break;
                case INVENTORY:
                    drawFromInventory(player, message.slotIndex);
                    break;
                case COVERUP:
                    drawFromCoverUp(player);
                    break;
                case HAND:
                    drawFromHand(player, message.slotIndex);
                    break;
            }
        });
        context.setPacketHandled(true);
    }

    private static void drawFromCurios(Player player, String curioSlotId, int slotIndex) {
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            handler.getStacksHandler(curioSlotId).ifPresent(stacksHandler -> {
                if (slotIndex >= stacksHandler.getStacks().getSlots()) return;
                ItemStack scabbard = stacksHandler.getStacks().getStackInSlot(slotIndex);
                if (!CuriosScabbardHelper.isScabbard(scabbard) || !CuriosScabbardHelper.hasStoredWeapon(scabbard)) return;

                ItemStack weapon = CuriosScabbardHelper.extractWeaponFromScabbard(scabbard);
                if (weapon.isEmpty()) return;

                player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
                CuriosScabbardHelper.clearWeaponFromScabbard(scabbard);
                stacksHandler.getStacks().setStackInSlot(slotIndex, scabbard);
                player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.0F);
            });
        });
    }

    private static void drawFromInventory(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= player.getInventory().getContainerSize()) return;
        ItemStack scabbard = player.getInventory().getItem(slotIndex);
        if (!CuriosScabbardHelper.isScabbard(scabbard) || !CuriosScabbardHelper.hasStoredWeapon(scabbard)) return;

        ItemStack weapon = CuriosScabbardHelper.extractWeaponFromScabbard(scabbard);
        if (weapon.isEmpty()) return;

        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        CuriosScabbardHelper.clearWeaponFromScabbard(scabbard);
        player.getInventory().setItem(slotIndex, scabbard);
        player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.0F);
    }

    private static void drawFromHand(Player player, int handIndex) {
        InteractionHand hand = handIndex == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack scabbard = player.getItemInHand(hand);
        if (!CuriosScabbardHelper.isScabbard(scabbard) || !CuriosScabbardHelper.hasStoredWeapon(scabbard)) return;

        // メインハンド鞘抜刀の場合、オフハンドが空でないと空鞘の置き場がない
        // (オフハンド鞘抜刀の場合、メインハンドが空でないと武器の置き場がない)
        if (hand == InteractionHand.MAIN_HAND) {
            if (!player.getItemInHand(InteractionHand.OFF_HAND).isEmpty()) return;
        } else {
            if (!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) return;
        }

        ItemStack weapon = CuriosScabbardHelper.extractWeaponFromScabbard(scabbard);
        if (weapon.isEmpty()) return;

        // 鞘を空にする
        CuriosScabbardHelper.clearWeaponFromScabbard(scabbard);

        if (hand == InteractionHand.MAIN_HAND) {
            // メインハンドの鞘 → 空鞘はオフハンドに置く (drop ロス防止)。
            // ItemStack 参照の取り違えを防ぐため、先に source slot を EMPTY にしてから書き込む。
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, scabbard);
            player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        } else {
            // オフハンドの鞘 → 武器をメインハンドへ、空鞘はオフハンドに残す
            player.setItemInHand(InteractionHand.OFF_HAND, scabbard);
            player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        }
        player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.0F);
    }

    private static void drawFromCoverUp(Player player) {
        CompoundTag entityData = player.getPersistentData();
        if (!entityData.contains("CoverUp")) return;

        CompoundTag savedItemTag = entityData.getCompound("CoverUp");
        ItemStack restoredItem = ItemStack.of(savedItemTag);
        if (restoredItem.isEmpty()) return;

        // Set item to reappearing state
        CompoundTag tag = restoredItem.getOrCreateTag();
        tag.putInt("BluepurgeState", 3); // Reappearing
        tag.putInt("BluepurgeTimer", 60);

        // Give item to player
        player.setItemInHand(InteractionHand.MAIN_HAND, restoredItem);

        // Clear CoverUp tag
        entityData.remove("CoverUp");

        // Reappearing particle effect
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        double particleRadius = 2.5;

        for (int i = 0; i < 30; i++) {
            player.level().addParticle(ParticleTypes.GLOW,
                x + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius,
                y + 1 + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius,
                z + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius,
                Mth.nextDouble(RandomSource.create(), -0.1, 0.1),
                Mth.nextDouble(RandomSource.create(), 0.1, 0.3),
                Mth.nextDouble(RandomSource.create(), -0.1, 0.1));

            player.level().addParticle(ParticleTypes.END_ROD,
                x + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius,
                y + 1 + Mth.nextDouble(RandomSource.create(), 0, 2),
                z + Mth.nextDouble(RandomSource.create(), -1, 1) * particleRadius,
                0, 0.05, 0);
        }

        // Speed boost
        if (!player.level().isClientSide()) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
        }
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        TheFourPrimitivesAndWeaponsMod.addNetworkMessage(
            BattouFromSpecificSlotPacket.class,
            BattouFromSpecificSlotPacket::buffer,
            BattouFromSpecificSlotPacket::new,
            BattouFromSpecificSlotPacket::handler
        );
    }
}
