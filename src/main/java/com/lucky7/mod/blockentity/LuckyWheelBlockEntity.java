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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
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

public class LuckyWheelBlockEntity extends BlockEntity {
    private boolean spinning = false;
    private float angle = 0.0f;
    private float velocity = 0.0f;
    private int targetSector = 0;
    private long spinStartTime = 0;
    private final int spinDurationTicks = 80; // 4 seconds spin
    private float lastClickAngle = 0.0f;

    public LuckyWheelBlockEntity(BlockPos pos, BlockState state) {
        super(Lucky7BlockEntities.LUCKY_WHEEL, pos, state);
    }

    public boolean isSpinning() { return spinning; }
    public float getAngle() { return angle; }

    public void spin(Player player) {
        if (spinning) return;
        if (level == null || level.isClientSide()) return;

        // Player can pay 1 Lucky Coin, 1 Golden Chip, 1 Gold Ingot, or 1 Diamond
        boolean paid = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (st.is(Lucky7Items.LUCKY_COIN) || st.is(Lucky7Items.GOLDEN_CHIP) || st.is(Items.GOLD_INGOT) || st.is(Items.DIAMOND)) {
                st.shrink(1);
                paid = true;
                break;
            }
        }

        if (!paid && !player.isCreative()) {
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.literal("⚠ Для вращения Колеса Сансары нужна Счастливая Монета, Золотой Слиток или Алмаз!").withStyle(ChatFormatting.YELLOW));
            }
            level.playSound(null, worldPosition, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.8f, 1.2f);
            return;
        }

        spinning = true;
        spinStartTime = level.getGameTime();
        targetSector = level.getRandom().nextInt(12);

        // Target angle calculation (12 sectors = 30 deg each)
        int fullRotations = 5 + level.getRandom().nextInt(4);
        float finalAngle = fullRotations * 360.0f + (targetSector * 30.0f) + 15.0f;
        velocity = (finalAngle * 2.0f) / spinDurationTicks;

        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0f, 1.4f);
        markDirtyAndSync();
    }

    public static void clientTick(Level world, BlockPos pos, BlockState state, LuckyWheelBlockEntity wheel) {
        if (wheel.spinning) {
            wheel.angle += wheel.velocity;
            wheel.velocity = Math.max(0.0f, wheel.velocity - (wheel.velocity / 40.0f));

            // Mechanical peg click sound on each 30-degree sector pass
            if (Math.abs(wheel.angle - wheel.lastClickAngle) >= 30.0f) {
                wheel.lastClickAngle = wheel.angle;
                world.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.8f, 1.6f, false);
            }
        }
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, LuckyWheelBlockEntity wheel) {
        if (!wheel.spinning) return;

        if (world.getGameTime() >= wheel.spinStartTime + wheel.spinDurationTicks) {
            wheel.spinning = false;
            wheel.velocity = 0.0f;
            if (world instanceof ServerLevel sw) {
                wheel.triggerSectorReward(sw, wheel.targetSector);
            }
            wheel.markDirtyAndSync();
        }
    }

    private void triggerSectorReward(ServerLevel sw, int sector) {
        sw.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.1f);
        sw.sendParticles(ParticleTypes.FIREWORK, worldPosition.getX() + 0.5, worldPosition.getY() + 1.8, worldPosition.getZ() + 0.5, 30, 0.3, 0.5, 0.3, 0.1);

        switch (sector) {
            case 0 -> { // Diamond Jackpot
                broadcast("💎 МЕГА-ДЖЕКПОТ! Колесо выдало 32 Алмаза и 4 Золотые Фишки!", ChatFormatting.AQUA);
                dropItem(sw, new ItemStack(Items.DIAMOND, 32));
                dropItem(sw, new ItemStack(Lucky7Items.GOLDEN_CHIP, 4));
            }
            case 1 -> { // Chicken Apocalypse (Streamer Prank)
                broadcast("🐔 КУРИНЫЙ АПОКАЛИПСИС! На базе высадился десант из 12 боевых кур!", ChatFormatting.YELLOW);
                for (int i = 0; i < 12; i++) {
                    Chicken chicken = EntityTypes.CHICKEN.spawn(sw, worldPosition.above(), EntitySpawnReason.EVENT);
                    if (chicken != null) {
                        chicken.setCustomName(Component.literal("🐔 Курьер Синдиката").withStyle(ChatFormatting.YELLOW));
                        chicken.setDeltaMovement((sw.getRandom().nextDouble() - 0.5) * 0.4, 0.35, (sw.getRandom().nextDouble() - 0.5) * 0.4);
                    }
                }
            }
            case 2 -> { // Space Levitation (Streamer Prank)
                broadcast("🚀 КОСМИЧЕСКАЯ ПРОГРАММА ЛИФИКСАПА! Испытайте невесомость!", ChatFormatting.LIGHT_PURPLE);
                for (ServerPlayer p : sw.players()) {
                    if (p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 36.0) {
                        p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 2));
                        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
                    }
                }
            }
            case 3 -> { // Gold Shower
                broadcast("💰 ЗОЛОТОЙ ДОЖДЬ! +64 Золотых самородка и 8 Счастливых Монет!", ChatFormatting.GOLD);
                dropItem(sw, new ItemStack(Items.GOLD_NUGGET, 64));
                dropItem(sw, new ItemStack(Lucky7Items.LUCKY_COIN, 8));
            }
            case 4 -> { // God Apple
                broadcast("🍏 БОЖЕСТВЕННЫЙ ДАР! Зачарованное золотое яблоко!", ChatFormatting.LIGHT_PURPLE);
                dropItem(sw, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
            }
            case 5 -> { // Fake Creeper Hiss Prank
                broadcast("🐍 ПССС... ШУТКА ОТ ДОНА КРИПЕРИОНЕ! Держи утешительные Изумруды!", ChatFormatting.GREEN);
                sw.playSound(null, worldPosition, SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 1.2f, 1.0f);
                dropItem(sw, new ItemStack(Items.EMERALD, 8));
            }
            case 6 -> { // Thunder & Iron
                broadcast("⚡ ГРОМОВАЯ УДАЧА! +8 Железных блоков!", ChatFormatting.YELLOW);
                sw.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, 25, 0.3, 0.3, 0.3, 0.1);
                sw.playSound(null, worldPosition, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.9f, 1.0f);
                dropItem(sw, new ItemStack(Items.IRON_BLOCK, 8));
            }
            case 7 -> { // Emerald Rain
                broadcast("❇ ИЗУМРУДНЫЙ КУШ: 24 Изумруда!", ChatFormatting.GREEN);
                dropItem(sw, new ItemStack(Items.EMERALD, 24));
            }
            case 8 -> { // Netherite Scraps
                broadcast("🔥 АДСКИЙ КЛАД: 4 Древних обломка!", ChatFormatting.DARK_RED);
                dropItem(sw, new ItemStack(Items.ANCIENT_DEBRIS, 4));
            }
            case 9 -> { // Mining Haste Buff
                broadcast("⛏ БЛАГОСЛОВЕНИЕ ШАХТЁРА: Спешка II и Скорость II на 3 минуты!", ChatFormatting.GOLD);
                for (ServerPlayer p : sw.players()) {
                    if (p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 36.0) {
                        p.addEffect(new MobEffectInstance(MobEffects.HASTE, 3600, 1));
                        p.addEffect(new MobEffectInstance(MobEffects.SPEED, 3600, 1));
                    }
                }
            }
            case 10 -> { // Mystery Fireworks + Chips
                broadcast("🎆 ПРАЗДНИК В КАЗИНО! 32 Фейерверка и 2 Золотые Фишки!", ChatFormatting.LIGHT_PURPLE);
                dropItem(sw, new ItemStack(Items.FIREWORK_ROCKET, 32));
                dropItem(sw, new ItemStack(Lucky7Items.GOLDEN_CHIP, 2));
            }
            default -> { // Totem of Undying
                broadcast("👑 КОРОЛЕВСКИЙ СЕКТОР: Тотем Бессмертия!", ChatFormatting.GOLD);
                dropItem(sw, new ItemStack(Items.TOTEM_OF_UNDYING, 1));
            }
        }
    }

    private void broadcast(String msg, ChatFormatting format) {
        if (level instanceof ServerLevel sw) {
            for (ServerPlayer p : sw.players()) {
                if (p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0) {
                    p.sendOverlayMessage(Component.literal("🎡 КОЛЕСО САНСАРЫ: " + msg).withStyle(format, ChatFormatting.BOLD));
                    p.sendSystemMessage(Component.literal("🎡 [Колесо Сансары] " + msg).withStyle(format));
                }
            }
        }
    }

    private void dropItem(ServerLevel sw, ItemStack stack) {
        ItemEntity entity = new ItemEntity(sw, worldPosition.getX() + 0.5, worldPosition.getY() + 1.25, worldPosition.getZ() + 0.5, stack);
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
        output.putBoolean("spinning", spinning);
        output.putFloat("angle", angle);
        output.putFloat("velocity", velocity);
        output.putInt("targetSector", targetSector);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        spinning = input.getBooleanOr("spinning", false);
        angle = input.getFloatOr("angle", 0.0f);
        velocity = input.getFloatOr("velocity", 0.0f);
        targetSector = input.getIntOr("targetSector", 0);
    }
}
