package com.kingxion.craftcodex.mixin;

import com.kingxion.craftcodex.client.gui.CraftCodexOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(
            method = "extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("TAIL")
    )
    private void onRender(GuiGraphicsExtractor g, int mx, int my, float delta, CallbackInfo ci) {
        Screen screen = (Screen)(Object) this;
        CraftCodexOverlay.onDrawBackground(screen, g);
    }
}
