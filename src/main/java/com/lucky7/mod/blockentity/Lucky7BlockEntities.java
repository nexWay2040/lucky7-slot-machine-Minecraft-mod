package com.lucky7.mod.blockentity;

import com.lucky7.mod.Lucky7Mod;
import com.lucky7.mod.block.Lucky7Blocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class Lucky7BlockEntities {
    public static final BlockEntityType<Lucky7BlockEntity> SLOT_MACHINE = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        Lucky7Mod.id("slot_machine"),
        FabricBlockEntityTypeBuilder.create(Lucky7BlockEntity::new, Lucky7Blocks.SLOT_MACHINE).build()
    );

    public static final BlockEntityType<LuckyWheelBlockEntity> LUCKY_WHEEL = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        Lucky7Mod.id("lucky_wheel"),
        FabricBlockEntityTypeBuilder.create(LuckyWheelBlockEntity::new, Lucky7Blocks.LUCKY_WHEEL).build()
    );

    public static final BlockEntityType<ShellGameBlockEntity> SHELL_GAME = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        Lucky7Mod.id("shell_game"),
        FabricBlockEntityTypeBuilder.create(ShellGameBlockEntity::new, Lucky7Blocks.SHELL_GAME).build()
    );

    public static void register() { }

    private Lucky7BlockEntities() { }
}
