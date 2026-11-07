package com.lucky7.mod.sound;

import com.lucky7.mod.Lucky7Mod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class Lucky7Sounds {
    public static final SoundEvent BUTTON_CLICK = register("button_click");
    public static final SoundEvent COIN_INSERT = register("coin_insert");
    public static final SoundEvent MACHINE_START = register("machine_start");
    public static final SoundEvent REEL_SPIN = register("reel_spin");
    public static final SoundEvent REEL_STOP = register("reel_stop");
    public static final SoundEvent SMALL_WIN = register("small_win");
    public static final SoundEvent JACKPOT = register("jackpot");

    private static SoundEvent register(String name) {
        var id = Lucky7Mod.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void register() { }

    private Lucky7Sounds() { }
}
