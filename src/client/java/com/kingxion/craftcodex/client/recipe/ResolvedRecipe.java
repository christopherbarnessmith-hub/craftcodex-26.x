package com.kingxion.craftcodex.client.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public record ResolvedRecipe(
        RecipeCategory category,
        List<Ingredient> ingredients,
        ItemStack output,
        int gridWidth,
        int gridHeight
) {}