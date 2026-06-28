package the_four_primitives_and_weapons.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * 大釜（バニラの water_cauldron）の位置ごとの染色色を保存する SavedData。
 * バニラ大釜は BlockEntity を持たないため、 色は ServerLevel 単位でここに永続化する。
 */
public class CauldronColorData extends SavedData {

	private static final String NAME = "tfp_cauldron_colors";

	private final Map<Long, Integer> colors = new HashMap<>();
	/** 位置ごとの「投入した染料の数」。 混色の重み付け平均に使う。 */
	private final Map<Long, Integer> counts = new HashMap<>();

	public static CauldronColorData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(CauldronColorData::load, CauldronColorData::new, NAME);
	}

	public Integer getColor(BlockPos pos) {
		return colors.get(pos.asLong());
	}

	/** これまでに投入された染料数 ( 0 = まだ無し )。 */
	public int getCount(BlockPos pos) {
		return counts.getOrDefault(pos.asLong(), 0);
	}

	public void setColor(BlockPos pos, int color) {
		colors.put(pos.asLong(), color);
		setDirty();
	}

	/** 色と投入数を同時に設定。 */
	public void setColor(BlockPos pos, int color, int count) {
		colors.put(pos.asLong(), color);
		counts.put(pos.asLong(), count);
		setDirty();
	}

	public void removeColor(BlockPos pos) {
		boolean changed = colors.remove(pos.asLong()) != null;
		changed |= counts.remove(pos.asLong()) != null;
		if (changed) setDirty();
	}

	public Map<Long, Integer> all() {
		return colors;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Map.Entry<Long, Integer> e : colors.entrySet()) {
			CompoundTag t = new CompoundTag();
			t.putLong("p", e.getKey());
			t.putInt("c", e.getValue());
			t.putInt("n", counts.getOrDefault(e.getKey(), 1));
			list.add(t);
		}
		tag.put("entries", list);
		return tag;
	}

	public static CauldronColorData load(CompoundTag tag) {
		CauldronColorData data = new CauldronColorData();
		ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag t = list.getCompound(i);
			data.colors.put(t.getLong("p"), t.getInt("c"));
			data.counts.put(t.getLong("p"), t.contains("n") ? t.getInt("n") : 1);
		}
		return data;
	}
}
