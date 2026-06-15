package com.kingxion.craftcodex;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CraftCodex implements ModInitializer {
	public static final String MOD_ID = "craftcodex";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Craft Codex initialized.");
	}
}