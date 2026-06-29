package the_four_primitives_and_weapons.item;

import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import the_four_primitives_and_weapons.util.SayaRegistry;

import java.util.List;

public class RapierSayaItem extends Item implements ICurioItem {

    public RapierSayaItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level world, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, world, list, flag);
        String sayaHex = the_four_primitives_and_weapons.util.SayaDesign.getBaseHex(stack);
        if (sayaHex != null) list.add(Component.translatable("tooltip.the_four_primitives_and_weapons.saya.base", sayaHex));
        Component sayaFinish = the_four_primitives_and_weapons.util.SayaStyles.finishName(
                the_four_primitives_and_weapons.util.SayaDesign.getStyle(stack),
                the_four_primitives_and_weapons.util.SayaDesign.getLacquer(stack));
        if (sayaFinish != null)
            list.add(Component.translatable("tooltip.the_four_primitives_and_weapons.saya.style", sayaFinish));
        if (stack.hasTag() && stack.getTag().contains("StoredRapier")) {
            ItemStack stored = ItemStack.of(stack.getTag().getCompound("StoredRapier"));
            list.add(Component.translatable("tooltip.the_four_primitives_and_weapons.saya.sheathed", stored.getHoverName()));
        } else {
            list.add(Component.translatable("tooltip.the_four_primitives_and_weapons.saya.empty_rapier"));
            list.add(Component.translatable("tooltip.the_four_primitives_and_weapons.saya.hint"));
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack itemstack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack itemstack) {
        return 0;
    }

    public static boolean canSheathe(ItemStack rapierStack) {
        return SayaRegistry.isRegistered(SayaRegistry.SayaType.RAPIER, rapierStack);
    }

    public static void sheatheRapier(Player player, ItemStack rapierStack, ItemStack sheathStack,
                                     InteractionHand rapierHand, InteractionHand sheathHand) {
        if (rapierHand == sheathHand) return; // 同じ手は不可
        CompoundTag sheathTag = sheathStack.getOrCreateTag();

        if (!sheathTag.contains("StoredRapier")) {
            CompoundTag rapierData = rapierStack.save(new CompoundTag());
            sheathTag.put("StoredRapier", rapierData);

            // 見た目は SayaModelWrapper が StoredRapier NBT を読んで動的に解決する。
            sheathStack.setTag(sheathTag);

            player.setItemInHand(rapierHand, ItemStack.EMPTY);
            player.setItemInHand(sheathHand, sheathStack);

            player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.4F);
        } else {
            player.displayClientMessage(Component.literal("§c鞘には既にレイピアが入っています"), true);
        }
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        String id = slotContext.identifier();
        return id.equals("belt") || id.equals("back");
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return false;
    }

    public static int getRapierModelData(ItemStack rapier) {
        return SayaRegistry.getModelData(SayaRegistry.SayaType.RAPIER, rapier);
    }
}
