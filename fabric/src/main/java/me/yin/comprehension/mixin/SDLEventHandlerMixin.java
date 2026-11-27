package me.yin.comprehension.mixin;

import com.mojang.blaze3d.platform.SDLEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SDLEventHandler.class)
public class SDLEventHandlerMixin {

    @Inject(
        method = {
            "handleKeyEvent",
            "handleTextInputEvent",
            "handleTextEditingEvent",
            "handleMouseMotionEvent",
            "handleMouseButtonEvent",
            "handleMouseWheelEvent",
            "handleDropCompleteEvent"
        },
        at = @At("HEAD"),
        cancellable = true
    )
    private void comprehension$dropRealInput(CallbackInfo ci) {
        ci.cancel();
    }
}
