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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class LuckyWheelBlockEntity extends BlockEntity {
    private boolean spinning = false;
    private float angle = 0.0f;
    private float finalTargetAngle = 0.0f;
    private int targetSector = 0;
    private long spinStartTime = 0;
    private final int spinDurationTicks = 70; // 3.5s clean spin
    private UUID activePlayerId = null;

    public LuckyWheelBlockEntity(BlockPos pos, BlockState state) {
        super(Lucky7BlockEntities.LUCKY_WHEEL, pos, state);
    }

    public boolean isSpinning() { return spinning; }
    public float getAngle() { return angle; }

    public void spin(Player player) {
        if (spinning) return;
        if (level == null || level.isClientSide()) return;

        // Accepts 2 Emeralds, 1 Lucky Coin, any Casino Chip, Gold Ingot, or Diamond
        boolean paid = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (st.is(Items.EMERALD) && st.getCount() >= 2) {
                st.shrink(2);
                paid = true;
                break;
            } else if (st.is(Lucky7Items.LUCKY_COIN)
                    || st.is(Lucky7Items.GOLDEN_CHIP)
                    || st.is(Lucky7Items.EMERALD_CHIP)
                    || st.is(Lucky7Items.DIAMOND_CHIP)
                    || st.is(Items.GOLD_INGOT)
                    || st.is(Items.DIAMOND)) {
                st.shrink(1);
                paid = true;
                break;
            }
        }

        if (!paid && !player.isCreative()) {
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.literal("⚠ Для вращения Колеса нужна Фишка, Монета Удачи, 2 Изумруда или Алмаз!").withStyle(ChatFormatting.YELLOW));
            }
            return;
        }

        spinning = true;
        spinStartTime = level.getGameTime();
        activePlayerId = player.getUUID();
        targetSector = level.getRandom().nextInt(12);

        // Normalize current angle into [0..360) and compute forward target angle aligned to sector
        float baseMod = ((angle % 360.0f) + 360.0f) % 360.0f;
        angle = baseMod;
        int fullRotations = 4 + level.getRandom().nextInt(3);
        finalTargetAngle = fullRotations * 360.0f + (targetSector * 30.0f);

        level.playSound(null, worldPosition, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.8f, 1.2f);
        markDirtyAndSync();
    }

    public static void clientTick(Level world, BlockPos pos, BlockState state, LuckyWheelBlockEntity wheel) {
        if (wheel.spinning) {
            float diff = wheel.finalTargetAngle - wheel.angle;
            if (diff > 0.4f) {
                wheel.angle += Math.max(1.1f, diff * 0.088f);
            } else {
                wheel.angle = wheel.finalTargetAngle;
            }
        }
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, LuckyWheelBlockEntity wheel) {
        if (!wheel.spinning) return;

        if (world.getGameTime() >= wheel.spinStartTime + wheel.spinDurationTicks) {
            wheel.spinning = false;
            wheel.angle = wheel.targetSector * 30.0f;
            wheel.finalTargetAngle = wheel.angle;
            if (world instanceof ServerLevel sw) {
                Player p = wheel.activePlayerId != null ? sw.getServer().getPlayerList().getPlayer(wheel.activePlayerId) : null;
                wheel.triggerSectorReward(sw, p, wheel.targetSector);
            }
            wheel.markDirtyAndSync();
        }
    }

    private void triggerSectorReward(ServerLevel sw, Player activePlayer, int sector) {
        switch (sector) {
            case 0 -> { // 777 JACKPOT (Gold - GOOD)
                broadcast("👑 [СЕКТОР 777] ДЖЕКПОТ! +32 Изумруда, +16 Алмазов и 4 Изумрудные Фишки!", ChatFormatting.GOLD);
                sw.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.1f);
                sw.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5, 45, 0.4, 0.5, 0.4, 0.15);
                dropItem(sw, new ItemStack(Items.EMERALD, 32));
                dropItem(sw, new ItemStack(Items.DIAMOND, 16));
                dropItem(sw, new ItemStack(Lucky7Items.EMERALD_CHIP, 4));
            }
            case 1 -> { // SKULL -8 EM (Black - BAD)
                broadcast("💀 [ЧЕРЕП -8] Проклятие мафии! Вы теряете 8 Изумрудов и получаете Иссушение!", ChatFormatting.RED);
                sw.playSound(null, worldPosition, SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 0.85f, 0.85f);
                sw.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.3, worldPosition.getZ() + 0.5, 25, 0.3, 0.3, 0.3, 0.05);
                if (activePlayer != null) {
                    stealEmeralds(activePlayer, 8);
                    activePlayer.hurtServer(sw, sw.damageSources().wither(), 5.0f);
                    activePlayer.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                }
            }
            case 2 -> { // +24 EM (Green - GOOD)
                broadcast("❇ [СЕКТОР +24] Изумрудный куш! +24 Изумруда и 2 Золотые Фишки!", ChatFormatting.GREEN);
                sw.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.2f);
                dropItem(sw, new ItemStack(Items.EMERALD, 24));
                dropItem(sw, new ItemStack(Lucky7Items.GOLDEN_CHIP, 2));
            }
            case 3 -> { // TNT BOOM (Red - BAD)
                broadcast("💣 [СЕКТОР TNT] ВЗРЫВНАЯ ЛОВУШКА! Бум прямо в лицо!", ChatFormatting.DARK_RED);
                sw.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.9f, 1.0f);
                sw.sendParticles(ParticleTypes.EXPLOSION_EMITTER, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.0);
                for (ServerPlayer p : sw.players()) {
                    if (p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 25.0) {
                        p.hurtServer(sw, sw.damageSources().explosion(null, null), 6.0f);
                        p.setDeltaMovement(0.0, 0.85, 0.0);
                        p.syncVelocity = true;
                    }
                }
            }
            case 4 -> { // +16 DIA (Cyan - GOOD)
                broadcast("💎 [СЕКТОР +16] Алмазная россыпь! +16 Алмазов и 2 Золотые Фишки!", ChatFormatting.AQUA);
                sw.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.3f);
                dropItem(sw, new ItemStack(Items.DIAMOND, 16));
                dropItem(sw, new ItemStack(Lucky7Items.GOLDEN_CHIP, 2));
            }
            case 5 -> { // TRAP (Dark Purple - BAD)
                broadcast("🗡 [СЕКТОР TRAP] ЗАСАДА! На вас напал охранник Дона Криперионе!", ChatFormatting.RED);
                sw.playSound(null, worldPosition, SoundEvents.CREEPER_PRIMED, SoundSource.HOSTILE, 0.9f, 0.95f);
                Skeleton guard = EntityTypes.SKELETON.spawn(sw, worldPosition.above(), EntitySpawnReason.EVENT);
                if (guard != null) {
                    guard.setCustomName(Component.literal("🕶 Охранник Рулетки").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    guard.setCustomNameVisible(true);
                    guard.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                    guard.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
                    guard.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
                    guard.addEffect(new MobEffectInstance(MobEffects.SPEED, 2400, 1));
                }
            }
            case 6 -> { // GIFT APPLE (Green - GOOD)
                broadcast("🍏 [СЕКТОР GIFT] Зачарованное Золотое Яблоко + 16 Изумрудов!", ChatFormatting.LIGHT_PURPLE);
                sw.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.3f);
                dropItem(sw, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
                dropItem(sw, new ItemStack(Items.EMERALD, 16));
            }
            case 7 -> { // FLY! (Red - BAD)
                broadcast("🚀 [СЕКТОР FLY!] Катапульта! Полёт в стратосферу без парашюта!", ChatFormatting.RED);
                sw.playSound(null, worldPosition, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 0.9f, 0.9f);
                for (ServerPlayer p : sw.players()) {
                    if (p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 25.0) {
                        p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 55, 6));
                    }
                }
            }
            case 8 -> { // +20 DEBRIS & COINS (Amber - GOOD)
                broadcast("🪙 [СЕКТОР +20] Золотой тайник! +20 Изумрудов, 4 Древних обломка и 4 Монеты!", ChatFormatting.GOLD);
                sw.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.15f);
                dropItem(sw, new ItemStack(Items.EMERALD, 20));
                dropItem(sw, new ItemStack(Items.ANCIENT_DEBRIS, 4));
                dropItem(sw, new ItemStack(Lucky7Items.LUCKY_COIN, 4));
            }
            case 9 -> { // HEX (Black - BAD)
                broadcast("🐔 [СЕКТОР HEX] Проклятие лудомана: тошнота, голод и куриный переполох!", ChatFormatting.RED);
                for (int i = 0; i < 6; i++) {
                    Chicken c = EntityTypes.CHICKEN.spawn(sw, worldPosition.above(), EntitySpawnReason.EVENT);
                    if (c != null) c.setCustomName(Component.literal("🐔 Курьер Налогов").withStyle(ChatFormatting.RED));
                }
                for (ServerPlayer p : sw.players()) {
                    if (p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 25.0) {
                        p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 240, 3));
                        p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 140, 0));
                    }
                }
            }
            case 10 -> { // SAVE TOTEM (Green - GOOD)
                broadcast("👑 [СЕКТОР SAVE] Тотем Бессмертия + 16 Изумрудов!", ChatFormatting.GREEN);
                sw.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 1.3f);
                dropItem(sw, new ItemStack(Items.TOTEM_OF_UNDYING, 1));
                dropItem(sw, new ItemStack(Items.EMERALD, 16));
            }
            default -> { // ZAP! (Red - BAD)
                broadcast("⚡ [СЕКТОР ZAP!] МОЛНИЯ С НЕБЕС! Разряд прямо по игроку!", ChatFormatting.DARK_RED);
                if (activePlayer != null) {
                    LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.spawn(sw, activePlayer.blockPosition(), EntitySpawnReason.EVENT);
                    if (bolt != null) bolt.setVisualOnly(false);
                }
            }
        }
    }

    private void stealEmeralds(Player player, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (st.is(Items.EMERALD)) {
                int take = Math.min(remaining, st.getCount());
                st.shrink(take);
                remaining -= take;
            }
        }
    }

    private void broadcast(String msg, ChatFormatting format) {
        if (level instanceof ServerLevel sw) {
            for (ServerPlayer p : sw.players()) {
                if (p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0) {
                    p.sendOverlayMessage(Component.literal(msg).withStyle(format, ChatFormatting.BOLD));
                    p.sendSystemMessage(Component.literal("🎡 " + msg).withStyle(format));
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
        output.putFloat("finalTargetAngle", finalTargetAngle);
        output.putInt("targetSector", targetSector);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        spinning = input.getBooleanOr("spinning", false);
        angle = input.getFloatOr("angle", 0.0f);
        finalTargetAngle = input.getFloatOr("finalTargetAngle", angle);
        targetSector = input.getIntOr("targetSector", 0);
    }
}
