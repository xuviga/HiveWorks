package com.xuvigan.hiveworks;

import com.xuvigan.hiveworks.apiary.ApiaryService;
import com.xuvigan.hiveworks.atlas.AtlasRewardRegistry;
import com.xuvigan.hiveworks.atlas.AtlasService;
import com.xuvigan.hiveworks.bee.BeeItem;
import com.xuvigan.hiveworks.boss.QueenBossService;
import com.xuvigan.hiveworks.bee.SpeciesRegistry;
import com.xuvigan.hiveworks.command.HiveCommand;
import com.xuvigan.hiveworks.config.ConfigManager;
import com.xuvigan.hiveworks.config.Messages;
import com.xuvigan.hiveworks.craft.RecipeRegistrar;
import com.xuvigan.hiveworks.gui.GuiListener;
import com.xuvigan.hiveworks.guard.GuardService;
import com.xuvigan.hiveworks.guard.GuardTargetListener;
import com.xuvigan.hiveworks.hive.FrameRegistry;
import com.xuvigan.hiveworks.hive.HiveListener;
import com.xuvigan.hiveworks.hive.HiveManager;
import com.xuvigan.hiveworks.hive.HiveTicker;
import com.xuvigan.hiveworks.integration.HiveWorksPlaceholders;
import com.xuvigan.hiveworks.item.FrameItem;
import com.xuvigan.hiveworks.item.ItemFactory;
import com.xuvigan.hiveworks.item.MeadItem;
import com.xuvigan.hiveworks.item.NetItem;
import com.xuvigan.hiveworks.item.PdcKeys;
import com.xuvigan.hiveworks.item.ProductItem;
import com.xuvigan.hiveworks.item.QueenItem;
import com.xuvigan.hiveworks.item.SmokerItem;
import com.xuvigan.hiveworks.product.MeadListener;
import com.xuvigan.hiveworks.product.ProductRegistry;
import com.xuvigan.hiveworks.raid.HiveRaidService;
import com.xuvigan.hiveworks.resourcepack.PackServer;
import com.xuvigan.hiveworks.resourcepack.ResourcePackListener;
import com.xuvigan.hiveworks.storage.Storage;
import com.xuvigan.hiveworks.storage.StorageFactory;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import com.xuvigan.hiveworks.util.Version;
import com.xuvigan.hiveworks.visual.HiveDisplayService;
import com.xuvigan.hiveworks.world.WildBeeListener;
import com.xuvigan.hiveworks.world.WildSwarmSpawner;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * HiveWorks entry point.
 *
 * <p>Cozy beekeeping for Paper 1.21.x - 26.x and Folia. No hard dependencies.</p>
 */
public final class HiveWorksPlugin extends JavaPlugin {

    private SchedulerAdapter schedulers;
    private ConfigManager configManager;
    private Messages messages;
    private FrameRegistry frameRegistry;
    private ProductRegistry productRegistry;
    private SpeciesRegistry speciesRegistry;
    private ItemFactory itemFactory;
    private BeeItem beeItem;
    private FrameItem frameItem;
    private NetItem netItem;
    private SmokerItem smokerItem;
    private MeadItem meadItem;
    private QueenItem queenItem;
    private ProductItem productItem;
    private AtlasService atlas;
    private AtlasRewardRegistry atlasRewards;
    private RecipeRegistrar recipeRegistrar;
    private WildSwarmSpawner wildSwarmSpawner;
    private PackServer packServer;
    private HiveDisplayService displays;
    private GuardService guardService;
    private HiveRaidService raidService;
    private QueenBossService queenBoss;
    private ApiaryService apiaries;
    private Storage storage;
    private HiveManager hiveManager;
    private HiveTicker ticker;

