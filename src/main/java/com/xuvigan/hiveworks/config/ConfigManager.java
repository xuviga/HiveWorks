package com.xuvigan.hiveworks.config;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Typed view over config.yml. Reloaded on /hive admin reload.
 */
public final class ConfigManager {

    private final HiveWorksPlugin plugin;

    private StorageType storageType;
    private String sqliteFile;
    private double baseMinutesPerLevel;
    private int offlineCapHours;
    private double nightRainMultiplier;
    private int tickIntervalSeconds;
    private int saveIntervalSeconds;
    private double weightFlowers;
    private double weightFrames;
    private double weightPurity;
    private double goodThreshold;
    private double eliteThreshold;
    private double legendaryThreshold;
    private int hivesPerChunk;
    private int hivesPerPlayer;
    private String resourcePackMode;
    private String resourcePackUrl;
    private int resourcePackPort;
    private String resourcePackPublicUrl;
    private String resourcePackPrompt;
    private boolean resourcePackRequired;
    private boolean customHiveModel;
    private boolean pollination;
    private boolean meadBuffs;
    private boolean foxRaider;
    private boolean wildSwarm;
    private int wildSwarmCheckSeconds;
    private int wildSwarmCooldownMinutes;
    private int wildSwarmLifetimeMinutes;
    private int wildSwarmMaxBees;
    private Set<String> worldBlacklist = Collections.emptySet();

    public ConfigManager(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        storageType = StorageType.parse(config.getString("storage.type", "SQLITE"));
        sqliteFile = config.getString("storage.sqlite-file", "data/hiveworks.db");

        baseMinutesPerLevel = Math.max(0.1, config.getDouble("production.base-minutes-per-level", 4.0));
        offlineCapHours = Math.max(1, config.getInt("production.offline-cap-hours", 8));
        nightRainMultiplier = clamp(config.getDouble("production.night-rain-multiplier", 0.5), 0.0, 1.0);
        tickIntervalSeconds = Math.max(5, config.getInt("production.tick-interval-seconds", 20));
        saveIntervalSeconds = Math.max(30, config.getInt("production.save-interval-seconds", 300));

        weightFlowers = Math.max(0.0, config.getDouble("quality.weights.flowers", 0.60));
        weightFrames = Math.max(0.0, config.getDouble("quality.weights.frames", 0.25));
        weightPurity = Math.max(0.0, config.getDouble("quality.weights.purity", 0.15));
        goodThreshold = config.getDouble("quality.good-threshold", 0.35);
        eliteThreshold = config.getDouble("quality.elite-threshold", 0.60);
        legendaryThreshold = config.getDouble("quality.legendary-threshold", 0.85);

        hivesPerChunk = Math.max(1, config.getInt("limits.hives-per-chunk", 4));
        hivesPerPlayer = Math.max(1, config.getInt("limits.hives-per-player", 32));

        resourcePackMode = config.getString("resource-pack.mode", "OFF").toUpperCase(Locale.ROOT);
        resourcePackUrl = config.getString("resource-pack.url", "");
        resourcePackPort = Math.max(1024, Math.min(65535, config.getInt("resource-pack.embedded-port", 8123)));
        resourcePackPublicUrl = config.getString("resource-pack.public-url", "");
        resourcePackPrompt = config.getString("resource-pack.prompt", "HiveWorks");
        resourcePackRequired = config.getBoolean("resource-pack.require", false);
        customHiveModel = config.getBoolean("visuals.custom-hive-model", false);

        pollination = config.getBoolean("features.pollination", true);
        meadBuffs = config.getBoolean("features.mead-buffs", true);
        foxRaider = config.getBoolean("features.fox-raider", true);
        wildSwarm = config.getBoolean("features.wild-swarm", true);
        wildSwarmCheckSeconds = Math.max(15, config.getInt("wild-swarm.check-seconds", 60));
        wildSwarmCooldownMinutes = Math.max(1, config.getInt("wild-swarm.cooldown-minutes", 10));
        wildSwarmLifetimeMinutes = Math.max(1, config.getInt("wild-swarm.lifetime-minutes", 10));
        wildSwarmMaxBees = Math.max(2, Math.min(10, config.getInt("wild-swarm.max-bees", 6)));

        Set<String> blacklist = new LinkedHashSet<>();
        for (String world : config.getStringList("worlds.blacklist")) {
            blacklist.add(world.toLowerCase(Locale.ROOT));
        }
        worldBlacklist = Collections.unmodifiableSet(blacklist);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public StorageType storageType() {
        return storageType;
    }

    public String sqliteFile() {
        return sqliteFile;
    }

    public double baseMinutesPerLevel() {
        return baseMinutesPerLevel;
    }

    public int offlineCapHours() {
        return offlineCapHours;
    }

    public double nightRainMultiplier() {
        return nightRainMultiplier;
    }

    public int tickIntervalSeconds() {
        return tickIntervalSeconds;
    }

    public int saveIntervalSeconds() {
        return saveIntervalSeconds;
    }

    public double weightFlowers() {
        return weightFlowers;
    }

    public double weightFrames() {
        return weightFrames;
    }

    public double weightPurity() {
        return weightPurity;
    }

    public double goodThreshold() {
        return goodThreshold;
    }

    public double eliteThreshold() {
        return eliteThreshold;
    }

    public double legendaryThreshold() {
        return legendaryThreshold;
    }

    public int hivesPerChunk() {
        return hivesPerChunk;
    }

    public int hivesPerPlayer() {
        return hivesPerPlayer;
    }

    public String resourcePackMode() {
        return resourcePackMode;
    }

    public String resourcePackUrl() {
        return resourcePackUrl;
    }

    public int resourcePackPort() {
        return resourcePackPort;
    }

    public String resourcePackPublicUrl() {
        return resourcePackPublicUrl;
    }

    public String resourcePackPrompt() {
        return resourcePackPrompt;
    }

    public boolean resourcePackRequired() {
        return resourcePackRequired;
    }

    public boolean customHiveModel() {
        return customHiveModel;
    }

    public boolean pollination() {
        return pollination;
    }

    public boolean meadBuffs() {
        return meadBuffs;
    }

    public boolean foxRaider() {
        return foxRaider;
    }

    public boolean wildSwarm() {
        return wildSwarm;
    }

    public int wildSwarmCheckSeconds() {
        return wildSwarmCheckSeconds;
    }

    public int wildSwarmCooldownMinutes() {
        return wildSwarmCooldownMinutes;
    }

    public int wildSwarmLifetimeMinutes() {
        return wildSwarmLifetimeMinutes;
    }

    public int wildSwarmMaxBees() {
        return wildSwarmMaxBees;
    }

    public boolean isWorldBlacklisted(String worldName) {
        return worldBlacklist.contains(worldName.toLowerCase(Locale.ROOT));
    }
}
