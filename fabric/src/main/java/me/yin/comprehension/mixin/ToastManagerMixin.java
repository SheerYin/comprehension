package me.yin.comprehension.mixin;

import me.yin.comprehension.DisplayLog;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public class ToastManagerMixin {

    @Inject(method = "addToast", at = @At("HEAD"))
    private void comprehension$recordToast(Toast toast, CallbackInfo ci) {
        DisplayLog.recordToast(toast);
    }
}
