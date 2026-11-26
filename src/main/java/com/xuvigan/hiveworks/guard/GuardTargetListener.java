package com.xuvigan.hiveworks.guard;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import org.bukkit.entity.Bee;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

/**
 * Keeps guard bees on hostile mobs, never on players or passive entities.
 */
public final class GuardTargetListener implements Listener {

    private final HiveWorksPlugin plugin;

    public GuardTargetListener(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Bee bee) || !GuardService.isGuard(bee)) {
            return;
        }
        LivingEntity target = event.getTarget();
        if (target == null) {
            return;
        }
        if (!plugin.guardService().isHostile(target)) {
            event.setCancelled(true);
        }
    }
}
