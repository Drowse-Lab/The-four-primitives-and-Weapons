package minecraftarmorweapon.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;

import minecraftarmorweapon.procedures.ZokuseiritokutyuwazaProcedure;
import minecraftarmorweapon.procedures.IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure;

import minecraftarmorweapon.init.MinecraftArmorWeaponModTabs;

import javax.annotation.Nullable;
import java.util.List;

public class EnmaItem extends SwordItem {
    public EnmaItem() {
        super(new Tier() {
            public int getUses() {
                return 0;
            }

            public float getSpeed() {
                return 10f;
            }

            public float getAttackDamageBonus() {
                return 2f;
            }

            public int getLevel() {
                return 1;
            }

            public int getEnchantmentValue() {
                return 10;
            }

            public Ingredient getRepairIngredient() {
                return Ingredient.of();
            }
        }, 3, -2.4f, 
        new Item.Properties()
            .tab(MinecraftArmorWeaponModTabs.TAB_WEAPON)
            .rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
        InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
        ZokuseiritokutyuwazaProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
        return ar;
    }

    @Override
    public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(itemstack, world, entity, slot, selected);
        if (selected)
            IronKatanaturuwoShoudeChituteiruJiannoteitukuProcedure.execute(world, entity.getX(), entity.getY(), entity.getZ(), entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);

        // 通常時の説明
        tooltip.add(new TranslatableComponent("tooltip.enmaitem.basic_info"));

        // SHIFTを押している場合の詳細情報
        if (net.minecraft.client.KeyboardHelper.isShiftPressed()) {
            tooltip.add(new TextComponent("JSON Data: {"));
            tooltip.add(new TextComponent("  \"generic.attack_damage\": " + stack.getDamageValue() + ","));
            tooltip.add(new TextComponent("  \"enchantment\": " + stack.getEnchantmentTags()));
            tooltip.add(new TextComponent("}"));
        } else {
            tooltip.add(new TranslatableComponent("tooltip.enmaitem.shift_for_more"));
        }
    }
}
