package com.kingxion.craftcodex.client.gui;

import com.kingxion.craftcodex.client.mixin.AbstractContainerScreenAccessor;
import com.kingxion.craftcodex.client.recipe.RecipeLookup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CraftCodexOverlay {
    private static final int PANEL_W  = 160;
    private static final int SLOT     = 18;
    private static final int PADDING  = 4;
    private static final int SEARCH_H = 14;
    private static final int TITLE_H  = 12;

    private static final List<ItemStack> ALL_ITEMS = new ArrayList<>();
    private static final List<ItemStack> FILTERED  = new ArrayList<>();
    private static String  searchText   = "";
    private static int     scrollOffset = 0;
    private static boolean visible      = true;
    private static int     hoveredIndex = -1;
    private static int panelX, panelY, panelH, cols, rows;

    private CraftCodexOverlay() {}

    public static void init() {
        if (ALL_ITEMS.isEmpty()) {
            for (Item item : BuiltInRegistries.ITEM)
                if (item != Items.AIR) ALL_ITEMS.add(new ItemStack(item));
            applyFilter();
        }
    }

    public static void onDrawBackground(Screen screen, GuiGraphicsExtractor g) {
        if (!visible || !(screen instanceof AbstractContainerScreen<?> cs)) return;
        var acc = (AbstractContainerScreenAccessor) cs;
        layout(acc, cs.height);

        g.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, 0xCC000000);
        g.renderOutline(panelX, panelY, PANEL_W, panelH, 0xFF444444);

        var font = Minecraft.getInstance().font;
        g.drawString(font, "Craft Codex", panelX + PADDING, panelY + PADDING, 0xAAAAAA);

        int searchY = panelY + TITLE_H + PADDING;
        g.fill(panelX + PADDING, searchY,
                panelX + PANEL_W - PADDING, searchY + SEARCH_H, 0xFF1A1A1A);
        g.renderOutline(panelX + PADDING, searchY,
                PANEL_W - PADDING * 2, SEARCH_H, 0xFF666666);
        g.drawString(font, searchText.isEmpty() ? "\u00a77Search..." : searchText,
                panelX + PADDING + 3, searchY + 3, 0xFFFFFF);

        int gridY = searchY + SEARCH_H + 4;
        hoveredIndex = -1;
        var mc = Minecraft.getInstance();
        int mx = (int)(mc.mouseHandler.xpos()
                * mc.getWindow().getGuiScaledWidth()  / mc.getWindow().getScreenWidth());
        int my = (int)(mc.mouseHandler.ypos()
                * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int idx = (scrollOffset + row) * cols + col;
                if (idx >= FILTERED.size()) break;
                int x = panelX + PADDING + col * SLOT;
                int y = gridY + row * SLOT;
                if (mx >= x && mx < x + SLOT && my >= y && my < y + SLOT) {
                    g.fill(x, y, x + SLOT, y + SLOT, 0x80FFFFFF);
                    hoveredIndex = idx;
                }
                g.renderItem(FILTERED.get(idx), x + 1, y + 1);
            }
        }

        // Scrollbar
        int totalRows = Math.max(1, ceilDiv(FILTERED.size(), cols));
        int barH = Math.max(10, panelH * rows / totalRows);
        int barY = panelY + (panelH - barH) * scrollOffset / Math.max(1, totalRows - rows);
        g.fill(panelX + PANEL_W - 3, barY, panelX + PANEL_W - 1, barY + barH, 0xFF888888);

        if (hoveredIndex >= 0 && hoveredIndex < FILTERED.size())
            g.renderTooltip(font, FILTERED.get(hoveredIndex), mx, my);
    }

    public static boolean onMouseClick(Screen screen, double mx, double my, int button) {
        if (!visible || !(screen instanceof AbstractContainerScreen<?>)) return false;
        if (hoveredIndex >= 0 && hoveredIndex < FILTERED.size()) {
            ItemStack clicked = FILTERED.get(hoveredIndex).copy();
            Minecraft.getInstance().setScreen(
                    new RecipeScreen(clicked, screen, button == 1));
            return true;
        }
        return false;
    }

    public static boolean onMouseScroll(Screen screen, double mx, double my, double delta) {
        if (!visible || !(screen instanceof AbstractContainerScreen<?>)) return false;
        if (mx < panelX || mx > panelX + PANEL_W) return false;
        int maxScroll = Math.max(0, ceilDiv(FILTERED.size(), cols) - rows);
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int)delta, maxScroll));
        return true;
    }

    public static boolean onKeyPress(Screen screen, int key) {
        if (!(screen instanceof AbstractContainerScreen<?>)) return false;
        if (visible && key == 259 && !searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.length() - 1);
            applyFilter();
            return true;
        }
        return false;
    }

    public static boolean onCharTyped(char chr) {
        if (!visible || chr < 32 || chr >= 127) return false;
        searchText += chr;
        applyFilter();
        return true;
    }

    public static void showRecipesForHovered(Screen screen) {
        ItemStack t = getHovered(screen);
        if (!t.isEmpty())
            Minecraft.getInstance().setScreen(new RecipeScreen(t, screen, false));
    }

    public static void showUsagesForHovered(Screen screen) {
        ItemStack t = getHovered(screen);
        if (!t.isEmpty())
            Minecraft.getInstance().setScreen(new RecipeScreen(t, screen, true));
    }

    private static ItemStack getHovered(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> cs) {
            var slot = ((AbstractContainerScreenAccessor) cs).getHoveredSlot();
            if (slot != null && !slot.getItem().isEmpty()) return slot.getItem().copy();
        }
        if (hoveredIndex >= 0 && hoveredIndex < FILTERED.size())
            return FILTERED.get(hoveredIndex).copy();
        return ItemStack.EMPTY;
    }

    private static void layout(AbstractContainerScreenAccessor acc, int screenH) {
        panelH = screenH - 16;
        panelX = acc.getLeftPos() + acc.getImageWidth() + 4;
        panelY = (screenH - panelH) / 2;
        cols   = Math.max(1, (PANEL_W - PADDING * 2) / SLOT);
        rows   = Math.max(1, (panelH - TITLE_H - PADDING - SEARCH_H - 4 - PADDING) / SLOT);
    }

    private static void applyFilter() {
        FILTERED.clear();
        String q = searchText.toLowerCase(Locale.ROOT);
        for (ItemStack s : ALL_ITEMS)
            if (q.isEmpty() || s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q))
                FILTERED.add(s);
        scrollOffset = 0;
    }

    private static int ceilDiv(int a, int b) { return (a + b - 1) / b; }
    public static boolean isVisible() { return visible; }
    public static void setVisible(boolean v) { visible = v; }
}