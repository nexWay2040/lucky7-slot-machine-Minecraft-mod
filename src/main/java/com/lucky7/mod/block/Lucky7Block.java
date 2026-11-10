package com.lucky7.mod.block;

import com.lucky7.mod.blockentity.Lucky7BlockEntity;
import com.lucky7.mod.sound.Lucky7Sounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class Lucky7Block extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final VoxelShape BASE_SHAPE = Block.box(0.5, 0.0, 0.5, 15.5, 16.0, 15.5);
    private static final VoxelShape TOP_SHAPE = Block.box(0.5, 0.0, 0.5, 15.5, 16.0, 15.5);

    public Lucky7Block(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? TOP_SHAPE : BASE_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? TOP_SHAPE : BASE_SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        if (pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(ctx)) {
            return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            BlockPos upPos = pos.above();
            level.setBlock(upPos, state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
            if (placer instanceof Player player && level.getBlockEntity(pos) instanceof Lucky7BlockEntity machine) {
                machine.setOwner(player);
            }
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return super.canSurvive(state, level, pos);
        } else {
            BlockState downState = level.getBlockState(pos.below());
            return downState.is(this) && downState.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess ticks,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random
    ) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y) {
            if (half == DoubleBlockHalf.LOWER && direction == Direction.UP) {
                if (!neighborState.is(this) || neighborState.getValue(HALF) != DoubleBlockHalf.UPPER) {
                    return Blocks.AIR.defaultBlockState();
                }
            } else if (half == DoubleBlockHalf.UPPER && direction == Direction.DOWN) {
                if (!neighborState.is(this) || neighborState.getValue(HALF) != DoubleBlockHalf.LOWER) {
                    return Blocks.AIR.defaultBlockState();
                }
            }
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            DoubleBlockHalf half = state.getValue(HALF);
            BlockPos otherPos = (half == DoubleBlockHalf.LOWER) ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(HALF) != half) {
                if (!player.isCreative() && half == DoubleBlockHalf.UPPER) {
                    popResource(level, pos, new ItemStack(Lucky7Blocks.SLOT_MACHINE_ITEM));
                }
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private @Nullable Lucky7BlockEntity getMachine(Level level, BlockPos pos, BlockState state) {
        BlockPos entityPos = (state.getValue(HALF) == DoubleBlockHalf.LOWER) ? pos : pos.below();
        return level.getBlockEntity(entityPos) instanceof Lucky7BlockEntity machine ? machine : null;
    }

    private boolean handlePhysicalInteraction(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        Direction facing = state.getValue(FACING);
        Direction rightSide = facing.getClockWise();

        boolean isRightSideFace = (hit.getDirection() == rightSide);

        double dx = hit.getLocation().x - pos.getX();
        double dy = hit.getLocation().y - pos.getY() + (state.getValue(HALF) == DoubleBlockHalf.UPPER ? 1.0 : 0.0);
        double dz = hit.getLocation().z - pos.getZ();

        double localX, localZ;
        switch (facing) {
            case NORTH -> { localX = 1.0 - dx; localZ = 1.0 - dz; }
            case SOUTH -> { localX = dx; localZ = dz; }
            case EAST  -> { localX = 1.0 - dz; localZ = dx; }
            case WEST  -> { localX = dz; localZ = 1.0 - dx; }
            default    -> { localX = dx; localZ = dz; }
        }
        double localY = dy;

        boolean isLeverHit = isRightSideFace || (localX >= 0.80);
        boolean isButtonHit = (localZ >= 0.84 && localY >= 0.90 && localY <= 1.25 && localX >= 0.15 && localX <= 0.48);
        boolean isSneakSpin = player.isShiftKeyDown();

        if (isLeverHit || isButtonHit || isSneakSpin) {
            Lucky7BlockEntity machine = getMachine(level, pos, state);
            if (machine != null) {
                if (machine.isSpinning()) {
                    return true;
                }
                if (machine.stakeValue() > 0 || machine.isFeverMode()) {
                    machine.startSpin(player);
                } else {
                    if (player instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal("⚠ Положите ставку в автомат (кликните по стеклу витрины)!").withStyle(ChatFormatting.YELLOW), true);
                    }
                    level.playSound(null, pos, Lucky7Sounds.BUTTON_CLICK, SoundSource.BLOCKS, 0.85f, 0.6f);
                }
            }
            return true;
        }

        return false;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (handlePhysicalInteraction(state, level, pos, player, hit)) {
            return InteractionResult.SUCCESS;
        }
        Lucky7BlockEntity machine = getMachine(level, pos, state);
        if (machine != null && player instanceof ServerPlayer serverPlayer) {
            machine.addViewer(player);
            serverPlayer.openMenu(machine);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (handlePhysicalInteraction(state, level, pos, player, hit)) {
            return InteractionResult.SUCCESS;
        }
        Lucky7BlockEntity machine = getMachine(level, pos, state);
        if (machine != null && player instanceof ServerPlayer serverPlayer) {
            machine.addViewer(player);
            serverPlayer.openMenu(machine);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        Lucky7BlockEntity machine = getMachine(level, pos, state);
        return machine != null ? machine.getComparatorOutput() : 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block sourceBlock, @Nullable Orientation orientation, boolean notify) {
        super.neighborChanged(state, level, pos, sourceBlock, orientation, notify);
        if (!level.isClientSide() && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
            if (powered) {
                Lucky7BlockEntity machine = getMachine(level, pos, state);
                if (machine != null && !machine.isSpinning() && (machine.stakeValue() > 0 || machine.isFeverMode())) {
                    machine.startSpin(null);
                }
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new Lucky7BlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(HALF) != DoubleBlockHalf.LOWER) return null;
        return (w, p, s, be) -> {
            if (!(be instanceof Lucky7BlockEntity machine)) return;
            if (w.isClientSide()) Lucky7BlockEntity.clientTick(w, p, s, machine);
            else Lucky7BlockEntity.serverTick(w, p, s, machine);
        };
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }
}
