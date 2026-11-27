package com.xuvigan.hiveworks.hive;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.api.HiveHarvestEvent;
import com.xuvigan.hiveworks.bee.BeeItemData;
import com.xuvigan.hiveworks.bee.BreedingService;
import com.xuvigan.hiveworks.bee.QueenItemData;
import com.xuvigan.hiveworks.bee.Species;
import com.xuvigan.hiveworks.production.FlowerScanner;
import com.xuvigan.hiveworks.production.Quality;
import com.xuvigan.hiveworks.production.QualityCalculator;
import com.xuvigan.hiveworks.product.ProductType;
import com.xuvigan.hiveworks.util.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hive registry, limits, bee placement and harvesting.
 */
public final class HiveManager {

    public enum BeeAddResult {
        ADDED,
        SLOT_OCCUPIED,
        INVALID_SLOT,
        DUPLICATE
    }

    public enum UpgradeResult {
        UPGRADED,
        MAX_TIER,
        DISABLED,
        MISSING_ITEMS
    }

    public enum BreedingStartResult {
        STARTED,
        DISABLED,
        ALREADY,
        NEED_BEES,
        NO_SPACE,
        NEED_CELL
    }

    public enum QueenInstallStatus {
        INSTALLED,
        REPLACED,
        NO_SPACE,
        DUPLICATE
    }

    public record QueenInstallResult(QueenInstallStatus status, BeeSlot replaced) {
    }

    public record UpgradeCost(ProductType product, int productAmount, Material material, int materialAmount) {
    }

    private final HiveWorksPlugin plugin;
    private final Map<String, HiveData> hives = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> ownerCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> chunkCounts = new ConcurrentHashMap<>();
    private final Set<UUID> beeIds = ConcurrentHashMap.newKeySet();
    private final BreedingService breedingService = new BreedingService();
    private final Random random = new Random();

    public HiveManager(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        for (HiveData hive : plugin.storage().loadAll()) {
            if (normalizeQueen(hive)) {
                plugin.storage().save(hive);
            }
            hives.put(hive.pos().key(), hive);
            index(hive);
            for (int slot = 0; slot < HiveData.MAX_BEES; slot++) {
                BeeSlot bee = hive.bee(slot);
                if (bee != null && !beeIds.add(bee.beeId())) {
                    plugin.getLogger().warning("Duplicate bee UUID " + bee.beeId()
                            + " in hive " + hive.pos().key());
                }
            }
        }
        plugin.getLogger().info("Loaded " + hives.size() + " hives.");
    }

    public void saveAll() {
        for (HiveData hive : hives.values()) {
            plugin.storage().save(hive);
        }
    }

    public void save(HiveData hive) {
        plugin.storage().save(hive);
    }

    public Collection<HiveData> all() {
        return Collections.unmodifiableCollection(hives.values());
    }

    public int count() {
        return hives.size();
    }

    public HiveData get(Block block) {
        return hives.get(HivePos.from(block).key());
    }

    public HiveData byKey(String key) {
        return hives.get(key);
    }

