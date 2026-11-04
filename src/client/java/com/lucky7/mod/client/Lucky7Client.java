package com.lucky7.mod.client;

import com.lucky7.mod.block.Lucky7Blocks;
import com.lucky7.mod.blockentity.Lucky7BlockEntities;
import com.lucky7.mod.client.render.Lucky7BlockEntityRenderer;
import com.lucky7.mod.client.screen.Lucky7Screen;
import com.lucky7.mod.client.sound.Lucky7SoundController;
import com.lucky7.mod.screen.Lucky7ScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;

public final class Lucky7Client implements ClientModInitializer {
    @Override public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(Lucky7Blocks.SLOT_MACHINE,RenderLayer.getCutout());
        BlockEntityRendererRegistry.register(Lucky7BlockEntities.SLOT_MACHINE,Lucky7BlockEntityRenderer::new);
        Lucky7SoundController.register();
        HandledScreens.register(Lucky7ScreenHandlers.MACHINE,Lucky7Screen::new);
    }
}
