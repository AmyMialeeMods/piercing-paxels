package amymialee.piercingpaxels.mixin;

import amymialee.piercingpaxels.items.PaxelItem;
import amymialee.piercingpaxels.registry.PiercingItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "onBreak", at = @At("HEAD"), cancellable = true)
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfo ci) {
        ItemStack stack = player.getMainHandStack();
        if (stack.getItem() instanceof PaxelItem) {
            ItemStack upgradePassive = PaxelItem.getUpgrade(stack, 1);
            if (upgradePassive != null && upgradePassive.isOf(PiercingItems.PASSIVE_SILENCE)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "dropStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private static void PiercingPaxels$PaxelPassives(BlockState state, World world, BlockPos pos, BlockEntity blockEntity, Entity entity, ItemStack stack, CallbackInfo ci) {
        if (world instanceof ServerWorld serverWorld && stack.getItem() instanceof PaxelItem) {
            ItemStack upgradePassive = PaxelItem.getUpgrade(stack, 1);
            if (upgradePassive != null) {
                if (upgradePassive.isOf(PiercingItems.PASSIVE_SMELT)) {
                    Block.getDroppedStacks(state, serverWorld, pos, blockEntity, entity, stack).forEach((stackX) -> {
                        ItemStack smelted = simulateSmelt(world, stackX);
                        if (smelted != null) {
                            Block.dropStack(world, pos, smelted);
                            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
                            serverWorld.spawnParticles(stack.getItem() == PiercingItems.NETHERITE_PAXEL ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, 8, 0.25D, 0.25D, 0.25D, 0.025D);
                        } else {
                            Block.dropStack(world, pos, stackX);
                        }
                    });
                    state.onStacksDropped(serverWorld, pos, stack, true);
                    ci.cancel();
                } else if (upgradePassive.isOf(PiercingItems.PASSIVE_VACUUM)) {
                    Block.getDroppedStacks(state, serverWorld, pos, blockEntity, entity, stack).forEach((stackX) -> dropStackVacuum(world, () -> new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), stackX), stackX));
                    state.onStacksDropped(serverWorld, pos, stack, true);
                    ci.cancel();
                }
            }
        }
    }

    private static void dropStackVacuum(World world, Supplier<ItemEntity> itemEntitySupplier, ItemStack stack) {
        if (world.isClient || stack.isEmpty() || !world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) {
            return;
        }
        ItemEntity itemEntity = itemEntitySupplier.get();
        itemEntity.setPickupDelay(0);
        world.spawnEntity(itemEntity);
    }

    private static final SimpleInventory fakeFurnace = new SimpleInventory(3);

    private static ItemStack simulateSmelt(World world, ItemStack input) {
        fakeFurnace.clear();
        fakeFurnace.setStack(0, input);
        Recipe<?> recipe = world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, fakeFurnace, world).orElse(null);
        if (recipe != null) {
            ItemStack output = recipe.getOutput();
            output.increment(input.getCount() - output.getCount());
            return output;
        }
        return null;
    }
}