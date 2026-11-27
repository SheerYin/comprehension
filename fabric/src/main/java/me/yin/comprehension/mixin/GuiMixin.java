package me.yin.comprehension.mixin;

import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public class GuiMixin {

    @Redirect(
        method = "addInitialScreens",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;onboardAccessibility:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean comprehension$skipAccessibilityOnboarding(Options options) {
        return false;
    }
}
