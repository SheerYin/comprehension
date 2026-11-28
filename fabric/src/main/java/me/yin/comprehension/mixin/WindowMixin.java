package me.yin.comprehension.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "isFocused", at = @At("HEAD"), cancellable = true)
    private void comprehension$alwaysFocused(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @ModifyArg(
        method = "createWindow",
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/device/GpuBackend;createWindow(Ljava/lang/String;IIJ)J"),
        index = 3
    )
    private long comprehension$createHidden(long flags) {
        return Boolean.getBoolean("comprehension.hidden") ? flags | SDLVideo.SDL_WINDOW_HIDDEN : flags;
    }
}
