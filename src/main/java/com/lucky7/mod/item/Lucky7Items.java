package com.lucky7.mod.item;

import com.lucky7.mod.Lucky7Mod;
import com.lucky7.mod.block.Lucky7Blocks;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

public final class Lucky7Items {
    public static final ResourceKey<Item> CASINO_CONTRACT_KEY = ResourceKey.create(Registries.ITEM, Lucky7Mod.id("casino_contract"));
    public static final ResourceKey<Item> GOLDEN_CHIP_KEY = ResourceKey.create(Registries.ITEM, Lucky7Mod.id("golden_chip"));
    public static final ResourceKey<Item> LUCKY_COIN_KEY = ResourceKey.create(Registries.ITEM, Lucky7Mod.id("lucky_coin"));
    public static final ResourceKey<Item> LEAFXUP_TROPHY_KEY = ResourceKey.create(Registries.ITEM, Lucky7Mod.id("leafxup_trophy"));

    public static final Item CASINO_CONTRACT = registerItem(
        CASINO_CONTRACT_KEY,
        CasinoContractItem::new,
        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
    );

    public static final Item GOLDEN_CHIP = registerItem(
        GOLDEN_CHIP_KEY,
        Item::new,
        new Item.Properties().stacksTo(64).rarity(Rarity.RARE)
    );

    public static final Item LUCKY_COIN = registerItem(
        LUCKY_COIN_KEY,
        Item::new,
        new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)
    );

    public static final Item LEAFXUP_TROPHY = registerItem(
        LEAFXUP_TROPHY_KEY,
        LeafxupTrophyItem::new,
        new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
    );

    private Lucky7Items() { }

    private static Item registerItem(ResourceKey<Item> key, Function<Item.Properties, Item> factory, Item.Properties props) {
        Item item = factory.apply(props.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void register() {
        CreativeModeTabEvents.modifyOutputEvent(Lucky7Blocks.LUCKY7_GROUP_KEY).register(entries -> {
            entries.accept(CASINO_CONTRACT);
            entries.accept(GOLDEN_CHIP);
            entries.accept(LUCKY_COIN);
            entries.accept(LEAFXUP_TROPHY);
        });

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(CASINO_CONTRACT);
            entries.accept(GOLDEN_CHIP);
            entries.accept(LUCKY_COIN);
            entries.accept(LEAFXUP_TROPHY);
        });
    }
}
