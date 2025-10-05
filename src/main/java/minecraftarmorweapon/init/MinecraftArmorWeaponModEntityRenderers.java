
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package minecraftarmorweapon.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;

import minecraftarmorweapon.client.renderer.SkeltonMobRenderer;
import minecraftarmorweapon.client.renderer.OtiruyoRenderer;
import minecraftarmorweapon.client.renderer.MeteorArrowRenderer;
import minecraftarmorweapon.client.renderer.KillotiruRenderer;
import minecraftarmorweapon.client.renderer.KatanaTobuRenderer;
import minecraftarmorweapon.client.renderer.FlyingAttackerRenderer;
import minecraftarmorweapon.client.renderer.CometRenderer;
import minecraftarmorweapon.client.renderer.CometKillRenderer;
import minecraftarmorweapon.client.renderer.BlackholeRenderer;
import minecraftarmorweapon.client.renderer.AlchemyCraftBlockEntityRenderer;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MinecraftArmorWeaponModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(MinecraftArmorWeaponModEntities.SKELTON_MOB.get(), SkeltonMobRenderer::new);
		event.registerEntityRenderer(MinecraftArmorWeaponModEntities.OTIRUYO.get(), OtiruyoRenderer::new);
		event.registerEntityRenderer(MinecraftArmorWeaponModEntities.KILLOTIRU.get(), KillotiruRenderer::new);
		event.registerEntityRenderer(MinecraftArmorWeaponModEntities.KATANA_TOBU.get(), KatanaTobuRenderer::new);
		event.registerEntityRenderer(MinecraftArmorWeaponModEntities.BLACKHOLE.get(), BlackholeRenderer::new);
		event.registerEntityRenderer(MinecraftArmorWeaponModEntities.LOKI_DECOYDASU.get(), ThrownItemRenderer::new);
		event.registerEntityRenderer(MinecraftArmorWeaponModEntities.TEST_BOW.get(), ThrownItemRenderer::new);
		event.registerEntityRenderer(MinecraftArmorWeaponModEntities.COMET.get(), CometRenderer::new);
		event.registerEntityRenderer(MinecraftArmorWeaponModEntities.COMET_KILL.get(), CometKillRenderer::new);
		event.registerEntityRenderer(MinecraftArmorWeaponModEntities.METEOR_ARROW.get(), MeteorArrowRenderer::new);
		event.registerEntityRenderer(MinecraftArmorWeaponModEntities.ALCHEMY_CRAFT_BLOCK_ENTITY.get(), AlchemyCraftBlockEntityRenderer::new);
		event.registerEntityRenderer(MinecraftArmorWeaponModEntities.FLYING_ATTACKER.get(), FlyingAttackerRenderer::new);
	}
}
