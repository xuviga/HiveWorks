package com.xuvigan.hiveworks.production;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.config.ConfigManager;
import com.xuvigan.hiveworks.hive.BeeSlot;
import com.xuvigan.hiveworks.hive.FrameSlot;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HiveMode;

/**
 * Production speed formula.
 */
public final class HoneyService {

    private final HiveWorksPlugin plugin;
    private final ConfigManager config;

    public HoneyService(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.configManager();
    }

    public double speed(HiveData hive, FlowerScanner.Result flowers, boolean nightOrRain, boolean snowy) {
        int bees = hive.occupiedBees();
        if (bees == 0) {
            return 0.0;
        }
        if (config.queenRequired() && !hive.hasQueen()) {
            return 0.0;
        }

        double beesFactor = Math.min(1.5, 0.4 + 0.2 * bees);
        if (hive.mode() == HiveMode.GUARD) {
            beesFactor *= 0.5;
        }
        BeeSlot queen = hive.queen();
        if (queen != null) {
            beesFactor *= 1.0 + (queen.genes().productivity() - 50) / 500.0;
        }
        double flowersFactor = flowersFactor(flowers.species());
        double frameBonus = 1.0;
        for (int slot = 0; slot < HiveData.MAX_FRAMES; slot++) {
            FrameSlot frame = hive.frame(slot);
            if (frame != null) {
                frameBonus += frame.type().speedBonus();
            }
        }
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
        return Math.max(0.0, beesFactor * flowersFactor * frameBonus * tierBonus * penalty
                * plugin.apiaries().speedBonus(hive));
    }

    private double flowersFactor(int species) {
        if (species <= 0) {
            return 0.50;
        }
        if (species <= 2) {
            return 0.85;
        }
        if (species <= 4) {
            return 1.00;
        }
        return 1.15;
    }
}
