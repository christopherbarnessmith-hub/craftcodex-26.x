package com.kingxion.craftcodex.client.recipe;

public enum RecipeCategory {
    CRAFTING("Crafting"),
    CRAFTING_SHAPELESS("Crafting (Shapeless)"),
    SMELTING("Smelting"),
    BLASTING("Blasting"),
    SMOKING("Smoking"),
    STONECUTTING("Stonecutter"),
    CAMPFIRE("Campfire Cooking"),
    SMITHING("Smithing"),
    CREATE_PROCESSING("Processing"),
    CRUSHING("Crushing"),
    MILLING("Milling"),
    PRESSING("Pressing"),
    MIXING("Mixing"),
    COMPACTING("Compacting"),
    DEPLOYING("Deploying"),
    SEQUENCED_ASSEMBLY("Sequenced Assembly"),
    MECHANICAL_CRAFTING("Mechanical Crafting"),
    SAWING("Sawing"),
    SPLASHING("Splashing"),
    HAUNTING("Haunting"),
    FILLING("Filling"),
    EMPTYING("Emptying"),
    POLISHING("Polishing");

    private final String displayName;
    RecipeCategory(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
