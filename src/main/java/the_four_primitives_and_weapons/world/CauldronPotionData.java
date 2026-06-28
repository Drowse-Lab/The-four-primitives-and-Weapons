package the_four_primitives_and_weapons.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * バニラの大釜（water_cauldron）の位置ごとに「貯めたポーションの混合エフェクト」を保存する SavedData。
 * バニラ大釜は BlockEntity を持たないため、 ServerLevel 単位でここに永続化する。
 * ( 見た目の色は {@link CauldronColorData} 側で管理 / 量は大釜の LEVEL プロパティで管理 )
 */
public class CauldronPotionData extends SavedData {

	private static final String NAME = "tfp_cauldron_potions";

	private final Map<Long, List<MobEffectInstance>> effects = new HashMap<>();

	public static CauldronPotionData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(CauldronPotionData::load, CauldronPotionData::new, NAME);
	}

	/** その位置に貯めた混合エフェクト ( 無ければ null )。 */
	public List<MobEffectInstance> getEffects(BlockPos pos) {
		return effects.get(pos.asLong());
	}

	public boolean has(BlockPos pos) {
		List<MobEffectInstance> e = effects.get(pos.asLong());
		return e != null && !e.isEmpty();
	}

	/** 貯蔵中の全ポーション大釜の位置 ( long ) を返す ( パーティクル演出用 )。 */
	public java.util.Set<Long> positions() {
		return new java.util.HashSet<>(effects.keySet());
	}

	public void setEffects(BlockPos pos, List<MobEffectInstance> list) {
		effects.put(pos.asLong(), new ArrayList<>(list));
		setDirty();
	}

	public void remove(BlockPos pos) {
		if (effects.remove(pos.asLong()) != null) setDirty();
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Map.Entry<Long, List<MobEffectInstance>> e : effects.entrySet()) {
			CompoundTag t = new CompoundTag();
			t.putLong("p", e.getKey());
			ListTag fx = new ListTag();
			for (MobEffectInstance mei : e.getValue()) {
				fx.add(mei.save(new CompoundTag()));
			}
			t.put("fx", fx);
			list.add(t);
		}
		tag.put("entries", list);
		return tag;
	}

	public static CauldronPotionData load(CompoundTag tag) {
		CauldronPotionData data = new CauldronPotionData();
		ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag t = list.getCompound(i);
			ListTag fx = t.getList("fx", Tag.TAG_COMPOUND);
			List<MobEffectInstance> effs = new ArrayList<>();
			for (int j = 0; j < fx.size(); j++) {
				MobEffectInstance mei = MobEffectInstance.load(fx.getCompound(j));
				if (mei != null) effs.add(mei);
			}
			if (!effs.isEmpty()) data.effects.put(t.getLong("p"), effs);
		}
		return data;
	}
}
