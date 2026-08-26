package the_four_primitives_and_weapons.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.entity.LunaCompanionEntity;

import java.util.List;

/** 所有プレイヤーへの致死ダメージが確定した時点で、死亡処理より先にLunaを回収する。 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID)
public final class LunaOwnerDeathRecallHandler {
    private LunaOwnerDeathRecallHandler() {}

    @SubscribeEvent
    public static void beforeOwnerDeath(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getAmount() < player.getHealth()
                || !(player.level() instanceof ServerLevel level)) return;

        List<LunaCompanionEntity> companions = level.getEntitiesOfClass(
                LunaCompanionEntity.class,
                player.getBoundingBox().inflate(128.0),
                luna -> luna.isOwnedBy(player.getUUID()));
        companions.forEach(LunaCompanionEntity::recallToOwner);
    }
}
