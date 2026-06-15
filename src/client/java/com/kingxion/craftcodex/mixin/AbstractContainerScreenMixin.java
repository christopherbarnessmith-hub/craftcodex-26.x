package com.kingxion.craftcodex.mixin;

import com.kingxion.craftcodex.client.gui.CraftCodexOverlay;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Inject(method = "allowMouseClick(Lnet/minecraft/client/input/MouseButtonEvent;)Z",
            at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object) this;
        if (CraftCodexOverlay.onMouseClick(screen,
                event.x(), event.y(), event.button())) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}