package the_four_primitives_and_weapons.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import the_four_primitives_and_weapons.util.SayaRegistry;

import java.util.List;

/** ダガー用の鞘。 {@link RapierSayaItem} をなぞった実装 ( StoredDagger / SayaType.DAGGER )。 */
public class DaggerSayaItem extends Item implements ICurioItem {

    public DaggerSayaItem() {
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

        if (stack.hasTag() && stack.getTag().contains("StoredDagger")) {
            ItemStack stored = ItemStack.of(stack.getTag().getCompound("StoredDagger"));
            list.add(Component.translatable("tooltip.the_four_primitives_and_weapons.saya.sheathed", stored.getHoverName()));
        } else {
            list.add(Component.translatable("tooltip.the_four_primitives_and_weapons.saya.empty_dagger"));
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

    public static boolean canSheathe(ItemStack daggerStack) {
        return SayaRegistry.isRegistered(SayaRegistry.SayaType.DAGGER, daggerStack);
    }

    public static void sheatheDagger(Player player, ItemStack daggerStack, ItemStack sheathStack,
                                     InteractionHand daggerHand, InteractionHand sheathHand) {
        if (daggerHand == sheathHand) return; // 同じ手は不可
        CompoundTag sheathTag = sheathStack.getOrCreateTag();

        if (!sheathTag.contains("StoredDagger")) {
            CompoundTag daggerData = daggerStack.save(new CompoundTag());
            sheathTag.put("StoredDagger", daggerData);

            // 見た目は SayaModelWrapper が StoredDagger NBT を読んで動的に解決する。
            sheathStack.setTag(sheathTag);

            player.setItemInHand(daggerHand, ItemStack.EMPTY);
            player.setItemInHand(sheathHand, sheathStack);

            player.playSound(SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 1.4F);
        } else {
            player.displayClientMessage(Component.literal("§c鞘には既にダガーが入っています"), true);
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

    public static int getDaggerModelData(ItemStack dagger) {
        return SayaRegistry.getModelData(SayaRegistry.SayaType.DAGGER, dagger);
    }
}
