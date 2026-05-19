package com.kevin.tiertagger.mixin;

import com.kevin.tiertagger.TierTagger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.client.render.entity.state.TextDisplayEntityRenderState;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.uku3lig.ukulib.utils.Ukutils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(DisplayEntityRenderer.TextDisplayEntityRenderer.class)
public class MixinTextDisplayEntityRenderer {
    // Replaces cachedInfo in the render state so that text positioning, background width,
    // and rendered contents all agree on the counter-appended line width
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity;Lnet/minecraft/client/render/entity/state/TextDisplayEntityRenderState;F)V",
            at = @At("RETURN"))
    private void addTier(DisplayEntity.TextDisplayEntity entity, TextDisplayEntityRenderState renderState, float f, CallbackInfo ci) {
        if (!TierTagger.getManager().getConfig().isEnabled()) return;
        if (renderState.textLines == null) return;
        if (!(entity.getVehicle() instanceof PlayerEntity player)) return;

        List<DisplayEntity.TextDisplayEntity.TextLine> lines = renderState.textLines.lines();
        for (int i = 0; i < lines.size(); i++) {
            final DisplayEntity.TextDisplayEntity.TextLine line = lines.get(i);
            final Text lineText = Ukutils.getStyledText(line.contents());
            final String lineString = lineText.getString();
            if (lineString.isBlank() || !lineString.contains(player.getNameForScoreboard())) continue;

            final Text modified = TierTagger.appendTier(player.getUuid(), player.getNameForScoreboard(), lineText);
            if (modified == lineText) return; // no pops or counter disabled

            final OrderedText modifiedSeq = modified.asOrderedText();
            final int newLineWidth = MinecraftClient.getInstance().textRenderer.getWidth(modified);

            final List<DisplayEntity.TextDisplayEntity.TextLine> newLines = new ArrayList<>(lines);
            newLines.set(i, new DisplayEntity.TextDisplayEntity.TextLine(modifiedSeq, newLineWidth));

            final int newMaxWidth = newLines.stream()
                    .mapToInt(DisplayEntity.TextDisplayEntity.TextLine::width)
                    .max().orElse(renderState.textLines.width());

            renderState.textLines = new DisplayEntity.TextDisplayEntity.TextLines(newLines, newMaxWidth);
            return;
        }
    }
}
