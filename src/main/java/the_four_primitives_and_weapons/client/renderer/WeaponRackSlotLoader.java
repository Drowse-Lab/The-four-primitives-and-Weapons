package the_four_primitives_and_weapons.client.renderer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Blockbench で編集可能な「武器配置位置マーカー」を rack model JSON から読み取る。
 *
 * モデル JSON 内に {@code "name": "slot1"} / {@code "name": "slot2"} の要素を置き、
 * その中心座標 (1/16 単位) をブロック単位 (0–1) に変換して提供する。
 * 要素には {@code "faces": {}} を指定して in-game では非表示にすること。
 *
 * 値はキャッシュされ、再起動するまで再読込しない。
 */
public class WeaponRackSlotLoader {

    public static class SlotPos {
        public final float x, y, z; // ブロック単位 (0.0 ～ 1.0)
        SlotPos(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    }

    private static final Map<String, Map<String, SlotPos>> CACHE = new HashMap<>();

    /**
     * 指定 rack タイプの指定スロット位置を取得。
     * 見つからなければ null。
     */
    public static SlotPos get(String rackType, String slotName) {
        return CACHE.computeIfAbsent(rackType, WeaponRackSlotLoader::load).get(slotName);
    }

    private static Map<String, SlotPos> load(String rackType) {
        Map<String, SlotPos> result = new HashMap<>();
        ResourceLocation rl = new ResourceLocation("the_four_primitives_and_weapons",
            "models/block/" + rackType + ".json");
        try {
            Optional<Resource> resOpt = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (resOpt.isEmpty()) {                return result;
            }
            try (InputStream is = resOpt.get().open();
                 InputStreamReader r = new InputStreamReader(is)) {
                JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                if (!root.has("elements")) {                    return result;
                }
                JsonArray elements = root.getAsJsonArray("elements");
                for (JsonElement el : elements) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    if (!obj.has("name")) continue;
                    String name = obj.get("name").getAsString();
                    if (!name.startsWith("slot")) continue;
                    if (!obj.has("from") || !obj.has("to")) continue;
                    JsonArray from = obj.getAsJsonArray("from");
                    JsonArray to = obj.getAsJsonArray("to");
                    if (from.size() < 3 || to.size() < 3) continue;
                    float cx = (from.get(0).getAsFloat() + to.get(0).getAsFloat()) / 2.0f / 16.0f;
                    float cy = (from.get(1).getAsFloat() + to.get(1).getAsFloat()) / 2.0f / 16.0f;
                    float cz = (from.get(2).getAsFloat() + to.get(2).getAsFloat()) / 2.0f / 16.0f;
                    result.put(name, new SlotPos(cx, cy, cz));                }
            }
        } catch (Throwable t) {        }
        return result;
    }

    /** /reload 等でマーカーを再読込したい時に呼ぶ。 */
    public static void clearCache() {
        CACHE.clear();    }

    /** リソース再読込で自動的にキャッシュクリア。 */
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(
        modid = "the_four_primitives_and_weapons",
        value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ReloadListener {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onPlayerLogin(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
            clearCache();
        }
    }
}
