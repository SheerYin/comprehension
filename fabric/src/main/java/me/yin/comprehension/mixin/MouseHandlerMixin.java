package me.yin.comprehension.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = {"resyncMousePosition", "cursorEntered"}, at = @At("HEAD"), cancellable = true)
    private void comprehension$ignoreRealCursor(CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(
        method = "grabMouse",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;grabMouse(Lcom/mojang/blaze3d/platform/Window;DD)V")
    )
    private void comprehension$keepRealCursorOnGrab(Window window, double xpos, double ypos) {
    }

    @Redirect(
        method = "releaseMouse",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants;releaseMouse(Lcom/mojang/blaze3d/platform/Window;DD)V")
    )
    private void comprehension$keepRealCursorOnRelease(Window window, double xpos, double ypos) {
    }
}
