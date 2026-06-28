package the_four_primitives_and_weapons.events;

import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import the_four_primitives_and_weapons.entity.SeedProjectileEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 種を持って「耕地 ( 上面 )・コンポスター・ソウルサンド ( 上面 ) 以外」 または「空中」 に
 * 右クリックすると、 種を飛ばす。 両手に種を持つとマシンガンのように弾幕が濃くなる。
 */
@Mod.EventBusSubscriber(modid = "the_four_primitives_and_weapons")
public class SeedShooterHandler {

    private static final Set<Item> SEEDS = Set.of(
            Items.WHEAT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.BEETROOT_SEEDS,
            Items.TORCHFLOWER_SEEDS, Items.PITCHER_POD);

    /** プレイヤーごとの直近発射 gameTime ( 連射の過剰発火防止 )。 */
    private static final Map<UUID, Long> LAST_FIRE = new HashMap<>();

    private static boolean isSeed(ItemStack s) {
        return s != null && !s.isEmpty() && SEEDS.contains(s.getItem());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!isSeed(event.getItemStack())) return;

        BlockState bs = event.getLevel().getBlockState(event.getPos());
        boolean farmlandPlant = bs.is(Blocks.FARMLAND) && event.getFace() == Direction.UP;   // 耕地に植える
        boolean composter = bs.is(Blocks.COMPOSTER);                                          // 堆肥化
        boolean soulSandPlant = bs.is(Blocks.SOUL_SAND) && event.getFace() == Direction.UP;   // ネザーウォート植え
        if (farmlandPlant || composter || soulSandPlant) return; // バニラの植え付け/堆肥に任せる

        if (!player.level().isClientSide) fire(player);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!isSeed(event.getItemStack())) return; // 空中右クリック等
        if (!player.level().isClientSide) fire(player);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void fire(Player player) {
        boolean mainSeed = isSeed(player.getMainHandItem());
        boolean offSeed = isSeed(player.getOffhandItem());
        if (!mainSeed && !offSeed) return;
        boolean dual = mainSeed && offSeed;

        // 連射スロットル ( 短め = 高頻度。 両手は毎tick、 片手は1tきおき )
        Level level = player.level();
        long now = level.getGameTime();
        Long last = LAST_FIRE.get(player.getUUID());
        if (last != null && now - last < (dual ? 1 : 2)) return;
        LAST_FIRE.put(player.getUUID(), now);

        int count = dual ? 12 : 1;         // 両手 = ショットガンの濃い弾幕
        float spread = dual ? 16f : 1.5f;  // 両手は広いコーン

        // 見た目に使う種 ( main 優先 )
        Item seedItem = (mainSeed ? player.getMainHandItem() : player.getOffhandItem()).getItem();

        for (int i = 0; i < count; i++) {
            SeedProjectileEntity proj = new SeedProjectileEntity(level, player);
            proj.setItem(new ItemStack(seedItem));
            proj.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            proj.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.6f, spread);
            level.addFreshEntity(proj);
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, dual ? 1.6f : 1.3f);

        // 消費 ( クリエイティブは消費しない )。 両手なら各手 1 個。
        if (!player.getAbilities().instabuild) {
            if (mainSeed) player.getMainHandItem().shrink(1);
            if (offSeed) player.getOffhandItem().shrink(1);
        }
    }
}
