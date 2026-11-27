package me.yin.comprehension.mixin;

import java.util.List;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TutorialToast.class)
public interface TutorialToastAccessor {

    @Accessor("lines")
    List<FormattedCharSequence> comprehension$getLines();
}
