package the_four_primitives_and_weapons.integration.jei;

import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import net.minecraft.client.renderer.Rect2i;
import the_four_primitives_and_weapons.client.event.PouchOverlay;

import java.util.Collections;
import java.util.List;

/**
 * 結晶ポーチのフローティングパネルが開いている領域を JEI に「GUI 占有域」として伝える。
 * これにより JEI のアイテム一覧がパネルの下に描画されず、 パネル越しに背後のアイテム名
 * ( ツールチップ ) が出てしまう問題を防ぐ。
 */
public class PouchPanelGuiHandler implements IGlobalGuiHandler {

    @Override
    public java.util.Collection<Rect2i> getGuiExtraAreas() {
        Rect2i area = PouchOverlay.getPanelArea();
        return area == null ? Collections.emptyList() : List.of(area);
    }
}
