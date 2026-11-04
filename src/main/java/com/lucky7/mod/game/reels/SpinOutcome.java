package com.lucky7.mod.game.reels;

import com.lucky7.mod.game.WinCategory;

import java.util.List;

public record SpinOutcome(WinCategory category,List<ReelSymbol> symbols) {
    public SpinOutcome {
        symbols=List.copyOf(symbols);
        if (symbols.size()!=ReelLayout.REEL_COUNT) throw new IllegalArgumentException("A spin needs exactly three symbols");
    }

    public String display() {
        return symbols.get(0).name()+" "+symbols.get(1).name()+" "+symbols.get(2).name();
    }
}
