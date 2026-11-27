package com.xuvigan.hiveworks.boss;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.bee.Species;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HiveManager;
import com.xuvigan.hiveworks.product.ProductType;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Full Queen ritual:
 * requirements -> countdown -> multi-phase boss with minions -> Queen item reward.
 *
 * <p>Folia mapping: rituals are checked from a global timer and validated on the
 * hive region, the boss and its minions tick on the boss entity's scheduler, and
 * bossbar membership is updated per player on that player's scheduler.</p>
 */
public final class QueenBossService implements Listener {

    public enum RitualResult {
        STARTED,
        DISABLED,
        NEED_TIER,
        NEED_QUEEN,
        NEED_NIGHT,
        NEED_CANDLES,
        NEED_ITEMS,
        COOLDOWN,
        ACTIVE,
        NOT_OWNER
    }

    public record RitualInfo(boolean enabled, boolean tierOk, boolean hasQueen, boolean night,
                             int candles, int needCandles, boolean hasItems, long cooldownMs,
                             boolean ritualActive, boolean bossActive) {
        public boolean ready() {
            return enabled && tierOk && hasQueen && night
                    && candles >= needCandles && hasItems
                    && cooldownMs <= 0 && !ritualActive && !bossActive;
        }
    }

    private static final class Ritual {
        private final String hiveKey;
        private final UUID owner;
        private final long startsAt;
        private final long endsAt;
        private final int jellyCost;
        private final int cellCost;
        private final BossBar bar;

        private Ritual(String hiveKey, UUID owner, long startsAt, long endsAt,
                       int jellyCost, int cellCost, BossBar bar) {
            this.hiveKey = hiveKey;
            this.owner = owner;
            this.startsAt = startsAt;
            this.endsAt = endsAt;
            this.jellyCost = jellyCost;
            this.cellCost = cellCost;
            this.bar = bar;
        }
    }

    private static final class BossState {
        private final UUID entityId;
        private final String hiveKey;
        private final UUID owner;
        private final double maxHealth;
        private final BossBar bar;
        private final WeakReference<Bee> entity;
        private final Map<UUID, WeakReference<Bee>> minions = new ConcurrentHashMap<>();
        private int phase;
        private long nextMinionAt;
        private long lastPlayerSeen;

        private BossState(UUID entityId, String hiveKey, UUID owner, double maxHealth,
                          BossBar bar, Bee entity) {
            this.entityId = entityId;
            this.hiveKey = hiveKey;
            this.owner = owner;
            this.maxHealth = Math.max(1.0, maxHealth);
            this.bar = bar;
            this.entity = new WeakReference<>(entity);
            this.lastPlayerSeen = System.currentTimeMillis();
        }

        private Bee livingEntity() {
            Bee value = entity.get();
            return value != null && value.isValid() && !value.isDead() ? value : null;
        }
    }

    private final HiveWorksPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final Random random = new Random();
    private final Map<String, Ritual> rituals = new ConcurrentHashMap<>();
    private final Map<UUID, BossState> bosses = new ConcurrentHashMap<>();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private ScheduledTask task;

    public QueenBossService(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.schedulers();
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = scheduler.runGlobalTimer(this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Ritual ritual : new ArrayList<>(rituals.values())) {
            abortRitual(ritual, "ritual-aborted");
        }
        for (BossState boss : new ArrayList<>(bosses.values())) {
            cleanupBoss(boss, true);
        }
        rituals.clear();
        bosses.clear();
    }

