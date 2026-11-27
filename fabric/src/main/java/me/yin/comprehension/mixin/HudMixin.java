package me.yin.comprehension.mixin;

import me.yin.comprehension.DisplayLog;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudMixin {

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void comprehension$recordTitle(Component title, CallbackInfo ci) {
        DisplayLog.recordTitle("title", title);
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void comprehension$recordSubtitle(Component subtitle, CallbackInfo ci) {
        DisplayLog.recordTitle("subtitle", subtitle);
    }

    @Inject(method = "clearTitles", at = @At("HEAD"))
    private void comprehension$recordClear(CallbackInfo ci) {
        DisplayLog.recordTitle("clear", Component.empty());
    }

    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void comprehension$recordActionbar(Component message, boolean animate, CallbackInfo ci) {
        DisplayLog.recordActionbar(message);
    }
}
