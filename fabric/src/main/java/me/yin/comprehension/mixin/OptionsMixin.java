package me.yin.comprehension.mixin;

import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public class OptionsMixin {

    @Inject(method = "getFinalSoundSourceVolume", at = @At("HEAD"), cancellable = true)
    private void comprehension$mute(SoundSource source, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(0.0F);
    }
}
