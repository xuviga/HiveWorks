package com.xuvigan.hiveworks.bee;

import java.util.Random;

/**
 * Bee genes, each 0..100.
 */
public record Genes(int productivity, int speed, int cold, int temper) {

    public static final Genes DEFAULT = new Genes(50, 50, 50, 50);

    public Genes {
        productivity = clamp(productivity);
        speed = clamp(speed);
        cold = clamp(cold);
        temper = clamp(temper);
    }

    public static Genes parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String[] parts = raw.split(",");
        if (parts.length != 4) {
            return DEFAULT;
        }
        try {
            return new Genes(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()),
                    Integer.parseInt(parts[3].trim())
            );
        } catch (NumberFormatException exception) {
            return DEFAULT;
        }
    }

    public String serialize() {
        return productivity + "," + speed + "," + cold + "," + temper;
    }

    public int average() {
        return (productivity + speed + cold + temper) / 4;
    }

    /**
     * 0.1 breeding: 50/50 inheritance, +/-5 drift, 10% mutation.
     */
    public Genes child(Genes other, Random random) {
        int[] left = {productivity, speed, cold, temper};
        int[] right = {other.productivity, other.speed, other.cold, other.temper};
        int[] result = new int[4];
        for (int index = 0; index < 4; index++) {
            int base = random.nextBoolean() ? left[index] : right[index];
            int drift = random.nextInt(11) - 5;
            if (random.nextInt(100) < 10) {
                drift += 10;
            }
            result[index] = clamp(base + drift);
        }
        return new Genes(result[0], result[1], result[2], result[3]);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
