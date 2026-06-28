package the_four_primitives_and_weapons.events;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.registries.ForgeRegistries;

import the_four_primitives_and_weapons.init.TheFourPrimitivesAndWeaponsModItems;

/**
 * ガラス瓶を持って生物mobを右クリックすると、少量のダメージを与えつつ採血し、
 * blood_bottle（血液入りの瓶）を入手できる。
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class BloodCollectHandler {

	private static final float COLLECT_DAMAGE = 1.0f; // 0.5ハート

	@SubscribeEvent
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		Player player = event.getEntity();
		InteractionHand hand = event.getHand();
		ItemStack stack = player.getItemInHand(hand);

		// ガラス瓶を持っているときだけ採血
		if (stack.getItem() != Items.GLASS_BOTTLE) {
			return;
		}

		Entity target = event.getTarget();
		// 生物mobのみ（プレイヤーからは採血しない）
		if (!(target instanceof LivingEntity living) || target instanceof Player || !living.isAlive()) {
			return;
		}

		Level level = player.level();
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));

		if (!level.isClientSide) {
			// 少量ダメージを与えて採血
			living.hurt(living.damageSources().playerAttack(player), COLLECT_DAMAGE);

			// ガラス瓶を1個消費（クリエイティブでは消費しない）
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}

			// blood_bottle を付与（採血元mobのUUID・種別をNBTに保存）
			ItemStack blood = new ItemStack(TheFourPrimitivesAndWeaponsModItems.BLOOD_BOTTLE.get());
			CompoundTag tag = blood.getOrCreateTag();
			tag.putUUID("BloodSourceUUID", living.getUUID());
			var typeKey = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
			if (typeKey != null) {
				tag.putString("BloodSourceType", typeKey.toString());
			}
			if (!player.addItem(blood)) {
				player.drop(blood, false);
			}

			level.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8f, 1.0f);
		}

		player.swing(hand, true);
	}
}
