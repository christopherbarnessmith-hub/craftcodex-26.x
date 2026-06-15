package com.kingxion.craftcodex.mixin;

import com.kingxion.craftcodex.client.gui.CraftCodexOverlay;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(long handle, CharacterEvent event, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof Screen) {
            if (CraftCodexOverlay.onCharTyped(event.codePoint().charAt(0))) {
                ci.cancel();
            }
        }
    }
}