    public Optional<HiveData> findNearest(Player player, double radius) {
        HiveData best = null;
        double bestDistance = radius * radius;
        for (HiveData hive : hives.values()) {
            if (!hive.pos().world().equals(player.getWorld().getName())) {
                continue;
            }
            double dx = (hive.pos().x() + 0.5) - player.getLocation().getX();
            double dy = (hive.pos().y() + 0.5) - player.getLocation().getY();
            double dz = (hive.pos().z() + 0.5) - player.getLocation().getZ();
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= bestDistance) {
                best = hive;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    public Optional<HiveData> findNearestOwned(Player player, double radius) {
        HiveData best = null;
        double bestDistance = radius * radius;
        for (HiveData hive : hives.values()) {
            if (!hive.owner().equals(player.getUniqueId())
                    && !player.hasPermission("hiveworks.admin")) {
                continue;
            }
            if (!hive.pos().world().equals(player.getWorld().getName())) {
                continue;
            }
            double dx = (hive.pos().x() + 0.5) - player.getLocation().getX();
            double dy = (hive.pos().y() + 0.5) - player.getLocation().getY();
            double dz = (hive.pos().z() + 0.5) - player.getLocation().getZ();
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= bestDistance) {
                best = hive;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    public boolean canManage(Player player, HiveData hive) {
        return hive.owner().equals(player.getUniqueId())
                || player.hasPermission("hiveworks.admin");
    }

    public HiveData create(Block block, Player player) {
        HivePos pos = HivePos.from(block);
        HiveData existing = hives.get(pos.key());
        if (existing != null) {
            return existing;
        }
        if (plugin.configManager().isWorldBlacklisted(pos.world())
                && !player.hasPermission("hiveworks.bypass.world")) {
            plugin.messages().send(player, "hive-world-blocked");
            return null;
        }

        int limit = limitFor(player);
        if (ownerCount(player.getUniqueId()) >= limit) {
            plugin.messages().send(player, "hive-limit", "limit", limit);
            return null;
        }
        if (chunkCount(pos.chunkKey()) >= plugin.configManager().hivesPerChunk()) {
            plugin.messages().send(player, "hive-limit", "limit", plugin.configManager().hivesPerChunk());
            return null;
        }

        HiveData hive = new HiveData(pos, player.getUniqueId());
        hive.setQueenBound(true);
        hives.put(pos.key(), hive);
        index(hive);
        if (plugin.apiaries() != null) {
            plugin.apiaries().invalidate();
        }
        plugin.storage().save(hive);
        plugin.messages().send(player, "hive-created",
                "bees", hive.occupiedBees(),
                "slots", hive.tier().beeSlots());
        return hive;
    }

    public void remove(HiveData hive) {
        hives.remove(hive.pos().key());
        unindex(hive);
        for (int slot = 0; slot < HiveData.MAX_BEES; slot++) {
            BeeSlot bee = hive.bee(slot);
            if (bee != null) {
                beeIds.remove(bee.beeId());
            }
        }
        if (plugin.apiaries() != null) {
            plugin.apiaries().invalidate();
        }
        plugin.storage().delete(hive.pos());
    }

    public BeeAddResult addBee(HiveData hive, int slot, BeeItemData data) {
        if (slot < 0 || slot >= hive.tier().beeSlots() || slot >= HiveData.MAX_BEES) {
            return BeeAddResult.INVALID_SLOT;
        }
        if (hive.bee(slot) != null) {
            return BeeAddResult.SLOT_OCCUPIED;
        }
        if (!beeIds.add(data.id())) {
            return BeeAddResult.DUPLICATE;
        }
        hive.setBee(slot, new BeeSlot(data.id(), data.speciesId(), data.genes(), data.purity()));
        hive.setQueenBound(true);
        if (hive.queenSlot() < 0) {
            hive.setQueenSlot(slot);
            hive.setQueenCycles(plugin.configManager().queenMaxCycles());
        }
        plugin.storage().save(hive);
        return BeeAddResult.ADDED;
    }

    /** Adds a bred bee into the first free slot. */
    public boolean addBredBee(HiveData hive, BeeSlot bee) {
        int slot = hive.firstFreeBeeSlot();
        if (slot < 0 || !beeIds.add(bee.beeId())) {
            return false;
        }
        hive.setBee(slot, bee);
        hive.setQueenBound(true);
        if (hive.queenSlot() < 0) {
            hive.setQueenSlot(slot);
            hive.setQueenCycles(plugin.configManager().queenMaxCycles());
        }
        plugin.storage().save(hive);
        return true;
    }

    public boolean isBeeIdInUse(UUID beeId) {
        return beeIds.contains(beeId);
    }

    /**
     * Installs a Queen item into the hive. If the hive already has a queen, the
     * old queen is returned so the caller can give the item back.
     */
    public QueenInstallResult installQueen(HiveData hive, QueenItemData data) {
        int slot = hive.queenSlot();
        if (slot < 0) {
            slot = hive.firstFreeBeeSlot();
        }
        if (slot < 0) {
            return new QueenInstallResult(QueenInstallStatus.NO_SPACE, null);
        }
        BeeSlot old = hive.bee(slot);
        if (old != null && old.beeId().equals(data.id())) {
            return new QueenInstallResult(QueenInstallStatus.DUPLICATE, null);
        }
        if (!beeIds.add(data.id())) {
            return new QueenInstallResult(QueenInstallStatus.DUPLICATE, null);
        }
        if (old != null) {
            beeIds.remove(old.beeId());
        }
        hive.setBee(slot, new BeeSlot(data.id(), data.speciesId(), data.genes(), data.purity()));
        hive.setQueenSlot(slot);
        hive.setQueenCycles(plugin.configManager().queenMaxCycles());
        hive.setQueenBound(true);
        plugin.storage().save(hive);
        return new QueenInstallResult(old == null
                ? QueenInstallStatus.INSTALLED : QueenInstallStatus.REPLACED, old);
    }

    public BeeSlot removeBee(HiveData hive, int slot) {
        BeeSlot bee = hive.bee(slot);
        if (bee == null) {
            return null;
        }
        hive.setBee(slot, null);
        beeIds.remove(bee.beeId());
        if (slot == hive.queenSlot()) {
            hive.setQueenSlot(-1);
            hive.setQueenCycles(0);
            hive.setQueenBound(true);
        }
        plugin.storage().save(hive);
        return bee;
    }

    /**
     * Feeds royal jelly to the queen.
     *
     * @return new cycle count, -1 when there is no queen
     */
    public int feedQueen(HiveData hive) {
        if (!hive.hasQueen()) {
            return -1;
        }
        int max = plugin.configManager().queenMaxCyclesCap();
        int next = Math.min(max, hive.queenCycles() + plugin.configManager().queenRoyalJellyCycles());
        hive.setQueenCycles(next);
        plugin.storage().save(hive);
        return next;
    }

    public Quality harvest(Player player, HiveData hive) {
        if (!canManage(player, hive)) {
            plugin.messages().send(player, "hive-not-owner");
            return null;
        }
        int levels = (int) Math.floor(hive.honey());
        if (levels <= 0) {
            plugin.messages().send(player, "hive-empty");
            return null;
        }
        World world = Bukkit.getWorld(hive.pos().world());
        if (world == null) {
            return null;
        }
        Block block = world.getBlockAt(hive.pos().x(), hive.pos().y(), hive.pos().z());
        FlowerScanner.Result flowers = scanFlowers(block, hive);

        QualityCalculator calculator = new QualityCalculator(
                plugin.configManager().weightFlowers(),
                plugin.configManager().weightFrames(),
                plugin.configManager().weightPurity(),
                plugin.configManager().goodThreshold(),
                plugin.configManager().eliteThreshold(),
                plugin.configManager().legendaryThreshold()
        );
        double score = calculator.score(
                flowers.species(),
                flowers.rare(),
                hive.frameQualityWeight(),
                hive.averagePurity(),
                hive.frameQualityBonus() + hive.tier().qualityBonus()
        );
        score = Math.min(1.0, score + plugin.apiaries().qualityBonus(hive) + hive.pollenBonus());
        Quality quality = calculator.qualityOf(score);

        HiveHarvestEvent event = new HiveHarvestEvent(hive, player, levels, quality);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return null;
        }

        hive.setHoney(Math.max(0.0, hive.honey() - levels));
        hive.setPollenBonus(0.0);
        hive.setLastTick(System.currentTimeMillis());
        wearFrames(hive);
        tickQueen(player, hive);

        boolean shears = player.getInventory().getItemInMainHand().getType() == Material.SHEARS;
        if (shears) {
            giveItem(player, plugin.itemFactory().product(ProductType.COMB, levels));
            plugin.messages().send(player, "hive-harvested-combs", "amount", levels);
        } else {
            int remaining = levels;
            while (remaining > 0) {
                int amount = Math.min(64, remaining);
                giveItem(player, plugin.itemFactory().honey(quality, amount));
                remaining -= amount;
            }
            plugin.messages().send(player, "hive-harvested",
                    "amount", levels,
                    "quality", plugin.messages().qualityName(quality));
        }
        rollByproducts(player, hive);
        plugin.storage().recordHarvest(player.getUniqueId(), player.getName(), levels, quality);
        applySting(player, hive);
        plugin.storage().save(hive);
        return quality;
    }

    /**
     * Flower scan is only legal on the hive region. When a player interacts from
     * another region we fall back to the snapshot refreshed by HiveTicker.
     */
    private FlowerScanner.Result scanFlowers(Block block, HiveData hive) {
        if (SchedulerAdapter.ownedNow(block)) {
            return FlowerScanner.scan(block, 8);
        }
        return new FlowerScanner.Result(hive.cachedFlowerSpecies(), 0, hive.cachedFlowerRare(),
                Collections.emptySet());
    }

    private void applySting(Player player, HiveData hive) {        if (!plugin.configManager().stingEnabled()
                || player.getGameMode() == org.bukkit.GameMode.CREATIVE
                || hive.occupiedBees() == 0) {
            return;
        }
        double reduction = 0.0;
        for (int slot = 0; slot < HiveData.MAX_FRAMES; slot++) {
            FrameSlot frame = hive.frame(slot);
            if (frame != null) {
                reduction += frame.type().stingReduction();
            }
        }
        reduction = Math.min(0.9, reduction);
        double smokerFactor = hasSmoker(player) ? 0.5 : 1.0;
        double temper = hive.averageTemper() / 100.0;
        double chance = plugin.configManager().stingBaseChance()
                * (0.25 + 0.75 * temper)
                * (1.0 - reduction)
                * smokerFactor;
        if (random.nextDouble() < chance) {
            player.damage(plugin.configManager().stingDamage());
            plugin.messages().send(player, "hive-stung");
        }
    }

    private boolean hasSmoker(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (plugin.smokerItem().isSmoker(item)) {
                return true;
            }
        }
        return plugin.smokerItem().isSmoker(player.getInventory().getItemInOffHand());
    }

    private void rollByproducts(Player player, HiveData hive) {
        for (int index = 0; index < hive.frameCount(FrameType.PROPOLIS); index++) {
            if (random.nextDouble() < 0.25) {
                giveItem(player, plugin.itemFactory().product(ProductType.PROPOLIS, 1));
            }
        }
        int royalFrames = hive.frameCount(FrameType.ROYAL);
        if (royalFrames > 0 && random.nextDouble() < Math.min(0.50, royalFrames * 0.10)) {
            giveItem(player, plugin.itemFactory().product(ProductType.ROYAL_JELLY, 1));
        }
        if (hive.mode() == HiveMode.COLLECTION) {
            giveItem(player, plugin.itemFactory().product(ProductType.POLLEN, 1));
        }
    }

    private void giveItem(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
    }

    private void wearFrames(HiveData hive) {
        for (int slot = 0; slot < HiveData.MAX_FRAMES; slot++) {
            FrameSlot frame = hive.frame(slot);
            if (frame != null) {
                hive.setFrame(slot, frame.wear());
            }
        }
    }

    private void tickQueen(Player player, HiveData hive) {
        if (!hive.hasQueen()) {
            return;
        }
        int next = hive.queenCycles() - 1;
        if (next > 0) {
            hive.setQueenCycles(next);
            return;
        }
        BeeSlot queen = hive.queen();
        if (queen != null) {
            beeIds.remove(queen.beeId());
        }
        hive.setBee(hive.queenSlot(), null);
        hive.setQueenSlot(-1);
        hive.setQueenCycles(0);
        hive.setQueenBound(true);
        plugin.messages().send(player, "queen-died");
    }

    private boolean normalizeQueen(HiveData hive) {
        if (hive.queenSlot() >= 0 && hive.bee(hive.queenSlot()) != null) {
            boolean changed = false;
            if (!hive.queenBound()) {
                hive.setQueenBound(true);
                changed = true;
            }
            if (hive.queenCycles() <= 0) {
                hive.setQueenCycles(plugin.configManager().queenMaxCycles());
                changed = true;
            }
            return changed;
        }
        if (!hive.queenBound()) {
            // Legacy 0.1 hive: promote the first bee once and remember the migration.
            hive.setQueenBound(true);
            for (int slot = 0; slot < HiveData.MAX_BEES; slot++) {
                if (hive.bee(slot) != null) {
                    hive.setQueenSlot(slot);
                    hive.setQueenCycles(plugin.configManager().queenMaxCycles());
                    break;
                }
            }
            return true;
        }
        boolean changed = hive.queenSlot() != -1 || hive.queenCycles() != 0;
        hive.setQueenSlot(-1);
        hive.setQueenCycles(0);
        return changed;
    }

    /** Drops all stored bees, frames and honey before the hive record is deleted. */
    public void dropContents(HiveData hive, Location location) {
        if (location.getWorld() == null) {
            return;
        }
        List<ItemStack> drops = new ArrayList<>();
        for (int slot = 0; slot < HiveData.MAX_BEES; slot++) {
            BeeSlot bee = hive.bee(slot);
            if (bee == null) {
                continue;
            }
            Optional<Species> species = plugin.speciesRegistry().byId(bee.speciesId());
            if (species.isPresent()) {
                drops.add(slot == hive.queenSlot()
                        ? plugin.itemFactory().queenItem(bee.beeId(), species.get(), bee.genes(), bee.purity())
                        : plugin.itemFactory().beeJar(bee.beeId(), species.get(), bee.genes(), bee.purity()));
            }
        }
        for (int slot = 0; slot < HiveData.MAX_FRAMES; slot++) {
            FrameSlot frame = hive.frame(slot);
            if (frame != null) {
                drops.add(plugin.itemFactory().frame(frame.type(), frame.usesLeft()));
            }
        }
        int remaining = (int) Math.floor(hive.honey());
        while (remaining > 0) {
            int amount = Math.min(64, remaining);
            drops.add(plugin.itemFactory().honey(Quality.ORDINARY, amount));
            remaining -= amount;
        }
        if (drops.isEmpty()) {
            return;
        }
        if (SchedulerAdapter.ownedNow(location)) {
            dropAll(location, drops);
        } else {
            plugin.schedulers().executeAt(location, () -> dropAll(location, drops));
        }
    }

    private void dropAll(Location location, List<ItemStack> drops) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        for (ItemStack drop : drops) {
            world.dropItemNaturally(location, drop);
        }
    }

    public UpgradeResult upgrade(Player player, HiveData hive) {
        if (!plugin.configManager().upgradeEnabled()) {
            return UpgradeResult.DISABLED;
        }
        HiveTier next = hive.tier().next();
        if (next == null) {
            return UpgradeResult.MAX_TIER;
        }
        UpgradeCost cost = upgradeCost(hive.tier());
        if (cost == null) {
            return UpgradeResult.MAX_TIER;
        }
        ItemStack product = plugin.itemFactory().product(cost.product(), cost.productAmount());
        ItemStack material = new ItemStack(cost.material(), cost.materialAmount());
        if (!player.getInventory().containsAtLeast(product, cost.productAmount())
                || !player.getInventory().containsAtLeast(material, cost.materialAmount())) {
            return UpgradeResult.MISSING_ITEMS;
        }
        player.getInventory().removeItem(product);
        player.getInventory().removeItem(material);
        hive.setTier(next);
        plugin.storage().save(hive);
        plugin.displays().ensure(hive);
        plugin.messages().send(player, "hive-upgraded",
                "tier", plugin.messages().tierName(next));
        return UpgradeResult.UPGRADED;
    }

    public UpgradeCost upgradeCost(HiveTier tier) {
        return switch (tier) {
            case I -> new UpgradeCost(ProductType.WAX, 8, Material.SPRUCE_PLANKS, 16);
            case II -> new UpgradeCost(ProductType.WAX, 12, Material.DARK_OAK_PLANKS, 16);
            case III -> new UpgradeCost(ProductType.WAX, 16, Material.AMETHYST_SHARD, 16);
            case IV -> null;
        };
    }

    public BreedingStartResult startBreeding(Player player, HiveData hive) {
        if (!plugin.configManager().breedingEnabled()) {
            return BreedingStartResult.DISABLED;
        }
        long now = System.currentTimeMillis();
        if (hive.breedingActive(now)) {
            return BreedingStartResult.ALREADY;
        }
        int first = -1;
        int second = -1;
        for (int slot = 0; slot < HiveData.MAX_BEES; slot++) {
            if (hive.bee(slot) == null) {
                continue;
            }
            if (first < 0) {
                first = slot;
            } else {
                second = slot;
                break;
            }
        }
        if (first < 0 || second < 0) {
            return BreedingStartResult.NEED_BEES;
        }
        if (hive.firstFreeBeeSlot() < 0) {
            return BreedingStartResult.NO_SPACE;
        }
        int cellCost = plugin.configManager().breedingRoyalCellCost();
        if (cellCost > 0) {
            ItemStack cell = plugin.itemFactory().product(ProductType.ROYAL_CELL, cellCost);
            if (!player.getInventory().containsAtLeast(cell, cellCost)) {
                return BreedingStartResult.NEED_CELL;
            }
            player.getInventory().removeItem(cell);
        }
        long readyAt = now + plugin.configManager().breedingMinutes() * 60_000L;
        hive.startBreeding(first, second, readyAt);
        plugin.storage().save(hive);
        plugin.messages().send(player, "breeding-started",
                "minutes", plugin.configManager().breedingMinutes());
        return BreedingStartResult.STARTED;
    }

    /** Advances all breeding timers. Called by HiveTicker. */
    public void tickBreeding(long now) {
        for (HiveData hive : hives.values()) {
            if (!hive.breedingReady(now)) {
                continue;
            }
            int first = hive.breedSlotA();
            int second = hive.breedSlotB();
            BeeSlot parentA = hive.bee(first);
            BeeSlot parentB = hive.bee(second);
            if (parentA == null || parentB == null) {
                hive.clearBreeding();
                plugin.storage().save(hive);
                notifyOwner(hive, "breeding-failed-parents");
                continue;
            }
            if (hive.firstFreeBeeSlot() < 0) {
                hive.clearBreeding();
                plugin.storage().save(hive);
                notifyOwner(hive, "breeding-no-space");
                continue;
            }
            Optional<BeeSlot> child = breedingService.breed(hive, first, second, random,
                    hive.tier().rareChance(), plugin.speciesRegistry());
            if (child.isEmpty() || !addBredBee(hive, child.get())) {
                hive.clearBreeding();
                plugin.storage().save(hive);
                notifyOwner(hive, "breeding-failed");
                continue;
            }
            hive.clearBreeding();
            plugin.storage().save(hive);
            String speciesId = child.get().speciesId();
            plugin.speciesRegistry().byId(speciesId).ifPresent(species ->
                    notifyOwner(hive, "breeding-done", "species", plugin.messages().speciesName(species)));
            Player owner = Bukkit.getPlayer(hive.owner());
            if (owner != null && owner.isOnline()) {
                plugin.atlas().discover(owner, speciesId);
            } else {
                plugin.atlas().discover(hive.owner(), "", speciesId);
            }
        }
    }

    private void notifyOwner(HiveData hive, String key, Object... placeholders) {
        Player owner = Bukkit.getPlayer(hive.owner());
        if (owner != null && owner.isOnline()) {
            plugin.messages().send(owner, key, placeholders);
        }
    }

    private int limitFor(Player player) {
        int limit = plugin.configManager().hivesPerPlayer();
        for (int value = limit + 1; value <= 512; value++) {
            if (player.hasPermission("hiveworks.limit." + value)) {
                limit = value;
            }
        }
        return limit;
    }

    private int ownerCount(UUID owner) {
        return ownerCounts.getOrDefault(owner, 0);
    }

    private int chunkCount(String chunkKey) {
        return chunkCounts.getOrDefault(chunkKey, 0);
    }

    private void index(HiveData hive) {
        ownerCounts.merge(hive.owner(), 1, Integer::sum);
        chunkCounts.merge(hive.pos().chunkKey(), 1, Integer::sum);
    }

    private void unindex(HiveData hive) {
        ownerCounts.computeIfPresent(hive.owner(), (key, value) -> value <= 1 ? null : value - 1);
        chunkCounts.computeIfPresent(hive.pos().chunkKey(), (key, value) -> value <= 1 ? null : value - 1);
    }

    public static boolean isHiveBlock(Material material) {
        return material == Material.BEEHIVE || material == Material.BEE_NEST;
    }
}
