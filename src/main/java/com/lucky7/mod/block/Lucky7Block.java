package com.lucky7.mod.block;

import com.lucky7.mod.blockentity.Lucky7BlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class Lucky7Block extends BlockWithEntity {
    public static final MapCodec<Lucky7Block> CODEC = createCodec(Lucky7Block::new);
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    private static final VoxelShape BASE_SHAPE = Block.createCuboidShape(0.5, 0.0, 0.5, 15.5, 16.0, 15.5);
    private static final VoxelShape TOP_SHAPE = Block.createCuboidShape(0.5, 0.0, 0.5, 15.5, 16.0, 15.5);

    public Lucky7Block(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(HALF) == DoubleBlockHalf.UPPER ? TOP_SHAPE : BASE_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(HALF) == DoubleBlockHalf.UPPER ? TOP_SHAPE : BASE_SHAPE;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        if (pos.getY() < world.getTopY() - 1 && world.getBlockState(pos.up()).canReplace(ctx)) {
            return getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (!world.isClient) {
            BlockPos upPos = pos.up();
            world.setBlockState(upPos, state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
            if (placer instanceof PlayerEntity player && world.getBlockEntity(pos) instanceof Lucky7BlockEntity machine) {
                machine.setOwner(player);
            }
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            return super.canPlaceAt(state, world, pos);
        } else {
            BlockState downState = world.getBlockState(pos.down());
            return downState.isOf(this) && downState.get(HALF) == DoubleBlockHalf.LOWER;
        }
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.get(HALF);
        if (direction.getAxis() == Direction.Axis.Y) {
            if (half == DoubleBlockHalf.LOWER && direction == Direction.UP) {
                if (!neighborState.isOf(this) || neighborState.get(HALF) != DoubleBlockHalf.UPPER) {
                    return Blocks.AIR.getDefaultState();
                }
            } else if (half == DoubleBlockHalf.UPPER && direction == Direction.DOWN) {
                if (!neighborState.isOf(this) || neighborState.get(HALF) != DoubleBlockHalf.LOWER) {
                    return Blocks.AIR.getDefaultState();
                }
            }
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            DoubleBlockHalf half = state.get(HALF);
            BlockPos otherPos = (half == DoubleBlockHalf.LOWER) ? pos.up() : pos.down();
            BlockState otherState = world.getBlockState(otherPos);
            if (otherState.isOf(this) && otherState.get(HALF) != half) {
                if (!player.isCreative() && half == DoubleBlockHalf.UPPER) {
                    dropStack(world, pos, new ItemStack(Lucky7Blocks.SLOT_MACHINE_ITEM));
                }
                world.setBlockState(otherPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                world.syncWorldEvent(player, 2001, otherPos, Block.getRawIdFromState(otherState));
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    private Lucky7BlockEntity getMachine(World world, BlockPos pos, BlockState state) {
        BlockPos entityPos = (state.get(HALF) == DoubleBlockHalf.LOWER) ? pos : pos.down();
        return world.getBlockEntity(entityPos) instanceof Lucky7BlockEntity machine ? machine : null;
    }

    private boolean handlePhysicalInteraction(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        Direction facing = state.get(FACING);
        Direction rightSide = facing.rotateYClockwise();

        boolean isRightSideFace = (hit.getSide() == rightSide);

        double dx = hit.getPos().x - pos.getX();
        double dy = hit.getPos().y - pos.getY() + (state.get(HALF) == DoubleBlockHalf.UPPER ? 1.0 : 0.0);
        double dz = hit.getPos().z - pos.getZ();

        double localX, localZ;
        switch (facing) {
            case NORTH -> { localX = 1.0 - dx; localZ = 1.0 - dz; }
            case SOUTH -> { localX = dx; localZ = dz; }
            case EAST  -> { localX = dz; localZ = 1.0 - dx; }
            case WEST  -> { localX = 1.0 - dz; localZ = dx; }
            default    -> { localX = dx; localZ = dz; }
        }
        double localY = dy;

        // Click on the lever (right side X >= 0.80), button on console deck, or Sneak+RightClick
        boolean isLeverHit = isRightSideFace || (localX >= 0.80);
        boolean isButtonHit = (localZ >= 0.84 && localY >= 0.90 && localY <= 1.25 && localX >= 0.15 && localX <= 0.48);
        boolean isSneakSpin = player.isSneaking();

        if (isLeverHit || isButtonHit || isSneakSpin) {
            Lucky7BlockEntity machine = getMachine(world, pos, state);
            if (machine != null) {
                if (machine.isSpinning()) {
                    return true;
                }
                if (machine.stakeValue() > 0) {
                    machine.startSpin(player);
                } else {
                    if (player instanceof ServerPlayerEntity sp) {
                        sp.sendMessage(net.minecraft.text.Text.literal("⚠ Положите ставку в автомат (кликните по стеклу витрины)!").formatted(net.minecraft.util.Formatting.YELLOW), true);
                    }
                    world.playSound(null, pos, com.lucky7.mod.sound.Lucky7Sounds.BUTTON_CLICK, net.minecraft.sound.SoundCategory.BLOCKS, 0.85f, 0.6f);
                }
            }
            return true;
        }

        return false;
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, net.minecraft.util.Hand hand, BlockHitResult hit) {
        if (world.isClient) return ItemActionResult.SUCCESS;
        if (handlePhysicalInteraction(state, world, pos, player, hit)) {
            return ItemActionResult.SUCCESS;
        }
        Lucky7BlockEntity machine = getMachine(world, pos, state);
        if (machine != null && player instanceof ServerPlayerEntity serverPlayer) {
            machine.addViewer(player);
            serverPlayer.openHandledScreen(machine);
        }
        return ItemActionResult.SUCCESS;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (handlePhysicalInteraction(state, world, pos, player, hit)) {
            return ActionResult.SUCCESS;
        }
        Lucky7BlockEntity machine = getMachine(world, pos, state);
        if (machine != null && player instanceof ServerPlayerEntity serverPlayer) {
            machine.addViewer(player);
            serverPlayer.openHandledScreen(machine);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        Lucky7BlockEntity machine = getMachine(world, pos, state);
        return machine != null ? machine.getComparatorOutput() : 0;
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (!world.isClient && state.get(HALF) == DoubleBlockHalf.LOWER) {
            boolean powered = world.isReceivingRedstonePower(pos) || world.isReceivingRedstonePower(pos.up());
            if (powered) {
                Lucky7BlockEntity machine = getMachine(world, pos, state);
                if (machine != null && !machine.isSpinning() && machine.stakeValue() > 0) {
                    machine.startSpin(null);
                }
            }
        }
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER ? new Lucky7BlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (state.get(HALF) != DoubleBlockHalf.LOWER) return null;
        return (w, p, s, be) -> {
            if (!(be instanceof Lucky7BlockEntity machine)) return;
            if (w.isClient) Lucky7BlockEntity.clientTick(w, p, s, machine);
            else Lucky7BlockEntity.serverTick(w, p, s, machine);
        };
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient && state.get(HALF) == DoubleBlockHalf.LOWER) {
            if (world.getBlockEntity(pos) instanceof Lucky7BlockEntity machine) {
                for (int i = 0; i < Lucky7BlockEntity.STAKE_SLOTS; i++) {
                    ItemStack stack = machine.getStack(i);
                    if (!stack.isEmpty()) {
                        ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                        machine.setStack(i, ItemStack.EMPTY);
                    }
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
