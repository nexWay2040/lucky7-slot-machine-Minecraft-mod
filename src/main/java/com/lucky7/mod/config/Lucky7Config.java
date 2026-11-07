package com.lucky7.mod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lucky7.mod.Lucky7Mod;
import com.lucky7.mod.game.probability.ProbabilitySettings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Lucky7Config {
    public boolean enabled = true;
    public int stakeSlots = 9;
    public int maxStakeValue = 1_000_000;
    public int maxStakeItems = 576;
    /** Chance to win at zero stake, and how much each point of stake adds. */
    public double winBase = 0.24;
    public double winPerValue = 0.0006;
    public double winMax = 0.85;
    public double jackpotBase = 0.008;
    public double jackpotPerValue = 0.00008;
    public double jackpotMax = 0.25;
    public double bigWeight = 0.16;
    public double mediumWeight = 0.28;
    public double smallWeight = 0.36;
    public double pairWeight = 0.20;
    public double smallMultiplier = 1.6;
    public double mediumMultiplier = 3.2;
    public double bigMultiplier = 7.0;
    public double pairMultiplier = 1.15;
    public double jackpotMultiplier = 25.0;
    public double maxPayoutMultiplier = 40.0;
    public int historyLimit = 20;
    public boolean attractionMode = true;
    public boolean soundsEnabled = true;
    public double masterVolume = 0.85;
    public double reelVolume = 0.65;
    public double effectsVolume = 0.9;
    public int spinTicks = 60;
    public Map<String, Integer> resourceValues = defaultResourceValues();
    public static final Lucky7Config INSTANCE = new Lucky7Config();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("lucky7.json");
        try {
            if (Files.exists(path)) {
                Lucky7Config c = GSON.fromJson(Files.readString(path), Lucky7Config.class);
                if (c != null) {
                    INSTANCE.copyFrom(c);
                    defaultResourceValues().forEach((k, v) -> INSTANCE.resourceValues.putIfAbsent(k, v));
                }
            }
            Files.writeString(path, GSON.toJson(INSTANCE));
        } catch (Exception e) {
            Lucky7Mod.LOGGER.warn("Cannot load Lucky 7 config; using defaults: {}", e.toString());
        }
    }

    private void copyFrom(Lucky7Config c) {
        enabled = c.enabled;
        stakeSlots = clamp(c.stakeSlots, 1, 9);
        maxStakeValue = Math.max(1_000_000, c.maxStakeValue);
        maxStakeItems = Math.max(576, c.maxStakeItems);
        winBase = unit(c.winBase);
        winPerValue = Math.max(0, Math.min(1, c.winPerValue));
        winMax = unit(c.winMax);
        if (winMax < winBase) winMax = winBase;
        jackpotBase = unit(c.jackpotBase);
        jackpotPerValue = Math.max(0, Math.min(1, c.jackpotPerValue));
        jackpotMax = unit(c.jackpotMax);
        if (jackpotMax > winMax) jackpotMax = winMax;
        bigWeight = weight(c.bigWeight, 0.16);
        mediumWeight = weight(c.mediumWeight, 0.28);
        smallWeight = weight(c.smallWeight, 0.36);
        pairWeight = weight(c.pairWeight, 0.20);
        if (bigWeight + mediumWeight + smallWeight + pairWeight <= 0) {
            bigWeight = 0.16;
            mediumWeight = 0.28;
            smallWeight = 0.36;
            pairWeight = 0.20;
        }
        smallMultiplier = multiplier(c.smallMultiplier, 1.6);
        mediumMultiplier = multiplier(c.mediumMultiplier, 3.2);
        bigMultiplier = multiplier(c.bigMultiplier, 7.0);
        pairMultiplier = multiplier(c.pairMultiplier, 1.15);
        jackpotMultiplier = multiplier(c.jackpotMultiplier, 25.0);
        maxPayoutMultiplier = multiplier(c.maxPayoutMultiplier, 40.0);
        historyLimit = clamp(c.historyLimit, 1, 100);
        attractionMode = c.attractionMode;
        soundsEnabled = c.soundsEnabled;
        masterVolume = unit(c.masterVolume);
        reelVolume = unit(c.reelVolume);
        effectsVolume = unit(c.effectsVolume);
        spinTicks = clamp(c.spinTicks, 20, 200);
        resourceValues = validatedValues(c.resourceValues);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double unit(double d) {
        return Double.isFinite(d) ? Math.max(0, Math.min(1, d)) : 0;
    }

    private static double weight(double value, double fallback) {
        return Double.isFinite(value) && value >= 0 ? value : fallback;
    }

    private static double multiplier(double value, double fallback) {
        return Double.isFinite(value) && value >= 0 ? Math.min(1_000, value) : fallback;
    }

    public static int value(Item item) {
        if (item == null) return 0;
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        int val = INSTANCE.resourceValues.getOrDefault(id, 0);
        if (val <= 0) {
            val = defaultResourceValues().getOrDefault(id, 0);
        }
        return val;
    }

    public ProbabilitySettings probabilitySettings() {
        return new ProbabilitySettings(winBase, winPerValue, winMax, jackpotBase, jackpotPerValue, jackpotMax, bigWeight, mediumWeight, smallWeight, pairWeight);
    }

    public List<Map.Entry<String, Integer>> lootTable() {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(resourceValues.entrySet());
        list.sort(Map.Entry.comparingByValue());
        return list;
    }

    private static LinkedHashMap<String, Integer> validatedValues(Map<String, Integer> values) {
        LinkedHashMap<String, Integer> validated = new LinkedHashMap<>();
        Map<String, Integer> source = values == null ? defaultResourceValues() : values;
        source.forEach((id, value) -> {
            if (id != null && Identifier.tryParse(id) != null && value != null && value > 0)
                validated.put(id, clamp(value, 1, 1_000_000));
        });
        return validated.isEmpty() ? defaultResourceValues() : validated;
    }

    private static LinkedHashMap<String, Integer> defaultResourceValues() {
        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
        values.put("lucky7:lucky_coin", 25);
        values.put("lucky7:golden_chip", 100);
        values.put("lucky7:leafxup_trophy", 777);

        values.put("minecraft:iron_nugget", 1);
        values.put("minecraft:gold_nugget", 1);
        values.put("minecraft:coal", 1);
        values.put("minecraft:coal_block", 9);
        values.put("minecraft:copper_ingot", 2);
        values.put("minecraft:raw_copper", 2);
        values.put("minecraft:copper_block", 18);
        values.put("minecraft:waxed_copper_block", 18);
        values.put("minecraft:cut_copper", 18);
        values.put("minecraft:raw_copper_block", 18);
        values.put("minecraft:amethyst_shard", 2);
        values.put("minecraft:amethyst_block", 8);
        values.put("minecraft:lapis_lazuli", 3);
        values.put("minecraft:lapis_block", 27);
        values.put("minecraft:redstone", 3);
        values.put("minecraft:redstone_block", 27);
        values.put("minecraft:quartz", 3);
        values.put("minecraft:quartz_block", 12);
        values.put("minecraft:iron_ingot", 4);
        values.put("minecraft:raw_iron", 4);
        values.put("minecraft:raw_iron_block", 36);
        values.put("minecraft:iron_block", 36);
        values.put("minecraft:gold_ingot", 8);
        values.put("minecraft:raw_gold", 8);
        values.put("minecraft:raw_gold_block", 72);
        values.put("minecraft:gold_block", 72);
        values.put("minecraft:emerald", 12);
        values.put("minecraft:emerald_block", 108);
        values.put("minecraft:diamond", 16);
        values.put("minecraft:diamond_block", 144);
        values.put("minecraft:ancient_debris", 24);
        values.put("minecraft:netherite_scrap", 20);
        values.put("minecraft:netherite_ingot", 36);
        values.put("minecraft:netherite_block", 324);

        values.put("minecraft:enchanted_golden_apple", 500);

        values.put("minecraft:iron_shovel", 4);
        values.put("minecraft:iron_sword", 8);
        values.put("minecraft:iron_hoe", 8);
        values.put("minecraft:iron_pickaxe", 12);
        values.put("minecraft:iron_axe", 12);

        values.put("minecraft:golden_shovel", 8);
        values.put("minecraft:golden_sword", 16);
        values.put("minecraft:golden_hoe", 16);
        values.put("minecraft:golden_pickaxe", 24);
        values.put("minecraft:golden_axe", 24);

        values.put("minecraft:diamond_shovel", 16);
        values.put("minecraft:diamond_sword", 32);
        values.put("minecraft:diamond_hoe", 32);
        values.put("minecraft:diamond_pickaxe", 48);
        values.put("minecraft:diamond_axe", 48);

        values.put("minecraft:netherite_shovel", 64);
        values.put("minecraft:netherite_sword", 80);
        values.put("minecraft:netherite_hoe", 80);
        values.put("minecraft:netherite_pickaxe", 96);
        values.put("minecraft:netherite_axe", 96);

        values.put("minecraft:iron_boots", 16);
        values.put("minecraft:iron_helmet", 20);
        values.put("minecraft:iron_leggings", 28);
        values.put("minecraft:iron_chestplate", 32);

        values.put("minecraft:golden_boots", 32);
        values.put("minecraft:golden_helmet", 40);
        values.put("minecraft:golden_leggings", 56);
        values.put("minecraft:golden_chestplate", 64);

        values.put("minecraft:diamond_boots", 64);
        values.put("minecraft:diamond_helmet", 80);
        values.put("minecraft:diamond_leggings", 112);
        values.put("minecraft:diamond_chestplate", 128);

        values.put("minecraft:netherite_boots", 114);
        values.put("minecraft:netherite_helmet", 130);
        values.put("minecraft:netherite_leggings", 162);
        values.put("minecraft:netherite_chestplate", 178);

        values.put("minecraft:totem_of_undying", 400);
        values.put("minecraft:elytra", 2500);
        values.put("minecraft:nether_star", 2000);
        values.put("minecraft:dragon_egg", 10000);
        values.put("minecraft:heart_of_the_sea", 300);
        values.put("minecraft:trident", 600);
        values.put("minecraft:heavy_core", 3000);
        values.put("minecraft:mace", 3500);
        values.put("minecraft:trial_key", 100);
        values.put("minecraft:ominous_trial_key", 250);
        values.put("minecraft:echo_shard", 150);
        values.put("minecraft:sculk_catalyst", 200);
        values.put("minecraft:wither_skeleton_skull", 250);
        values.put("minecraft:netherite_upgrade_smithing_template", 600);
        values.put("minecraft:beacon", 2500);

        return values;
    }
}
