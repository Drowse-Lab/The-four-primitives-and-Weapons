package the_four_primitives_and_weapons.client;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;
import the_four_primitives_and_weapons.network.CauldronColorMessage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * クライアント側の大釜色キャッシュ + バニラ大釜の水(tintindex 0)への着色。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CauldronColorClient {

	private static final Map<Long, Integer> COLORS = new ConcurrentHashMap<>();

	public static Integer get(BlockPos pos) {
		return COLORS.get(pos.asLong());
	}

	/** サーバーからの同期メッセージを反映（クライアントスレッドで呼ぶ） */
	public static void apply(CauldronColorMessage m) {
		if (m.replaceAll) COLORS.clear();
		for (int i = 0; i < m.positions.length; i++) {
			if (m.colors[i] == CauldronColorMessage.REMOVE) COLORS.remove(m.positions[i]);
			else COLORS.put(m.positions[i], m.colors[i]);
		}
		// 色を変えただけではブロックは再描画されない（ブロックカラーは再メッシュ時のみ評価）。
		// 該当セクションを dirty にして再描画させ、 水の色を即時反映する。
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.level != null && mc.levelRenderer != null) {
			for (long p : m.positions) {
				BlockPos pos = BlockPos.of(p);
				mc.levelRenderer.setSectionDirty(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
			}
		}
	}

	/** バニラ水ブロックと同じ既定水色（バイオーム情報が無い時のフォールバック） */
	private static final int DEFAULT_WATER = 0x3F76E4;

	@SubscribeEvent
	public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
		event.getBlockColors().register((state, level, pos, tintIndex) -> {
			if (tintIndex == 0 && pos != null) {
				Integer c = COLORS.get(pos.asLong());
				if (c != null) return 0xFF000000 | c;                 // 染色済み → その色
				if (level != null) return net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(level, pos);
				return DEFAULT_WATER;                                  // 普通の水 → バニラの水色
			}
			return -1;
		}, Blocks.WATER_CAULDRON);
	}
}
