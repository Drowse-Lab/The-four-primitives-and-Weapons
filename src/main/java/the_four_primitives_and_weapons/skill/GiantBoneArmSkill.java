package the_four_primitives_and_weapons.skill;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import the_four_primitives_and_weapons.entity.GiantBoneArmEntity;

/**
 * 上腕骨刀の特殊技「巨骨の腕」発動ロジック (サーバー側)。
 * プレイヤーの肩から巨大な骨の腕を生やし、薙ぎ払い→地叩きの自動コンボを行う。
 */
public class GiantBoneArmSkill {

	/** 発動後のクールダウン (tick)。アイテムのクールダウンとして付与。 */
	public static final int COOLDOWN = 140; // 7秒

	public static void fire(Player player) {
		Level level = player.level();
		if (level.isClientSide)
			return;

		// 既に自分の腕が出ていれば二重発動しない
		boolean already = !level.getEntitiesOfClass(GiantBoneArmEntity.class,
				player.getBoundingBox().inflate(24),
				e -> e.getOwner() == player).isEmpty();
		if (already)
			return;

		// 武器に侵食(CORROSION)属性が付いていればレベルを渡す → 骨表面にガラス被膜
		ItemStack weapon = player.getMainHandItem();
		int corrosionLevel = 0;
		if (the_four_primitives_and_weapons.damage.ElementalDamageUtils.getElementType(weapon)
				== the_four_primitives_and_weapons.damage.ElementType.CORROSION) {
			corrosionLevel = the_four_primitives_and_weapons.damage.ElementalDamageUtils.getElementLevel(weapon);
		}

		float castYaw = player.getYRot();
		GiantBoneArmEntity arm = new GiantBoneArmEntity(level, player, castYaw, corrosionLevel);
		level.addFreshEntity(arm);

		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 1.5f, 0.5f);

		// アイテムにクールダウン付与
		ItemStack held = player.getMainHandItem();
		if (!held.isEmpty())
			player.getCooldowns().addCooldown(held.getItem(), COOLDOWN);
	}
}
