package com.lucky7.mod.game.reels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ReelLayout {
    public static final int REEL_COUNT = 3;
    public static final int SYMBOLS_PER_REEL = 18;
    public static final int SYMBOL_COUNT = ReelSymbol.values().length;

    public record ReelStop(int reel, int position, ReelSymbol symbol, String id) {
        @Override
        public String toString() {
            return id + "[" + symbol.name() + "]";
        }
    }

    private static final List<List<ReelSymbol>> STRIPS = List.of(
        List.of(
            ReelSymbol.SEVEN, ReelSymbol.CHERRY, ReelSymbol.BAR, ReelSymbol.LEMON,
            ReelSymbol.CROWN, ReelSymbol.ORANGE, ReelSymbol.STAR, ReelSymbol.GRAPES,
            ReelSymbol.GOLDEN_SEVEN, ReelSymbol.CLOVER, ReelSymbol.BELL, ReelSymbol.WATERMELON,
            ReelSymbol.DIAMOND, ReelSymbol.MOON, ReelSymbol.DOUBLE_BAR, ReelSymbol.HORSESHOE,
            ReelSymbol.COIN, ReelSymbol.SKULL
        ),
        List.of(
            ReelSymbol.BELL, ReelSymbol.GOLDEN_SEVEN, ReelSymbol.ORANGE, ReelSymbol.BAR,
            ReelSymbol.MOON, ReelSymbol.CHERRY, ReelSymbol.DIAMOND, ReelSymbol.CLOVER,
            ReelSymbol.LEMON, ReelSymbol.CROWN, ReelSymbol.WATERMELON, ReelSymbol.SEVEN,
            ReelSymbol.HORSESHOE, ReelSymbol.GRAPES, ReelSymbol.SKULL, ReelSymbol.STAR,
            ReelSymbol.DOUBLE_BAR, ReelSymbol.COIN
        ),
        List.of(
            ReelSymbol.COIN, ReelSymbol.LEMON, ReelSymbol.SEVEN, ReelSymbol.SKULL,
            ReelSymbol.BAR, ReelSymbol.GRAPES, ReelSymbol.CROWN, ReelSymbol.CHERRY,
            ReelSymbol.HORSESHOE, ReelSymbol.GOLDEN_SEVEN, ReelSymbol.STAR, ReelSymbol.ORANGE,
            ReelSymbol.DOUBLE_BAR, ReelSymbol.MOON, ReelSymbol.BELL, ReelSymbol.DIAMOND,
            ReelSymbol.WATERMELON, ReelSymbol.CLOVER
        )
    );

    // Pre-indexed table of unique ReelStop objects with distinct IDs for all 54 positions
    private static final List<List<ReelStop>> STOPS;

    static {
        List<List<ReelStop>> reelList = new ArrayList<>(REEL_COUNT);
        for (int r = 0; r < REEL_COUNT; r++) {
            List<ReelSymbol> strip = STRIPS.get(r);
            List<ReelStop> stops = new ArrayList<>(SYMBOLS_PER_REEL);
            for (int p = 0; p < SYMBOLS_PER_REEL; p++) {
                ReelSymbol sym = strip.get(p);
                // Unique ID per reel and position: e.g. "reel_0_pos_00_seven"
                String uniqueId = String.format(Locale.ROOT, "reel_%d_pos_%02d_%s", r, p, sym.id());
                stops.add(new ReelStop(r, p, sym, uniqueId));
            }
            reelList.add(Collections.unmodifiableList(stops));
        }
        STOPS = Collections.unmodifiableList(reelList);
    }

    private ReelLayout() {
    }

    public static List<ReelSymbol> strip(int reel) {
        return STRIPS.get(Math.floorMod(reel, REEL_COUNT));
    }

    public static List<ReelStop> stops(int reel) {
        return STOPS.get(Math.floorMod(reel, REEL_COUNT));
    }

    public static ReelStop stopAt(int reel, int position) {
        List<ReelStop> reelStops = stops(reel);
        return reelStops.get(Math.floorMod(position, SYMBOLS_PER_REEL));
    }

    public static String uniqueId(int reel, int position) {
        return stopAt(reel, position).id();
    }

    public static ReelSymbol symbolAt(int reel, int position) {
        List<ReelSymbol> strip = strip(reel);
        return strip.get(Math.floorMod(position, strip.size()));
    }

    public static int positionOf(int reel, ReelSymbol symbol) {
        int position = strip(reel).indexOf(symbol);
        if (position < 0) {
            throw new IllegalArgumentException("Symbol is missing from reel " + reel + ": " + symbol);
        }
        return position;
    }
}
