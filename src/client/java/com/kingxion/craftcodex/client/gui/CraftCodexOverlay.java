package com.kingxion.craftcodex.client.gui;

import com.kingxion.craftcodex.client.config.CraftCodexConfig;
import com.kingxion.craftcodex.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CraftCodexOverlay {
    private static final int SLOT     = 18;
    private static final int PADDING  = 4;
    private static final int SEARCH_H = 14;
    private static final int TITLE_H  = 12;
    private static final int GEAR     = 12;
    private static final int BOOKMARK_W = 26;

    private static final List<ItemStack> ALL_ITEMS = new ArrayList<>();
    private static final List<ItemStack> FILTERED  = new ArrayList<>();
    private static final List<ItemStack> BOOKMARKS = new ArrayList<>();
    private static final Path BOOKMARK_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("craftcodex-bookmarks.txt");
    private static String  searchText   = "";
    private static int     scrollOffset = 0;
    private static boolean visible      = true;
    private static boolean cheatMode    = false;
    private static boolean suppressNextChar = false;
    private static boolean searchFocused = false;
    private static int     hoveredIndex = -1;
    private static int     hoveredBookmarkIndex = -1;
    private static int panelX, panelY, panelW, panelH, bookmarkX, bookmarkY, cols, rows, gearX, gearY, searchX, searchY;

    private CraftCodexOverlay() {}

    public static void configure() {
        panelW = PADDING * 2 + SLOT * CraftCodexConfig.columns;
        cheatMode = CraftCodexConfig.cheatModeAllowed && CraftCodexConfig.cheatModeDefault;
    }

    public static void init() {
        if (ALL_ITEMS.isEmpty()) {
            try {
                for (Item item : BuiltInRegistries.ITEM)
                    if (shouldShowItem(item)) ALL_ITEMS.add(new ItemStack(item));
                loadBookmarks();
                applyFilter();
            } catch (NullPointerException | IllegalStateException ignored) {
                ALL_ITEMS.clear();
                FILTERED.clear();
            }
        }
    }

    public static void onDrawBackground(Screen screen, GuiGraphicsExtractor g) {
        if (!visible || !(screen instanceof AbstractContainerScreen<?> cs)) return;
        init();
        if (ALL_ITEMS.isEmpty()) return;
        var acc = (AbstractContainerScreenAccessor) cs;
        layout(acc, cs.height);

        drawBookmarks(g);

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xDDE9E9E9);
        g.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFFFFFFFF);
        g.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFFFFFFFF);
        g.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF555555);
        g.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF555555);

        var font = Minecraft.getInstance().font;
        g.text(font, "Codex", panelX + PADDING, panelY + PADDING, 0xFF404040, false);

        int gearColor = cheatMode ? 0xFF54E080 : 0xFF666666;
        g.fill(gearX, gearY, gearX + GEAR, gearY + GEAR, 0xFFC8C8C8);
        g.outline(gearX, gearY, GEAR, GEAR, gearColor);
        g.centeredText(font, "\u2699", gearX + GEAR / 2, gearY + 2, gearColor);

        g.fill(searchX, searchY,
                panelX + panelW - PADDING, searchY + SEARCH_H, 0xFF202020);
        g.outline(searchX, searchY,
                panelW - PADDING * 2, SEARCH_H, searchFocused ? 0xFF54A8E0 : 0xFF666666);
        g.text(font, searchText.isEmpty() ? "Search..." : searchText,
                panelX + PADDING + 3, searchY + 3,
                searchText.isEmpty() ? 0xFF777777 : 0xFFFFFFFF);

        int gridY = searchY + SEARCH_H + 4;
        hoveredIndex = -1;
        hoveredBookmarkIndex = -1;
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
                g.item(FILTERED.get(idx), x + 1, y + 1);
            }
        }

        // Scrollbar
        int totalRows = Math.max(1, ceilDiv(FILTERED.size(), cols));
        int barH = Math.max(10, panelH * rows / totalRows);
        int barY = panelY + (panelH - barH) * scrollOffset / Math.max(1, totalRows - rows);
        g.fill(panelX + panelW - 3, barY, panelX + panelW - 1, barY + barH, 0xFF888888);

        if (hoveredIndex >= 0 && hoveredIndex < FILTERED.size())
            g.setTooltipForNextFrame(font, FILTERED.get(hoveredIndex), mx, my);
        if (hoveredBookmarkIndex >= 0 && hoveredBookmarkIndex < BOOKMARKS.size())
            g.setTooltipForNextFrame(font, BOOKMARKS.get(hoveredBookmarkIndex), mx, my);
        if (mx >= gearX && mx < gearX + GEAR && my >= gearY && my < gearY + GEAR)
            g.setTooltipForNextFrame(font,
                    Component.literal("Left: Cheat " + (cheatMode ? "On" : "Off")
                            + " | Right: Hide Unavailable "
                            + (CraftCodexConfig.hideUnavailableRecipes ? "On" : "Off")),
                    mx, my);
    }

    public static boolean onMouseClick(Screen screen, double mx, double my, int button, int modifiers) {
        if (!visible || !(screen instanceof AbstractContainerScreen<?>)) return false;
        searchFocused = mx >= searchX && mx < panelX + panelW - PADDING
                && my >= searchY && my < searchY + SEARCH_H;
        if (searchFocused) return true;
        if (mx >= gearX && mx < gearX + GEAR && my >= gearY && my < gearY + GEAR) {
            if (button == 1) {
                CraftCodexConfig.hideUnavailableRecipes = !CraftCodexConfig.hideUnavailableRecipes;
                CraftCodexConfig.save();
                sendStatus("Craft Codex hide unavailable recipes "
                        + (CraftCodexConfig.hideUnavailableRecipes ? "enabled" : "disabled"));
            } else if (CraftCodexConfig.cheatModeAllowed) {
                cheatMode = !cheatMode;
                sendStatus("Craft Codex cheat mode " + (cheatMode ? "enabled" : "disabled"));
            }
            return true;
        }
        if (hoveredBookmarkIndex >= 0 && hoveredBookmarkIndex < BOOKMARKS.size()) {
            openOrCheat(BOOKMARKS.get(hoveredBookmarkIndex).copy(), screen, button, modifiers);
            return true;
        }
        if (hoveredIndex >= 0 && hoveredIndex < FILTERED.size()) {
            openOrCheat(FILTERED.get(hoveredIndex).copy(), screen, button, modifiers);
            return true;
        }
        return false;
    }

    public static boolean onMouseScroll(Screen screen, double mx, double my, double delta) {
        if (!visible || !(screen instanceof AbstractContainerScreen<?>)) return false;
        if (mx < panelX || mx > panelX + panelW) return false;
        int maxScroll = Math.max(0, ceilDiv(FILTERED.size(), cols) - rows);
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int)delta, maxScroll));
        return true;
    }

    public static boolean onKeyPress(Screen screen, int key, int modifiers) {
        if (!(screen instanceof AbstractContainerScreen<?>)) return false;
        if (visible && key == GLFW.GLFW_KEY_A) {
            ItemStack stack = getHovered(screen);
            if (!stack.isEmpty()) {
                boolean shiftDown = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
                if (shiftDown) removeBookmark(stack);
                else addBookmark(stack);
                suppressNextChar = true;
                return true;
            }
        }
        if (visible && searchFocused && key == GLFW.GLFW_KEY_ESCAPE) {
            searchFocused = false;
            return true;
        }
        if (visible && searchFocused && key == 259 && !searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.length() - 1);
            applyFilter();
            return true;
        }
        return false;
    }

    public static boolean onCharTyped(char chr) {
        if (suppressNextChar) {
            suppressNextChar = false;
            return true;
        }
        if (!visible || !searchFocused || chr < 32 || chr >= 127) return false;
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
        panelW = PADDING * 2 + SLOT * CraftCodexConfig.columns;
        panelX = CraftCodexConfig.overlayOnLeft
                ? acc.getLeftPos() - panelW - 4
                : acc.getLeftPos() + acc.getImageWidth() + 4;
        panelY = (screenH - panelH) / 2;
        bookmarkX = acc.getLeftPos() - BOOKMARK_W - 4;
        bookmarkY = panelY;
        gearX  = panelX + panelW - PADDING - GEAR;
        gearY  = panelY + PADDING - 1;
        searchX = panelX + PADDING;
        searchY = panelY + TITLE_H + PADDING;
        cols   = Math.max(1, (panelW - PADDING * 2) / SLOT);
        rows   = Math.max(1, (panelH - TITLE_H - PADDING - SEARCH_H - 4 - PADDING) / SLOT);
    }

    private static void applyFilter() {
        FILTERED.clear();
        String q = searchText.toLowerCase(Locale.ROOT);
        for (ItemStack s : ALL_ITEMS)
            if (matchesSearch(s, q))
                FILTERED.add(s);
        scrollOffset = 0;
    }

    private static int ceilDiv(int a, int b) { return (a + b - 1) / b; }
    private static boolean shouldShowItem(Item item) {
        if (item == Items.AIR) return false;
        if (!CraftCodexConfig.hideAdminItems) return true;
        return item != Items.COMMAND_BLOCK
                && item != Items.CHAIN_COMMAND_BLOCK
                && item != Items.REPEATING_COMMAND_BLOCK
                && item != Items.COMMAND_BLOCK_MINECART
                && item != Items.STRUCTURE_BLOCK
                && item != Items.STRUCTURE_VOID
                && item != Items.JIGSAW
                && item != Items.DEBUG_STICK
                && item != Items.BARRIER
                && item != Items.LIGHT;
    }

    private static boolean matchesSearch(ItemStack stack, String query) {
        if (query.isEmpty()) return true;
        String itemName = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
        String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().toLowerCase(Locale.ROOT);
        for (String token : query.split("\\s+")) {
            if (token.isEmpty()) continue;
            if (token.startsWith("@")) {
                if (!namespace.contains(token.substring(1))) return false;
            } else if (token.startsWith("#")) {
                if (!matchesTag(stack, token.substring(1))) return false;
            } else if (!itemName.contains(token) && !id.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesTag(ItemStack stack, String rawTag) {
        String tag = rawTag.contains(":") ? rawTag : "minecraft:" + rawTag;
        Identifier id = Identifier.tryParse(tag);
        if (id == null) return false;
        TagKey<Item> key = TagKey.create(Registries.ITEM, id);
        return stack.is(holder -> holder.is(key));
    }

    private static void drawBookmarks(GuiGraphicsExtractor g) {
        if (BOOKMARKS.isEmpty()) return;
        var mc = Minecraft.getInstance();
        int mx = (int)(mc.mouseHandler.xpos()
                * mc.getWindow().getGuiScaledWidth()  / mc.getWindow().getScreenWidth());
        int my = (int)(mc.mouseHandler.ypos()
                * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());

        int visibleBookmarks = Math.min(BOOKMARKS.size(), Math.max(1, panelH / SLOT));
        g.fill(bookmarkX, bookmarkY, bookmarkX + BOOKMARK_W, bookmarkY + visibleBookmarks * SLOT + PADDING * 2, 0xCC000000);
        g.outline(bookmarkX, bookmarkY, BOOKMARK_W, visibleBookmarks * SLOT + PADDING * 2, 0xFF444444);
        for (int i = 0; i < visibleBookmarks; i++) {
            int x = bookmarkX + PADDING;
            int y = bookmarkY + PADDING + i * SLOT;
            if (mx >= x && mx < x + SLOT && my >= y && my < y + SLOT) {
                g.fill(x, y, x + SLOT, y + SLOT, 0x80FFFFFF);
                hoveredBookmarkIndex = i;
            }
            g.item(BOOKMARKS.get(i), x + 1, y + 1);
        }
    }

    private static void addBookmark(ItemStack stack) {
        for (ItemStack bookmark : BOOKMARKS) {
            if (ItemStack.isSameItemSameComponents(bookmark, stack)) return;
        }
        BOOKMARKS.add(stack.copyWithCount(1));
        saveBookmarks();
    }

    private static void removeBookmark(ItemStack stack) {
        for (int i = 0; i < BOOKMARKS.size(); i++) {
            if (ItemStack.isSameItemSameComponents(BOOKMARKS.get(i), stack)) {
                BOOKMARKS.remove(i);
                saveBookmarks();
                return;
            }
        }
    }

    private static void loadBookmarks() {
        if (!BOOKMARKS.isEmpty() || !Files.exists(BOOKMARK_PATH)) return;
        try {
            for (String line : Files.readAllLines(BOOKMARK_PATH)) {
                Identifier id = Identifier.tryParse(line.trim());
                if (id == null) continue;
                Item item = BuiltInRegistries.ITEM.getValue(id);
                if (shouldShowItem(item)) BOOKMARKS.add(new ItemStack(item));
            }
        } catch (IOException ignored) {
        }
    }

    private static void saveBookmarks() {
        try {
            Files.createDirectories(BOOKMARK_PATH.getParent());
            List<String> lines = new ArrayList<>();
            for (ItemStack stack : BOOKMARKS) {
                lines.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            }
            Files.write(BOOKMARK_PATH, lines);
        } catch (IOException ignored) {
        }
    }

    private static void openOrCheat(ItemStack clicked, Screen screen, int button, int modifiers) {
        if (cheatMode && CraftCodexConfig.cheatModeAllowed) {
            boolean shiftDown = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
            cheatItem(clicked, shiftDown ? clicked.getItem().getDefaultMaxStackSize() : 1);
            return;
        }
        Minecraft.getInstance().setScreen(new RecipeScreen(clicked, screen, false));
    }

    private static void cheatItem(ItemStack stack, int count) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || stack.isEmpty()) return;

        ItemStack grant = stack.copyWithCount(Math.max(1, count));
        GameType mode = mc.gameMode.getPlayerMode();
        if (mode.isCreative()) {
            int selectedSlot = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedItem(grant.copy());
            mc.gameMode.handleCreativeModeItemAdd(grant, 36 + selectedSlot);
            return;
        }

        if (mc.getConnection() != null) {
            var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            mc.getConnection().sendCommand("give @s " + itemId + " " + grant.getCount());
        }
    }

    private static void sendStatus(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal(text));
    }

    public static boolean isVisible() { return visible; }
    public static void setVisible(boolean v) { visible = v; }
}
