package com.lucky7.mod.client.sound;

import com.lucky7.mod.blockentity.Lucky7BlockEntity;
import com.lucky7.mod.config.Lucky7Config;
import com.lucky7.mod.sound.Lucky7Sounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class ReelLoopSound extends AbstractTickableSoundInstance {
    private final BlockPos pos;
    private final Lucky7BlockEntity machine;
    private boolean stopped;

    public ReelLoopSound(Lucky7BlockEntity machine) {
        super(Lucky7Sounds.REEL_SPIN, SoundSource.BLOCKS, RandomSource.create());
        this.machine = machine;
        this.pos = machine.getBlockPos().immutable();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.volume = (float) (Lucky7Config.INSTANCE.masterVolume * Lucky7Config.INSTANCE.reelVolume);
        this.pitch = 1.0f;
    }

    @Override
    public void tick() {
        Minecraft client = Minecraft.getInstance();
        if (stopped) return;
        if (client.level == null || !machine.isSpinning() || client.level.getBlockEntity(pos) != machine || !Lucky7Config.INSTANCE.soundsEnabled) {
            stopped = true;
            stop();
            return;
        }
        x = pos.getX() + 0.5;
        y = pos.getY() + 0.5;
        z = pos.getZ() + 0.5;
        volume = (float) (Lucky7Config.INSTANCE.masterVolume * Lucky7Config.INSTANCE.reelVolume);
    }
}
