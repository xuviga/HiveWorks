package com.xuvigan.hiveworks.hive;

import com.xuvigan.hiveworks.bee.Genes;

import java.util.UUID;

/**
 * In-memory hive state. Persisted by Storage.
 *
 * <p>On Folia a single hive is touched from several region threads (its own region,
 * the GUI/command region of a player and the global tick). All accessors are
 * therefore synchronized so a hive never exposes a torn state mid-update.</p>
 */
public final class HiveData {

    public static final int MAX_BEES = 6;
    public static final int MAX_FRAMES = 4;

    private final HivePos pos;
    private final UUID owner;
    private HiveTier tier = HiveTier.I;
    private HiveMode mode = HiveMode.APIARY;
    private double honey;
    private long lastTick;
    private int queenSlot = -1;
    private int queenCycles;
    private boolean queenBound;
    private double pollenBonus;
    private int breedSlotA = -1;
    private int breedSlotB = -1;
    private long breedReadyAt;
    private int cachedFlowerSpecies;
    private boolean cachedFlowerRare;
    private long flowerScannedAt;
    private final BeeSlot[] bees = new BeeSlot[MAX_BEES];
    private final FrameSlot[] frames = new FrameSlot[MAX_FRAMES];

    // Region-snapshot of the surrounding world, refreshed by HiveTicker so the GUI
    // and commands never have to touch blocks from a foreign region.
    private boolean cachedStorm;
    private boolean cachedNight;
    private boolean cachedSnowy;
    private long environmentAt;
    private int cachedCandles;

    public HiveData(HivePos pos, UUID owner) {
        this.pos = pos;
        this.owner = owner;
        this.lastTick = System.currentTimeMillis();
    }

    public HivePos pos() {
        return pos;
    }

    public UUID owner() {
        return owner;
    }

    public synchronized HiveTier tier() {
        return tier;
    }

    public synchronized void setTier(HiveTier tier) {
        this.tier = tier == null ? HiveTier.I : tier;
    }

    public synchronized HiveMode mode() {
        return mode;
    }

    public synchronized void setMode(HiveMode mode) {
        this.mode = mode == null ? HiveMode.APIARY : mode;
    }

    public synchronized double honey() {
        return honey;
    }

    public synchronized void setHoney(double honey) {
        this.honey = Math.max(0.0, honey);
    }

    public synchronized long lastTick() {
        return lastTick;
    }

    public synchronized void setLastTick(long lastTick) {
        this.lastTick = lastTick;
    }

    public synchronized int queenSlot() {
        return queenSlot;
    }

    public synchronized void setQueenSlot(int queenSlot) {
        this.queenSlot = Math.max(-1, Math.min(MAX_BEES - 1, queenSlot));
    }

    public synchronized int queenCycles() {
        return queenCycles;
    }

    public synchronized void setQueenCycles(int queenCycles) {
        this.queenCycles = Math.max(0, queenCycles);
    }

    public synchronized BeeSlot queen() {
        return queenSlot >= 0 ? bee(queenSlot) : null;
    }

    public synchronized boolean hasQueen() {
        return queen() != null;
    }

    public synchronized boolean queenBound() {
        return queenBound;
    }

    public synchronized void setQueenBound(boolean queenBound) {
        this.queenBound = queenBound;
    }

    /** One-shot quality bonus from pollen, consumed by the next harvest. */
    public synchronized double pollenBonus() {
        return pollenBonus;
    }

    public synchronized void setPollenBonus(double pollenBonus) {
        this.pollenBonus = Math.max(0.0, Math.min(1.0, pollenBonus));
    }

    public synchronized int breedSlotA() {
        return breedSlotA;
    }

    public synchronized int breedSlotB() {
        return breedSlotB;
    }

    public synchronized long breedReadyAt() {
        return breedReadyAt;
    }

    public synchronized void startBreeding(int firstSlot, int secondSlot, long readyAt) {
        this.breedSlotA = firstSlot;
        this.breedSlotB = secondSlot;
        this.breedReadyAt = Math.max(0L, readyAt);
    }

    public synchronized void clearBreeding() {
        this.breedSlotA = -1;
        this.breedSlotB = -1;
        this.breedReadyAt = 0L;
    }

    public synchronized boolean breedingActive(long now) {
        return breedReadyAt > 0L && breedSlotA >= 0 && breedSlotB >= 0;
    }

    public synchronized boolean breedingReady(long now) {
        return breedingActive(now) && now >= breedReadyAt;
    }

    public synchronized int firstFreeBeeSlot() {
        for (int slot = 0; slot < tier.beeSlots() && slot < MAX_BEES; slot++) {
            if (bees[slot] == null) {
                return slot;
            }
        }
        return -1;
    }

