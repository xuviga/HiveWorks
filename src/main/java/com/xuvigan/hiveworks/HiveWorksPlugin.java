package com.xuvigan.hiveworks;

import com.xuvigan.hiveworks.atlas.AtlasService;
import com.xuvigan.hiveworks.bee.BeeItem;
import com.xuvigan.hiveworks.bee.SpeciesRegistry;
import com.xuvigan.hiveworks.command.HiveCommand;
import com.xuvigan.hiveworks.config.ConfigManager;
import com.xuvigan.hiveworks.config.Messages;
import com.xuvigan.hiveworks.craft.RecipeRegistrar;
import com.xuvigan.hiveworks.gui.GuiListener;
import com.xuvigan.hiveworks.guard.GuardService;
import com.xuvigan.hiveworks.guard.GuardTargetListener;
import com.xuvigan.hiveworks.hive.HiveListener;
import com.xuvigan.hiveworks.hive.HiveManager;
import com.xuvigan.hiveworks.hive.HiveTicker;
import com.xuvigan.hiveworks.item.FrameItem;
import com.xuvigan.hiveworks.item.ItemFactory;
import com.xuvigan.hiveworks.item.NetItem;
import com.xuvigan.hiveworks.item.PdcKeys;
import com.xuvigan.hiveworks.item.ProductItem;
import com.xuvigan.hiveworks.resourcepack.PackServer;
import com.xuvigan.hiveworks.resourcepack.ResourcePackListener;
import com.xuvigan.hiveworks.storage.Storage;
import com.xuvigan.hiveworks.storage.StorageFactory;
import com.xuvigan.hiveworks.visual.HiveDisplayService;
import com.xuvigan.hiveworks.world.WildBeeListener;
import com.xuvigan.hiveworks.world.WildSwarmSpawner;
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
    private FrameItem frameItem;
    private NetItem netItem;
    private ProductItem productItem;
    private AtlasService atlas;
    private RecipeRegistrar recipeRegistrar;
    private WildSwarmSpawner wildSwarmSpawner;
    private PackServer packServer;
    private HiveDisplayService displays;
    private GuardService guardService;
    private Storage storage;
    private HiveManager hiveManager;
    private HiveTicker ticker;

    @Override
    public void onEnable() {
        PdcKeys.init(this);
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.messages = new Messages(this);
        this.itemFactory = new ItemFactory(this);
        this.beeItem = new BeeItem();
        this.frameItem = new FrameItem();
        this.netItem = new NetItem();
        this.productItem = new ProductItem();
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

        this.atlas = new AtlasService(this);
        this.recipeRegistrar = new RecipeRegistrar(this);
        this.recipeRegistrar.registerAll();
        this.displays = new HiveDisplayService(this);

        this.ticker = new HiveTicker(this);
        this.ticker.start();

        this.wildSwarmSpawner = new WildSwarmSpawner(this);
        this.wildSwarmSpawner.start();

        this.packServer = new PackServer(this, configManager);
        this.packServer.start();

        this.guardService = new GuardService(this);
        this.guardService.start();

        getServer().getPluginManager().registerEvents(new HiveListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new WildBeeListener(this), this);
        getServer().getPluginManager().registerEvents(new GuardTargetListener(this), this);
        getServer().getPluginManager().registerEvents(new ResourcePackListener(this), this);

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
        if (guardService != null) {
            guardService.stop();
        }
        if (displays != null) {
            displays.removeAll();
        }
        if (packServer != null) {
            packServer.stop();
        }
        if (wildSwarmSpawner != null) {
            wildSwarmSpawner.stop();
        }
        if (ticker != null) {
            ticker.stop();
        }
        if (recipeRegistrar != null) {
            recipeRegistrar.unregisterAll();
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
        if (packServer != null) {
            packServer.stop();
            packServer.start();
        }
        if (displays != null && !configManager.customHiveModel()) {
            displays.removeAll();
        }
        if (guardService != null) {
            guardService.stop();
            guardService.start();
        }
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

    public FrameItem frameItem() {
        return frameItem;
    }

    public NetItem netItem() {
        return netItem;
    }

    public ProductItem productItem() {
        return productItem;
    }

    public AtlasService atlas() {
        return atlas;
    }

    public PackServer packServer() {
        return packServer;
    }

    public HiveDisplayService displays() {
        return displays;
    }

    public GuardService guardService() {
        return guardService;
    }

    public Storage storage() {
        return storage;
    }

    public HiveManager hives() {
        return hiveManager;
    }
}
