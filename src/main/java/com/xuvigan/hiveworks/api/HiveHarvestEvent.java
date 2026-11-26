package com.xuvigan.hiveworks.api;

import com.xuvigan.hiveworks.hive.HiveData;
import com.xuvigan.hiveworks.production.Quality;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before honey is harvested. Cancel to keep the honey in the hive.
 */
public final class HiveHarvestEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final HiveData hive;
    private final Player player;
    private final int amount;
    private final Quality quality;
    private boolean cancelled;

    public HiveHarvestEvent(HiveData hive, Player player, int amount, Quality quality) {
        this.hive = hive;
        this.player = player;
        this.amount = amount;
        this.quality = quality;
    }

    public HiveData hive() {
        return hive;
    }

    public Player player() {
        return player;
    }

    public int amount() {
        return amount;
    }

    public Quality quality() {
        return quality;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
