package me.yin.comprehension.mixin;

import net.minecraft.client.Options;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.client.tutorial.TutorialSteps;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Tutorial.class)
public class TutorialMixin {

    @Redirect(
        method = "start",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;tutorialStep:Lnet/minecraft/client/tutorial/TutorialSteps;", opcode = Opcodes.GETFIELD)
    )
    private TutorialSteps comprehension$skipTutorial(Options options) {
        return TutorialSteps.NONE;
    }
}
