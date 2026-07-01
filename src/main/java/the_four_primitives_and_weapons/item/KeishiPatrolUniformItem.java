package the_four_primitives_and_weapons.item;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import the_four_primitives_and_weapons.client.model.MeijiUniformArmorModels;

import java.util.function.Consumer;

/**
 * 警視抜刀隊 巡査 制服（ダブルボタン/細線/草鞋）。 軽装（革～銅程度）。
 */
public abstract class KeishiPatrolUniformItem extends ArmorItem {

	private static final String TEXTURE = "the_four_primitives_and_weapons:textures/entities/keishi_patrol_uniform.png";
	private static final String GLOVES = "the_four_primitives_and_weapons:textures/entities/keishi_patrol_gloves.png";

	public KeishiPatrolUniformItem(ArmorItem.Type type, Item.Properties properties) {
		super(new ArmorMaterial() {
			@Override
			public int getDurabilityForType(ArmorItem.Type type) {
				return new int[]{13, 15, 16, 11}[type.getSlot().getIndex()] * 12;
			}

			@Override
			public int getDefenseForType(ArmorItem.Type type) {
				return new int[]{2, 3, 4, 1}[type.getSlot().getIndex()];
			}

			@Override
			public int getEnchantmentValue() {
				return 9;
			}

			@Override
			public SoundEvent getEquipSound() {
				return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("item.armor.equip_leather"));
			}

			@Override
			public Ingredient getRepairIngredient() {
				return Ingredient.of(Items.LEATHER);
			}

			@Override
			public String getName() {
				return "keishi_patrol_uniform";
			}

			@Override
			public float getToughness() {
				return 0f;
			}

			@Override
			public float getKnockbackResistance() {
				return 0f;
			}
		}, type, properties);
	}

	public static class Helmet extends KeishiPatrolUniformItem {
		public Helmet() { super(ArmorItem.Type.HELMET, new Item.Properties()); }

		@Override
		public void initializeClient(Consumer<IClientItemExtensions> consumer) {
			consumer.accept(new IClientItemExtensions() {
				@Override
				@OnlyIn(Dist.CLIENT)
				public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
					return MeijiUniformArmorModels.helmet(living, defaultModel);
				}
			});
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) { return TEXTURE; }
	}

	public static class Chestplate extends KeishiPatrolUniformItem implements DyeableLeatherItem {
		public Chestplate() { super(ArmorItem.Type.CHESTPLATE, new Item.Properties()); }

		@Override
		public void initializeClient(Consumer<IClientItemExtensions> consumer) {
			consumer.accept(new IClientItemExtensions() {
				@Override
				@OnlyIn(Dist.CLIENT)
				public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
					return MeijiUniformArmorModels.chest(living, defaultModel);
				}
			});
		}

		// 色はバニラの display.color ではなく独自タグに保存する。
		// ( display.color に入れるとバニラが「Dyed」 と表示してしまうため )
		private static final String COLOR_TAG = "GloveColor";

		@Override
		public boolean hasCustomColor(ItemStack stack) {
			CompoundTag tag = stack.getTag();
			if (tag != null && tag.contains(COLOR_TAG, Tag.TAG_INT)) return true;
			return displayColor(stack) >= 0; // 皮装備方式 display.color でも可
		}

		/** 手袋の色。 未染色なら白。 */
		@Override
		public int getColor(ItemStack stack) {
			CompoundTag tag = stack.getTag();
			if (tag != null && tag.contains(COLOR_TAG, Tag.TAG_INT)) return tag.getInt(COLOR_TAG);
			int d = displayColor(stack);
			return d >= 0 ? d : 0xFFFFFF;
		}

		/** /give …{display:{color:N}} で入れた色。 無ければ -1。 */
		private static int displayColor(ItemStack stack) {
			CompoundTag tag = stack.getTag();
			if (tag != null && tag.contains("display", 10)) {
				CompoundTag d = tag.getCompound("display");
				if (d.contains("color", 99)) return d.getInt("color") & 0xFFFFFF;
			}
			return -1;
		}

		@Override
		public void setColor(ItemStack stack, int color) {
			stack.getOrCreateTag().putInt(COLOR_TAG, color);
		}

		@Override
		public void clearColor(ItemStack stack) {
			CompoundTag tag = stack.getTag();
			if (tag != null) tag.remove(COLOR_TAG);
		}
		// 色(16進数)のツールチップ表示は DyeTooltipHandler で一括処理する

		// type=="overlay" → 制服本体(非染色) / それ以外(=染色される層) → 手袋
		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
			return "overlay".equals(type) ? TEXTURE : GLOVES;
		}
	}

	public static class Leggings extends KeishiPatrolUniformItem {
		public Leggings() { super(ArmorItem.Type.LEGGINGS, new Item.Properties()); }

		@Override
		public void initializeClient(Consumer<IClientItemExtensions> consumer) {
			consumer.accept(new IClientItemExtensions() {
				@Override
				@OnlyIn(Dist.CLIENT)
				public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
					return MeijiUniformArmorModels.leggings(living, defaultModel);
				}
			});
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) { return TEXTURE; }
	}

	public static class Boots extends KeishiPatrolUniformItem {
		public Boots() { super(ArmorItem.Type.BOOTS, new Item.Properties()); }

		@Override
		public void initializeClient(Consumer<IClientItemExtensions> consumer) {
			consumer.accept(new IClientItemExtensions() {
				@Override
				@OnlyIn(Dist.CLIENT)
				public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
					return MeijiUniformArmorModels.boots(living, defaultModel);
				}
			});
		}

		@Override
		public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) { return TEXTURE; }
	}
}
