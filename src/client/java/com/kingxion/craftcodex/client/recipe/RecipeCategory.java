package com.kingxion.craftcodex.client.recipe;

public enum RecipeCategory {
    CRAFTING("Crafting"),
    CRAFTING_SHAPELESS("Crafting (Shapeless)"),
    SMELTING("Smelting"),
    BLASTING("Blasting"),
    SMOKING("Smoking"),
    STONECUTTING("Stonecutter"),
    CAMPFIRE("Campfire Cooking"),
    SMITHING("Smithing");

    private final String displayName;
    RecipeCategory(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}