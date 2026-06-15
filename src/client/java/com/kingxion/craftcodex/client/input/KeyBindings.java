package com.kingxion.craftcodex.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public final class KeyBindings {
    public static KeyMapping showRecipes;
    public static KeyMapping showUsages;
    public static KeyMapping toggleOverlay;
    public static KeyMapping cheatItem;

    private KeyBindings() {}

    public static void register() {
        KeyMapping.Category category = new KeyMapping.Category("key.category.craftcodex");

        showRecipes = new KeyMapping("key.craftcodex.show_recipes",
                InputConstants.Type.KEYSYM, InputConstants.KEY_R, category);
        showUsages = new KeyMapping("key.craftcodex.show_usages",
                InputConstants.Type.KEYSYM, InputConstants.KEY_U, category);
        toggleOverlay = new KeyMapping("key.craftcodex.toggle_overlay",
                InputConstants.Type.KEYSYM, InputConstants.KEY_O, category);
        cheatItem = new KeyMapping("key.craftcodex.cheat_item",
                InputConstants.Type.KEYSYM, InputConstants.KEY_UNKNOWN, category);

        KeyMappingHelper.registerKeyMapping(showRecipes);
        KeyMappingHelper.registerKeyMapping(showUsages);
        KeyMappingHelper.registerKeyMapping(toggleOverlay);
        KeyMappingHelper.registerKeyMapping(cheatItem);
    }
}