    @Override
    public void onEnable() {
        this.schedulers = new SchedulerAdapter(this);
        PdcKeys.init(this);
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.messages = new Messages(this);
        this.frameRegistry = new FrameRegistry(this);
        this.productRegistry = new ProductRegistry(this);
        this.itemFactory = new ItemFactory(this);
        this.beeItem = new BeeItem();
        this.frameItem = new FrameItem();
        this.netItem = new NetItem();
        this.smokerItem = new SmokerItem();
        this.meadItem = new MeadItem();
        this.queenItem = new QueenItem();
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

        this.apiaries = new ApiaryService(this);
        this.apiaries.load();

        this.atlasRewards = new AtlasRewardRegistry(this);
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

        this.raidService = new HiveRaidService(this);
        this.raidService.start();

        this.queenBoss = new QueenBossService(this);
        this.queenBoss.start();

        getServer().getPluginManager().registerEvents(new HiveListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new WildBeeListener(this), this);
        getServer().getPluginManager().registerEvents(new GuardTargetListener(this), this);
        getServer().getPluginManager().registerEvents(new ResourcePackListener(this), this);
        getServer().getPluginManager().registerEvents(new MeadListener(this), this);
        getServer().getPluginManager().registerEvents(queenBoss, this);

        HiveCommand command = new HiveCommand(this);
        PluginCommand pluginCommand = getCommand("hive");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new HiveWorksPlaceholders(this).register();
                getLogger().info("PlaceholderAPI expansion registered.");
            } catch (Throwable throwable) {
                getLogger().warning("PlaceholderAPI expansion failed: " + throwable.getMessage());
            }
        }

        getLogger().info("HiveWorks " + getPluginMeta().getVersion() + " enabled on "
                + (Version.isFolia() ? "Folia (regionised)" : "Paper (main thread)")
                + ": " + speciesRegistry.all().size() + " species, " + hiveManager.all().size() + " hives.");
    }

    @Override
    public void onDisable() {
        safely("stop queen boss", () -> {
            if (queenBoss != null) {
                queenBoss.stop();
            }
        });
        safely("stop raids", () -> {
            if (raidService != null) {
                raidService.stop();
            }
        });
        safely("stop guards", () -> {
            if (guardService != null) {
                guardService.stop();
            }
        });
        safely("remove displays", () -> {
            if (displays != null) {
                displays.removeAll();
            }
        });
        safely("stop pack server", () -> {
            if (packServer != null) {
                packServer.stop();
            }
        });
        safely("stop wild swarms", () -> {
            if (wildSwarmSpawner != null) {
                wildSwarmSpawner.stop();
            }
        });
        safely("stop ticker", () -> {
            if (ticker != null) {
                ticker.stop();
            }
        });
        safely("unregister recipes", () -> {
            if (recipeRegistrar != null) {
                recipeRegistrar.unregisterAll();
            }
        });
        // Saving must happen even if one of the shutdown hooks above failed.
        safely("save hives", () -> {
            if (hiveManager != null) {
                hiveManager.saveAll();
            }
        });
        safely("cancel tasks", () -> {
            if (schedulers != null) {
                schedulers.cancelTasks();
            }
        });
        safely("close storage", () -> {
            if (storage != null) {
                storage.close();
            }
        });
    }

    private void safely(String what, Runnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            getLogger().warning("Shutdown step failed (" + what + "): " + throwable);
        }
    }

    /** Reloads config, messages and species without touching stored hives. */
    public void reloadAll() {
        configManager.reload();
        messages.reload();
        frameRegistry.reload();
        productRegistry.reload();
        speciesRegistry.reload();
        atlasRewards.reload();
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
        if (raidService != null) {
            raidService.stop();
            raidService.start();
        }
    }

    public ConfigManager configManager() {
        return configManager;
    }

    /** Folia-aware scheduler seam shared by every service. */
    public SchedulerAdapter schedulers() {
        return schedulers;
    }

    public Messages messages() {
        return messages;
    }

    public FrameRegistry frames() {
        return frameRegistry;
    }

    public ProductRegistry products() {
        return productRegistry;
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

    public SmokerItem smokerItem() {
        return smokerItem;
    }

    public MeadItem meadItem() {
        return meadItem;
    }

    public QueenItem queenItem() {
        return queenItem;
    }

    public ProductItem productItem() {
        return productItem;
    }

    public AtlasService atlas() {
        return atlas;
    }

    public AtlasRewardRegistry atlasRewards() {
        return atlasRewards;
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

    public HiveRaidService raids() {
        return raidService;
    }

    public QueenBossService queenBoss() {
        return queenBoss;
    }

    public ApiaryService apiaries() {
        return apiaries;
    }

    public Storage storage() {
        return storage;
    }

    public HiveManager hives() {
        return hiveManager;
    }
}
