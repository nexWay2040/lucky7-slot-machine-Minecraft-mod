package com.lucky7.mod.game;

import com.lucky7.mod.game.probability.ProbabilityCalculator;
import com.lucky7.mod.game.probability.ProbabilityProfile;
import com.lucky7.mod.game.probability.ProbabilitySettings;
import com.lucky7.mod.game.reels.ReelLayout;
import com.lucky7.mod.game.reels.ReelSymbol;
import com.lucky7.mod.game.reels.SpinEngine;
import com.lucky7.mod.game.reels.SpinOutcome;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProbabilityAndReelTest {
    @Test
    void configuredProbabilitiesAreNormalizedAndMonotonic() {
        double previousJackpot=-1;
        double previousWin=-1;
        for(int value:new int[]{0,1,4,16,24,64,128,256}) {
            ProbabilityProfile profile=ProbabilityCalculator.calculate(value,ProbabilitySettings.DEFAULTS);
            double sum=profile.jackpot()+profile.bigWin()+profile.mediumWin()+profile.smallWin()+profile.pair()+profile.noWin();
            assertEquals(1.0,sum,1.0e-12);
            assertTrue(profile.jackpot()>=previousJackpot);
            assertTrue(profile.anyWin()>=previousWin);
            previousJackpot=profile.jackpot();
            previousWin=profile.anyWin();
        }
        assertEquals(0.008,ProbabilityCalculator.calculate(4,ProbabilitySettings.DEFAULTS).jackpot(),1.0e-12);
        assertEquals(0.032,ProbabilityCalculator.calculate(16,ProbabilitySettings.DEFAULTS).jackpot(),1.0e-12);
        assertEquals(0.048,ProbabilityCalculator.calculate(24,ProbabilitySettings.DEFAULTS).jackpot(),1.0e-12);
    }

    @Test
    void reelStripsContainEverySymbolExactlyOnce() {
        for(int reel=0;reel<ReelLayout.REEL_COUNT;reel++) {
            assertEquals(ReelLayout.SYMBOL_COUNT,ReelLayout.strip(reel).size());
            assertEquals(ReelLayout.SYMBOL_COUNT,new HashSet<>(ReelLayout.strip(reel)).size());
            for(ReelSymbol symbol:ReelSymbol.values()) assertEquals(symbol,ReelLayout.symbolAt(reel,ReelLayout.positionOf(reel,symbol)));
        }
    }

    @Test
    void generatedOutcomesAlwaysMatchTheirCategory() {
        ProbabilityProfile profile=ProbabilityCalculator.calculate(24,ProbabilitySettings.DEFAULTS);
        SplittableRandom random=new SplittableRandom(0x777L);
        for(int i=0;i<100_000;i++) {
            SpinOutcome outcome=SpinEngine.roll(profile,random);
            assertEquals(outcome.category(),SpinEngine.classify(outcome.symbols()));
        }
    }

    @Test
    void sampledDistributionTracksTheConfiguredProfile() {
        ProbabilityProfile profile=ProbabilityCalculator.calculate(16,ProbabilitySettings.DEFAULTS);
        EnumMap<WinCategory,Integer> counts=new EnumMap<>(WinCategory.class);
        SplittableRandom random=new SplittableRandom(0x1a2b3c4dL);
        int samples=500_000;
        for(int i=0;i<samples;i++) counts.merge(SpinEngine.roll(profile,random).category(),1,Integer::sum);
        for(WinCategory category:WinCategory.values()) {
            double actual=counts.getOrDefault(category,0)/(double)samples;
            double expected=profile.probability(category);
            double sigma=Math.sqrt(Math.max(1.0e-12,expected*(1.0-expected)/samples));
            assertEquals(expected,actual,Math.max(0.0005,6.0*sigma),category.name());
        }
    }
}
