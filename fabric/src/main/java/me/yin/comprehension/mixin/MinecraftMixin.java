package me.yin.comprehension.mixin;

import me.yin.comprehension.Frames;
import me.yin.comprehension.Screenshots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(
        method = "renderFrame",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render()V", shift = At.Shift.AFTER)
    )
    private void comprehension$afterFrameRendered(boolean advanceGameTime, CallbackInfo ci) {
        Screenshots.onFrameRendered();
        Frames.onFrameRendered();
    }

    @Redirect(
        method = "resizeGui",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;")
    )
    private Object comprehension$fixedGuiScale(OptionInstance<?> option) {
        return Integer.getInteger("comprehension.guiScale", 0);
    }
}
