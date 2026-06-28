package the_four_primitives_and_weapons.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * バニラの大釜（water_cauldron）の位置ごとに「貯めた血液瓶のNBT」を保存する SavedData。
 * バニラ大釜は BlockEntity を持たないため、ServerLevel 単位でここに永続化する。
 * 各血液瓶の NBT（採血元の UUID 等）をそのまま保持し、取り出すと同じ NBT の血液瓶に戻る。
 * 量は大釜の LEVEL プロパティで管理し、見た目の色は {@link CauldronColorData} 側で管理する。
 */
public class CauldronBloodData extends SavedData {

	private static final String NAME = "tfp_cauldron_blood";

	/** 位置ごとに、注がれた血液瓶のNBTをスタック（LIFO）で保持。 */
	private final Map<Long, Deque<CompoundTag>> blood = new HashMap<>();

	public static CauldronBloodData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(CauldronBloodData::load, CauldronBloodData::new, NAME);
	}

	public boolean has(BlockPos pos) {
		Deque<CompoundTag> d = blood.get(pos.asLong());
		return d != null && !d.isEmpty();
	}

	public int count(BlockPos pos) {
		Deque<CompoundTag> d = blood.get(pos.asLong());
		return d == null ? 0 : d.size();
	}

	/** 血液瓶のNBT（null可）を1つ貯める。 */
	public void push(BlockPos pos, CompoundTag bottleTag) {
		blood.computeIfAbsent(pos.asLong(), k -> new ArrayDeque<>())
				.push(bottleTag == null ? new CompoundTag() : bottleTag.copy());
		setDirty();
	}

	/** 最後に貯めた血液瓶のNBTを取り出す（無ければ null）。 */
	public CompoundTag pop(BlockPos pos) {
		Deque<CompoundTag> d = blood.get(pos.asLong());
		if (d == null || d.isEmpty()) return null;
		CompoundTag t = d.pop();
		if (d.isEmpty()) blood.remove(pos.asLong());
		setDirty();
		return t;
	}

	public void remove(BlockPos pos) {
		if (blood.remove(pos.asLong()) != null) setDirty();
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Map.Entry<Long, Deque<CompoundTag>> e : blood.entrySet()) {
			CompoundTag t = new CompoundTag();
			t.putLong("p", e.getKey());
			ListTag bottles = new ListTag();
			// スタックの底→上の順で保存し、ロード時に push し直して順序を復元する
			java.util.List<CompoundTag> ordered = new java.util.ArrayList<>(e.getValue());
			for (int i = ordered.size() - 1; i >= 0; i--) {
				bottles.add(ordered.get(i));
			}
			t.put("bottles", bottles);
			list.add(t);
		}
		tag.put("entries", list);
		return tag;
	}

	public static CauldronBloodData load(CompoundTag tag) {
		CauldronBloodData data = new CauldronBloodData();
		ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag t = list.getCompound(i);
			ListTag bottles = t.getList("bottles", Tag.TAG_COMPOUND);
			Deque<CompoundTag> d = new ArrayDeque<>();
			for (int j = 0; j < bottles.size(); j++) {
				d.push(bottles.getCompound(j));
			}
			if (!d.isEmpty()) data.blood.put(t.getLong("p"), d);
		}
		return data;
	}
}