    public boolean isBossOrMinion(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (bosses.containsKey(entity.getUniqueId())) {
            return true;
        }
        for (BossState boss : bosses.values()) {
            if (boss.minions.containsKey(entity.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public RitualInfo info(Player player, HiveData hive) {
        World world = Bukkit.getWorld(hive.pos().world());
        int candles;
        if (world != null && SchedulerAdapter.ownedNow(world, hive.pos().x(), hive.pos().z())) {
            candles = countCandles(world, hive, 5);
            hive.setCachedCandles(candles);
        } else {
            candles = hive.cachedCandles();
        }
        int need = plugin.configManager().ritualCandles();
        boolean jelly = plugin.configManager().ritualRoyalJelly() <= 0
                || player.getInventory().containsAtLeast(
                plugin.itemFactory().product(ProductType.ROYAL_JELLY,
                        plugin.configManager().ritualRoyalJelly()),
                plugin.configManager().ritualRoyalJelly());
        boolean cells = plugin.configManager().ritualRoyalCells() <= 0
                || player.getInventory().containsAtLeast(
                plugin.itemFactory().product(ProductType.ROYAL_CELL,
                        plugin.configManager().ritualRoyalCells()),
                plugin.configManager().ritualRoyalCells());
        long cooldown = Math.max(0L, cooldowns.getOrDefault(hive.pos().key(), 0L)
                - System.currentTimeMillis());
        boolean ritualActive = rituals.containsKey(hive.pos().key());
        boolean bossActive = bosses.values().stream()
                .anyMatch(boss -> boss.hiveKey.equals(hive.pos().key()));
        return new RitualInfo(
                plugin.configManager().bossEnabled(),
                hive.tier() == com.xuvigan.hiveworks.hive.HiveTier.IV,
                hive.hasQueen(),
                world != null && isNight(world),
                candles,
                need,
                jelly && cells,
                cooldown,
                ritualActive,
                bossActive
        );
    }

    public RitualResult startRitual(Player player, HiveData hive) {
        if (!plugin.hives().canManage(player, hive)) {
            return RitualResult.NOT_OWNER;
        }
        RitualInfo info = info(player, hive);
        if (!info.enabled()) {
            return RitualResult.DISABLED;
        }
        if (!info.tierOk()) {
            return RitualResult.NEED_TIER;
        }
        if (!info.hasQueen()) {
            return RitualResult.NEED_QUEEN;
        }
        if (!info.night()) {
            return RitualResult.NEED_NIGHT;
        }
        if (info.ritualActive() || info.bossActive()) {
            return RitualResult.ACTIVE;
        }
        if (info.cooldownMs() > 0L) {
            return RitualResult.COOLDOWN;
        }
        if (info.candles() < info.needCandles()) {
            return RitualResult.NEED_CANDLES;
        }
        if (!info.hasItems()) {
            return RitualResult.NEED_ITEMS;
        }
        World world = Bukkit.getWorld(hive.pos().world());
        if (world == null) {
            return RitualResult.NEED_TIER;
        }
        Location center = center(hive, world);
        int jellyCost = plugin.configManager().ritualRoyalJelly();
        int cellCost = plugin.configManager().ritualRoyalCells();
        if (jellyCost > 0) {
            player.getInventory().removeItem(plugin.itemFactory().product(ProductType.ROYAL_JELLY, jellyCost));
        }
        if (cellCost > 0) {
            player.getInventory().removeItem(plugin.itemFactory().product(ProductType.ROYAL_CELL, cellCost));
        }
        long now = System.currentTimeMillis();
        long endsAt = now + plugin.configManager().ritualCountdownSeconds() * 1000L;
        BossBar bar = Bukkit.createBossBar(strip(plugin.messages().formatted("ritual-bar",
                        "seconds", plugin.configManager().ritualCountdownSeconds())),
                BarColor.PURPLE, BarStyle.SOLID);
        bar.setProgress(1.0);
        Ritual ritual = new Ritual(hive.pos().key(), player.getUniqueId(), now, endsAt,
                jellyCost, cellCost, bar);
        rituals.put(hive.pos().key(), ritual);
        updateViewers(bar, center, plugin.configManager().bossArenaRadius());
        scheduler.executeAtHive(hive.pos(),
                () -> world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.7f));
        plugin.messages().send(player, "ritual-started",
                "seconds", plugin.configManager().ritualCountdownSeconds());
        return RitualResult.STARTED;
    }

    /** Admin/debug instant boss without requirements or cost. */
    public boolean forceStart(Player player, HiveData hive) {
        World world = Bukkit.getWorld(hive.pos().world());
        if (world == null) {
            return false;
        }
        if (rituals.containsKey(hive.pos().key()) || bosses.values().stream()
                .anyMatch(boss -> boss.hiveKey.equals(hive.pos().key()))) {
            plugin.messages().send(player, "boss-already");
            return false;
        }
        scheduler.executeAtHive(hive.pos(),
                () -> spawnBoss(hive.pos().key(), hive.owner(), world, center(hive, world)));
        return true;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Ritual ritual : new ArrayList<>(rituals.values())) {
            HiveData hive = plugin.hives().byKey(ritual.hiveKey);
            World world = hive == null ? null : Bukkit.getWorld(hive.pos().world());
            if (hive == null || world == null) {
                abortRitual(ritual, "ritual-aborted");
                continue;
            }
            Location center = center(hive, world);
            updateViewers(ritual.bar, center, plugin.configManager().bossArenaRadius());
            long remaining = Math.max(0L, ritual.endsAt - now);
            long total = Math.max(1L, ritual.endsAt - ritual.startsAt);
            ritual.bar.setProgress(Math.max(0.0, Math.min(1.0, 1.0 - (double) remaining / total)));
            ritual.bar.setTitle(strip(plugin.messages().formatted("ritual-bar",
                    "seconds", (remaining + 999L) / 1000L)));
            scheduler.executeAtHive(hive.pos(), () -> validateRitual(ritual, hive, world, center, now));
        }
    }

    /** Runs on the hive region: abort if the hive block disappeared, else spawn. */
    private void validateRitual(Ritual ritual, HiveData hive, World world, Location center, long now) {
        if (!HiveManager.isHiveBlock(world.getBlockAt(
                hive.pos().x(), hive.pos().y(), hive.pos().z()).getType())) {
            abortRitual(ritual, "ritual-aborted");
            return;
        }
        if (now >= ritual.endsAt && rituals.remove(ritual.hiveKey) != null) {
            ritual.bar.removeAll();
            spawnBoss(ritual.hiveKey, ritual.owner, world, center);
        }
    }

    /** Runs on the hive region. */
    private void spawnBoss(String hiveKey, UUID owner, World world, Location center) {
        double health = plugin.configManager().bossHealth();
        Bee boss = world.spawn(center, Bee.class, entity -> {
            entity.customName(plugin.messages().component("boss-name"));
            entity.setCustomNameVisible(true);
            entity.setPersistent(false);
            entity.setRemoveWhenFarAway(false);
            entity.setMaxHealth(health);
            entity.setHealth(health);
            entity.setAnger(20 * 60 * 30);
        });
        BossBar bar = Bukkit.createBossBar(strip(plugin.messages().raw("boss-name")),
                BarColor.RED, BarStyle.SOLID);
        bar.setProgress(1.0);
        BossState state = new BossState(boss.getUniqueId(), hiveKey, owner, health, bar, boss);
        state.nextMinionAt = System.currentTimeMillis() + plugin.configManager().bossMinionIntervalSeconds() * 1000L;
        bosses.put(boss.getUniqueId(), state);
        updateViewers(bar, center, plugin.configManager().bossArenaRadius());
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.7f);
        notifyNearby(world, center, "ritual-boss-spawned");
        scheduler.runForTimer(boss, () -> bossTick(state), 20L, 20L);
    }

    /** Runs on the boss entity's region. */
    private void bossTick(BossState boss) {
        long now = System.currentTimeMillis();
        Bee entity = boss.livingEntity();
        HiveData hive = plugin.hives().byKey(boss.hiveKey);
        if (entity == null || hive == null) {
            cleanupBoss(boss, false);
            return;
        }
        World world = entity.getWorld();
        updateViewers(boss.bar, entity.getLocation(), plugin.configManager().bossArenaRadius());
        double health = Math.max(0.0, entity.getHealth());
        boss.bar.setProgress(Math.max(0.0, Math.min(1.0, health / boss.maxHealth)));

        if (boss.phase == 0 && health <= boss.maxHealth * 2.0 / 3.0) {
            boss.phase = 1;
            boss.bar.setColor(BarColor.YELLOW);
            spawnMinions(boss, hive, world, plugin.configManager().bossMinions());
            notifyNearby(world, entity.getLocation(), "ritual-phase-1");
        } else if (boss.phase == 1 && health <= boss.maxHealth / 3.0) {
            boss.phase = 2;
            boss.bar.setColor(BarColor.RED);
            spawnMinions(boss, hive, world, plugin.configManager().bossMinions() * 2);
            modifier(entity, "SPEED", 20 * 60, 1);
            modifier(entity, "STRENGTH", 20 * 60, 0);
            notifyNearby(world, entity.getLocation(), "ritual-phase-2");
        }

        Player target = nearestPlayer(world, entity.getLocation(),
                plugin.configManager().bossArenaRadius());
        if (target != null) {
            boss.lastPlayerSeen = now;
            entity.setTarget(target);
        } else if (now - boss.lastPlayerSeen > 60_000L) {
            cleanupBoss(boss, true);
            return;
        }

        cleanupMinions(boss);
        if (now >= boss.nextMinionAt && boss.minions.size() < plugin.configManager().bossMinionCap()) {
            int count = Math.max(1, plugin.configManager().bossMinions());
            spawnMinions(boss, hive, world, count);
            boss.nextMinionAt = now + plugin.configManager().bossMinionIntervalSeconds() * 1000L;
        }
    }

    private void spawnMinions(BossState boss, HiveData hive, World world, int count) {
        if (world == null) {
            return;
        }
        Location center = center(hive, world);
        for (int index = 0; index < count; index++) {
            Location spot = center.clone().add(
                    random.nextDouble() * 6 - 3,
                    random.nextDouble() * 2 + 1,
                    random.nextDouble() * 6 - 3);
            if (!SchedulerAdapter.ownedNow(spot) || !spot.getBlock().getType().isAir()) {
                continue;
            }
            Bee minion = world.spawn(spot, Bee.class, entity -> {
                entity.customName(Component.text(strip(plugin.messages().raw("boss-minion-name"))));
                entity.setCustomNameVisible(false);
                entity.setPersistent(false);
                entity.setRemoveWhenFarAway(false);
                entity.setMaxHealth(20.0);
                entity.setHealth(20.0);
                entity.setAnger(20 * 60 * 10);
            });
            Player target = nearestPlayer(world, minion.getLocation(),
                    plugin.configManager().bossArenaRadius());
            if (target != null) {
                minion.setTarget(target);
            }
            boss.minions.put(minion.getUniqueId(), new WeakReference<>(minion));
        }
    }

    private void cleanupMinions(BossState boss) {
        Iterator<Map.Entry<UUID, WeakReference<Bee>>> iterator = boss.minions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, WeakReference<Bee>> entry = iterator.next();
            Bee minion = entry.getValue().get();
            if (minion == null || !minion.isValid() || minion.isDead()) {
                iterator.remove();
            }
        }
    }

