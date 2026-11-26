package com.xuvigan.hiveworks.util;

import org.bukkit.Bukkit;

/**
 * Version helpers. Compiled against 1.21.1, runs on 1.21.x - 26.x.
 */
public final class Version {

    private Version() {
    }

    public static String minecraftVersion() {
        String raw = Bukkit.getBukkitVersion();
        int dash = raw.indexOf('-');
        return dash > 0 ? raw.substring(0, dash) : raw;
    }

    public static boolean isAtLeast(String current, String required) {
        if (required == null || required.isBlank()) {
            return true;
        }
        int[] left = parse(current);
        int[] right = parse(required);
        for (int index = 0; index < 3; index++) {
            if (left[index] != right[index]) {
                return left[index] > right[index];
            }
        }
        return true;
    }

    private static int[] parse(String version) {
        int[] parts = new int[3];
        String[] split = version.split("\\.");
        for (int index = 0; index < 3 && index < split.length; index++) {
            String digits = split[index].replaceAll("[^0-9].*$", "");
            if (!digits.isEmpty()) {
                try {
                    parts[index] = Integer.parseInt(digits);
                } catch (NumberFormatException ignored) {
                    parts[index] = 0;
                }
            }
        }
        return parts;
    }
}
