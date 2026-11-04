package com.lucky7.mod.game.reels;

import com.lucky7.mod.game.WinCategory;
import com.lucky7.mod.game.probability.ProbabilityProfile;

import java.util.List;
import java.util.random.RandomGenerator;

public final class SpinEngine {
    private static final List<ReelSymbol> BIG_SYMBOLS=List.of(ReelSymbol.BAR,ReelSymbol.CROWN,ReelSymbol.DIAMOND);
    private static final List<ReelSymbol> MEDIUM_SYMBOLS=List.of(ReelSymbol.STAR,ReelSymbol.BELL);
    private static final List<ReelSymbol> SMALL_SYMBOLS=List.of(ReelSymbol.CHERRY,ReelSymbol.LEMON);

    private SpinEngine() {
    }

    public static SpinOutcome roll(ProbabilityProfile profile,RandomGenerator random) {
        WinCategory category=profile.choose(random);
        return switch (category) {
            case JACKPOT -> triple(random.nextBoolean()?ReelSymbol.SEVEN:ReelSymbol.GOLDEN_SEVEN,category);
            case BIG_WIN -> triple(pick(BIG_SYMBOLS,random),category);
            case MEDIUM_WIN -> triple(pick(MEDIUM_SYMBOLS,random),category);
            case SMALL_WIN -> triple(pick(SMALL_SYMBOLS,random),category);
            case PAIR -> pair(random);
            case NO_WIN -> noWin(random);
        };
    }

    public static WinCategory classify(List<ReelSymbol> symbols) {
        ReelSymbol first=symbols.get(0),second=symbols.get(1),third=symbols.get(2);
        if (first==second&&second==third) {
            if (first==ReelSymbol.SEVEN||first==ReelSymbol.GOLDEN_SEVEN) return WinCategory.JACKPOT;
            if (BIG_SYMBOLS.contains(first)) return WinCategory.BIG_WIN;
            if (MEDIUM_SYMBOLS.contains(first)) return WinCategory.MEDIUM_WIN;
            return WinCategory.SMALL_WIN;
        }
        return first==second||second==third||first==third?WinCategory.PAIR:WinCategory.NO_WIN;
    }

    private static SpinOutcome triple(ReelSymbol symbol,WinCategory category) {
        return new SpinOutcome(category,List.of(symbol,symbol,symbol));
    }

    private static SpinOutcome pair(RandomGenerator random) {
        ReelSymbol paired=randomSymbol(random);
        ReelSymbol different;
        do different=randomSymbol(random); while (different==paired);
        int oddReel=random.nextInt(ReelLayout.REEL_COUNT);
        ReelSymbol[] result={paired,paired,paired};
        result[oddReel]=different;
        return new SpinOutcome(WinCategory.PAIR,List.of(result));
    }

    private static SpinOutcome noWin(RandomGenerator random) {
        ReelSymbol first=randomSymbol(random);
        ReelSymbol second;
        ReelSymbol third;
        do second=randomSymbol(random); while (second==first);
        do third=randomSymbol(random); while (third==first||third==second);
        return new SpinOutcome(WinCategory.NO_WIN,List.of(first,second,third));
    }

    private static ReelSymbol randomSymbol(RandomGenerator random) {
        ReelSymbol[] values=ReelSymbol.values();
        return values[random.nextInt(values.length)];
    }

    private static ReelSymbol pick(List<ReelSymbol> values,RandomGenerator random) {
        return values.get(random.nextInt(values.size()));
    }
}
