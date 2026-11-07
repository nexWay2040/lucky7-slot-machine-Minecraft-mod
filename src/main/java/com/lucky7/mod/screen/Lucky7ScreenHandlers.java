package com.lucky7.mod.screen;

import com.lucky7.mod.Lucky7Mod;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public final class Lucky7ScreenHandlers {
    public static final MenuType<Lucky7ScreenHandler> MACHINE = Registry.register(
        BuiltInRegistries.MENU,
        Lucky7Mod.id("machine"),
        new ExtendedMenuType<>(Lucky7ScreenHandler::new, BlockPos.STREAM_CODEC)
    );

    private Lucky7ScreenHandlers() { }

    public static void register() { }
}
