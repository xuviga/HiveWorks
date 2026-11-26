package com.xuvigan.hiveworks.production;

import com.xuvigan.hiveworks.config.ConfigManager;
import com.xuvigan.hiveworks.hive.BeeSlot;
import com.xuvigan.hiveworks.hive.FrameType;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HiveMode;

/**
 * Production speed formula.
 */
public final class HoneyService {

    private final ConfigManager config;

    public HoneyService(ConfigManager config) {
        this.config = config;
    }

    public double speed(HiveData hive, FlowerScanner.Result flowers, boolean nightOrRain, boolean snowy) {
        int bees = hive.occupiedBees();
        if (bees == 0) {
            return 0.0;
        }
        if (config.queenRequired() && !hive.hasQueen()) {
            return 0.0;
        }

        double beesFactor = Math.min(1.5, bees / 3.0);
        if (hive.mode() == HiveMode.GUARD) {
            beesFactor *= 0.5;
        }
        BeeSlot queen = hive.queen();
        if (queen != null) {
            beesFactor *= 1.0 + (queen.genes().productivity() - 50) / 500.0;
        }
        double flowersFactor = flowersFactor(flowers.species());
        double frameBonus = 1.0 + hive.frameCount(FrameType.HONEY) * 0.25;
        double tierBonus = 1.0 + hive.tier().speedBonus();

        double penalty = 1.0;
        if (nightOrRain) {
            penalty *= config.nightRainMultiplier();
        }
        if (snowy) {
            // Cold-adapted bees ignore part of the snow penalty.
            double coldFactor = hive.averageCold() / 100.0;
            penalty *= 0.5 + 0.5 * coldFactor;
        }
        return Math.max(0.0, beesFactor * flowersFactor * frameBonus * tierBonus * penalty);
    }

    private double flowersFactor(int species) {
        if (species <= 0) {
            return 0.25;
        }
        if (species <= 2) {
            return 0.60;
        }
        if (species <= 4) {
            return 1.00;
        }
        return 1.15;
    }
}
