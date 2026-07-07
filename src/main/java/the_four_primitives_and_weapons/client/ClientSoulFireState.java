package the_four_primitives_and_weapons.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * クライアント側で「炎を魂の炎(青い炎)として描画すべきエンティティ」の集合を保持する。
 * サーバーからの {@link the_four_primitives_and_weapons.network.SoulFireSyncPacket} で更新され、
 * 炎描画の Mixin ({@code EntityRenderDispatcherFireMixin} / {@code ScreenEffectFireMixin}) から参照される。
 *
 * <p>通常の炎テクスチャ(fire_0/fire_1)の赤↔青を入れ替えた「同じ形の青い炎」スプライトに
 * 差し替えて描画する。 これらは block アトラスに single ソースとして登録済み。</p>
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientSoulFireState {

	/** バニラの ModelBakery.FIRE_0/FIRE_1 と同じ block アトラス上の、青くした炎スプライト。 */
	public static final Material BLUE_FIRE_0 = new Material(TextureAtlas.LOCATION_BLOCKS,
			new ResourceLocation("the_four_primitives_and_weapons", "block/blue_fire_0"));
	public static final Material BLUE_FIRE_1 = new Material(TextureAtlas.LOCATION_BLOCKS,
			new ResourceLocation("the_four_primitives_and_weapons", "block/blue_fire_1"));

	private static final Set<Integer> SOUL_BURNING = ConcurrentHashMap.newKeySet();

	public static void set(int entityId, boolean soul) {
		if (soul) {
			SOUL_BURNING.add(entityId);
		} else {
			SOUL_BURNING.remove(entityId);
		}
	}

	public static boolean isSoul(Entity entity) {
		return entity != null && !SOUL_BURNING.isEmpty() && SOUL_BURNING.contains(entity.getId());
	}

	public static void clear() {
		SOUL_BURNING.clear();
	}

	/**
	 * 保険のクリーンアップ。 消滅/視界外(entity==null)になった ID だけを間引く。
	 *
	 * <p>「炎が消えたら外す」判定はここでは行わない。 同期パケットが届いた直後は
	 * まだ炎フラグ(isOnFire)がクライアントへ同期されておらず、ここで消すと
	 * サーバーは状態変化なしとみなして再送しないため、永久にオレンジのままになる。
	 * 炎終了時の解除はサーバーが送る false パケット({@link #set})に任せる。</p>
	 */
	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || SOUL_BURNING.isEmpty()) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			SOUL_BURNING.clear();
			return;
		}
		SOUL_BURNING.removeIf(id -> mc.level.getEntity(id) == null);
	}
}