    private void cleanupBoss(BossState boss, boolean removeEntity) {
        boss.bar.removeAll();
        bosses.remove(boss.entityId);
        if (removeEntity) {
            Bee entity = boss.livingEntity();
            if (entity != null) {
                scheduler.executeFor(entity, entity::remove);
            }
        }
        for (WeakReference<Bee> reference : boss.minions.values()) {
            Bee minion = reference.get();
            if (minion != null && minion.isValid()) {
                scheduler.executeFor(minion, minion::remove);
            }
        }
        boss.minions.clear();
    }

    private void abortRitual(Ritual ritual, String messageKey) {
        if (rituals.remove(ritual.hiveKey) == null) {
            return;
        }
        ritual.bar.removeAll();
        HiveData hive = plugin.hives().byKey(ritual.hiveKey);
        World world = hive == null ? null : Bukkit.getWorld(hive.pos().world());
        if (world != null) {
            Location center = center(hive, world);
            scheduler.executeAtHive(hive.pos(), () -> {
                if (ritual.jellyCost > 0) {
                    world.dropItemNaturally(center, plugin.itemFactory().product(
                            ProductType.ROYAL_JELLY, ritual.jellyCost));
                }
                if (ritual.cellCost > 0) {
                    world.dropItemNaturally(center, plugin.itemFactory().product(
                            ProductType.ROYAL_CELL, ritual.cellCost));
                }
            });
        }
        Player owner = Bukkit.getPlayer(ritual.owner);
        if (owner != null && owner.isOnline()) {
            plugin.messages().send(owner, messageKey);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        BossState boss = bosses.get(event.getEntity().getUniqueId());
        if (boss == null || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        double next = Math.max(0.0, living.getHealth() - event.getFinalDamage());
        boss.bar.setProgress(Math.max(0.0, Math.min(1.0, next / boss.maxHealth)));
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        BossState boss = bosses.get(event.getEntity().getUniqueId());
        if (boss == null) {
            return;
        }
        cleanupBoss(boss, false);
        HiveData hive = plugin.hives().byKey(boss.hiveKey);
        World world = event.getEntity().getWorld();
        Location location = event.getEntity().getLocation();
        dropQueenReward(hive, world, location);
        world.dropItemNaturally(location, plugin.itemFactory().product(ProductType.ROYAL_JELLY, 8));
        world.dropItemNaturally(location, plugin.itemFactory().product(ProductType.ROYAL_CELL, 4));
        world.dropItemNaturally(location, new ItemStack(Material.AMETHYST_SHARD, 16));
        if (hive != null) {
            hive.setQueenCycles(plugin.configManager().queenMaxCyclesCap());
            plugin.storage().save(hive);
            cooldowns.put(hive.pos().key(), System.currentTimeMillis()
                    + plugin.configManager().ritualCooldownMinutes() * 60_000L);
        }
        notifyNearby(world, location, "boss-defeated");
        Player owner = Bukkit.getPlayer(boss.owner);
        if (owner != null && owner.isOnline()) {
            plugin.messages().send(owner, "boss-defeated");
        }
    }

    private void dropQueenReward(HiveData hive, World world, Location location) {
        Optional<Species> species = hive == null || hive.queen() == null
                ? Optional.empty()
                : plugin.speciesRegistry().byId(hive.queen().speciesId());
        if (species.isEmpty()) {
            species = Optional.of(plugin.speciesRegistry().random(random));
        }
        world.dropItemNaturally(location, plugin.itemFactory().queenItem(
                species.get(), species.get().baseGenes(), species.get().basePurity()));
    }

    private void modifier(LivingEntity entity, String effectName, int duration, int amplifier) {
        PotionEffectType type = PotionEffectType.getByName(effectName);
        if (type != null) {
            entity.addPotionEffect(new PotionEffect(type, duration, amplifier, false, true, true));
        }
    }

    /** Bar membership is updated on each player's own scheduler. */
    private void updateViewers(BossBar bar, Location center, double radius) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        java.util.Set<Player> current = new java.util.HashSet<>(bar.getPlayers());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != world) {
                if (current.contains(player)) {
                    scheduler.executeFor(player, () -> bar.removePlayer(player));
                }
                continue;
            }
            scheduler.executeFor(player, () -> {
                if (!player.isOnline() || player.getWorld() != world) {
                    bar.removePlayer(player);
                    return;
                }
                boolean inRange = player.getLocation().distanceSquared(center) <= radius * radius;
                if (inRange) {
                    bar.addPlayer(player);
                } else {
                    bar.removePlayer(player);
                }
            });
        }
    }

    private Player nearestPlayer(World world, Location center, double radius) {
        Player best = null;
        double bestDistance = radius * radius;
        for (Player player : world.getPlayers()) {
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(center);
            if (distance <= bestDistance) {
                best = player;
                bestDistance = distance;
            }
        }
        return best;
    }

    private int countCandles(World world, HiveData hive, int radius) {
        int candles = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int blockX = hive.pos().x() + x;
                int blockZ = hive.pos().z() + z;
                if (!SchedulerAdapter.ownedChunkNow(world, blockX >> 4, blockZ >> 4)) {
                    continue;
                }
                for (int y = -2; y <= 3; y++) {
                    String name = world.getBlockAt(blockX, hive.pos().y() + y, blockZ).getType().name();
                    if (name.equals("CANDLE") || name.endsWith("_CANDLE")) {
                        candles++;
                    }
                }
            }
        }
        return candles;
    }

    private boolean isNight(World world) {
        long time = world.getTime() % 24_000L;
        return time >= 13_000L && time <= 23_000L;
    }

    private Location center(HiveData hive, World world) {
        return new Location(world, hive.pos().x() + 0.5, hive.pos().y() + 1.0,
                hive.pos().z() + 0.5);
    }

    private void notifyNearby(World world, Location center, String key) {
        if (world == null) {
            return;
        }
        double radius = plugin.configManager().bossArenaRadius();
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= radius * radius) {
                plugin.messages().send(player, key);
            }
        }
    }

    private String strip(String message) {
        return message == null ? "" : message.replaceAll("<[^>]+>", "");
    }
}
