package com.lucky7.mod.block;

import com.lucky7.mod.Lucky7Mod;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public final class Lucky7Blocks {
    public static final ResourceKey<Block> SLOT_MACHINE_KEY = ResourceKey.create(Registries.BLOCK, Lucky7Mod.id("slot_machine"));
    public static final ResourceKey<Item> SLOT_MACHINE_ITEM_KEY = ResourceKey.create(Registries.ITEM, Lucky7Mod.id("slot_machine"));

    public static final ResourceKey<Block> LUCKY_WHEEL_KEY = ResourceKey.create(Registries.BLOCK, Lucky7Mod.id("lucky_wheel"));
    public static final ResourceKey<Item> LUCKY_WHEEL_ITEM_KEY = ResourceKey.create(Registries.ITEM, Lucky7Mod.id("lucky_wheel"));

    public static final ResourceKey<Block> SHELL_GAME_KEY = ResourceKey.create(Registries.BLOCK, Lucky7Mod.id("shell_game"));
    public static final ResourceKey<Item> SHELL_GAME_ITEM_KEY = ResourceKey.create(Registries.ITEM, Lucky7Mod.id("shell_game"));

    public static final Block SLOT_MACHINE = registerBlock(
        SLOT_MACHINE_KEY,
        Lucky7Block::new,
        BlockBehaviour.Properties.of()
            .strength(5.0f, 15.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .lightLevel(state -> 5)
    );
    public static final BlockItem SLOT_MACHINE_ITEM = registerBlockItem(SLOT_MACHINE_ITEM_KEY, SLOT_MACHINE);

    public static final Block LUCKY_WHEEL = registerBlock(
        LUCKY_WHEEL_KEY,
        LuckyWheelBlock::new,
        BlockBehaviour.Properties.of()
            .strength(4.0f, 10.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
            .lightLevel(state -> 8)
    );
    public static final BlockItem LUCKY_WHEEL_ITEM = registerBlockItem(LUCKY_WHEEL_ITEM_KEY, LUCKY_WHEEL);

    public static final Block SHELL_GAME = registerBlock(
        SHELL_GAME_KEY,
        ShellGameBlock::new,
        BlockBehaviour.Properties.of()
            .strength(3.0f, 6.0f)
            .sound(SoundType.WOOD)
            .noOcclusion()
            .lightLevel(state -> 4)
    );
    public static final BlockItem SHELL_GAME_ITEM = registerBlockItem(SHELL_GAME_ITEM_KEY, SHELL_GAME);

    public static final ResourceKey<CreativeModeTab> LUCKY7_GROUP_KEY = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        Lucky7Mod.id("general")
    );

    public static final CreativeModeTab LUCKY7_GROUP = FabricCreativeModeTab.builder()
        .icon(() -> new ItemStack(SLOT_MACHINE_ITEM))
        .title(Component.translatable("itemGroup.lucky7.general"))
        .displayItems((context, entries) -> {
            entries.accept(SLOT_MACHINE_ITEM);
            entries.accept(LUCKY_WHEEL_ITEM);
            entries.accept(SHELL_GAME_ITEM);
        })
        .build();

    private Lucky7Blocks() { }

    private static Block registerBlock(ResourceKey<Block> key, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
        Block block = factory.apply(props.setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    private static BlockItem registerBlockItem(ResourceKey<Item> key, Block block) {
        Item.Properties itemProps = new Item.Properties().setId(key).useBlockDescriptionPrefix();
        BlockItem blockItem = new BlockItem(block, itemProps);
        blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
        return Registry.register(BuiltInRegistries.ITEM, key, blockItem);
    }

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, LUCKY7_GROUP_KEY, LUCKY7_GROUP);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(SLOT_MACHINE_ITEM);
            entries.accept(LUCKY_WHEEL_ITEM);
            entries.accept(SHELL_GAME_ITEM);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
            entries.accept(SLOT_MACHINE_ITEM);
            entries.accept(LUCKY_WHEEL_ITEM);
        });
    }
}
