package me.yin.comprehension.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public class FramerateLimitTrackerMixin {

    @Unique
    private static final int comprehension$maxFps = Integer.getInteger("comprehension.maxFps", 30);

    @Inject(method = "getThrottleReason", at = @At("HEAD"), cancellable = true)
    private void comprehension$neverThrottle(CallbackInfoReturnable<FramerateLimitTracker.FramerateThrottleReason> cir) {
        cir.setReturnValue(FramerateLimitTracker.FramerateThrottleReason.NONE);
    }

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void comprehension$fixedFramerateLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(comprehension$maxFps);
    }
}
