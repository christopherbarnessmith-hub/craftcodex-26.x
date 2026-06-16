package com.kingxion.craftcodex.client.gui;

import com.kingxion.craftcodex.client.recipe.RecipeCategory;
import com.kingxion.craftcodex.client.recipe.RecipeLookup;
import com.kingxion.craftcodex.client.recipe.ResolvedRecipe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class RecipeScreen extends Screen {
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 166;
    private static final int SLOT    = 18;
    private static final int PADDING = 8;
    private static final int BUTTON  = 14;
    private static final int TAB_W   = 22;
    private static final int TAB_H   = 24;
    private static final int HEADER_H = 30;
    private static final int RECIPES_PER_PAGE = 1;

    private final ItemStack subject;
    private final Screen    parent;
    private final boolean   usages;
    private List<ResolvedRecipe> allRecipes;
    private List<ResolvedRecipe> recipes;
    private List<RecipeCategory> categories;
    private RecipeCategory selectedCategory;
    private int page = 0;
    private int panelX, panelY;

    public RecipeScreen(ItemStack subject, Screen parent, boolean usages) {
        super(Component.literal(subject.getHoverName().getString()));
        this.subject = subject;
        this.parent  = parent;
        this.usages  = usages;
    }

    @Override
    protected void init() {
        super.init();
        panelX  = (this.width  - PANEL_W) / 2;
        panelY  = (this.height - PANEL_H) / 2;
        allRecipes = usages ? RecipeLookup.getUsagesFor(subject)
                : RecipeLookup.getRecipesFor(subject);
        categories = categoriesFor(allRecipes);
        selectedCategory = categories.isEmpty() ? null : categories.getFirst();
        applyCategoryFilter();
        page = 0;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);

        drawJeiPanel(g);

        if (recipes.isEmpty()) {
            drawText(g, usages ? "No usages found." : "No recipes found.",
                    panelX + 48, panelY + PANEL_H / 2 - 4, 0xFF404040);
            return;
        }

        int pages = pageCount();
        int start = page * RECIPES_PER_PAGE;
        ResolvedRecipe firstRecipe = recipes.get(start);

        drawPageButton(g, panelX + PADDING, panelY + PADDING, page > 0, false);
        drawPageButton(g, panelX + PANEL_W - PADDING - BUTTON, panelY + PADDING, page < pages - 1, true);
        drawText(g, cleanCategoryName(firstRecipe), panelX + 28, panelY + 8, 0xFF404040);
        drawText(g, (page + 1) + "/" + pages,
                panelX + PANEL_W - PADDING - BUTTON - font.width((page + 1) + "/" + pages) - 6,
                panelY + 8, 0xFF404040);
        drawCategoryTabs(g, mx, my);

        int recipeY = panelY + HEADER_H + (categories.isEmpty() ? 12 : 18);
        for (int i = 0; i < RECIPES_PER_PAGE; i++) {
            int recipeIndex = start + i;
            if (recipeIndex >= recipes.size()) break;
            drawRecipe(g, recipes.get(recipeIndex), recipeY, mx, my);
        }
    }

    private void drawPageButton(GuiGraphicsExtractor g, int x, int y, boolean enabled, boolean right) {
        g.fill(x, y, x + BUTTON, y + BUTTON, enabled ? 0xFFD7D7D7 : 0xFFE6E6E6);
        g.fill(x, y, x + BUTTON, y + 1, 0xFFFFFFFF);
        g.fill(x, y, x + 1, y + BUTTON, 0xFFFFFFFF);
        g.fill(x + BUTTON - 1, y, x + BUTTON, y + BUTTON, 0xFF777777);
        g.fill(x, y + BUTTON - 1, x + BUTTON, y + BUTTON, 0xFF777777);
        drawText(g, right ? ">" : "<", x + 5, y + 3, enabled ? 0xFF404040 : 0xFFA0A0A0);
    }

    private void drawCategoryTabs(GuiGraphicsExtractor g, int mx, int my) {
        if (categories.isEmpty()) return;
        int x = panelX + PADDING;
        int y = panelY - TAB_H + 1;
        for (int i = 0; i < categories.size() && i < 9; i++) {
            RecipeCategory category = categories.get(i);
            int tx = x + i * (TAB_W - 1);
            boolean selected = category == selectedCategory;
            int tabBottom = y + TAB_H + (selected ? 2 : 0);
            g.fill(tx, y, tx + TAB_W, tabBottom, selected ? 0xFFE9E9E9 : 0xFFCFCFCF);
            g.fill(tx, y, tx + TAB_W, y + 1, 0xFFFFFFFF);
            g.fill(tx, y, tx + 1, tabBottom, 0xFFFFFFFF);
            g.fill(tx + TAB_W - 1, y, tx + TAB_W, tabBottom, 0xFF555555);
            if (!selected) g.fill(tx, tabBottom - 1, tx + TAB_W, tabBottom, 0xFF555555);
            g.item(categoryIcon(category), tx + 3, y + 4);
            if (mx >= tx && mx < tx + TAB_W && my >= y && my < tabBottom)
                g.setTooltipForNextFrame(font, Component.literal(category.getDisplayName()), mx, my);
        }
    }

    private void drawRecipe(GuiGraphicsExtractor g, ResolvedRecipe recipe, int y, int mx, int my) {
        int gridX = panelX + 36;
        int outputX = panelX + 121;
        int outputY = y + 18;

        drawGrid(g, recipe, gridX, y, mx, my);

        int arrowX = panelX + 94;
        int arrowY = y + 21;
        g.fill(arrowX, arrowY + 6, arrowX + 20, arrowY + 12, 0xFF9E9E9E);
        drawText(g, ">", arrowX + 18, arrowY + 5, 0xFF9E9E9E);

        drawSlot(g, outputX, outputY);
        g.item(recipe.output(), outputX + 1, outputY + 1);
        g.itemDecorations(font, recipe.output(), outputX + 1, outputY + 1);
        if (isMouseOverSlot(mx, my, outputX, outputY))
            g.setTooltipForNextFrame(font, recipe.output(), mx, my);
    }

    private void drawGrid(GuiGraphicsExtractor g, ResolvedRecipe recipe, int sx, int sy, int mx, int my) {
        List<Ingredient> ings = recipe.ingredients();
        boolean crafting = recipe.category() == RecipeCategory.CRAFTING
                || recipe.category() == RecipeCategory.CRAFTING_SHAPELESS
                || recipe.category() == RecipeCategory.MECHANICAL_CRAFTING
                || ings.size() > 1;
        if (crafting) {
            int w = Math.min(3, Math.max(1, recipe.gridWidth()));
            int h = Math.min(3, Math.max(1, recipe.gridHeight()));
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int i = row * w + col;
                    int x = sx + col * SLOT, y = sy + row * SLOT;
                    drawSlot(g, x, y);
                    if (row < h && col < w && i < ings.size() && !ings.get(i).isEmpty()) {
                        List<ItemStack> stacks = stacksFor(ings.get(i));
                        if (!stacks.isEmpty()) {
                            ItemStack stack = cycledStack(stacks);
                            g.item(stack, x + 1, y + 1);
                            if (isMouseOverSlot(mx, my, x, y))
                                g.setTooltipForNextFrame(font, stack, mx, my);
                        }
                    }
                }
            }
        } else {
            int x = sx + SLOT;
            int y = sy + SLOT;
            drawSlot(g, x, y);
            if (!ings.isEmpty()) {
                List<ItemStack> stacks = stacksFor(ings.get(0));
                if (!stacks.isEmpty()) {
                    ItemStack stack = cycledStack(stacks);
                    g.item(stack, x + 1, y + 1);
                    if (isMouseOverSlot(mx, my, x, y))
                        g.setTooltipForNextFrame(font, stack, mx, my);
                }
            }
        }
    }

    private void drawSlot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + SLOT, y + SLOT, 0xFF8B8B8B);
        g.fill(x + 1, y + 1, x + SLOT, y + 2, 0xFFEFEFEF);
        g.fill(x + 1, y + 1, x + 2, y + SLOT, 0xFFEFEFEF);
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xFFA8A8A8);
        g.fill(x + SLOT - 1, y + 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
        g.fill(x + 1, y + SLOT - 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
    }

    private void drawJeiPanel(GuiGraphicsExtractor g) {
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xFFE9E9E9);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, 0xFFFFFFFF);
        g.fill(panelX, panelY, panelX + 1, panelY + PANEL_H, 0xFFFFFFFF);
        g.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xFF555555);
        g.fill(panelX, panelY + PANEL_H - 1, panelX + PANEL_W, panelY + PANEL_H, 0xFF555555);
        g.fill(panelX + 2, panelY + 2, panelX + PANEL_W - 2, panelY + PANEL_H - 2, 0xFFE1E1E1);
    }

    private static boolean isMouseOverSlot(int mx, int my, int x, int y) {
        return mx >= x && mx < x + SLOT && my >= y && my < y + SLOT;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == 263 && page > 0)                  { page--; return true; }
        if (key == 262 && page < pageCount() - 1)    { page++; return true; }
        if (key == 256) { minecraft.setScreen(parent); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int leftX = panelX + PADDING;
        int rightX = panelX + PANEL_W - PADDING - BUTTON;
        int arrowY = panelY + PADDING;
        if (mx >= leftX && mx < leftX + BUTTON && my >= arrowY && my < arrowY + BUTTON && page > 0) {
            page--;
            return true;
        }
        if (mx >= rightX && mx < rightX + BUTTON && my >= arrowY && my < arrowY + BUTTON
                && page < pageCount() - 1) {
            page++;
            return true;
        }
        if (clickCategoryTab(mx, my)) return true;

        ItemStack clicked = getClickedRecipeStack((int) mx, (int) my);
        if (!clicked.isEmpty()) {
            minecraft.setScreen(new RecipeScreen(clicked.copy(), this, event.button() != 0));
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return true;
    }

    private static List<ItemStack> stacksFor(Ingredient ingredient) {
        return ingredient.items()
                .map(ItemStack::new)
                .filter(stack -> !stack.isEmpty())
                .toList();
    }

    private static ItemStack cycledStack(List<ItemStack> stacks) {
        int tick = (int)(System.currentTimeMillis() / 1000) % stacks.size();
        return stacks.get(tick);
    }

    private ItemStack getClickedRecipeStack(int mx, int my) {
        if (recipes.isEmpty()) return ItemStack.EMPTY;

        int recipeIndex = page * RECIPES_PER_PAGE;
        if (recipeIndex >= recipes.size()) return ItemStack.EMPTY;

        ResolvedRecipe recipe = recipes.get(recipeIndex);
        int gridX = panelX + 36;
        int recipeY = panelY + HEADER_H + (categories.isEmpty() ? 12 : 18);
        int outputX = panelX + 121;
        int outputY = recipeY + 18;

        if (isMouseOverSlot(mx, my, outputX, outputY)) {
            return recipe.output();
        }

        return getClickedIngredient(recipe, gridX, recipeY, mx, my);
    }

    private ItemStack getClickedIngredient(ResolvedRecipe recipe, int sx, int sy, int mx, int my) {
        List<Ingredient> ingredients = recipe.ingredients();
        boolean gridRecipe = recipe.category() == RecipeCategory.CRAFTING
                || recipe.category() == RecipeCategory.CRAFTING_SHAPELESS
                || recipe.category() == RecipeCategory.MECHANICAL_CRAFTING
                || ingredients.size() > 1;

        if (gridRecipe) {
            int w = Math.min(3, Math.max(1, recipe.gridWidth()));
            int h = Math.min(3, Math.max(1, recipe.gridHeight()));
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int x = sx + col * SLOT;
                    int y = sy + row * SLOT;
                    int i = row * w + col;
                    if (row < h && col < w && i < ingredients.size() && isMouseOverSlot(mx, my, x, y)) {
                        return stackForIngredient(ingredients.get(i));
                    }
                }
            }
            return ItemStack.EMPTY;
        }

        int x = sx + SLOT;
        int y = sy + SLOT;
        if (isMouseOverSlot(mx, my, x, y) && !ingredients.isEmpty()) {
            return stackForIngredient(ingredients.getFirst());
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack stackForIngredient(Ingredient ingredient) {
        if (ingredient.isEmpty()) return ItemStack.EMPTY;
        List<ItemStack> stacks = stacksFor(ingredient);
        return stacks.isEmpty() ? ItemStack.EMPTY : cycledStack(stacks);
    }

    private void drawText(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.text(font, text, x, y, color, false);
    }

    private boolean clickCategoryTab(double mx, double my) {
        if (categories.isEmpty()) return false;
        int x = panelX + PADDING;
        int y = panelY - TAB_H + 1;
        for (int i = 0; i < categories.size() && i < 9; i++) {
            int tx = x + i * (TAB_W - 1);
            if (mx >= tx && mx < tx + TAB_W && my >= y && my < y + TAB_H + 2) {
                selectedCategory = categories.get(i);
                applyCategoryFilter();
                page = 0;
                return true;
            }
        }
        return false;
    }

    private void applyCategoryFilter() {
        if (selectedCategory == null) {
            recipes = allRecipes;
            return;
        }
        recipes = allRecipes.stream()
                .filter(recipe -> recipe.category() == selectedCategory)
                .toList();
    }

    private static List<RecipeCategory> categoriesFor(List<ResolvedRecipe> recipes) {
        List<RecipeCategory> result = new ArrayList<>();
        for (ResolvedRecipe recipe : recipes) {
            if (!result.contains(recipe.category())) result.add(recipe.category());
        }
        return result;
    }

    private static String cleanCategoryName(ResolvedRecipe recipe) {
        if (recipe.category() == RecipeCategory.CRAFTING_SHAPELESS) return "Crafting";
        return recipe.category().getDisplayName();
    }

    private static ItemStack categoryIcon(RecipeCategory category) {
        return switch (category) {
            case CRAFTING, CRAFTING_SHAPELESS -> new ItemStack(Items.CRAFTING_TABLE);
            case SMELTING -> new ItemStack(Items.FURNACE);
            case BLASTING -> new ItemStack(Items.BLAST_FURNACE);
            case SMOKING -> new ItemStack(Items.SMOKER);
            case CAMPFIRE -> new ItemStack(Items.CAMPFIRE);
            case STONECUTTING -> new ItemStack(Items.STONECUTTER);
            case SMITHING -> new ItemStack(Items.SMITHING_TABLE);
            case CRUSHING, MILLING -> new ItemStack(Items.GRINDSTONE);
            case PRESSING, COMPACTING -> new ItemStack(Items.ANVIL);
            case MIXING -> new ItemStack(Items.CAULDRON);
            case DEPLOYING, SEQUENCED_ASSEMBLY, MECHANICAL_CRAFTING -> new ItemStack(Items.PISTON);
            case SAWING -> new ItemStack(Items.STONECUTTER);
            case SPLASHING, FILLING -> new ItemStack(Items.WATER_BUCKET);
            case HAUNTING -> new ItemStack(Items.SOUL_CAMPFIRE);
            case EMPTYING -> new ItemStack(Items.BUCKET);
            case POLISHING -> new ItemStack(Items.SANDSTONE);
            default -> new ItemStack(Items.HOPPER);
        };
    }

    private int pageCount() {
        return Math.max(1, (recipes.size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE);
    }

    @Override public boolean isPauseScreen() { return false; }
}
