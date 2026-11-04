package com.lucky7.mod.block;

import com.lucky7.mod.Lucky7Mod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

public final class Lucky7Blocks {
    public static final Block SLOT_MACHINE = new Lucky7Block(
        AbstractBlock.Settings.create()
            .strength(3.5f)
            .requiresTool()
            .nonOpaque()
            .luminance(state -> 5)
    );

    public static final BlockItem SLOT_MACHINE_ITEM = new BlockItem(SLOT_MACHINE, new Item.Settings());

    public static final RegistryKey<ItemGroup> LUCKY7_GROUP_KEY = RegistryKey.of(
        RegistryKeys.ITEM_GROUP,
        Lucky7Mod.id("general")
    );

    public static final ItemGroup LUCKY7_GROUP = FabricItemGroup.builder()
        .icon(() -> new ItemStack(SLOT_MACHINE_ITEM))
        .displayName(Text.translatable("itemGroup.lucky7.general"))
        .entries((context, entries) -> entries.add(SLOT_MACHINE_ITEM))
        .build();

    private Lucky7Blocks() { }

    public static void register() {
        Registry.register(Registries.BLOCK, Lucky7Mod.id("slot_machine"), SLOT_MACHINE);
        Registry.register(Registries.ITEM, Lucky7Mod.id("slot_machine"), SLOT_MACHINE_ITEM);
        Registry.register(Registries.ITEM_GROUP, LUCKY7_GROUP_KEY, LUCKY7_GROUP);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(SLOT_MACHINE_ITEM));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> entries.add(SLOT_MACHINE_ITEM));
    }
}