    public synchronized int cachedFlowerSpecies() {
        return cachedFlowerSpecies;
    }

    public synchronized boolean cachedFlowerRare() {
        return cachedFlowerRare;
    }

    public synchronized long flowerScannedAt() {
        return flowerScannedAt;
    }

    public synchronized void setFlowerCache(int species, boolean rare, long scannedAt) {
        this.cachedFlowerSpecies = species;
        this.cachedFlowerRare = rare;
        this.flowerScannedAt = scannedAt;
    }

    // ------------------------------------------------------------ environment

    public synchronized void setEnvironment(boolean storm, boolean night, boolean snowy, long at) {
        this.cachedStorm = storm;
        this.cachedNight = night;
        this.cachedSnowy = snowy;
        this.environmentAt = at;
    }

    public synchronized boolean cachedStorm() {
        return cachedStorm;
    }

    public synchronized boolean cachedNight() {
        return cachedNight;
    }

    public synchronized boolean cachedSnowy() {
        return cachedSnowy;
    }

    public synchronized long environmentAt() {
        return environmentAt;
    }

    public synchronized int cachedCandles() {
        return cachedCandles;
    }

    public synchronized void setCachedCandles(int candles) {
        this.cachedCandles = Math.max(0, candles);
    }

    // ------------------------------------------------------------------ slots

    public synchronized BeeSlot bee(int slot) {
        return slot >= 0 && slot < MAX_BEES ? bees[slot] : null;
    }

    public synchronized void setBee(int slot, BeeSlot bee) {
        if (slot >= 0 && slot < MAX_BEES) {
            bees[slot] = bee;
        }
    }

    public synchronized FrameSlot frame(int slot) {
        return slot >= 0 && slot < MAX_FRAMES ? frames[slot] : null;
    }

    public synchronized void setFrame(int slot, FrameSlot frame) {
        if (slot >= 0 && slot < MAX_FRAMES) {
            frames[slot] = frame;
        }
    }

    public synchronized BeeSlot[] bees() {
        return bees.clone();
    }

    public synchronized FrameSlot[] frames() {
        return frames.clone();
    }

    public synchronized int honeyMax() {
        int max = 5;
        for (FrameSlot frame : frames) {
            if (frame != null) {
                max += frame.type().maxHoneyBonus();
            }
        }
        return Math.min(8, max);
    }

    public synchronized int occupiedBees() {
        int count = 0;
        for (BeeSlot bee : bees) {
            if (bee != null) {
                count++;
            }
        }
        return count;
    }

    public synchronized int framesUsed() {
        int count = 0;
        for (FrameSlot frame : frames) {
            if (frame != null) {
                count++;
            }
        }
        return count;
    }

    public synchronized int averagePurity() {
        int total = 0;
        int count = 0;
        for (BeeSlot bee : bees) {
            if (bee != null) {
                total += bee.purity();
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }

    public synchronized int averageCold() {
        int total = 0;
        int count = 0;
        for (BeeSlot bee : bees) {
            if (bee != null) {
                total += bee.genes().cold();
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }

    public synchronized int averageTemper() {
        int total = 0;
        int count = 0;
        for (BeeSlot bee : bees) {
            if (bee != null) {
                total += bee.genes().temper();
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }

    public synchronized int frameCount(FrameType type) {
        int count = 0;
        for (FrameSlot frame : frames) {
            if (frame != null && frame.type() == type) {
                count++;
            }
        }
        return count;
    }

    public synchronized double frameQualityWeight() {
        double total = 0.0;
        for (FrameSlot frame : frames) {
            if (frame != null) {
                total += frame.type().qualityWeight();
            }
        }
        return Math.min(1.0, total);
    }

    public synchronized double frameQualityBonus() {
        double total = 0.0;
        for (FrameSlot frame : frames) {
            if (frame != null) {
                total += frame.type().qualityBonus();
            }
        }
        return total;
    }

    public synchronized boolean addBee(BeeSlot bee) {
        for (int slot = 0; slot < tier.beeSlots() && slot < MAX_BEES; slot++) {
            if (bees[slot] == null) {
                bees[slot] = bee;
                return true;
            }
        }
        return false;
    }

    public synchronized void clearBees() {
        for (int index = 0; index < MAX_BEES; index++) {
            bees[index] = null;
        }
    }

    public synchronized void clearFrames() {
        for (int index = 0; index < MAX_FRAMES; index++) {
            frames[index] = null;
        }
    }

    /**
     * Convenience factory for tests and admin tools.
     */
    public static BeeSlot beeSlot(UUID id, String speciesId, Genes genes, int purity) {
        return new BeeSlot(id, speciesId, genes, purity);
    }
}
