package com.xuvigan.hiveworks.atlas;

import com.xuvigan.hiveworks.item.ItemFactory;
import com.xuvigan.hiveworks.product.ProductType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * One atlas completion reward loaded from atlas.yml.
 */
public record AtlasReward(int threshold, String name, Material material, int amount, ProductType product) {

    public ItemStack create(ItemFactory items) {
        if (product != null) {
            return items.product(product, amount);
        }
        return new ItemStack(material, Math.max(1, Math.min(64, amount)));
    }
}
