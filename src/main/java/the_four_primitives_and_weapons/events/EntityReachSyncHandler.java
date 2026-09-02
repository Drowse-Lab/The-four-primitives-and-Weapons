package the_four_primitives_and_weapons.events;

import the_four_primitives_and_weapons.init.MawExtraAttributes;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * カスタム entity_reach (デフォルト 3.0 からの差分) を Forge のリーチ attribute へ反映する。
 *
 * 以前は LivingEntity#getAttributeValue へ Mixin していたが、あのメソッドは移動速度・攻撃力など
 * 全 attribute の取得で毎tick何度も呼ばれるホットパスのため、負荷軽減のため
 * 「差分を transient modifier として貼り直す」方式へ変更した。
 * 判定はサーバー側のプレイヤーのみ・10 tick に 1 回で、値が変わっていなければ何もしない。
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class EntityReachSyncHandler {

    /** 反映用 modifier の固定 UUID。 */
    private static final UUID REACH_MODIFIER_ID = UUID.fromString("3f7f1d54-9a3c-4f2a-9c1e-2b6d0a5e77c1");
    private static final String REACH_MODIFIER_NAME = "tfpw_entity_reach";
    private static final int UPDATE_INTERVAL = 10;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return; // サーバー側で貼れば attribute 同期でクライアントへ届く
        if (player.tickCount % UPDATE_INTERVAL != 0) return;

        AttributeInstance forgeReach = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        AttributeInstance custom = player.getAttribute(MawExtraAttributes.ENTITY_REACH.get());
        if (forgeReach == null || custom == null) return;

        double bonus = custom.getValue() - MawExtraAttributes.ENTITY_REACH.get().getDefaultValue();
        AttributeModifier applied = forgeReach.getModifier(REACH_MODIFIER_ID);

        if (bonus == 0.0) {
            if (applied != null) forgeReach.removeModifier(REACH_MODIFIER_ID);
            return;
        }
        if (applied != null) {
            if (applied.getAmount() == bonus) return; // 変化なし
            forgeReach.removeModifier(REACH_MODIFIER_ID);
        }
        forgeReach.addTransientModifier(new AttributeModifier(
            REACH_MODIFIER_ID, REACH_MODIFIER_NAME, bonus, AttributeModifier.Operation.ADDITION));
    }
}
