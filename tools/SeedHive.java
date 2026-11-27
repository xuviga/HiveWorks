import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Smoke-test helper for the isolated Folia server: seeds or prints one hive row.
 *
 * <p>Not part of the plugin jar. Usage:</p>
 * <pre>
 * java -cp sqlite-jdbc.jar SeedHive.java &lt;db&gt; &lt;x&gt; &lt;y&gt; &lt;z&gt; [species]
 * java -cp sqlite-jdbc.jar SeedHive.java &lt;db&gt; --print
 * </pre>
 */
public final class SeedHive {

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String db = args[0];
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db)) {
            if (args.length > 1 && "--print".equals(args[1])) {
                print(connection);
                return;
            }
            int x = Integer.parseInt(args[1]);
            int y = Integer.parseInt(args[2]);
            int z = Integer.parseInt(args[3]);
            String species = args.length > 4 ? args[4] : "forest";
            seed(connection, x, y, z, species);
        }
    }

    private static void seed(Connection connection, int x, int y, int z, String species) throws Exception {
        long now = System.currentTimeMillis();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO hives (world, x, y, z, owner_uuid, tier, mode, honey, last_tick, created_at,"
                        + " queen_slot, queen_cycles, queen_bound, pollen_bonus, breed_a, breed_b, breed_ready)"
                        + " VALUES ('world', ?, ?, ?, '00000000-0000-0000-0000-000000000001', 0, 'APIARY',"
                        + " 2.5, ?, ?, 0, 10, 1, 0.0, -1, -1, 0)"
                        + " ON CONFLICT(world, x, y, z) DO UPDATE SET honey = 2.5, last_tick = ?")) {
            statement.setInt(1, x);
            statement.setInt(2, y);
            statement.setInt(3, z);
            statement.setLong(4, now);
            statement.setLong(5, now);
            statement.setLong(6, now);
            statement.executeUpdate();
        }

        int hiveId;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM hives WHERE world = 'world' AND x = ? AND y = ? AND z = ?")) {
            statement.setInt(1, x);
            statement.setInt(2, y);
            statement.setInt(3, z);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalStateException("hive row was not inserted");
                }
                hiveId = result.getInt(1);
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM hive_bees WHERE hive_id = ?")) {
            statement.setInt(1, hiveId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO hive_bees (hive_id, slot, bee_uuid, species, gene_prod, gene_speed,"
                        + " gene_cold, gene_temper, purity)"
                        + " VALUES (?, 0, '00000000-0000-0000-0000-0000000000b1', ?, 50, 50, 50, 50, 60)")) {
            statement.setInt(1, hiveId);
            statement.setString(2, species);
            statement.executeUpdate();
        }
        System.out.println("SEED: hive=" + hiveId + " at " + x + "," + y + "," + z + " species=" + species);
    }

    private static void print(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, world, x, y, z, honey, last_tick FROM hives ORDER BY id");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                System.out.println("HIVE: id=" + result.getInt("id")
                        + " " + result.getString("world") + " " + result.getInt("x") + ","
                        + result.getInt("y") + "," + result.getInt("z")
                        + " honey=" + result.getDouble("honey")
                        + " lastTick=" + result.getLong("last_tick"));
            }
        }
    }
}
