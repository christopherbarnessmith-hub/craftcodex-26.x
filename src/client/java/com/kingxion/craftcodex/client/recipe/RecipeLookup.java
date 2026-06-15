package com.kingxion.craftcodex.client.recipe;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RecipeLookup {
    private RecipeLookup() {}

    public static List<ResolvedRecipe> getRecipesFor(ItemStack item) {
        List<ResolvedRecipe> results = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return results;
        RecipeManager mgr = mc.level.getRecipeManager();
        collect(results, item, mgr.byType(RecipeType.CRAFTING), mc);
        collect(results, item, mgr.byType(RecipeType.SMELTING), mc);
        collect(results, item, mgr.byType(RecipeType.BLASTING), mc);
        collect(results, item, mgr.byType(RecipeType.SMOKING), mc);
        collect(results, item, mgr.byType(RecipeType.STONECUTTING), mc);
        collect(results, item, mgr.byType(RecipeType.CAMPFIRE_COOKING), mc);
        collect(results, item, mgr.byType(RecipeType.SMITHING), mc);
        return results;
    }

    public static List<ResolvedRecipe> getUsagesFor(ItemStack item) {
        List<ResolvedRecipe> results = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return results;
        RecipeManager mgr = mc.level.getRecipeManager();
        for (RecipeType<?> type : List.of(RecipeType.CRAFTING, RecipeType.SMELTING,
                RecipeType.BLASTING, RecipeType.SMOKING, RecipeType.STONECUTTING,
                RecipeType.CAMPFIRE_COOKING, RecipeType.SMITHING))
            collectUsages(results, item, mgr.byType(type), mc);
        return results;
    }

    private static <T extends Recipe<?>> void collect(
            List<ResolvedRecipe> results, ItemStack target,
            Iterable<RecipeHolder<T>> holders, Minecraft mc) {
        for (var h : holders) {
            var out = h.value().getResultItem(mc.level.registryAccess());
            if (!ItemStack.isSameItem(out, target)) continue;
            toResolved(h.value(), out).ifPresent(results::add);
        }
    }

    private static <T extends Recipe<?>> void collectUsages(
            List<ResolvedRecipe> results, ItemStack target,
            Iterable<RecipeHolder<T>> holders, Minecraft mc) {
        for (var h : holders) {
            boolean uses = h.value().getIngredients().stream()
                    .anyMatch(ing -> { for (var s : ing.getItems())
                        if (ItemStack.isSameItem(s, target)) return true; return false; });
            if (!uses) continue;
            var out = h.value().getResultItem(mc.level.registryAccess());
            toResolved(h.value(), out).ifPresent(results::add);
        }
    }

    private static Optional<ResolvedRecipe> toResolved(Recipe<?> r, ItemStack out) {
        if (r instanceof ShapedRecipe x)
            return Optional.of(new ResolvedRecipe(RecipeCategory.CRAFTING,
                    x.getIngredients(), out, x.getWidth(), x.getHeight()));
        if (r instanceof ShapelessRecipe x) { int s = x.getIngredients().size();
            return Optional.of(new ResolvedRecipe(RecipeCategory.CRAFTING_SHAPELESS,
                    x.getIngredients(), out, Math.min(s,3), (s+2)/3)); }
        if (r instanceof SmokingRecipe x)
            return Optional.of(new ResolvedRecipe(RecipeCategory.SMOKING,
                    x.getIngredients(), out, 1, 1));
        if (r instanceof BlastingRecipe x)
            return Optional.of(new ResolvedRecipe(RecipeCategory.BLASTING,
                    x.getIngredients(), out, 1, 1));
        if (r instanceof SmeltingRecipe x)
            return Optional.of(new ResolvedRecipe(RecipeCategory.SMELTING,
                    x.getIngredients(), out, 1, 1));
        if (r instanceof StonecutterRecipe x)
            return Optional.of(new ResolvedRecipe(RecipeCategory.STONECUTTING,
                    x.getIngredients(), out, 1, 1));
        if (r instanceof CampfireCookingRecipe x)
            return Optional.of(new ResolvedRecipe(RecipeCategory.CAMPFIRE,
                    x.getIngredients(), out, 1, 1));
        if (r instanceof SmithingRecipe x)
            return Optional.of(new ResolvedRecipe(RecipeCategory.SMITHING,
                    x.getIngredients(), out, 1, 1));
        return Optional.empty();
    }
}