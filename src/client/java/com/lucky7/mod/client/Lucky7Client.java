package com.lucky7.mod.client;

import com.lucky7.mod.blockentity.Lucky7BlockEntities;
import com.lucky7.mod.client.render.Lucky7BlockEntityRenderer;
import com.lucky7.mod.client.render.LuckyWheelBlockEntityRenderer;
import com.lucky7.mod.client.render.ShellGameBlockEntityRenderer;
import com.lucky7.mod.client.screen.Lucky7Screen;
import com.lucky7.mod.client.sound.Lucky7SoundController;
import com.lucky7.mod.screen.Lucky7ScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class Lucky7Client implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(Lucky7BlockEntities.SLOT_MACHINE, Lucky7BlockEntityRenderer::new);
        BlockEntityRenderers.register(Lucky7BlockEntities.LUCKY_WHEEL, LuckyWheelBlockEntityRenderer::new);
        BlockEntityRenderers.register(Lucky7BlockEntities.SHELL_GAME, ShellGameBlockEntityRenderer::new);
        Lucky7SoundController.register();
        MenuScreens.register(Lucky7ScreenHandlers.MACHINE, Lucky7Screen::new);
    }
}
