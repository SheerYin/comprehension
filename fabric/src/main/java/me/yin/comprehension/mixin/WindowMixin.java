package me.yin.comprehension.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "isFocused", at = @At("HEAD"), cancellable = true)
    private void comprehension$alwaysFocused(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
