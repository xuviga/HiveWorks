package com.xuvigan.hiveworks.product;

import org.bukkit.Material;

/**
 * Non-honey hive products.
 */
public enum ProductType {

    COMB("<yellow>Соты</yellow>", Material.HONEYCOMB, 2010),
    WAX("<gold>Воск</gold>", Material.HONEYCOMB, 2011),
    PROPOLIS("<dark_green>Прополис</dark_green>", Material.GREEN_DYE, 2012),
    ROYAL_JELLY("<light_purple>Маточное молочко</light_purple>", Material.HONEY_BOTTLE, 2013),
    POLLEN("<yellow>Пыльца</yellow>", Material.YELLOW_DYE, 2014);

    private final String displayName;
    private final Material material;
    private final int customModelData;

    ProductType(String displayName, Material material, int customModelData) {
        this.displayName = displayName;
        this.material = material;
        this.customModelData = customModelData;
    }

    public String displayName() {
        return displayName;
    }

    public Material material() {
        return material;
    }

    public int customModelData() {
        return customModelData;
    }
}
