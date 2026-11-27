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
    private boolean queenRequired;
    private int queenMaxCycles;
    private int queenRoyalJellyCycles;
    private int queenMaxCyclesCap;
    private boolean guardEnabled;
    private int guardBees;
    private double guardRadius;
    private int guardCooldownSeconds;
    private int guardLifetimeSeconds;
    private boolean guardAttackCreepers;
    private int guardIntervalSeconds;
    private int raidIntervalSeconds;
    private double raidChance;
    private double raidBearChance;
    private int raidCooldownMinutes;
    private int raidLifetimeSeconds;
    private int raidMaxActive;
    private double raidDistance;
    private int raidFoxSteal;
    private int raidBearSteal;
    private double apiaryRadius;
    private int apiaryMinHives;
    private int apiaryMinFlowers;
    private int apiaryMinFences;
    private double apiarySpeedBonus;
    private double apiaryQualityBonus;
    private boolean dropContentsOnBreak;
    private int flowerCacheSeconds;
    private int flowerScansPerTick;
    private int saveBatchSize;
    private boolean breedingEnabled;
    private int breedingMinutes;
    private int breedingRoyalCellCost;
    private int pollinationRadius;
    private double pollinationChancePerBee;
    private double pollinationMaxChance;
    private double pollenBonus;
    private boolean stingEnabled;
    private double stingBaseChance;
    private double stingDamage;
    private boolean upgradeEnabled;
    private boolean bossEnabled;
    private int ritualCountdownSeconds;
    private int ritualCooldownMinutes;
    private int ritualCandles;
    private int ritualRoyalJelly;
    private int ritualRoyalCells;
    private double bossHealth;
    private int bossMinions;
    private int bossMinionIntervalSeconds;
    private int bossMinionCap;
    private double bossArenaRadius;
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

        resourcePackMode = normalizePackMode(config.get("resource-pack.mode"));
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
        queenRequired = config.getBoolean("queen.required", true);
        queenMaxCycles = Math.max(1, config.getInt("queen.max-cycles", 40));
        queenRoyalJellyCycles = Math.max(1, config.getInt("queen.royal-jelly-cycles", 20));
        queenMaxCyclesCap = Math.max(queenMaxCycles, config.getInt("queen.max-cycles-cap", 80));
        guardEnabled = config.getBoolean("features.guard", true);
        guardBees = Math.max(1, Math.min(6, config.getInt("guard.bees", 2)));
        guardRadius = Math.max(3.0, Math.min(32.0, config.getDouble("guard.radius", 8.0)));
        guardCooldownSeconds = Math.max(5, config.getInt("guard.cooldown-seconds", 30));
        guardLifetimeSeconds = Math.max(10, config.getInt("guard.lifetime-seconds", 60));
        guardAttackCreepers = config.getBoolean("guard.attack-creepers", false);
        guardIntervalSeconds = Math.max(1, config.getInt("guard.interval-seconds", 2));
        raidIntervalSeconds = Math.max(15, config.getInt("raid.interval-seconds", 60));
        raidChance = clamp(config.getDouble("raid.chance", 0.25), 0.0, 1.0);
        raidBearChance = clamp(config.getDouble("raid.bear-chance", 0.20), 0.0, 1.0);
        raidCooldownMinutes = Math.max(1, config.getInt("raid.cooldown-minutes", 15));
        raidLifetimeSeconds = Math.max(10, config.getInt("raid.lifetime-seconds", 60));
        raidMaxActive = Math.max(1, Math.min(10, config.getInt("raid.max-active", 3)));
        raidDistance = Math.max(3.0, Math.min(24.0, config.getDouble("raid.distance", 8.0)));
        raidFoxSteal = Math.max(1, config.getInt("raid.fox-steal", 1));
        raidBearSteal = Math.max(1, config.getInt("raid.bear-steal", 3));
        apiaryRadius = clamp(config.getDouble("apiary.radius", 10.0), 3.0, 32.0);
        apiaryMinHives = Math.max(2, config.getInt("apiary.min-hives", 3));
        apiaryMinFlowers = Math.max(1, config.getInt("apiary.min-flowers", 3));
        apiaryMinFences = Math.max(4, config.getInt("apiary.min-fences", 8));
        apiarySpeedBonus = Math.max(1.0, config.getDouble("apiary.speed-bonus", 1.15));
        apiaryQualityBonus = Math.max(0.0, config.getDouble("apiary.quality-bonus", 0.05));

        dropContentsOnBreak = config.getBoolean("hive.drop-contents-on-break", true);
        flowerCacheSeconds = Math.max(30, config.getInt("production.flower-cache-seconds", 300));
        flowerScansPerTick = Math.max(1, Math.min(256, config.getInt("production.flower-scans-per-tick", 8)));
        saveBatchSize = Math.max(1, Math.min(512, config.getInt("production.save-batch-size", 20)));

        breedingEnabled = config.getBoolean("breeding.enabled", true);
        breedingMinutes = Math.max(1, config.getInt("breeding.minutes", 30));
        breedingRoyalCellCost = Math.max(0, Math.min(16,
                config.getInt("breeding.royal-cell-cost", config.getInt("breeding.royal-jelly-cost", 1))));

        pollinationRadius = Math.max(1, Math.min(16, config.getInt("pollination.radius", 5)));
        pollinationChancePerBee = clamp(config.getDouble("pollination.chance-per-bee", 0.05), 0.0, 1.0);
        pollinationMaxChance = clamp(config.getDouble("pollination.max-chance", 0.15), 0.0, 1.0);

        pollenBonus = clamp(config.getDouble("pollen.bonus", 0.10), 0.0, 1.0);
        stingEnabled = config.getBoolean("sting.enabled", true);
        stingBaseChance = clamp(config.getDouble("sting.base-chance", 0.15), 0.0, 1.0);
        stingDamage = Math.max(0.0, Math.min(20.0, config.getDouble("sting.damage", 2.0)));
        upgradeEnabled = config.getBoolean("upgrade.enabled", true);

        bossEnabled = config.getBoolean("boss.enabled", true);
        ritualCountdownSeconds = Math.max(3, Math.min(120, config.getInt("boss.ritual.countdown-seconds", 15)));
        ritualCooldownMinutes = Math.max(1, config.getInt("boss.ritual.cooldown-minutes", 60));
        ritualCandles = Math.max(0, Math.min(16, config.getInt("boss.ritual.candles", 4)));
        ritualRoyalJelly = Math.max(0, Math.min(64, config.getInt("boss.ritual.royal-jelly", 4)));
        ritualRoyalCells = Math.max(0, Math.min(64, config.getInt("boss.ritual.royal-cells", 2)));
        bossHealth = Math.max(20.0, Math.min(2000.0, config.getDouble("boss.health", 300.0)));
        bossMinions = Math.max(0, Math.min(16, config.getInt("boss.minions", 2)));
        bossMinionIntervalSeconds = Math.max(3, config.getInt("boss.minion-interval-seconds", 10));
        bossMinionCap = Math.max(1, Math.min(32, config.getInt("boss.minion-cap", 8)));
        bossArenaRadius = Math.max(8.0, Math.min(64.0, config.getDouble("boss.arena-radius", 32.0)));

        Set<String> blacklist = new LinkedHashSet<>();
        for (String world : config.getStringList("worlds.blacklist")) {
            blacklist.add(world.toLowerCase(Locale.ROOT));
        }
        worldBlacklist = Collections.unmodifiableSet(blacklist);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalizePackMode(Object raw) {
        if (raw == null) {
            return "OFF";
        }
        String value = String.valueOf(raw).toUpperCase(Locale.ROOT);
        if ("FALSE".equals(value) || "NO".equals(value) || "DISABLED".equals(value)) {
            return "OFF";
        }
        if ("TRUE".equals(value) || "YES".equals(value)) {
            return "EMBEDDED";
        }
        return value;
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

    public boolean queenRequired() {
        return queenRequired;
    }

    public int queenMaxCycles() {
        return queenMaxCycles;
    }

    public int queenRoyalJellyCycles() {
        return queenRoyalJellyCycles;
    }

    public int queenMaxCyclesCap() {
        return queenMaxCyclesCap;
    }

    public boolean guardEnabled() {
        return guardEnabled;
    }

    public int guardBees() {
        return guardBees;
    }

    public double guardRadius() {
        return guardRadius;
    }

    public int guardCooldownSeconds() {
        return guardCooldownSeconds;
    }

    public int guardLifetimeSeconds() {
        return guardLifetimeSeconds;
    }

    public boolean guardAttackCreepers() {
        return guardAttackCreepers;
    }

    public int guardIntervalSeconds() {
        return guardIntervalSeconds;
    }

    public int raidIntervalSeconds() {
        return raidIntervalSeconds;
    }

    public double raidChance() {
        return raidChance;
    }

    public double raidBearChance() {
        return raidBearChance;
    }

    public int raidCooldownMinutes() {
        return raidCooldownMinutes;
    }

    public int raidLifetimeSeconds() {
        return raidLifetimeSeconds;
    }

    public int raidMaxActive() {
        return raidMaxActive;
    }

    public double raidDistance() {
        return raidDistance;
    }

    public int raidFoxSteal() {
        return raidFoxSteal;
    }

    public int raidBearSteal() {
        return raidBearSteal;
    }

    public double apiaryRadius() {
        return apiaryRadius;
    }

    public int apiaryMinHives() {
        return apiaryMinHives;
    }

    public int apiaryMinFlowers() {
        return apiaryMinFlowers;
    }

    public int apiaryMinFences() {
        return apiaryMinFences;
    }

    public double apiarySpeedBonus() {
        return apiarySpeedBonus;
    }

    public double apiaryQualityBonus() {
        return apiaryQualityBonus;
    }

    public boolean dropContentsOnBreak() {
        return dropContentsOnBreak;
    }

    public int flowerCacheSeconds() {
        return flowerCacheSeconds;
    }

    public int flowerScansPerTick() {
        return flowerScansPerTick;
    }

    public int saveBatchSize() {
        return saveBatchSize;
    }

    public boolean breedingEnabled() {
        return breedingEnabled;
    }

    public int breedingMinutes() {
        return breedingMinutes;
    }

    public int breedingRoyalCellCost() {
        return breedingRoyalCellCost;
    }

    public int pollinationRadius() {
        return pollinationRadius;
    }

    public double pollinationChancePerBee() {
        return pollinationChancePerBee;
    }

    public double pollinationMaxChance() {
        return pollinationMaxChance;
    }

    public double pollenBonus() {
        return pollenBonus;
    }

    public boolean stingEnabled() {
        return stingEnabled;
    }

    public double stingBaseChance() {
        return stingBaseChance;
    }

    public double stingDamage() {
        return stingDamage;
    }

    public boolean upgradeEnabled() {
        return upgradeEnabled;
    }

    public boolean bossEnabled() {
        return bossEnabled;
    }

    public int ritualCountdownSeconds() {
        return ritualCountdownSeconds;
    }

    public int ritualCooldownMinutes() {
        return ritualCooldownMinutes;
    }

    public int ritualCandles() {
        return ritualCandles;
    }

    public int ritualRoyalJelly() {
        return ritualRoyalJelly;
    }

    public int ritualRoyalCells() {
        return ritualRoyalCells;
    }

    public double bossHealth() {
        return bossHealth;
    }

    public int bossMinions() {
        return bossMinions;
    }

    public int bossMinionIntervalSeconds() {
        return bossMinionIntervalSeconds;
    }

    public int bossMinionCap() {
        return bossMinionCap;
    }

    public double bossArenaRadius() {
        return bossArenaRadius;
    }

    public boolean isWorldBlacklisted(String worldName) {
        return worldBlacklist.contains(worldName.toLowerCase(Locale.ROOT));
    }
}
