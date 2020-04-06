package li.cryx.convth.block;

import li.cryx.convth.ConvenientThingsMod;
import li.cryx.convth.feature.ResourcePlants;
import net.fabricmc.fabric.api.block.FabricBlockSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TickPriority;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * A plant that grows resources.
 *
 * <p>
 * Growing mechanics is handled by the parent class <code>CropBlock</code>.
 * </p>
 *
 * @author cryxli
 * @see ResourcePlants
 */
public class AbstractResourcePlant extends CropBlock {

    // seconds
    private static final int AVERAGE_RANDOM_TICK = 34;

    /**
     * Chance (1 in 100) to drop an additional seed when mature plant is broken.
     */
    private static final int SEED_DROP_CHANCE = 5;

    /**
     * The item that is the seed for this plan.
     */
    private Item seedItem;

    /**
     * The item produced by this plant.
     */
    private Item resourceItem;

    /**
     * The item produced by this plant.
     */
    private Supplier<Item> resourceItemSupplier;

    /**
     * LootContext for picking plant while leaving it planted
     */
    private LootContextParameter<Boolean> onPick = new LootContextParameter<Boolean>(new Identifier(ConvenientThingsMod.MOD_ID,"picked"));

    private int colorTint;

    private AbstractResourcePlant() {
        super(FabricBlockSettings.of(Material.PLANT).noCollision().ticksRandomly().breakInstantly()
                .sounds(BlockSoundGroup.CROP).build());
    }

    /**
     * Create a new resource plant.
     *
     * @param resourceItem The <code>Item</code> produced by this plant.
     */
    public AbstractResourcePlant(final Item resourceItem, int colorTint) {
        this();
        this.colorTint = colorTint;
        this.resourceItem = resourceItem;
    }

    public AbstractResourcePlant(final Supplier<Item> resourceItemSupplier, int colorTint) {
        this();
        this.colorTint = colorTint;
        this.resourceItemSupplier = resourceItemSupplier;
    }

    /*@Override
    public void afterBreak(final World world, final PlayerEntity player, final BlockPos pos, final BlockState state,
                           final BlockEntity blockEntity, final ItemStack stack) {
        if (!world.isClient()) {
            final boolean mature = isMature(state);
            // Always drop the seed when the plant is broken.
            final int amount;
            if (mature && SEED_DROP_CHANCE > world.random.nextInt(100)) {
                // Drop an additional seed, if the plant is mature with a certain chance.
                amount = 2;
            } else {
                amount = 1;
            }
            dropStack(world, pos, new ItemStack(getSeedsItem(), amount));

            if (mature) {
                // Drop one resource item, if the plant is mature.
                dropStack(world, pos, new ItemStack(getResourceItem(), 1));
            }
        }
    }*/

    private Item getResourceItem() {
        if (resourceItem == null) {
            resourceItem = resourceItemSupplier.get();
            resourceItemSupplier = null;
        }
        return resourceItem;
    }

    @Override
    public ItemConvertible getSeedsItem() {
        return seedItem;
    }

    // not happy to override a deprecated method
    @Override
    public ActionResult onUse(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player,
                              final Hand hand, final BlockHitResult hit) {
        if (!world.isClient() && isMature(state)) {
            // harvest without breaking
            getDroppedStacks(state,new LootContext.Builder((ServerWorld)world).setLuck(player.getLuck()).putNullable(onPick,true)).forEach((itemStack -> {
                dropStack(world,pos,itemStack);
            }));
            world.setBlockState(pos, getDefaultState(), 2);
            return ActionResult.PASS;
        } else {
            return super.onUse(state, world, pos, player, hand, hit);
        }
    }

    @Override
    public void scheduledTick(final BlockState state, final ServerWorld world, final BlockPos pos,
                              final Random random) {
        super.scheduledTick(state, world, pos, random);
        if (50 > world.random.nextInt(100)) {
            world.getBlockTickScheduler().schedule(pos, this, 10 * AVERAGE_RANDOM_TICK, TickPriority.LOW);
        }
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContext.Builder builder) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        boolean picked = false;
        try {
            picked = builder.getNullable(onPick);
        }catch(NullPointerException e){ }
        if (!picked) {
            stacks.add(new ItemStack(this.seedItem, 1));
        }
        if (this.isMature(state)) {
            stacks.add(new ItemStack(getResourceItem(), 1));
            if (SEED_DROP_CHANCE > builder.getWorld().getRandom().nextInt(100)) {
                stacks.add(new ItemStack(this.seedItem, 1));
            }
        }

        return stacks;
    }

    public void setSeedItem(final Item seedItem) {
        this.seedItem = seedItem;
    }

    public int getColorTint() {
        return colorTint;
    }

}
