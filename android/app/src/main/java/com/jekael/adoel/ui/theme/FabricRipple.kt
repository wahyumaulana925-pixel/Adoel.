package com.jekael.adoel.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/**
 * App-wide press feedback in place of Material's default circular ripple — a tilted, squashed
 * ellipse expanding from the touch point, echoing a wave crossing woven fabric rather than water
 * on a flat surface. Provided via [LocalIndication] in [AdoelTheme] so every `clickable`/`Button`/
 * card tap picks it up automatically, no per-call-site changes needed.
 */
object FabricWaveIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        FabricWaveIndicationNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = javaClass.hashCode()
}

private class FabricWaveIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {
    private var pressPosition: Offset = Offset.Zero
    private val progress = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        pressPosition = interaction.pressPosition
                        progress.snapTo(0f)
                        progress.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        progress.animateTo(0f, tween(220))
                    }
                }
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val p = progress.value
        if (p <= 0f) return
        val maxRadius = size.maxDimension * 0.85f
        val radius = maxRadius * p
        // Rotated + squashed circle instead of a plain one — the "wave" cue. Rotation echoes the
        // twill texture's diagonal direction (see Texture.kt); the squash keeps it reading as an
        // ellipse rather than a tilted circle (which would look identical to an untilted one).
        rotate(degrees = 20f, pivot = pressPosition) {
            scale(scaleX = 1.5f, scaleY = 0.6f, pivot = pressPosition) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f * (1f - p * 0.4f)),
                    radius = radius,
                    center = pressPosition,
                )
            }
        }
    }
}
