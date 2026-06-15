package com.kingxion.craftcodex.client.gui;

import com.kingxion.craftcodex.client.recipe.RecipeCategory;
import com.kingxion.craftcodex.client.recipe.RecipeLookup;
import com.kingxion.craftcodex.client.recipe.ResolvedRecipe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class RecipeScreen extends Screen {
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 220;
    private static final int SLOT    = 18;
    private static final int PADDING = 8;

    private final ItemStack subject;
    private final Screen    parent;
    private final boolean   usages;
    private List<ResolvedRecipe> recipes;
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
        recipes = usages ? RecipeLookup.getUsagesFor(subject)
                : RecipeLookup.getRecipesFor(subject);
        page = 0;
    }

    @Override
    public void render(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        renderBackground(g, mx, my, delta);

        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xEE1A1A1A);
        g.renderOutline(panelX, panelY, PANEL_W, PANEL_H, 0xFF444444);

        int cx = panelX + PANEL_W / 2;
        int y  = panelY + PADDING;

        g.renderItem(subject, panelX + PADDING, y);
        g.drawString(font, (usages ? "Usages: " : "Recipes: ")
                        + subject.getHoverName().getString(),
                panelX + PADDING + 20, y + 4, 0xFFFFFF);
        y += 24;

        if (recipes.isEmpty()) {
            g.drawCenteredString(font, usages ? "No usages found." : "No recipes found.",
                    cx, y, 0xAAAAAA);
            g.drawCenteredString(font, "\u00a77[Esc] Close",
                    cx, panelY + PANEL_H - 14, 0xAAAAAA);
            return;
        }

        ResolvedRecipe recipe = recipes.get(page);
        g.drawCenteredString(font, recipe.category().getDisplayName(), cx, y, 0xAAAAAA);
        y += 10;

        if (recipes.size() > 1) {
            g.drawString(font, "\u25c4", panelX + PADDING, y,
                    page > 0 ? 0xFFFFFF : 0x555555);
            g.drawCenteredString(font, (page + 1) + " / " + recipes.size(), cx, y, 0xCCCCCC);
            g.drawString(font, "\u25ba",
                    panelX + PANEL_W - PADDING - font.width("\u25ba"), y,
                    page < recipes.size() - 1 ? 0xFFFFFF : 0x555555);
        }
        y += 14;

        y = drawGrid(g, recipe, panelX + PADDING, y);
        y += 8;
        g.drawCenteredString(font, "\u25bc", cx, y, 0xFFFFFF);
        y += 12;

        int outX = cx - SLOT / 2;
        g.fill(outX - 1, y - 1, outX + SLOT + 1, y + SLOT + 1, 0xFF333333);
        g.renderItem(recipe.output(), outX, y);
        g.renderItemDecorations(font, recipe.output(), outX, y);
        if (mx >= outX && mx <= outX + SLOT && my >= y && my <= y + SLOT)
            g.renderTooltip(font, recipe.output(), mx, my);

        g.drawCenteredString(font, "\u00a77[\u2190\u2192] Page  [Esc] Close",
                cx, panelY + PANEL_H - 14, 0xAAAAAA);
    }

    private int drawGrid(GuiGraphicsExtractor g, ResolvedRecipe recipe, int sx, int sy) {
        List<Ingredient> ings = recipe.ingredients();
        boolean crafting = recipe.category() == RecipeCategory.CRAFTING
                || recipe.category() == RecipeCategory.CRAFTING_SHAPELESS;
        if (crafting) {
            int w = recipe.gridWidth(), h = recipe.gridHeight();
            for (int row = 0; row < h; row++) {
                for (int col = 0; col < w; col++) {
                    int i = row * w + col;
                    int x = sx + col * SLOT, y = sy + row * SLOT;
                    g.fill(x, y, x + SLOT - 1, y + SLOT - 1, 0xFF2A2A2A);
                    if (i < ings.size() && !ings.get(i).isEmpty()) {
                        ItemStack[] stacks = ings.get(i).getItems();
                        if (stacks.length > 0) {
                            int tick = (int)(System.currentTimeMillis() / 1000) % stacks.length;
                            g.renderItem(stacks[tick], x + 1, y + 1);
                        }
                    }
                }
            }
            return sy + h * SLOT;
        } else {
            if (!ings.isEmpty()) {
                ItemStack[] stacks = ings.get(0).getItems();
                if (stacks.length > 0) {
                    int tick = (int)(System.currentTimeMillis() / 1000) % stacks.length;
                    g.fill(sx, sy, sx + SLOT - 1, sy + SLOT - 1, 0xFF2A2A2A);
                    g.renderItem(stacks[tick], sx + 1, sy + 1);
                }
            }
            return sy + SLOT;
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 263 && page > 0)                  { page--; return true; }
        if (key == 262 && page < recipes.size() - 1) { page++; return true; }
        if (key == 256) { minecraft.setScreen(parent); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int arrowY = panelY + PADDING + 24 + 10;
        if (mx >= panelX + PADDING && mx <= panelX + PADDING + 10
                && my >= arrowY && my <= arrowY + 10 && page > 0) { page--; return true; }
        int rx = panelX + PANEL_W - PADDING - font.width("\u25ba");
        if (mx >= rx && mx <= rx + 10
                && my >= arrowY && my <= arrowY + 10
                && page < recipes.size() - 1) { page++; return true; }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean isPauseScreen() { return false; }
}