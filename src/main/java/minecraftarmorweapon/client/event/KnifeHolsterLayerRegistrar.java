package minecraftarmorweapon.client.event;

import minecraftarmorweapon.MinecraftArmorWeaponMod;
import minecraftarmorweapon.client.renderer.KnifeHolsterLayer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * プレイヤーレンダラーに KnifeHolsterLayer を追加する登録クラス。
 * MOD bus (Mod.EventBusSubscriber.Bus.MOD) の AddLayers イベントで登録。
 */
@Mod.EventBusSubscriber(modid = MinecraftArmorWeaponMod.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KnifeHolsterLayerRegistrar {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // "default" (スティーブ) と "slim" (アレックス) 両方のプレイヤーレンダラーに追加
        for (String skin : event.getSkins()) {
            EntityRenderer<?> r = event.getSkin(skin);
            if (r instanceof PlayerRenderer pr) {
                ((LivingEntityRenderer) pr).addLayer(
                    new KnifeHolsterLayer((PlayerRenderer) pr));
            }
        }
    }
}
