package com.lucky7.mod.sound;

import com.lucky7.mod.Lucky7Mod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;

public final class Lucky7Sounds {
    public static final SoundEvent BUTTON_CLICK=register("button_click");
    public static final SoundEvent COIN_INSERT=register("coin_insert");
    public static final SoundEvent MACHINE_START=register("machine_start");
    public static final SoundEvent REEL_SPIN=register("reel_spin");
    public static final SoundEvent REEL_STOP=register("reel_stop");
    public static final SoundEvent SMALL_WIN=register("small_win");
    public static final SoundEvent JACKPOT=register("jackpot");
    private static SoundEvent register(String name) {
        var id=Lucky7Mod.id(name);
        return Registry.register(Registries.SOUND_EVENT,id,SoundEvent.of(id));
    }
    public static void register() { }
    private Lucky7Sounds() { }
}
