package com.xuvigan.hiveworks.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;

/**
 * Item model bridge.
 *
 * <p>1.21.4+ uses the {@code minecraft:item_model} component (setItemModel),
 * older versions fall back to custom model data. Reflection keeps a single
 * jar compiling against both 1.21.1 and 26.3.</p>
 */
public final class ItemModels {

    private static Method setItemModel;
    private static boolean resolved;

    private ItemModels() {
    }

    public static boolean setItemModel(ItemMeta meta, NamespacedKey key) {
        if (!resolved) {
            resolved = true;
            try {
                setItemModel = ItemMeta.class.getMethod("setItemModel", NamespacedKey.class);
            } catch (NoSuchMethodException ignored) {
                setItemModel = null;
            }
        }
        if (setItemModel == null) {
            return false;
        }
        try {
            setItemModel.invoke(meta, key);
            return true;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}
