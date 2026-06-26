package the_four_primitives_and_weapons.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

/**
 * 視界揺れ (画面揺れ)。{@link ViewportEvent.ComputeCameraAngles} でカメラの
 * roll / yaw / pitch にオフセットを加えるため、プレイヤーの実視点(回転値)は変えずに
 * 「視界ごと」揺れる。地叩きの衝撃用。距離減衰した intensity で開始する。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, value = Dist.CLIENT)
public class ScreenShakeHandler {

	private static float intensity = 0f;
	private static int duration = 0;
	private static int remaining = 0;
	private static float age = 0f; // 振動位相 (tick)

	/** 揺れ開始。強い揺れが来たら上書き(強い方を採用)。 */
	public static void start(float newIntensity, int newDuration) {
		if (newIntensity <= 0f || newDuration <= 0)
			return;
		// 既存の残り揺れより弱ければ無視
		float current = duration > 0 ? intensity * ((float) remaining / duration) : 0f;
		if (newIntensity >= current) {
			intensity = newIntensity;
			duration = newDuration;
			remaining = newDuration;
		}
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.isPaused())
			return;
		age += 1f;
		if (remaining > 0)
			remaining--;
	}

	@SubscribeEvent
	public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		if (remaining <= 0 || duration <= 0)
			return;
		float partial = (float) event.getPartialTick();
		float t = (remaining - partial) / duration; // 1→0 で減衰
		if (t <= 0f)
			return;
		float decay = t * t; // 終盤を速く収束
		float amp = 6.0f * intensity * decay; // 最大振れ角(度)
		float time = (age + partial);

		// 複数周波数の重ね合わせでランダム感を出す
		float roll = (Mth.sin(time * 1.7f) + 0.5f * Mth.sin(time * 3.3f)) * amp;
		float pitch = (Mth.sin(time * 2.1f + 1.3f)) * amp * 0.5f;
		float yaw = (Mth.cos(time * 1.9f + 0.7f)) * amp * 0.5f;

		event.setRoll(event.getRoll() + roll);
		event.setPitch(event.getPitch() + pitch);
		event.setYaw(event.getYaw() + yaw);
	}
}
