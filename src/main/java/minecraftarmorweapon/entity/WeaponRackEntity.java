package minecraftarmorweapon.entity;

import minecraftarmorweapon.init.CustomEntityInit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

/**
 * Arsenal (doctor4t) の WeaponRackEntity を Forge 1.20.1 用に 1:1 移植。
 * vanilla の ItemFrame を継承し、displayable タグ判定 / 非表示化トグル / 解除条件をオーバーライド。
 */
public class WeaponRackEntity extends ItemFrame {

	public static final TagKey<Item> DISPLAYABLE = TagKey.create(Registries.ITEM,
		new ResourceLocation("minecraft_armor_weapon", "displayable"));

	private static final Predicate<Entity> HANGING_PREDICATE = e -> e instanceof HangingEntity;

	public WeaponRackEntity(EntityType<? extends WeaponRackEntity> type, Level level) {
		super(type, level);
	}

	public WeaponRackEntity(Level level, BlockPos pos, Direction facing) {
		this(CustomEntityInit.WEAPON_RACK.get(), level, pos, facing);
	}

	public WeaponRackEntity(EntityType<? extends WeaponRackEntity> type, Level level, BlockPos pos, Direction facing) {
		super(type, level, pos, facing);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		// Sneak+右クリックで台座を非表示化トグル: 武器が入っている + 手が空の時のみ
		if (player.isShiftKeyDown()
				&& !this.getItem().isEmpty()
				&& player.getItemInHand(hand).isEmpty()) {
			this.setInvisible(!this.isInvisible());
			return InteractionResult.SUCCESS;
		}

		ItemStack stackInHand = player.getItemInHand(hand);
		if (!this.getItem().isEmpty()
				|| (this.getItem().isEmpty() && stackInHand.is(DISPLAYABLE))) {
			return super.interact(player, hand);
		}
		return InteractionResult.PASS;
	}

	@Override
	public boolean isInvisible() {
		return super.isInvisible() && !this.getItem().isEmpty();
	}

	@Override
	public boolean survives() {
		return this.level().getEntities(this, this.getBoundingBox(), HANGING_PREDICATE).isEmpty();
	}

	@Override
	protected ItemStack getFrameItemStack() {
		return new ItemStack(CustomEntityInit.WEAPON_RACK_ITEM.get());
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return source.getEntity() instanceof Player p
			&& this.getItem().isEmpty()
			&& !p.isShiftKeyDown();
	}
}
