package com.lucky7.mod.client.sound;

import com.lucky7.mod.blockentity.Lucky7BlockEntity;
import com.lucky7.mod.config.Lucky7Config;
import com.lucky7.mod.sound.Lucky7Sounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public final class ReelLoopSound extends MovingSoundInstance {
    private final BlockPos pos;
    private final Lucky7BlockEntity machine;
    private boolean stopped;
    public ReelLoopSound(Lucky7BlockEntity machine) {
        super(Lucky7Sounds.REEL_SPIN,SoundCategory.BLOCKS,Random.create());
        this.machine=machine;
        this.pos=machine.getPos().toImmutable();
        this.x=pos.getX()+0.5; this.y=pos.getY()+0.5; this.z=pos.getZ()+0.5;
        this.repeat=true; this.repeatDelay=0; this.attenuationType=AttenuationType.LINEAR;
        this.volume=(float)(Lucky7Config.INSTANCE.masterVolume*Lucky7Config.INSTANCE.reelVolume);
        this.pitch=1.0f;
    }
    @Override public void tick() {
        MinecraftClient client=MinecraftClient.getInstance();
        if(stopped) return;
        if(client.world==null||!machine.isSpinning()||client.world.getBlockEntity(pos)!=machine||!Lucky7Config.INSTANCE.soundsEnabled) {
            stopped=true; setDone(); return;
        }
        x=pos.getX()+0.5; y=pos.getY()+0.5; z=pos.getZ()+0.5;
        volume=(float)(Lucky7Config.INSTANCE.masterVolume*Lucky7Config.INSTANCE.reelVolume);
    }
}
