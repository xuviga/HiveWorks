package com.xuvigan.hiveworks.storage;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.bee.Genes;
import com.xuvigan.hiveworks.hive.BeeSlot;
import com.xuvigan.hiveworks.hive.FrameSlot;
import com.xuvigan.hiveworks.hive.FrameType;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HiveMode;
import com.xuvigan.hiveworks.hive.HivePos;
import com.xuvigan.hiveworks.hive.HiveTier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * YAML fallback. Intended for small servers and debugging.
 */
public final class YamlStorage implements Storage {

    private final HiveWorksPlugin plugin;
    private final File file;
    private YamlConfiguration data;

    public YamlStorage(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/hives.yml");
    }

    @Override
    public void init() throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create data directory: " + parent.getAbsolutePath());
        }
        this.data = YamlConfiguration.loadConfiguration(file);
        plugin.getLogger().info("YAML storage ready: " + file.getName());
    }

    @Override
    public List<HiveData> loadAll() {
        List<HiveData> hives = new ArrayList<>();
        ConfigurationSection root = data.getConfigurationSection("hives");
        if (root == null) {
            return hives;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            try {
                HiveData hive = new HiveData(
                        new HivePos(
                                section.getString("world", "world"),
                                section.getInt("x"),
                                section.getInt("y"),
                                section.getInt("z")
                        ),
                        UUID.fromString(section.getString("owner", UUID.randomUUID().toString()))
                );
                hive.setTier(tierOf(section.getString("tier")));
                hive.setMode(modeOf(section.getString("mode")));
                hive.setHoney(section.getDouble("honey"));
                hive.setLastTick(section.getLong("last_tick", System.currentTimeMillis()));
                hive.setQueenSlot(section.getInt("queen_slot", -1));
                hive.setQueenCycles(section.getInt("queen_cycles", 0));
                hive.setQueenBound(section.getBoolean("queen_bound", false));

                ConfigurationSection bees = section.getConfigurationSection("bees");
                if (bees != null) {
                    for (String slotKey : bees.getKeys(false)) {
                        ConfigurationSection bee = bees.getConfigurationSection(slotKey);
                        if (bee == null) {
                            continue;
                        }
                        hive.setBee(Integer.parseInt(slotKey), new BeeSlot(
                                UUID.fromString(bee.getString("bee_uuid", UUID.randomUUID().toString())),
                                bee.getString("species", "forest"),
                                new Genes(
                                        bee.getInt("gene_prod", 50),
                                        bee.getInt("gene_speed", 50),
                                        bee.getInt("gene_cold", 50),
                                        bee.getInt("gene_temper", 50)
                                ),
                                bee.getInt("purity", 50)
                        ));
                    }
                }

                ConfigurationSection frames = section.getConfigurationSection("frames");
                if (frames != null) {
                    for (String slotKey : frames.getKeys(false)) {
                        ConfigurationSection frame = frames.getConfigurationSection(slotKey);
                        if (frame == null) {
                            continue;
                        }
                        hive.setFrame(Integer.parseInt(slotKey), new FrameSlot(
                                frameOf(frame.getString("type")),
                                frame.getInt("uses_left", 1)
                        ));
                    }
                }
                hives.add(hive);
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Skipping malformed hive '" + key + "': " + exception.getMessage());
            }
        }
        return hives;
    }

    @Override
    public Set<String> discoveredSpecies(UUID uuid) {
        return new LinkedHashSet<>(data.getStringList("players." + uuid + ".discovered"));
    }

    @Override
    public void discoverSpecies(UUID uuid, String playerName, String speciesId) {
        String path = "players." + uuid;
        List<String> discovered = new ArrayList<>(data.getStringList(path + ".discovered"));
        if (!discovered.contains(speciesId)) {
            discovered.add(speciesId);
        }
        data.set(path + ".name", playerName);
        data.set(path + ".discovered", discovered);
        flush();
    }

    @Override
    public void save(HiveData hive) {
        String path = "hives." + safeKey(hive.pos().key());
        data.set(path + ".world", hive.pos().world());
        data.set(path + ".x", hive.pos().x());
        data.set(path + ".y", hive.pos().y());
        data.set(path + ".z", hive.pos().z());
        data.set(path + ".owner", hive.owner().toString());
        data.set(path + ".tier", hive.tier().name());
        data.set(path + ".mode", hive.mode().name());
        data.set(path + ".honey", hive.honey());
        data.set(path + ".last_tick", hive.lastTick());
        data.set(path + ".queen_slot", hive.queenSlot());
        data.set(path + ".queen_cycles", hive.queenCycles());
        data.set(path + ".queen_bound", hive.queenBound());
        data.set(path + ".bees", null);
        data.set(path + ".frames", null);

        for (int slot = 0; slot < HiveData.MAX_BEES; slot++) {
            BeeSlot bee = hive.bee(slot);
            if (bee == null) {
                continue;
            }
            String beePath = path + ".bees." + slot;
            data.set(beePath + ".bee_uuid", bee.beeId().toString());
            data.set(beePath + ".species", bee.speciesId());
            data.set(beePath + ".gene_prod", bee.genes().productivity());
            data.set(beePath + ".gene_speed", bee.genes().speed());
            data.set(beePath + ".gene_cold", bee.genes().cold());
            data.set(beePath + ".gene_temper", bee.genes().temper());
            data.set(beePath + ".purity", bee.purity());
        }

        for (int slot = 0; slot < HiveData.MAX_FRAMES; slot++) {
            FrameSlot frame = hive.frame(slot);
            if (frame == null) {
                continue;
            }
            String framePath = path + ".frames." + slot;
            data.set(framePath + ".type", frame.type().name());
            data.set(framePath + ".uses_left", frame.usesLeft());
        }
        flush();
    }

    @Override
    public void delete(HivePos pos) {
        data.set("hives." + safeKey(pos.key()), null);
        flush();
    }

    @Override
    public void close() {
        flush();
    }

    private void flush() {
        try {
            data.save(file);
        } catch (IOException exception) {
            throw new StorageException("Failed to save hives.yml", exception);
        }
    }

    private static String safeKey(String key) {
        return key.replace(':', '_').replace(',', '_');
    }

    private static HiveTier tierOf(String name) {
        try {
            return HiveTier.valueOf(name == null ? "I" : name);
        } catch (IllegalArgumentException exception) {
            return HiveTier.I;
        }
    }

    private static HiveMode modeOf(String name) {
        try {
            return HiveMode.valueOf(name == null ? "APIARY" : name);
        } catch (IllegalArgumentException exception) {
            return HiveMode.APIARY;
        }
    }

    private static FrameType frameOf(String name) {
        try {
            return FrameType.valueOf(name == null ? "WAX" : name);
        } catch (IllegalArgumentException exception) {
            return FrameType.WAX;
        }
    }
}
