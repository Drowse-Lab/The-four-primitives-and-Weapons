package the_four_primitives_and_weapons.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import the_four_primitives_and_weapons.TheFourPrimitivesAndWeaponsMod;

import net.minecraft.client.KeyMapping;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 結晶ポーチのフローティングパネルを開く / 閉じるキーバインド ( デフォルト: B )。
 *
 *   - インベントリでポーチにカーソルを合わせて B → {@link PouchOverlay} が開く
 *   - 開いている時に B → 閉じる
 *   - 画面を開いていない時の B は何もしない
 *
 * 実際の入力処理・描画は {@link PouchOverlay} 側。 ここはキー登録のみ。
 * MCreator が再生成する {@code TheFourPrimitivesAndWeaponsModKeyMappings} とは別に登録し、
 * 再生成で消えないようにする。
 */
@Mod.EventBusSubscriber(modid = TheFourPrimitivesAndWeaponsMod.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CrystalPouchKeybind {

    // UNIVERSAL: インベントリ画面 ( GUI ) 内でもキーを判定できるようにする
    public static final KeyMapping OPEN_POUCH = new KeyMapping(
        "key.the_four_primitives_and_weapons.crystal_pouch",
        KeyConflictContext.UNIVERSAL,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_B,
        "key.categories.inventory");

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_POUCH);
    }
}
