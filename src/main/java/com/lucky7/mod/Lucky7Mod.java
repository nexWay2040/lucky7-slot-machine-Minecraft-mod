package com.lucky7.mod;

import com.lucky7.mod.block.Lucky7Blocks;
import com.lucky7.mod.blockentity.Lucky7BlockEntities;
import com.lucky7.mod.blockentity.Lucky7BlockEntity;
import com.lucky7.mod.config.Lucky7Config;
import com.lucky7.mod.item.Lucky7Items;
import com.lucky7.mod.network.Lucky7ActionPayload;
import com.lucky7.mod.screen.Lucky7ScreenHandler;
import com.lucky7.mod.screen.Lucky7ScreenHandlers;
import com.lucky7.mod.sound.Lucky7Sounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Lucky7Mod implements ModInitializer {
    public static final String MOD_ID = "lucky7";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Lucky7Config.load();
        Lucky7Blocks.register();
        Lucky7Items.register();
        Lucky7BlockEntities.register();
        Lucky7Sounds.register();
        Lucky7ScreenHandlers.register();

        PayloadTypeRegistry.serverboundPlay().register(Lucky7ActionPayload.TYPE, Lucky7ActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Lucky7ActionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                if (player == null || player.isRemoved()) return;
                Lucky7BlockEntity machine = null;
                if (player.containerMenu instanceof Lucky7ScreenHandler handler) {
                    machine = handler.machine();
                }
                if (machine == null) {
                    var level = player.level();
                    var be = level.getBlockEntity(payload.pos());
                    if (be == null) be = level.getBlockEntity(payload.pos().below());
                    if (be instanceof Lucky7BlockEntity m) machine = m;
                }
                if (machine != null) {
                    if (player.distanceToSqr(Vec3.atCenterOf(machine.getBlockPos())) <= 64.0) {
                        LOGGER.info("Received action {} from player {} for machine at {}", payload.action(), player.getName().getString(), machine.getBlockPos());
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

        // Automatically give Don Creeperione's Contract to every player upon entering the world (supports Co-op)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            if (!player.entityTags().contains("lucky7_starter_contract")) {
                player.addTag("lucky7_starter_contract");
                ItemStack contract = new ItemStack(Lucky7Items.CASINO_CONTRACT);
                if (!player.getInventory().add(contract)) {
                    player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), contract));
                }
                player.sendSystemMessage(Component.literal("📜 [Lucky 7 Syndicate] Вы получили пригласительный Контракт Дона Криперионе! Возьмите его в руки и нажмите ПКМ.").withStyle(ChatFormatting.GOLD));
            }
        });

        LOGGER.info("Lucky 7 v2.0.0 initialized for Minecraft 26.3: Storyline, 3D Casinos & VIP Rewards ready!");
    }
}
