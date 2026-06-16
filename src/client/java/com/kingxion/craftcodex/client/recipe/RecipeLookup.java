package com.kingxion.craftcodex.client.recipe;

import com.kingxion.craftcodex.client.config.CraftCodexConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class RecipeLookup {
    private RecipeLookup() {}

    public static List<ResolvedRecipe> getRecipesFor(ItemStack item) {
        List<ResolvedRecipe> results = new ArrayList<>();
        for (RecipeDisplayEntry entry : knownRecipes()) {
            Optional<ResolvedRecipe> recipe = toResolved(entry);
            if (recipe.isPresent() && ItemStack.isSameItem(recipe.get().output(), item)) {
                results.add(recipe.get());
            }
        }
        return results;
    }

    public static List<ResolvedRecipe> getUsagesFor(ItemStack item) {
        List<ResolvedRecipe> results = new ArrayList<>();
        for (RecipeDisplayEntry entry : knownRecipes()) {
            Optional<ResolvedRecipe> recipe = toResolved(entry);
            if (recipe.isPresent() && uses(recipe.get(), item)) {
                results.add(recipe.get());
            }
        }
        return results;
    }

    private static Collection<RecipeDisplayEntry> knownRecipes() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return List.of();
        if (CraftCodexConfig.hideUnavailableRecipes) {
            return clientRecipeBookRecipes(mc.player.getRecipeBook());
        }
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            return serverRecipes(mc.getSingleplayerServer().getRecipeManager());
        }
        return clientRecipeBookRecipes(mc.player.getRecipeBook());
    }

    private static Collection<RecipeDisplayEntry> serverRecipes(RecipeManager manager) {
        List<RecipeDisplayEntry> entries = new ArrayList<>();
        Set<Object> seen = new HashSet<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            manager.listDisplaysForRecipe(holder.id(), entry -> {
                if (seen.add(entry.id())) entries.add(entry);
            });
        }
        return entries;
    }

    private static Collection<RecipeDisplayEntry> clientRecipeBookRecipes(ClientRecipeBook book) {
        List<RecipeDisplayEntry> entries = new ArrayList<>();
        Set<Object> seen = new HashSet<>();
        for (RecipeCollection collection : book.getCollections()) {
            for (RecipeDisplayEntry entry : collection.getRecipes()) {
                if (seen.add(entry.id())) entries.add(entry);
            }
        }
        return entries;
    }

    private static Optional<ResolvedRecipe> toResolved(RecipeDisplayEntry entry) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return Optional.empty();

        var context = SlotDisplayContext.fromLevel(mc.level);
        RecipeDisplay display = entry.display();

        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            ItemStack output = output(entry, shaped.result());
            return nonEmpty(output, new ResolvedRecipe(
                    RecipeCategory.CRAFTING,
                    ingredients(shaped.ingredients(), context),
                    output,
                    shaped.width(),
                    shaped.height()));
        }

        if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            List<Ingredient> ingredients = ingredients(shapeless.ingredients(), context);
            ItemStack output = output(entry, shapeless.result());
            int size = Math.max(1, ingredients.size());
            return nonEmpty(output, new ResolvedRecipe(
                    RecipeCategory.CRAFTING_SHAPELESS,
                    ingredients,
                    output,
                    Math.min(size, 3),
                    (size + 2) / 3));
        }

        if (display instanceof FurnaceRecipeDisplay furnace) {
            ItemStack output = output(entry, furnace.result());
            return nonEmpty(output, new ResolvedRecipe(
                    RecipeCategory.SMELTING,
                    ingredient(furnace.ingredient(), context),
                    output,
                    1,
                    1));
        }

        if (display instanceof StonecutterRecipeDisplay stonecutter) {
            ItemStack output = output(entry, stonecutter.result());
            return nonEmpty(output, new ResolvedRecipe(
                    RecipeCategory.STONECUTTING,
                    ingredient(stonecutter.input(), context),
                    output,
                    1,
                    1));
        }

        if (display instanceof SmithingRecipeDisplay smithing) {
            List<Ingredient> ingredients = new ArrayList<>();
            ingredients.addAll(ingredient(smithing.template(), context));
            ingredients.addAll(ingredient(smithing.base(), context));
            ingredients.addAll(ingredient(smithing.addition(), context));
            ItemStack output = output(entry, smithing.result());
            return nonEmpty(output, new ResolvedRecipe(
                    RecipeCategory.SMITHING,
                    ingredients,
                    output,
                    3,
                    1));
        }

        return toGenericResolved(entry, display, context);
    }

    private static Optional<ResolvedRecipe> nonEmpty(ItemStack output, ResolvedRecipe recipe) {
        return output.isEmpty() ? Optional.empty() : Optional.of(recipe);
    }

    private static ItemStack output(RecipeDisplayEntry entry, SlotDisplay result) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return ItemStack.EMPTY;
        var context = SlotDisplayContext.fromLevel(mc.level);
        List<ItemStack> entryResults = entry.resultItems(context);
        if (!entryResults.isEmpty()) return entryResults.getFirst();
        return result.resolveForFirstStack(context);
    }

    private static List<Ingredient> ingredients(List<SlotDisplay> slots, net.minecraft.util.context.ContextMap context) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (SlotDisplay slot : slots) {
            ingredients.addAll(ingredient(slot, context));
        }
        return ingredients;
    }

    private static List<Ingredient> ingredient(SlotDisplay slot, net.minecraft.util.context.ContextMap context) {
        List<ItemStack> stacks = slot.resolveForStacks(context);
        if (stacks.isEmpty()) return List.of();
        return List.of(Ingredient.of(stacks.stream().map(ItemStack::getItem)));
    }

    private static Optional<ResolvedRecipe> toGenericResolved(
            RecipeDisplayEntry entry,
            RecipeDisplay display,
            net.minecraft.util.context.ContextMap context) {
        List<SlotDisplay> inputs = new ArrayList<>();
        List<SlotDisplay> outputs = new ArrayList<>();
        collectSlots(display, inputs, outputs);

        List<Ingredient> ingredients = ingredients(inputs, context);
        ItemStack output = firstOutput(entry, outputs, context);
        if (output.isEmpty()) return Optional.empty();

        RecipeCategory category = inferCategory(display);
        int count = Math.max(1, ingredients.size());
        int width = category == RecipeCategory.MECHANICAL_CRAFTING ? 3 : Math.min(3, count);
        int height = category == RecipeCategory.MECHANICAL_CRAFTING ? 3 : (count + width - 1) / width;

        return Optional.of(new ResolvedRecipe(
                category,
                ingredients,
                output,
                width,
                Math.min(3, Math.max(1, height))));
    }

    private static void collectSlots(RecipeDisplay display, List<SlotDisplay> inputs, List<SlotDisplay> outputs) {
        RecordComponent[] components = display.getClass().getRecordComponents();
        if (components == null) return;

        for (RecordComponent component : components) {
            try {
                Object value = component.getAccessor().invoke(display);
                String name = component.getName().toLowerCase(Locale.ROOT);
                if (value instanceof SlotDisplay slot) {
                    addSlot(name, slot, inputs, outputs);
                } else if (value instanceof List<?> values) {
                    for (Object element : values) {
                        if (element instanceof SlotDisplay slot) {
                            addSlot(name, slot, inputs, outputs);
                        }
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static void addSlot(String name, SlotDisplay slot, List<SlotDisplay> inputs, List<SlotDisplay> outputs) {
        if (name.contains("station") || name.contains("fuel") || name.contains("remainder")) return;
        if (name.contains("result") || name.contains("output")) {
            outputs.add(slot);
            return;
        }
        if (name.contains("ingredient") || name.contains("input") || name.contains("base")
                || name.contains("addition") || name.contains("template") || name.contains("item")) {
            inputs.add(slot);
        }
    }

    private static ItemStack firstOutput(
            RecipeDisplayEntry entry,
            List<SlotDisplay> outputs,
            net.minecraft.util.context.ContextMap context) {
        List<ItemStack> entryResults = entry.resultItems(context);
        if (!entryResults.isEmpty()) return entryResults.getFirst();
        for (SlotDisplay slot : outputs) {
            ItemStack stack = slot.resolveForFirstStack(context);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static RecipeCategory inferCategory(RecipeDisplay display) {
        String name = display.getClass().getName().toLowerCase(Locale.ROOT);
        if (!isCreateLoaded() && !name.contains("create")) return RecipeCategory.CREATE_PROCESSING;
        if (!isCreateLoaded()) return RecipeCategory.CREATE_PROCESSING;
        if (name.contains("mechanical") && name.contains("craft")) return RecipeCategory.MECHANICAL_CRAFTING;
        if (name.contains("sequenced")) return RecipeCategory.SEQUENCED_ASSEMBLY;
        if (name.contains("crush")) return RecipeCategory.CRUSHING;
        if (name.contains("mill")) return RecipeCategory.MILLING;
        if (name.contains("press")) return RecipeCategory.PRESSING;
        if (name.contains("mix")) return RecipeCategory.MIXING;
        if (name.contains("compact")) return RecipeCategory.COMPACTING;
        if (name.contains("deploy")) return RecipeCategory.DEPLOYING;
        if (name.contains("saw") || name.contains("cutting")) return RecipeCategory.SAWING;
        if (name.contains("splash")) return RecipeCategory.SPLASHING;
        if (name.contains("haunt")) return RecipeCategory.HAUNTING;
        if (name.contains("fill")) return RecipeCategory.FILLING;
        if (name.contains("empty")) return RecipeCategory.EMPTYING;
        if (name.contains("polish") || name.contains("sandpaper")) return RecipeCategory.POLISHING;
        return RecipeCategory.CREATE_PROCESSING;
    }

    private static boolean isCreateLoaded() {
        FabricLoader loader = FabricLoader.getInstance();
        return loader.isModLoaded("create") || loader.isModLoaded("create-fabric");
    }

    private static boolean uses(ResolvedRecipe recipe, ItemStack item) {
        for (Ingredient ingredient : recipe.ingredients()) {
            if (ingredient.test(item)) return true;
        }
        return false;
    }
}
