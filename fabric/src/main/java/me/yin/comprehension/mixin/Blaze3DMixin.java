package me.yin.comprehension.mixin;

import com.mojang.blaze3d.Blaze3D;
import java.net.URI;
import java.nio.file.Path;
import me.yin.comprehension.SideEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Blaze3D.class)
public class Blaze3DMixin {

    @Inject(method = "openUri", at = @At("HEAD"), cancellable = true)
    private static void comprehension$recordOpenUri(URI uri, CallbackInfo ci) {
        SideEffects.openUri(uri.toString());
        ci.cancel();
    }

    @Inject(method = "openPath", at = @At("HEAD"), cancellable = true)
    private static void comprehension$recordOpenPath(Path path, CallbackInfo ci) {
        SideEffects.openPath(path.toString());
        ci.cancel();
    }
}
