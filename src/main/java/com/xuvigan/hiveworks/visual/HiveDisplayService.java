package com.xuvigan.hiveworks.visual;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HiveManager;
import com.xuvigan.hiveworks.item.PdcKeys;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional ItemDisplay overlay that renders the custom hive model above the vanilla block.
 *
 * <p>Every entity lookup, spawn and removal is executed on the region that owns
 * the hive block.</p>
 */
public final class HiveDisplayService {

    private static final Transformation HIVE_TRANSFORM = new Transformation(
            new Vector3f(0.0f, 0.0f, 0.0f),
            new AxisAngle4f(0.0f, 0.0f, 1.0f, 0.0f),
            new Vector3f(1.02f, 1.02f, 1.02f),
            new AxisAngle4f(0.0f, 0.0f, 1.0f, 0.0f)
    );

    private static final Display.Brightness HIVE_BRIGHTNESS = new Display.Brightness(15, 15);

    private final HiveWorksPlugin plugin;
    private final SchedulerAdapter scheduler;
    private final Map<String, UUID> displays = new ConcurrentHashMap<>();
    private final Map<String, Long> lastEnsure = new ConcurrentHashMap<>();

    public HiveDisplayService(HiveWorksPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.schedulers();
    }

    /** Region-safe entry point: dispatches to the hive region when needed. */
    public void ensure(HiveData hive) {
        if (!plugin.configManager().customHiveModel()) {
            return;
        }
        World world = Bukkit.getWorld(hive.pos().world());
        if (world == null) {
            return;
        }
        if (SchedulerAdapter.ownedNow(world, hive.pos().x(), hive.pos().z())) {
            ensureNow(hive);
        } else {
            scheduler.executeAtHive(hive.pos(), () -> ensureNow(hive));
        }
    }

    /** Must be called from the region that owns the hive block. */
    public void ensureNow(HiveData hive) {
        if (!plugin.configManager().customHiveModel()) {
            return;
        }
        World world = Bukkit.getWorld(hive.pos().world());
        if (world == null || !world.isChunkLoaded(hive.pos().x() >> 4, hive.pos().z() >> 4)) {
            return;
        }
        Block block = world.getBlockAt(hive.pos().x(), hive.pos().y(), hive.pos().z());
        if (!HiveManager.isHiveBlock(block.getType())) {
            return;
        }

        String key = hive.pos().key();
        long now = System.currentTimeMillis();
        Long checkedAt = lastEnsure.get(key);
        if (checkedAt != null && now - checkedAt < 30_000L) {
            UUID cached = displays.get(key);
            Entity cachedEntity = cached == null ? null : Bukkit.getEntity(cached);
            if (cachedEntity instanceof ItemDisplay display && display.isValid()) {
                updateTier(display, hive);
                return;
            }
            if (cachedEntity != null && cachedEntity.isValid()) {
                return;
            }
        }
        lastEnsure.put(key, now);

        Location center = new Location(world,
                hive.pos().x() + 0.5,
                hive.pos().y() + 0.5,
                hive.pos().z() + 0.5);

        UUID id = displays.get(key);
        Entity existing = id == null ? null : Bukkit.getEntity(id);
        for (Entity nearby : world.getNearbyEntities(center, 1.5, 1.5, 1.5)) {
            if (!(nearby instanceof ItemDisplay candidate)) {
                continue;
            }
            String candidateKey = candidate.getPersistentDataContainer()
                    .get(PdcKeys.HIVE_KEY, PersistentDataType.STRING);
            if (!key.equals(candidateKey)) {
                continue;
            }
            if (existing == null) {
                existing = candidate;
                displays.put(key, candidate.getUniqueId());
            } else if (!candidate.getUniqueId().equals(existing.getUniqueId())) {
                candidate.remove();
            }
        }
        if (existing instanceof ItemDisplay display && display.isValid()) {
            updateTier(display, hive);
            return;
        }

        ItemDisplay display = world.spawn(center, ItemDisplay.class, entity -> {
            entity.setItemStack(plugin.itemFactory().hiveDisplay(hive.tier()));
            entity.setTransformation(HIVE_TRANSFORM);
            entity.setBrightness(HIVE_BRIGHTNESS);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setPersistent(false);
            entity.setSilent(true);
            entity.setShadowRadius(0.0f);
            entity.setShadowStrength(0.0f);
            entity.setCustomNameVisible(false);
            entity.getPersistentDataContainer().set(PdcKeys.HIVE_DISPLAY, PersistentDataType.BYTE, (byte) 1);
            entity.getPersistentDataContainer().set(PdcKeys.HIVE_KEY, PersistentDataType.STRING, key);
            entity.getPersistentDataContainer().set(PdcKeys.DISPLAY_TIER, PersistentDataType.STRING, hive.tier().name());
        });
        displays.put(key, display.getUniqueId());
    }

    public void remove(HiveData hive) {
        String key = hive.pos().key();
        lastEnsure.remove(key);
        World world = Bukkit.getWorld(hive.pos().world());
        if (world != null && SchedulerAdapter.ownedNow(world, hive.pos().x(), hive.pos().z())) {
            removeNow(hive, key);
        } else {
            scheduler.executeAtHive(hive.pos(), () -> removeNow(hive, key));
        }
    }

    private void removeNow(HiveData hive, String key) {
        UUID id = displays.remove(key);
        Entity entity = id == null ? null : Bukkit.getEntity(id);
        if (entity != null) {
            entity.remove();
            return;
        }
        World world = Bukkit.getWorld(hive.pos().world());
        if (world == null) {
            return;
        }
        Location center = new Location(world,
                hive.pos().x() + 0.5,
                hive.pos().y() + 0.5,
                hive.pos().z() + 0.5);
        for (Entity nearby : world.getNearbyEntities(center, 1.0, 1.0, 1.0)) {
            if (nearby instanceof ItemDisplay
                    && key.equals(nearby.getPersistentDataContainer().get(PdcKeys.HIVE_KEY, PersistentDataType.STRING))) {
                nearby.remove();
            }
        }
    }

    public void removeAll() {
        for (HiveData hive : plugin.hives().all()) {
            String key = hive.pos().key();
            // Never remove entities directly here: removeAll is also called from
            // onDisable on the shutdown thread, where the world data is gone.
            scheduler.executeAtHive(hive.pos(), () -> removeNow(hive, key));
        }
        displays.clear();
        lastEnsure.clear();
    }

    private void updateTier(ItemDisplay display, HiveData hive) {
        String tier = display.getPersistentDataContainer().get(PdcKeys.DISPLAY_TIER, PersistentDataType.STRING);
        if (hive.tier().name().equals(tier)) {
            return;
        }
        display.setItemStack(plugin.itemFactory().hiveDisplay(hive.tier()));
        display.getPersistentDataContainer().set(PdcKeys.DISPLAY_TIER, PersistentDataType.STRING, hive.tier().name());
    }
}
