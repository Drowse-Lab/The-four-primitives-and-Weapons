package the_four_primitives_and_weapons.events;

import the_four_primitives_and_weapons.item.ZombieHeartItem;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * ゾンビの心臓(頭装備)のハンドラ。
 *
 *  - 頭に装備している間だけ 最大体力 +4 を付与する ( 属性/NBT 経路に依存しない確実な方式 )。
 *    装備した瞬間に増えた 4 分を回復して "空ハート" ではなく満タンで見えるようにする。
 *  - [診断] 装備変化 / 被ダメージ時に 頭スロット・インベントリの心臓の有無をログ出力し、
 *    "外して被ダメージで消える" 現象の発生箇所を特定する ( 原因特定後に削除予定 )。
 */
@Mod.EventBusSubscriber
public class ZombieHeartHandler {

	/** 最大体力ボーナス用の固定 UUID。 */
	private static final UUID MAX_HEALTH_UUID = UUID.fromString("b7c2f1e4-0a3d-4c9e-9b6f-2d5a7e8c1f30");
	private static final double BONUS = 4.0;

	private static boolean isHeart(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.getItem() instanceof ZombieHeartItem;
	}

	@SubscribeEvent
	public static void onEquipChange(LivingEquipmentChangeEvent event) {
		if (event.getSlot() != EquipmentSlot.HEAD)
			return;
		LivingEntity entity = event.getEntity();
		AttributeInstance maxHp = entity.getAttribute(Attributes.MAX_HEALTH);
		if (maxHp == null)
			return;

		boolean nowWearing = isHeart(event.getTo());
		boolean wasWearing = isHeart(event.getFrom());

		// 既存の修飾を必ず一旦除去 ( 冪等 )。
		if (maxHp.getModifier(MAX_HEALTH_UUID) != null)
			maxHp.removeModifier(MAX_HEALTH_UUID);

		if (nowWearing) {
			maxHp.addTransientModifier(new AttributeModifier(
					MAX_HEALTH_UUID, "Zombie Heart max health", BONUS, AttributeModifier.Operation.ADDITION));
			// 増えた分を回復して満タン表示に ( "体力が増えない" ように見える対策 )。
			entity.setHealth(Math.min((float) maxHp.getValue(), entity.getHealth() + (float) BONUS));
		}

		System.out.println("[ZombieHeart] equipChange entity=" + entity.getName().getString()
				+ " was=" + event.getFrom().getItem() + " to=" + event.getTo().getItem()
				+ " nowWearing=" + nowWearing + " wasWearing=" + wasWearing
				+ " maxHP=" + maxHp.getValue() + " curHP=" + entity.getHealth());
	}

	/** [診断] 被ダメージ時、心臓が頭 / インベントリのどこにあるかを記録する。 */
	@SubscribeEvent
	public static void onHurt(LivingHurtEvent event) {
		if (!(event.getEntity() instanceof Player player))
			return;
		if (player.level().isClientSide())
			return;
		boolean onHead = isHeart(player.getItemBySlot(EquipmentSlot.HEAD));
		int inInv = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (isHeart(player.getInventory().getItem(i)))
				inInv++;
		}
		if (onHead || inInv > 0) {
			System.out.println("[ZombieHeart] onHurt src=" + event.getSource().getMsgId()
					+ " amount=" + event.getAmount() + " onHead=" + onHead + " inInventory=" + inInv);
		}
	}
}
