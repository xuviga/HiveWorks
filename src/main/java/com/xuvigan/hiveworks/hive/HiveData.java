package com.xuvigan.hiveworks.hive;

import com.xuvigan.hiveworks.bee.Genes;

import java.util.UUID;

/**
 * In-memory hive state. Persisted by Storage.
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
    private int cachedFlowerSpecies;
    private boolean cachedFlowerRare;
    private long flowerScannedAt;
    private final BeeSlot[] bees = new BeeSlot[MAX_BEES];
    private final FrameSlot[] frames = new FrameSlot[MAX_FRAMES];

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

    public HiveTier tier() {
        return tier;
    }

    public void setTier(HiveTier tier) {
        this.tier = tier == null ? HiveTier.I : tier;
    }

    public HiveMode mode() {
        return mode;
    }

    public void setMode(HiveMode mode) {
        this.mode = mode == null ? HiveMode.APIARY : mode;
    }

    public double honey() {
        return honey;
    }

    public void setHoney(double honey) {
        this.honey = Math.max(0.0, honey);
    }

    public long lastTick() {
        return lastTick;
    }

    public void setLastTick(long lastTick) {
        this.lastTick = lastTick;
    }

    public int queenSlot() {
        return queenSlot;
    }

    public void setQueenSlot(int queenSlot) {
        this.queenSlot = Math.max(-1, Math.min(MAX_BEES - 1, queenSlot));
    }

    public int queenCycles() {
        return queenCycles;
    }

    public void setQueenCycles(int queenCycles) {
        this.queenCycles = Math.max(0, queenCycles);
    }

    public BeeSlot queen() {
        return queenSlot >= 0 ? bee(queenSlot) : null;
    }

    public boolean hasQueen() {
        return queen() != null;
    }

    public boolean queenBound() {
        return queenBound;
    }

    public void setQueenBound(boolean queenBound) {
        this.queenBound = queenBound;
    }

    public int cachedFlowerSpecies() {
        return cachedFlowerSpecies;
    }

    public boolean cachedFlowerRare() {
        return cachedFlowerRare;
    }

    public long flowerScannedAt() {
        return flowerScannedAt;
    }

    public void setFlowerCache(int species, boolean rare, long scannedAt) {
        this.cachedFlowerSpecies = species;
        this.cachedFlowerRare = rare;
        this.flowerScannedAt = scannedAt;
    }

    public BeeSlot bee(int slot) {
        return slot >= 0 && slot < MAX_BEES ? bees[slot] : null;
    }

    public void setBee(int slot, BeeSlot bee) {
        if (slot >= 0 && slot < MAX_BEES) {
            bees[slot] = bee;
        }
    }

    public FrameSlot frame(int slot) {
        return slot >= 0 && slot < MAX_FRAMES ? frames[slot] : null;
    }

    public void setFrame(int slot, FrameSlot frame) {
        if (slot >= 0 && slot < MAX_FRAMES) {
            frames[slot] = frame;
        }
    }

    public BeeSlot[] bees() {
        return bees.clone();
    }

    public FrameSlot[] frames() {
        return frames.clone();
    }

    public int honeyMax() {
        int max = 5;
        for (FrameSlot frame : frames) {
            if (frame != null) {
                max += frame.type().maxHoneyBonus();
            }
        }
        return Math.min(8, max);
    }

    public int occupiedBees() {
        int count = 0;
        for (BeeSlot bee : bees) {
            if (bee != null) {
                count++;
            }
        }
        return count;
    }

    public int framesUsed() {
        int count = 0;
        for (FrameSlot frame : frames) {
            if (frame != null) {
                count++;
            }
        }
        return count;
    }

    public int averagePurity() {
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

    public int averageCold() {
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

    public int frameCount(FrameType type) {
        int count = 0;
        for (FrameSlot frame : frames) {
            if (frame != null && frame.type() == type) {
                count++;
            }
        }
        return count;
    }

    public double frameQualityWeight() {
        double total = 0.0;
        for (FrameSlot frame : frames) {
            if (frame != null) {
                total += frame.type().qualityWeight();
            }
        }
        return Math.min(1.0, total);
    }

    public double frameQualityBonus() {
        double total = 0.0;
        for (FrameSlot frame : frames) {
            if (frame != null) {
                total += frame.type().qualityBonus();
            }
        }
        return total;
    }

    public boolean addBee(BeeSlot bee) {
        for (int slot = 0; slot < tier.beeSlots() && slot < MAX_BEES; slot++) {
            if (bees[slot] == null) {
                bees[slot] = bee;
                return true;
            }
        }
        return false;
    }

    public void clearBees() {
        for (int index = 0; index < MAX_BEES; index++) {
            bees[index] = null;
        }
    }

    public void clearFrames() {
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
