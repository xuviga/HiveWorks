package com.xuvigan.hiveworks.production;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QualityCalculatorTest {

    private final QualityCalculator calculator = QualityCalculator.defaults();

    @Test
    void emptySetupIsOrdinary() {
        double score = calculator.score(0, false, 0.0, 0, 0.0);
        assertEquals(0.0, score, 0.0001);
        assertEquals(Quality.ORDINARY, calculator.qualityOf(score));
    }

    @Test
    void perfectSetupIsLegendary() {
        double score = calculator.score(5, true, 1.0, 100, 0.08);
        assertEquals(1.0, score, 0.0001);
        assertEquals(Quality.LEGENDARY, calculator.qualityOf(score));
    }

    @Test
    void thresholdsAreInclusive() {
        assertEquals(Quality.GOOD, calculator.qualityOf(0.35));
        assertEquals(Quality.ELITE, calculator.qualityOf(0.60));
        assertEquals(Quality.LEGENDARY, calculator.qualityOf(0.85));
    }

    @Test
    void extraBonusCannotExceedOne() {
        assertEquals(1.0, calculator.score(5, true, 1.0, 100, 5.0), 0.0001);
    }

    @Test
    void rareFlowersAddHalfSpecies() {
        double withoutRare = calculator.score(2, false, 0.0, 0, 0.0);
        double withRare = calculator.score(2, true, 0.0, 0, 0.0);
        assertEquals(0.60 * 0.4, withoutRare, 0.0001);
        assertEquals(0.60 * 0.5, withRare, 0.0001);
    }
}
