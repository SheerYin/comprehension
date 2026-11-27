package me.yin.comprehension.mixin;

import java.util.List;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SystemToast.class)
public interface SystemToastAccessor {

    @Accessor("titleLines")
    List<FormattedCharSequence> comprehension$getTitleLines();

    @Accessor("messageLines")
    List<FormattedCharSequence> comprehension$getMessageLines();
}
