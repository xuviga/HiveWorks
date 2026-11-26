package com.xuvigan.hiveworks;

import com.xuvigan.hiveworks.bee.BeeItem;
import com.xuvigan.hiveworks.bee.SpeciesRegistry;
import com.xuvigan.hiveworks.command.HiveCommand;
import com.xuvigan.hiveworks.config.ConfigManager;
import com.xuvigan.hiveworks.config.Messages;
import com.xuvigan.hiveworks.gui.GuiListener;
import com.xuvigan.hiveworks.hive.HiveListener;
import com.xuvigan.hiveworks.hive.HiveManager;
import com.xuvigan.hiveworks.hive.HiveTicker;
import com.xuvigan.hiveworks.item.ItemFactory;
import com.xuvigan.hiveworks.item.PdcKeys;
import com.xuvigan.hiveworks.storage.Storage;
import com.xuvigan.hiveworks.storage.StorageFactory;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * HiveWorks entry point.
 *
 * <p>Cozy beekeeping for Paper 1.21.x - 26.x. No hard dependencies.</p>
 */
public final class HiveWorksPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private Messages messages;
    private SpeciesRegistry speciesRegistry;
    private ItemFactory itemFactory;
    private BeeItem beeItem;
    private Storage storage;
    private HiveManager hiveManager;
    private HiveTicker ticker;

    @Override
    public void onEnable() {
        PdcKeys.init(this);
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.messages = new Messages(this);
        this.itemFactory = new ItemFactory();
        this.beeItem = new BeeItem();
        this.speciesRegistry = new SpeciesRegistry(this);

        try {
            this.storage = StorageFactory.create(this, configManager);
        } catch (Exception exception) {
            getLogger().severe("Storage initialization failed: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.hiveManager = new HiveManager(this);
        this.hiveManager.load();

        this.ticker = new HiveTicker(this);
        this.ticker.start();

        getServer().getPluginManager().registerEvents(new HiveListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        HiveCommand command = new HiveCommand(this);
        PluginCommand pluginCommand = getCommand("hive");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getLogger().info("HiveWorks " + getPluginMeta().getVersion() + " enabled: "
                + speciesRegistry.all().size() + " species, " + hiveManager.all().size() + " hives.");
    }

    @Override
    public void onDisable() {
        if (ticker != null) {
            ticker.stop();
        }
        if (hiveManager != null) {
            hiveManager.saveAll();
        }
        if (storage != null) {
            storage.close();
        }
    }

    /** Reloads config, messages and species without touching stored hives. */
    public void reloadAll() {
        configManager.reload();
        messages.reload();
        speciesRegistry.reload();
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public Messages messages() {
        return messages;
    }

    public SpeciesRegistry speciesRegistry() {
        return speciesRegistry;
    }

    public ItemFactory itemFactory() {
        return itemFactory;
    }

    public BeeItem beeItem() {
        return beeItem;
    }

    public Storage storage() {
        return storage;
    }

    public HiveManager hives() {
        return hiveManager;
    }
}
