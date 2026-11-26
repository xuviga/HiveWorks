package com.xuvigan.hiveworks.gui;

import com.xuvigan.hiveworks.hive.HiveData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marks HiveWorks inventories.
 */
public final class GuiHolder implements InventoryHolder {

    private final HiveData hive;
    private Inventory inventory;

    public GuiHolder(HiveData hive) {
        this.hive = hive;
    }

    public HiveData hive() {
        return hive;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
