package the_four_primitives_and_weapons.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.init.BladeDimensionItems;
import the_four_primitives_and_weapons.entity.StabbedWeaponEntity;
import net.minecraft.world.phys.Vec3;

/** 専用の境界剣を剣界の書で導く、剣の原への往還儀式。 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public final class BladeDimensionTravelHandler {
    public static final ResourceKey<Level> BLADE_FIELD = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(TheFourPrimitivesAndWeaponsMod.MODID, "blade_field"));

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getEntity().isShiftKeyDown()) return;
        ItemStack weapon = event.getEntity().getMainHandItem();
        ItemStack book = event.getEntity().getOffhandItem();
        if (!weapon.is(BladeDimensionItems.BOUNDARY_BLADE.get()) ||
                !book.is(BladeDimensionItems.BLADE_FIELD_TOME.get())) return;

        if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer player) beginTravelRitual(player);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
    }

    private static void beginTravelRitual(ServerPlayer player) {
        if (player.getCooldowns().isOnCooldown(weaponItem())) return;
        Vec3 look = player.getLookAngle();
        StabbedWeaponEntity blade = new StabbedWeaponEntity(player.level());
        blade.setItem(new ItemStack(BladeDimensionItems.BOUNDARY_BLADE.get()));
        blade.setStabYaw(player.getYRot() + 180.0F);
        blade.setTilt(0.0F); // レンダラーの180度反転と合わせ、切先を真下へ向ける
        blade.setRoll(0.0F);
        blade.setScale(1.35F);
        blade.setRadius(1.2F);
        blade.setRitualLifetime(35);
        blade.moveTo(player.getX() + look.x * 2.0D, player.getEyeY() + 1.0D,
                player.getZ() + look.z * 2.0D, 0.0F, 0.0F);
        player.level().addFreshEntity(blade);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRIGGER,
                SoundSource.PLAYERS, 0.8F, 0.7F);
        player.getCooldowns().addCooldown(weaponItem(), 60);
        TheFourPrimitivesAndWeaponsMod.queueServerWork(25, () -> finishTravel(player));
    }

    private static void finishTravel(ServerPlayer player) {
        if (player.isRemoved() || !player.isAlive()) return;
        boolean returning = player.level().dimension().equals(BLADE_FIELD);
        ServerLevel target = player.server.getLevel(returning ? Level.OVERWORLD : BLADE_FIELD);
        if (target == null) {
            player.displayClientMessage(Component.literal("§c剣の原へ通じる道が見つからない"), true);
            return;
        }
        BlockPos destination = returning ? target.getSharedSpawnPos() : new BlockPos(0, 80, 0);
        // FULLチャンクまで生成し、地形整形Featureの完了前に高さを読む競合を防ぐ。
        target.getChunk(destination.getX() >> 4, destination.getZ() >> 4);
        int safeY = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                destination.getX(), destination.getZ());
        BlockPos top = new BlockPos(destination.getX(), safeY, destination.getZ());
        target.playSound(null, top, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.7F, 1.2F);
        player.teleportTo(target, top.getX() + 0.5D, top.getY() + 0.05D, top.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
    }

    private static net.minecraft.world.item.Item weaponItem() {
        return BladeDimensionItems.BOUNDARY_BLADE.get();
    }
}
