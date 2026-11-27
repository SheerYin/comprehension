package me.yin.comprehension.mixin;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.TitleScreen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Redirect(
        method = "/^lambda[$]createNormalMenuOptions[$]/",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;skipMultiplayerWarning:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean comprehension$skipMultiplayerWarning(Options options) {
        return true;
    }
}
