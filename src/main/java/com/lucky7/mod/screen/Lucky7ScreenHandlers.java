package com.lucky7.mod.screen;

import com.lucky7.mod.Lucky7Mod;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.math.BlockPos;

public final class Lucky7ScreenHandlers {
    public static final ScreenHandlerType<Lucky7ScreenHandler> MACHINE=Registry.register(
        Registries.SCREEN_HANDLER,
        Lucky7Mod.id("machine"),
        new ExtendedScreenHandlerType<>(Lucky7ScreenHandler::new,BlockPos.PACKET_CODEC)
    );
    private Lucky7ScreenHandlers() { }
    public static void register() { }
}
