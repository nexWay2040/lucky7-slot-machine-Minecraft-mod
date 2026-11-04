package com.lucky7.mod;

import com.lucky7.mod.block.Lucky7Blocks;
import com.lucky7.mod.blockentity.Lucky7BlockEntities;
import com.lucky7.mod.blockentity.Lucky7BlockEntity;
import com.lucky7.mod.config.Lucky7Config;
import com.lucky7.mod.network.Lucky7ActionPayload;
import com.lucky7.mod.screen.Lucky7ScreenHandlers;
import com.lucky7.mod.sound.Lucky7Sounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Lucky7Mod implements ModInitializer {
    public static final String MOD_ID = "lucky7";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Lucky7Config.load();
        Lucky7Blocks.register();
        Lucky7BlockEntities.register();
        Lucky7Sounds.register();
        Lucky7ScreenHandlers.register();

        PayloadTypeRegistry.playC2S().register(Lucky7ActionPayload.ID, Lucky7ActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Lucky7ActionPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                if (player == null || player.isRemoved()) return;
                Lucky7BlockEntity machine = null;
                if (player.currentScreenHandler instanceof com.lucky7.mod.screen.Lucky7ScreenHandler handler) {
                    machine = handler.machine();
                }
                if (machine == null) {
                    var world = player.getWorld();
                    var be = world.getBlockEntity(payload.pos());
                    if (be == null) be = world.getBlockEntity(payload.pos().down());
                    if (be instanceof Lucky7BlockEntity m) machine = m;
                }
                if (machine != null) {
                    if (player.squaredDistanceTo(machine.getPos().toCenterPos()) <= 64.0) {
                        LOGGER.info("Received action {} from player {} for machine at {}", payload.action(), player.getName().getString(), machine.getPos());
                        if (payload.action() == 0) {
                            machine.startSpin(player);
                        } else if (payload.action() == 1) {
                            machine.returnStake(player);
                        }
                    }
                } else {
                    LOGGER.warn("Machine not found for action payload from {} at {}", player.getName().getString(), payload.pos());
                }
            });
        });

        LOGGER.info("Lucky 7 initialized: retro 3D casino slot machine ready!");
    }
}
