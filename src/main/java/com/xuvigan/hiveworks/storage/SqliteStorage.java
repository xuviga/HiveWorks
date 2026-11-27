package com.xuvigan.hiveworks.storage;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.apiary.Apiary;
import com.xuvigan.hiveworks.bee.Genes;
import com.xuvigan.hiveworks.config.ConfigManager;
import com.xuvigan.hiveworks.hive.BeeSlot;
import com.xuvigan.hiveworks.hive.FrameSlot;
import com.xuvigan.hiveworks.hive.FrameType;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HiveMode;
import com.xuvigan.hiveworks.hive.HivePos;
import com.xuvigan.hiveworks.hive.HiveTier;
import com.xuvigan.hiveworks.production.Quality;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SQLite storage, schema mirrors docs/DESIGN.md §7.
 */
public final class SqliteStorage implements Storage {

    private final HiveWorksPlugin plugin;
    private final File file;
    private Connection connection;

    public SqliteStorage(HiveWorksPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), config.sqliteFile());
    }

    @Override
    public void init() throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create data directory: " + parent.getAbsolutePath());
        }
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS hives (
                        id         INTEGER PRIMARY KEY AUTOINCREMENT,
                        world      TEXT NOT NULL,
                        x          INTEGER NOT NULL,
                        y          INTEGER NOT NULL,
                        z          INTEGER NOT NULL,
                        owner_uuid TEXT NOT NULL,
                        tier       INTEGER NOT NULL,
                        mode       TEXT NOT NULL,
                        honey      REAL NOT NULL,
                        last_tick  INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        UNIQUE(world, x, y, z)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS hive_bees (
                        hive_id    INTEGER NOT NULL,
                        slot       INTEGER NOT NULL,
                        bee_uuid   TEXT NOT NULL,
                        species    TEXT NOT NULL,
                        gene_prod  INTEGER NOT NULL,
                        gene_speed INTEGER NOT NULL,
                        gene_cold  INTEGER NOT NULL,
                        gene_temper INTEGER NOT NULL,
                        purity     INTEGER NOT NULL,
                        PRIMARY KEY (hive_id, slot),
                        FOREIGN KEY (hive_id) REFERENCES hives(id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS hive_frames (
                        hive_id    INTEGER NOT NULL,
                        slot       INTEGER NOT NULL,
                        frame_type TEXT NOT NULL,
                        uses_left  INTEGER NOT NULL,
                        PRIMARY KEY (hive_id, slot),
                        FOREIGN KEY (hive_id) REFERENCES hives(id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid            TEXT PRIMARY KEY,
                        name            TEXT NOT NULL,
                        discovered      TEXT NOT NULL DEFAULT '',
                        total_harvests  INTEGER NOT NULL DEFAULT 0,
                        total_honey     INTEGER NOT NULL DEFAULT 0,
                        legend_harvests INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL)");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS apiaries (
                        id         TEXT PRIMARY KEY,
                        owner_uuid TEXT NOT NULL,
                        name       TEXT NOT NULL,
                        world      TEXT NOT NULL,
                        x          INTEGER NOT NULL,
                        y          INTEGER NOT NULL,
                        z          INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """);
        }
        ensureColumn("hives", "queen_slot", "INTEGER NOT NULL DEFAULT -1");
        ensureColumn("hives", "queen_cycles", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("hives", "queen_bound", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("hives", "pollen_bonus", "REAL NOT NULL DEFAULT 0");
        ensureColumn("hives", "breed_a", "INTEGER NOT NULL DEFAULT -1");
        ensureColumn("hives", "breed_b", "INTEGER NOT NULL DEFAULT -1");
        ensureColumn("hives", "breed_ready", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("players", "atlas_claimed", "TEXT NOT NULL DEFAULT ''");
        plugin.getLogger().info("SQLite storage ready: " + file.getName());
    }

    private void ensureColumn(String table, String column, String definition) throws SQLException {
        boolean exists = false;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                if (column.equalsIgnoreCase(result.getString("name"))) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
            plugin.getLogger().info("SQLite migration: added " + table + "." + column);
        }
    }

    @Override
    public List<HiveData> loadAll() {
        Map<Integer, HiveData> byId = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT * FROM hives")) {
            while (result.next()) {
                HiveData hive = new HiveData(
                        new HivePos(
                                result.getString("world"),
                                result.getInt("x"),
                                result.getInt("y"),
                                result.getInt("z")
                        ),
                        UUID.fromString(result.getString("owner_uuid"))
                );
                hive.setTier(tierOf(result.getInt("tier")));
                hive.setMode(modeOf(result.getString("mode")));
                hive.setHoney(result.getDouble("honey"));
                hive.setLastTick(result.getLong("last_tick"));
                hive.setQueenSlot(result.getInt("queen_slot"));
                hive.setQueenCycles(result.getInt("queen_cycles"));
                hive.setQueenBound(result.getInt("queen_bound") != 0);
                hive.setPollenBonus(result.getDouble("pollen_bonus"));
                int breedA = result.getInt("breed_a");
                int breedB = result.getInt("breed_b");
                long breedReady = result.getLong("breed_ready");
                if (breedA >= 0 && breedB >= 0 && breedReady > 0L) {
                    hive.startBreeding(breedA, breedB, breedReady);
                }
                byId.put(result.getInt("id"), hive);
            }
        } catch (SQLException exception) {
            throw new StorageException("Failed to load hives", exception);
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT slot, bee_uuid, species, gene_prod, gene_speed, gene_cold, gene_temper, purity "
                        + "FROM hive_bees WHERE hive_id = ?")) {
            for (Map.Entry<Integer, HiveData> entry : byId.entrySet()) {
                statement.setInt(1, entry.getKey());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        entry.getValue().setBee(result.getInt("slot"), new BeeSlot(
                                UUID.fromString(result.getString("bee_uuid")),
                                result.getString("species"),
                                new Genes(
                                        result.getInt("gene_prod"),
                                        result.getInt("gene_speed"),
                                        result.getInt("gene_cold"),
                                        result.getInt("gene_temper")
                                ),
                                result.getInt("purity")
                        ));
                    }
                }
            }
        } catch (SQLException exception) {
            throw new StorageException("Failed to load hives", exception);
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT slot, frame_type, uses_left FROM hive_frames WHERE hive_id = ?")) {
            for (Map.Entry<Integer, HiveData> entry : byId.entrySet()) {
                statement.setInt(1, entry.getKey());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        entry.getValue().setFrame(result.getInt("slot"), new FrameSlot(
                                frameOf(result.getString("frame_type")),
                                result.getInt("uses_left")
                        ));
                    }
                }
            }
        } catch (SQLException exception) {
            throw new StorageException("Failed to load hives", exception);
        }

        return new ArrayList<>(byId.values());
    }

    @Override
    public List<Apiary> loadApiaries() {
        List<Apiary> apiaries = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT * FROM apiaries")) {
            while (result.next()) {
                apiaries.add(new Apiary(
                        UUID.fromString(result.getString("id")),
                        UUID.fromString(result.getString("owner_uuid")),
                        result.getString("name"),
                        new HivePos(
                                result.getString("world"),
                                result.getInt("x"),
                                result.getInt("y"),
                                result.getInt("z")
                        ),
                        result.getLong("created_at")
                ));
            }
        } catch (SQLException exception) {
            throw new StorageException("Failed to load apiaries", exception);
        }
        return apiaries;
    }

    @Override
    public void saveApiary(Apiary apiary) {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO apiaries (id, owner_uuid, name, world, x, y, z, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    owner_uuid = excluded.owner_uuid,
                    name = excluded.name,
                    world = excluded.world,
                    x = excluded.x,
                    y = excluded.y,
                    z = excluded.z
                """)) {
            statement.setString(1, apiary.id().toString());
            statement.setString(2, apiary.owner().toString());
            statement.setString(3, apiary.name());
            statement.setString(4, apiary.center().world());
            statement.setInt(5, apiary.center().x());
            statement.setInt(6, apiary.center().y());
            statement.setInt(7, apiary.center().z());
            statement.setLong(8, apiary.createdAt());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Failed to save apiary " + apiary.id(), exception);
        }
    }

    @Override
    public void deleteApiary(UUID id) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM apiaries WHERE id = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Failed to delete apiary " + id, exception);
        }
    }

    @Override
    public Set<String> discoveredSpecies(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT discovered FROM players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    String raw = result.getString("discovered");
                    if (raw == null || raw.isBlank()) {
                        return new LinkedHashSet<>();
                    }
                    return new LinkedHashSet<>(Arrays.asList(raw.split(",")));
                }
            }
        } catch (SQLException exception) {
            throw new StorageException("Failed to load atlas for " + uuid, exception);
        }
        return new LinkedHashSet<>();
    }

    @Override
    public void discoverSpecies(UUID uuid, String playerName, String speciesId) {
        Set<String> discovered = discoveredSpecies(uuid);
        discovered.add(speciesId);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO players (uuid, name, discovered)
                VALUES (?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    name = CASE WHEN excluded.name = '' THEN players.name ELSE excluded.name END,
                    discovered = excluded.discovered
                """)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, playerName);
            statement.setString(3, String.join(",", discovered));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Failed to save atlas for " + uuid, exception);
        }
    }

    @Override
    public void recordHarvest(UUID uuid, String playerName, int honey, Quality quality) {
        int legendary = quality == Quality.LEGENDARY ? 1 : 0;
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO players (uuid, name, total_harvests, total_honey, legend_harvests)
                VALUES (?, ?, 1, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    name = excluded.name,
                    total_harvests = players.total_harvests + 1,
                    total_honey = players.total_honey + excluded.total_honey,
                    legend_harvests = players.legend_harvests + excluded.legend_harvests
                """)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, playerName);
            statement.setInt(3, Math.max(0, honey));
            statement.setInt(4, legendary);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Failed to record harvest for " + uuid, exception);
        }
    }

    @Override
    public List<TopEntry> topHarvesters(int limit) {
        List<TopEntry> top = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT uuid, name, total_honey, total_harvests, legend_harvests
                FROM players
                ORDER BY total_honey DESC, total_harvests DESC
                LIMIT ?
                """)) {
            statement.setInt(1, Math.max(1, Math.min(100, limit)));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    top.add(new TopEntry(
                            UUID.fromString(result.getString("uuid")),
                            result.getString("name"),
                            result.getLong("total_honey"),
                            result.getLong("total_harvests"),
                            result.getLong("legend_harvests")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new StorageException("Failed to load top harvesters", exception);
        }
        return top;
    }

    @Override
    public Set<Integer> claimedAtlasRewards(UUID uuid) {
        Set<Integer> claimed = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT atlas_claimed FROM players WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return claimed;
                }
                String raw = result.getString("atlas_claimed");
                if (raw == null || raw.isBlank()) {
                    return claimed;
                }
                for (String part : raw.split(",")) {
                    try {
                        claimed.add(Integer.parseInt(part.trim()));
                    } catch (NumberFormatException ignored) {
                        // ignore malformed entries
                    }
                }
            }
        } catch (SQLException exception) {
            throw new StorageException("Failed to load atlas rewards for " + uuid, exception);
        }
        return claimed;
    }

    @Override
    public void claimAtlasReward(UUID uuid, int threshold) {
        Set<Integer> claimed = claimedAtlasRewards(uuid);
        claimed.add(threshold);
        String raw = claimed.stream().sorted().map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO players (uuid, name, atlas_claimed)
                VALUES (?, '', ?)
                ON CONFLICT(uuid) DO UPDATE SET atlas_claimed = excluded.atlas_claimed
                """)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, raw);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Failed to claim atlas reward for " + uuid, exception);
        }
    }

    @Override
    public void save(HiveData hive) {
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO hives (world, x, y, z, owner_uuid, tier, mode, honey, last_tick, created_at,
                                       queen_slot, queen_cycles, queen_bound, pollen_bonus, breed_a, breed_b, breed_ready)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(world, x, y, z) DO UPDATE SET
                        owner_uuid = excluded.owner_uuid,
                        tier = excluded.tier,
                        mode = excluded.mode,
                        honey = excluded.honey,
                        last_tick = excluded.last_tick,
                        queen_slot = excluded.queen_slot,
                        queen_cycles = excluded.queen_cycles,
                        queen_bound = excluded.queen_bound,
                        pollen_bonus = excluded.pollen_bonus,
                        breed_a = excluded.breed_a,
                        breed_b = excluded.breed_b,
                        breed_ready = excluded.breed_ready
                    """)) {
                statement.setString(1, hive.pos().world());
                statement.setInt(2, hive.pos().x());
                statement.setInt(3, hive.pos().y());
                statement.setInt(4, hive.pos().z());
                statement.setString(5, hive.owner().toString());
                statement.setInt(6, hive.tier().ordinal());
                statement.setString(7, hive.mode().name());
                statement.setDouble(8, hive.honey());
                statement.setLong(9, hive.lastTick());
                statement.setLong(10, System.currentTimeMillis());
                statement.setInt(11, hive.queenSlot());
                statement.setInt(12, hive.queenCycles());
                statement.setInt(13, hive.queenBound() ? 1 : 0);
                statement.setDouble(14, hive.pollenBonus());
                statement.setInt(15, hive.breedSlotA());
                statement.setInt(16, hive.breedSlotB());
                statement.setLong(17, hive.breedReadyAt());
                statement.executeUpdate();
            }

            int hiveId = hiveId(hive.pos());
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM hive_bees WHERE hive_id = ?")) {
                statement.setInt(1, hiveId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO hive_bees (hive_id, slot, bee_uuid, species, gene_prod, gene_speed, gene_cold, gene_temper, purity)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                for (int slot = 0; slot < HiveData.MAX_BEES; slot++) {
                    BeeSlot bee = hive.bee(slot);
                    if (bee == null) {
                        continue;
                    }
                    statement.setInt(1, hiveId);
                    statement.setInt(2, slot);
                    statement.setString(3, bee.beeId().toString());
                    statement.setString(4, bee.speciesId());
                    statement.setInt(5, bee.genes().productivity());
                    statement.setInt(6, bee.genes().speed());
                    statement.setInt(7, bee.genes().cold());
                    statement.setInt(8, bee.genes().temper());
                    statement.setInt(9, bee.purity());
                    statement.addBatch();
                }
                statement.executeBatch();
            }

            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM hive_frames WHERE hive_id = ?")) {
                statement.setInt(1, hiveId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO hive_frames (hive_id, slot, frame_type, uses_left) VALUES (?, ?, ?, ?)")) {
                for (int slot = 0; slot < HiveData.MAX_FRAMES; slot++) {
                    FrameSlot frame = hive.frame(slot);
                    if (frame == null) {
                        continue;
                    }
                    statement.setInt(1, hiveId);
                    statement.setInt(2, slot);
                    statement.setString(3, frame.type().name());
                    statement.setInt(4, frame.usesLeft());
                    statement.addBatch();
                }
                statement.executeBatch();
            }

            connection.commit();
        } catch (SQLException exception) {
            rollbackQuietly();
            throw new StorageException("Failed to save hive " + hive.pos().key(), exception);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // connection is closed or broken; nothing useful to do here
            }
        }
    }

    @Override
    public void delete(HivePos pos) {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM hives WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            statement.setString(1, pos.world());
            statement.setInt(2, pos.x());
            statement.setInt(3, pos.y());
            statement.setInt(4, pos.z());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new StorageException("Failed to delete hive " + pos.key(), exception);
        }
    }

    @Override
    public void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Failed to close SQLite: " + exception.getMessage());
        }
    }

    private int hiveId(HivePos pos) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM hives WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            statement.setString(1, pos.world());
            statement.setInt(2, pos.x());
            statement.setInt(3, pos.y());
            statement.setInt(4, pos.z());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }
        }
        throw new SQLException("Hive row disappeared: " + pos.key());
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // already broken
        }
    }

    private static HiveTier tierOf(int ordinal) {
        HiveTier[] values = HiveTier.values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }

    private static HiveMode modeOf(String name) {
        try {
            return HiveMode.valueOf(name);
        } catch (IllegalArgumentException exception) {
            return HiveMode.APIARY;
        }
    }

    private static FrameType frameOf(String name) {
        try {
            return FrameType.valueOf(name);
        } catch (IllegalArgumentException exception) {
            return FrameType.WAX;
        }
    }
}
