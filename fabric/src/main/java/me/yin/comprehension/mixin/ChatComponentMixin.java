package me.yin.comprehension.mixin;

import java.util.Locale;
import me.yin.comprehension.DisplayLog;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @Inject(method = "logChatMessage", at = @At("HEAD"))
    private void comprehension$recordChat(GuiMessage message, CallbackInfo ci) {
        DisplayLog.recordChat(message.source().name().toLowerCase(Locale.ROOT), message.content());
    }
}
