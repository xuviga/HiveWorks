package com.xuvigan.hiveworks.guard;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HiveMode;
import com.xuvigan.hiveworks.item.PdcKeys;
import com.xuvigan.hiveworks.raid.HiveRaidService;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Wither;
import org.bukkit.persistence.PersistentDataType;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GUARD mode: temporary angry bees defend the apiary from hostile mobs.
 *
 * <p>The global timer only decides which hive needs defence; spawning, targeting
 * and removal happen on the hive region or on the guard bee's own scheduler.</p>
 */
public final class GuardService {

    private static final class GuardBee {
        private final String hiveKey;
        private final long expiresAt;
        private final WeakReference<Bee> bee;

        private GuardBee(String hiveKey, long expiresAt, Bee bee) {
            this.hiveKey = hiveKey;
            this.expiresAt = expiresAt;
            this.bee = new WeakReference<>(bee);
        }

        private Bee entity() {
            Bee value = bee.get();
            return value != null && value.isValid() && !value.isDead() ? value : null;
        }
    }

    private final HiveWorksPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final Map<UUID, GuardBee> guards = new ConcurrentHashMap<>();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private ScheduledTask task;

    public GuardService(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.schedulers();
    }

    public void start() {
        if (task != null) {
            return;
        }
        if (!plugin.configManager().guardEnabled()) {
            plugin.getLogger().info("Guard mode disabled by config.");
            return;
        }
        long period = plugin.configManager().guardIntervalSeconds() * 20L;
        this.task = scheduler.runGlobalTimer(this::tick, 20L, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeAll();
    }

    /** Clears the cooldown so a freshly switched hive can react immediately. */
    public void activate(HiveData hive) {
        cooldowns.remove(hive.pos().key());
    }

    public void clearHive(String hiveKey) {
        cooldowns.remove(hiveKey);
        Iterator<Map.Entry<UUID, GuardBee>> iterator = guards.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, GuardBee> entry = iterator.next();
            if (!entry.getValue().hiveKey.equals(hiveKey)) {
                continue;
            }
            Bee bee = entry.getValue().entity();
            if (bee != null) {
                scheduler.executeFor(bee, bee::remove);
            }
            iterator.remove();
        }
    }

    public void removeAll() {
        for (GuardBee guard : guards.values()) {
            Bee bee = guard.entity();
            if (bee != null) {
                scheduler.executeFor(bee, bee::remove);
            }
        }
        guards.clear();
        cooldowns.clear();
    }

    public boolean isHostile(LivingEntity living) {
        if (living instanceof Player || living instanceof ArmorStand) {
            return false;
        }
        if (HiveRaidService.isRaider(living)) {
            return true;
        }
        if (plugin.queenBoss() != null && plugin.queenBoss().isBossOrMinion(living)) {
            return true;
        }
        if (living instanceof Creeper && !plugin.configManager().guardAttackCreepers()) {
            return false;
        }
        if (living instanceof Warden || living instanceof Wither || living instanceof EnderDragon) {
            return false;
        }
        return living instanceof Monster || living instanceof Slime || living instanceof Phantom;
    }

    public static boolean isGuard(Bee bee) {
        Byte value = bee.getPersistentDataContainer().get(PdcKeys.GUARD, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        cleanup(now);

        for (HiveData hive : plugin.hives().all()) {
            if (hive.mode() != HiveMode.GUARD || hive.occupiedBees() == 0) {
                continue;
            }
            scheduler.executeAtHive(hive.pos(), () -> defend(hive, now));
        }
    }

    /** Runs on the hive region. */
    private void defend(HiveData hive, long now) {
        World world = Bukkit.getWorld(hive.pos().world());
        if (world == null || !world.isChunkLoaded(hive.pos().x() >> 4, hive.pos().z() >> 4)) {
            return;
        }
        String key = hive.pos().key();
        if (now < cooldowns.getOrDefault(key, 0L)) {
            return;
        }
        int alive = countGuards(key);
        if (alive >= plugin.configManager().guardBees()) {
            return;
        }

        Location center = new Location(world,
                hive.pos().x() + 0.5,
                hive.pos().y() + 0.5,
                hive.pos().z() + 0.5);
        LivingEntity target = findTarget(center, plugin.configManager().guardRadius());
        if (target == null) {
            return;
        }
        for (int index = alive; index < plugin.configManager().guardBees(); index++) {
            spawnGuard(world, center, key, target, now);
        }
        cooldowns.put(key, now + plugin.configManager().guardCooldownSeconds() * 1000L);
    }

    private void spawnGuard(World world, Location center, String hiveKey, LivingEntity target, long now) {
        long expiresAt = now + plugin.configManager().guardLifetimeSeconds() * 1000L;
        Location location = center.clone().add(
                random.nextDouble() * 2.0 - 1.0,
                1.0,
                random.nextDouble() * 2.0 - 1.0
        );
        Bee bee = world.spawn(location, Bee.class, entity -> {
            entity.setPersistent(false);
            entity.setRemoveWhenFarAway(true);
            entity.setAnger(600);
            entity.setTarget(target);
            entity.customName(Component.text("Пчела-страж", NamedTextColor.RED));
            entity.setCustomNameVisible(false);
            entity.getPersistentDataContainer().set(PdcKeys.GUARD, PersistentDataType.BYTE, (byte) 1);
            entity.getPersistentDataContainer().set(PdcKeys.GUARD_HIVE, PersistentDataType.STRING, hiveKey);
            entity.getPersistentDataContainer().set(PdcKeys.GUARD_EXPIRES, PersistentDataType.LONG, expiresAt);
        });
        guards.put(bee.getUniqueId(), new GuardBee(hiveKey, expiresAt, bee));
        long lifetimeTicks = Math.max(20L, plugin.configManager().guardLifetimeSeconds() * 20L);
        scheduler.runForLater(bee, () -> {
            if (bee.isValid()) {
                bee.remove();
            }
            guards.remove(bee.getUniqueId());
        }, lifetimeTicks);
    }

    private LivingEntity findTarget(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }
        LivingEntity best = null;
        double bestDistance = radius * radius;
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || !isHostile(living)) {
                continue;
            }
            double distance = living.getLocation().distanceSquared(center);
            if (distance < bestDistance) {
                best = living;
                bestDistance = distance;
            }
        }
        return best;
    }

    private int countGuards(String hiveKey) {
        int count = 0;
        for (Map.Entry<UUID, GuardBee> entry : guards.entrySet()) {
            GuardBee guard = entry.getValue();
            if (!guard.hiveKey.equals(hiveKey)) {
                continue;
            }
            if (guard.entity() == null) {
                guards.remove(entry.getKey());
                continue;
            }
            count++;
        }
        return count;
    }

    /** Data-only sweep; entity removal is handled by the entity scheduler. */
    private void cleanup(long now) {
        Iterator<Map.Entry<UUID, GuardBee>> iterator = guards.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, GuardBee> entry = iterator.next();
            GuardBee guard = entry.getValue();
            HiveData hive = plugin.hives().byKey(guard.hiveKey);
            boolean invalid = guard.entity() == null
                    || now > guard.expiresAt
                    || hive == null
                    || hive.mode() != HiveMode.GUARD;
            if (invalid) {
                iterator.remove();
            }
        }
    }
}
