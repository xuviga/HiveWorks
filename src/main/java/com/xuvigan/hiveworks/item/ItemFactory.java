package com.xuvigan.hiveworks.item;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.bee.Genes;
import com.xuvigan.hiveworks.bee.Species;
import com.xuvigan.hiveworks.hive.FrameType;
import com.xuvigan.hiveworks.hive.HiveTier;
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
        meta.displayName(mini.deserialize("<yellow>Пчела в банке</yellow> <dark_gray>•</dark_gray> " + species.displayName()));
        List<Component> lore = new ArrayList<>(List.of(
                mini.deserialize("<gray>Вид: " + species.displayName()),
                mini.deserialize("<gray>Продуктивность: <white>" + genes.productivity() + "</white>"),
                mini.deserialize("<gray>Скорость: <white>" + genes.speed() + "</white>"),
                mini.deserialize("<gray>Морозостойкость: <white>" + genes.cold() + "</white>"),
                mini.deserialize("<gray>Нрав: <white>" + genes.temper() + "</white>"),
                mini.deserialize("<dark_gray>Чистота линии: " + purity + "%")
        ));
        if (queen) {
            lore.add(0, mini.deserialize("<light_purple>Матка семьи</light_purple>"));
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

    public ItemStack honey(Quality quality, int amount) {
        ItemStack item = new ItemStack(Material.HONEY_BOTTLE, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mini.deserialize(quality.displayName() + " мёд"));
        meta.lore(List.of(mini.deserialize("<gray>Собран на пасеке HiveWorks.")));
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
        meta.displayName(mini.deserialize("<gold>" + type.displayName() + "</gold>"));
        meta.lore(List.of(
                mini.deserialize("<gray>Осталось сборов: <white>" + remaining + "/" + type.maxUses() + "</white>"),
                mini.deserialize("<gray>Эффект: <white>" + type.effectDescription() + "</white>"),
                mini.deserialize("<dark_gray>Вставьте в слот рамки улья.</dark_gray>")
        ));
        applyModel(meta, "frame_" + type.name().toLowerCase(Locale.ROOT), type.customModelData());

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(PdcKeys.KIND, PersistentDataType.STRING, "frame");
        pdc.set(PdcKeys.FRAME, PersistentDataType.STRING, type.name());
        pdc.set(PdcKeys.USES, PersistentDataType.INTEGER, remaining);

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack product(ProductType type, int amount) {
        ItemStack item = new ItemStack(type.material(), Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mini.deserialize(type.displayName()));
        meta.lore(List.of(mini.deserialize("<gray>Продукт пасеки HiveWorks.")));
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
        meta.displayName(mini.deserialize("<aqua>Сачок</aqua>"));
        meta.lore(List.of(
                mini.deserialize("<gray>ПКМ по дикой пчеле, чтобы поймать.</gray>"),
                mini.deserialize("<dark_gray>Прочность расходуется при ловле.</dark_gray>")
        ));
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
        meta.displayName(mini.deserialize("<gray>Дымарь</gray>"));
        meta.lore(List.of(
                mini.deserialize("<gray>Успокаивает пчёл при обслуживании улья.</gray>"),
                mini.deserialize("<dark_gray>Снижает шанс укуса.</dark_gray>")
        ));
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
        meta.displayName(mini.deserialize("<gold>" + tier.display() + "</gold>"));
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
