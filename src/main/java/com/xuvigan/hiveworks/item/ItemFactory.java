package com.xuvigan.hiveworks.item;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.bee.Genes;
import com.xuvigan.hiveworks.bee.Species;
import com.xuvigan.hiveworks.hive.FrameType;
import com.xuvigan.hiveworks.hive.HiveTier;
import com.xuvigan.hiveworks.product.MeadType;
import com.xuvigan.hiveworks.product.ProductType;
import com.xuvigan.hiveworks.production.Quality;
import com.xuvigan.hiveworks.util.Version;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds every HiveWorks item. Custom model data matches the resource pack plan.
 */
public final class ItemFactory {

    private final HiveWorksPlugin plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public ItemFactory(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public ItemStack beeJar(Species species, Genes genes, int purity) {
        return beeJar(UUID.randomUUID(), species, genes, purity);
    }

    public ItemStack beeJar(UUID id, Species species, Genes genes, int purity) {
        return beeJar(id, species, genes, purity, false);
    }

    public ItemStack beeJar(UUID id, Species species, Genes genes, int purity, boolean queen) {
        ItemStack item = new ItemStack(Material.GLASS_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        String speciesName = plugin.messages().speciesName(species);
        meta.displayName(mini.deserialize(plugin.messages().formatted("item-bee-jar-name", "species", speciesName)));
        List<Component> lore = plugin.messages().formattedList("item-bee-jar-lore",
                        "species", speciesName,
                        "productivity", genes.productivity(),
                        "speed", genes.speed(),
                        "cold", genes.cold(),
                        "temper", genes.temper(),
                        "purity", purity)
                .stream()
                .map(mini::deserialize)
                .collect(Collectors.toCollection(ArrayList::new));
        if (queen) {
            lore.add(0, mini.deserialize(plugin.messages().formatted("item-bee-jar-queen")));
        }
        meta.lore(lore);
        applyModel(meta, "bee_jar_" + species.id(), species.customModelData());
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(PdcKeys.KIND, PersistentDataType.STRING, "bee");
        pdc.set(PdcKeys.BEE_UUID, PersistentDataType.STRING, id.toString());
        pdc.set(PdcKeys.SPECIES, PersistentDataType.STRING, species.id());
        pdc.set(PdcKeys.GENES, PersistentDataType.STRING, genes.serialize());
        pdc.set(PdcKeys.PURITY, PersistentDataType.INTEGER, Math.max(0, Math.min(100, purity)));

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack queenItem(Species species, Genes genes, int purity) {
        return queenItem(UUID.randomUUID(), species, genes, purity);
    }

    public ItemStack queenItem(UUID id, Species species, Genes genes, int purity) {
        ItemStack item = new ItemStack(Material.GLASS_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        String speciesName = plugin.messages().speciesName(species);
        meta.displayName(mini.deserialize(plugin.messages().formatted("item-queen-name",
                "species", speciesName)));
        meta.lore(plugin.messages().formattedList("item-queen-lore",
                        "species", speciesName,
                        "productivity", genes.productivity(),
                        "speed", genes.speed(),
                        "cold", genes.cold(),
                        "temper", genes.temper(),
                        "purity", Math.max(0, Math.min(100, purity)))
                .stream()
                .map(mini::deserialize)
                .toList());
        applyModel(meta, "bee_jar_" + species.id(), species.customModelData());
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(PdcKeys.KIND, PersistentDataType.STRING, "queen");
        pdc.set(PdcKeys.BEE_UUID, PersistentDataType.STRING, id.toString());
        pdc.set(PdcKeys.SPECIES, PersistentDataType.STRING, species.id());
        pdc.set(PdcKeys.GENES, PersistentDataType.STRING, genes.serialize());
        pdc.set(PdcKeys.PURITY, PersistentDataType.INTEGER, Math.max(0, Math.min(100, purity)));
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack honey(Quality quality, int amount) {
        ItemStack item = new ItemStack(Material.HONEY_BOTTLE, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        String qualityName = plugin.messages().qualityName(quality);
        meta.displayName(mini.deserialize(plugin.messages().formatted("item-honey-name", "quality", qualityName)));
        meta.lore(plugin.messages().formattedList("item-honey-lore").stream()
                .map(mini::deserialize)
                .toList());
        applyModel(meta, "honey_" + quality.name().toLowerCase(Locale.ROOT), quality.customModelData());

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(PdcKeys.KIND, PersistentDataType.STRING, "honey");
        pdc.set(PdcKeys.QUALITY, PersistentDataType.STRING, quality.name());

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack frame(FrameType type, int usesLeft) {
        int remaining = Math.max(1, Math.min(type.maxUses(), usesLeft));
        ItemStack item = new ItemStack(Material.HONEYCOMB);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mini.deserialize(plugin.messages().formatted("item-frame-name",
                "frame", plugin.messages().frameName(type))));
        meta.lore(plugin.messages().formattedList("item-frame-lore",
                        "uses", remaining,
                        "max", type.maxUses(),
                        "effect", plugin.messages().frameEffect(type))
                .stream()
                .map(mini::deserialize)
                .toList());
        applyModel(meta, "frame_" + type.name().toLowerCase(Locale.ROOT), type.customModelData());

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(PdcKeys.KIND, PersistentDataType.STRING, "frame");
        pdc.set(PdcKeys.FRAME, PersistentDataType.STRING, type.name());
        pdc.set(PdcKeys.USES, PersistentDataType.INTEGER, remaining);

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack mead(MeadType type) {
        ItemStack item = new ItemStack(Material.HONEY_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mini.deserialize(plugin.messages().formatted("item-mead-name",
                "mead", plugin.messages().raw("mead." + type.id() + ".name"))));
        meta.lore(plugin.messages().formattedList("item-mead-lore").stream()
                .map(mini::deserialize)
                .toList());
        applyModel(meta, "mead_" + type.id(), type.customModelData());
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(PdcKeys.KIND, PersistentDataType.STRING, "mead");
        pdc.set(PdcKeys.PRODUCT, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack regenerationPotion() {
        ItemStack item = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta meta =
                (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
        meta.addCustomEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.REGENERATION, 20 * 20, 0), true);
        meta.displayName(mini.deserialize(plugin.messages().formatted("item-potion-name")));
        meta.lore(plugin.messages().formattedList("item-potion-lore").stream()
                .map(mini::deserialize)
                .toList());
        applyModel(meta, "potion_regeneration", 2120);
        meta.getPersistentDataContainer().set(PdcKeys.KIND, PersistentDataType.STRING, "potion");
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack product(ProductType type, int amount) {
        ItemStack item = new ItemStack(type.material(), Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mini.deserialize(plugin.messages().formatted("item-product-name",
                "product", plugin.messages().productName(type))));
        meta.lore(plugin.messages().formattedList("item-product-lore").stream()
                .map(mini::deserialize)
                .toList());
        applyModel(meta, "product_" + type.name().toLowerCase(Locale.ROOT), type.customModelData());

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(PdcKeys.KIND, PersistentDataType.STRING, "product");
        pdc.set(PdcKeys.PRODUCT, PersistentDataType.STRING, type.name());

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack net() {
        ItemStack item = new ItemStack(Material.BRUSH);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mini.deserialize(plugin.messages().formatted("item-net-name")));
        meta.lore(plugin.messages().formattedList("item-net-lore").stream()
                .map(mini::deserialize)
                .toList());
        applyModel(meta, "net", 2101);
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
        }
        meta.getPersistentDataContainer().set(PdcKeys.KIND, PersistentDataType.STRING, "net");
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack smoker() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mini.deserialize(plugin.messages().formatted("item-smoker-name")));
        meta.lore(plugin.messages().formattedList("item-smoker-lore").stream()
                .map(mini::deserialize)
                .toList());
        applyModel(meta, "smoker", 2102);
        if (meta instanceof Damageable damageable) {
            damageable.setDamage(0);
        }
        meta.getPersistentDataContainer().set(PdcKeys.KIND, PersistentDataType.STRING, "smoker");
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack hiveDisplay(HiveTier tier) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mini.deserialize(plugin.messages().formatted("item-hive-display-name",
                "tier", plugin.messages().tierName(tier))));
        applyModel(meta, tier.modelName(), tier.customModelData());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack icon(Material material, String name, List<String> lore, int customModelData) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mini.deserialize(name));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream().map(mini::deserialize).collect(Collectors.toList()));
        }
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack pane(Material material) {
        return icon(material, "<dark_gray> </dark_gray>", List.of(), 0);
    }

    public Component component(String miniMessage) {
        return mini.deserialize(miniMessage);
    }

    private void applyModel(ItemMeta meta, String model, int legacyCustomModelData) {
        if (Version.isAtLeast(Version.minecraftVersion(), "1.21.4")
                && ItemModels.setItemModel(meta, new NamespacedKey(plugin, model))) {
            return;
        }
        meta.setCustomModelData(legacyCustomModelData);
    }
}
