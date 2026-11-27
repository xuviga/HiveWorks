package com.xuvigan.hiveworks.craft;

import com.xuvigan.hiveworks.HiveWorksPlugin;
import com.xuvigan.hiveworks.hive.FrameType;
import com.xuvigan.hiveworks.product.MeadType;
import com.xuvigan.hiveworks.product.ProductType;
import com.xuvigan.hiveworks.production.Quality;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.1.1 recipes: net, smoker, five frames and comb -> wax.
 */
public final class RecipeRegistrar {

    private final HiveWorksPlugin plugin;
    private final List<NamespacedKey> keys = new ArrayList<>();

    public RecipeRegistrar(HiveWorksPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        register(netRecipe());
        register(smokerRecipe());
        register(waxRecipe());
        register(royalCellRecipe());
        register(regenerationPotionRecipe());
        register(frameRecipe(FrameType.WAX,
                new RecipeChoice.ExactChoice(plugin.itemFactory().product(ProductType.WAX, 1))));
        register(frameRecipe(FrameType.HONEY, new RecipeChoice.MaterialChoice(Material.HONEY_BOTTLE)));
        register(frameRecipe(FrameType.ROYAL,
                new RecipeChoice.ExactChoice(plugin.itemFactory().product(ProductType.ROYAL_JELLY, 1))));
        register(frameRecipe(FrameType.PROPOLIS,
                new RecipeChoice.ExactChoice(plugin.itemFactory().product(ProductType.PROPOLIS, 1))));
        register(frameRecipe(FrameType.AMETHYST, new RecipeChoice.MaterialChoice(Material.AMETHYST_SHARD)));
        register(meadRecipe(MeadType.MEADOW, Quality.ORDINARY));
        register(meadRecipe(MeadType.FOREST, Quality.GOOD));
        register(meadRecipe(MeadType.ELITE, Quality.ELITE));
        register(candleRecipe(Material.CANDLE, null));
        register(candleRecipe(Material.RED_CANDLE, Material.RED_DYE));
        register(candleRecipe(Material.YELLOW_CANDLE, Material.YELLOW_DYE));
        register(candleRecipe(Material.BLUE_CANDLE, Material.BLUE_DYE));
        plugin.getLogger().info("Registered " + keys.size() + " custom recipes.");
    }

    public void unregisterAll() {
        for (NamespacedKey key : keys) {
            Bukkit.removeRecipe(key);
        }
        keys.clear();
    }

    private void register(Recipe recipe) {
        Bukkit.addRecipe(recipe);
        if (recipe instanceof org.bukkit.Keyed keyed) {
            keys.add(keyed.getKey());
        }
    }

    private ShapelessRecipe netRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(key("net"), plugin.itemFactory().net());
        recipe.addIngredient(Material.STICK);
        recipe.addIngredient(3, Material.STRING);
        return recipe;
    }

    private ShapedRecipe smokerRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(key("smoker"), plugin.itemFactory().smoker());
        recipe.shape("III", "ITI", "LLL");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('T', Material.TORCH);
        recipe.setIngredient('L', Material.LEATHER);
        return recipe;
    }

    private Recipe waxRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(
                key("wax_from_comb"),
                plugin.itemFactory().product(ProductType.WAX, 1)
        );
        recipe.addIngredient(new RecipeChoice.ExactChoice(plugin.itemFactory().product(ProductType.COMB, 1)));
        return recipe;
    }

    private ShapelessRecipe candleRecipe(Material output, Material dye) {
        ShapelessRecipe recipe = new ShapelessRecipe(
                key("candle_" + output.name().toLowerCase(java.util.Locale.ROOT)),
                new org.bukkit.inventory.ItemStack(output, 2)
        );
        recipe.addIngredient(new RecipeChoice.ExactChoice(plugin.itemFactory().product(ProductType.WAX, 1)));
        recipe.addIngredient(Material.STRING);
        if (dye != null) {
            recipe.addIngredient(dye);
        }
        return recipe;
    }

    private ShapelessRecipe royalCellRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(
                key("royal_cell"),
                plugin.itemFactory().product(ProductType.ROYAL_CELL, 1)
        );
        recipe.addIngredient(new RecipeChoice.ExactChoice(plugin.itemFactory().product(ProductType.WAX, 1)));
        recipe.addIngredient(new RecipeChoice.ExactChoice(plugin.itemFactory().product(ProductType.ROYAL_JELLY, 1)));
        recipe.addIngredient(Material.HONEYCOMB);
        return recipe;
    }

    private ShapelessRecipe regenerationPotionRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(
                key("potion_regeneration"),
                plugin.itemFactory().regenerationPotion()
        );
        recipe.addIngredient(new RecipeChoice.ExactChoice(
                plugin.itemFactory().product(ProductType.ROYAL_JELLY, 1)));
        recipe.addIngredient(Material.GLASS_BOTTLE);
        return recipe;
    }

    private ShapedRecipe frameRecipe(FrameType type, RecipeChoice center) {
        ShapedRecipe recipe = new ShapedRecipe(
                key("frame_" + type.name().toLowerCase()),
                plugin.itemFactory().frame(type, type.maxUses())
        );
        recipe.shape("SSS", "SCS", "SSS");
        recipe.setIngredient('S', Material.STICK);
        recipe.setIngredient('C', center);
        return recipe;
    }

    private ShapelessRecipe meadRecipe(MeadType type, Quality quality) {
        ShapelessRecipe recipe = new ShapelessRecipe(key("mead_" + type.id()), plugin.itemFactory().mead(type));
        recipe.addIngredient(new RecipeChoice.ExactChoice(plugin.itemFactory().honey(quality, 1)));
        recipe.addIngredient(Material.WHEAT);
        recipe.addIngredient(Material.GLASS_BOTTLE);
        return recipe;
    }

    private NamespacedKey key(String name) {
        return new NamespacedKey(plugin, name);
    }
}
