package me.yin.comprehension.mixin;

import me.yin.comprehension.SideEffects;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "setClipboard", at = @At("HEAD"), cancellable = true)
    private void comprehension$virtualClipboardWrite(String clipboard, CallbackInfo ci) {
        SideEffects.writeClipboard(clipboard);
        ci.cancel();
    }

    @Inject(method = "getClipboard", at = @At("HEAD"), cancellable = true)
    private void comprehension$virtualClipboardRead(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(SideEffects.readClipboard());
    }
}
