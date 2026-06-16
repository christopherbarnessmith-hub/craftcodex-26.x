package com.kingxion.craftcodex.client;

import com.kingxion.craftcodex.client.gui.CraftCodexOverlay;
import com.kingxion.craftcodex.client.input.KeyBindings;
import com.kingxion.craftcodex.client.config.CraftCodexConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class CraftCodexClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CraftCodexConfig.load();
		KeyBindings.register();
		CraftCodexOverlay.configure();

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.screen instanceof AbstractContainerScreen<?> screen) {
				if (KeyBindings.showRecipes.consumeClick())
					CraftCodexOverlay.showRecipesForHovered(screen);
				if (KeyBindings.showUsages.consumeClick())
					CraftCodexOverlay.showUsagesForHovered(screen);
				if (KeyBindings.toggleOverlay.consumeClick())
					CraftCodexOverlay.setVisible(!CraftCodexOverlay.isVisible());
			}
		});

	}
}
