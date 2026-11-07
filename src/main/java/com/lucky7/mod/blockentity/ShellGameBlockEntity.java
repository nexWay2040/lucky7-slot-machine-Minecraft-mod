package com.lucky7.mod.blockentity;

import com.lucky7.mod.item.Lucky7Items;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ShellGameBlockEntity extends BlockEntity {
    public enum State { IDLE, SHOWING, SHUFFLING, WAITING_CHOICE, WON, LOST }

    private State state = State.IDLE;
    private int prizeCup = 1; // 0 = Left, 1 = Center, 2 = Right
    private long stateStartTime = 0;
    private final int shuffleTicks = 60; // 3 seconds shuffle
    private int chosenCup = -1;

    public ShellGameBlockEntity(BlockPos pos, BlockState state) {
        super(Lucky7BlockEntities.SHELL_GAME, pos, state);
    }

    public State getGameState() { return state; }
    public int getPrizeCup() { return prizeCup; }
    public int getChosenCup() { return chosenCup; }
    public float getShuffleProgress() {
        if (state != State.SHUFFLING || level == null) return 1.0f;
        return Math.min(1.0f, (float) (level.getGameTime() - stateStartTime) / shuffleTicks);
    }

    public void interact(Player player, int clickedCup) {
        if (level == null || level.isClientSide()) return;

        if (state == State.IDLE || state == State.WON || state == State.LOST) {
            // Start a new game - Cost: 1 Diamond or 1 Golden Chip
            boolean paid = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack st = player.getInventory().getItem(i);
                if (st.is(Items.DIAMOND) || st.is(Lucky7Items.GOLDEN_CHIP)) {
                    st.shrink(1);
                    paid = true;
                    break;
                }
            }

            if (!paid && !player.isCreative()) {
                if (player instanceof ServerPlayer sp) {
                    sp.sendOverlayMessage(Component.literal("⚠ Для игры в Напёрстки нужна ставка: 1 Алмаз или 1 Золотая Фишка!").withStyle(ChatFormatting.YELLOW));
                }
                level.playSound(null, worldPosition, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.8f, 1.2f);
                return;
            }

            prizeCup = level.getRandom().nextInt(3);
            state = State.SHOWING;
            stateStartTime = level.getGameTime();
            chosenCup = -1;

            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.literal("👀 ШУЛЕР: «Кручу-верчу, запутать хочу! Алмаз под напёрстком #" + (prizeCup + 1) + "!»").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
            level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0f, 1.2f);
            markDirtyAndSync();
        } else if (state == State.WAITING_CHOICE) {
            // Player picks a cup
            chosenCup = clickedCup;
            if (level instanceof ServerLevel sw) {
                if (chosenCup == prizeCup) {
                    state = State.WON;
                    stateStartTime = sw.getGameTime();
                    broadcast("🎉 В ЯБЛОЧКО! Ты переиграл шулера и забрал 3 АЛМАЗА и Золотую Фишку!", ChatFormatting.GREEN);
                    dropItem(sw, new ItemStack(Items.DIAMOND, 3));
                    dropItem(sw, new ItemStack(Lucky7Items.GOLDEN_CHIP, 1));
                    sw.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.2f);
                    sw.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 40, 0.3, 0.4, 0.3, 0.15);
                } else {
                    state = State.LOST;
                    stateStartTime = sw.getGameTime();
                    broadcast("🤡 МИМО! Алмаз был под напёрстком #" + (prizeCup + 1) + "! Шулер хихикает и прячет ставку в рукав!", ChatFormatting.RED);
                    sw.playSound(null, worldPosition, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 1.0f, 0.9f);
                    sw.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 20, 0.2, 0.2, 0.2, 0.05);
                }
            }
            markDirtyAndSync();
        }
    }

    public static void clientTick(Level world, BlockPos pos, BlockState state, ShellGameBlockEntity table) {
        // Client animation is driven smoothly by getShuffleProgress() in the renderer
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, ShellGameBlockEntity table) {
        long elapsed = world.getGameTime() - table.stateStartTime;

        if (table.state == State.SHOWING && elapsed >= 20) {
            table.state = State.SHUFFLING;
            table.stateStartTime = world.getGameTime();
            world.playSound(null, pos, SoundEvents.WOOL_STEP, SoundSource.BLOCKS, 1.0f, 1.0f);
            table.markDirtyAndSync();
        } else if (table.state == State.SHUFFLING && elapsed >= table.shuffleTicks) {
            table.state = State.WAITING_CHOICE;
            world.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0f, 1.5f);
            table.broadcast("❓ Выбирай напёрсток: Кликни ПКМ по Левому, Среднему или Правому!", ChatFormatting.YELLOW);
            table.markDirtyAndSync();
        } else if ((table.state == State.WON || table.state == State.LOST) && elapsed >= 80) {
            table.state = State.IDLE;
            table.markDirtyAndSync();
        }
    }

    private void broadcast(String msg, ChatFormatting format) {
        if (level instanceof ServerLevel sw) {
            for (ServerPlayer p : sw.players()) {
                if (p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 36.0) {
                    p.sendOverlayMessage(Component.literal(msg).withStyle(format, ChatFormatting.BOLD));
                }
            }
        }
    }

    private void dropItem(ServerLevel sw, ItemStack stack) {
        ItemEntity entity = new ItemEntity(sw, worldPosition.getX() + 0.5, worldPosition.getY() + 1.15, worldPosition.getZ() + 0.5, stack);
        entity.setDeltaMovement(0.0, 0.25, 0.0);
        sw.addFreshEntity(entity);
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level instanceof ServerLevel sw) {
            sw.getChunkSource().blockChanged(worldPosition);
            sw.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("state", state.ordinal());
        output.putInt("prizeCup", prizeCup);
        output.putInt("chosenCup", chosenCup);
        output.putLong("startTime", stateStartTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int s = input.getIntOr("state", 0);
        state = (s >= 0 && s < State.values().length) ? State.values()[s] : State.IDLE;
        prizeCup = input.getIntOr("prizeCup", 1);
        chosenCup = input.getIntOr("chosenCup", -1);
        stateStartTime = input.getLongOr("startTime", 0L);
    }
}
