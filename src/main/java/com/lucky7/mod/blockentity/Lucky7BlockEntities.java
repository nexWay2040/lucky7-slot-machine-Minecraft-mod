package com.lucky7.mod.blockentity;

import com.lucky7.mod.Lucky7Mod;
import com.lucky7.mod.block.Lucky7Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class Lucky7BlockEntities {
 public static final BlockEntityType<Lucky7BlockEntity> SLOT_MACHINE = Registry.register(Registries.BLOCK_ENTITY_TYPE, Lucky7Mod.id("slot_machine"), BlockEntityType.Builder.create(Lucky7BlockEntity::new, Lucky7Blocks.SLOT_MACHINE).build());
 public static void register() { }
}
