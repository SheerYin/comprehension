package me.yin.comprehension.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import me.yin.comprehension.VirtualKeyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InputConstants.class)
public class InputConstantsMixin {

    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void comprehension$readVirtualKeyboard(int key, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(VirtualKeyboard.isDown(key));
    }
}
