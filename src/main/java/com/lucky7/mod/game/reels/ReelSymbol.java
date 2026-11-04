package com.lucky7.mod.game.reels;

import java.util.Locale;

public enum ReelSymbol {
    SEVEN,
    GOLDEN_SEVEN,
    BAR,
    DOUBLE_BAR,
    CHERRY,
    LEMON,
    ORANGE,
    GRAPES,
    WATERMELON,
    BELL,
    HORSESHOE,
    CLOVER,
    STAR,
    DIAMOND,
    CROWN,
    SKULL,
    MOON,
    COIN;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ReelSymbol byName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return LEMON;
        }
    }
}
