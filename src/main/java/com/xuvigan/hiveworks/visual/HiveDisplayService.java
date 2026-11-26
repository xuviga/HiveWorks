package com.xuvigan.hiveworks.visual;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.hive.HiveManager;
import com.xuvigan.hiveworks.item.PdcKeys;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional ItemDisplay overlay that renders the custom hive model above the vanilla block.
 */
public final class HiveDisplayService {

    private final HiveWorksPlugin plugin;
    private final Map<String, UUID> displays = new ConcurrentHashMap<>();

    public HiveDisplayService(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public void ensure(HiveData hive) {
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
        UUID id = displays.get(key);
        Entity existing = id == null ? null : Bukkit.getEntity(id);
        if (existing instanceof ItemDisplay display && display.isValid()) {
            updateTier(display, hive);
            return;
        }

        Location location = new Location(world,
                hive.pos().x() + 0.5,
                hive.pos().y() + 0.5,
                hive.pos().z() + 0.5,
                0.0f,
                0.0f);
        ItemDisplay display = world.spawn(location, ItemDisplay.class, entity -> {
            entity.setItemStack(plugin.itemFactory().hiveDisplay(hive.tier()));
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
        for (UUID id : displays.values()) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
        displays.clear();
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
