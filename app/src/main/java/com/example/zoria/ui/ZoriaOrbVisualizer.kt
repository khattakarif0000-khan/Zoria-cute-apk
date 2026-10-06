package com.example.zoria.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StateErrorColor
import com.example.ui.theme.StateIdleColor
import com.example.ui.theme.StateListeningColor
import com.example.ui.theme.StateProcessingColor
import com.example.ui.theme.StateSpeakingColor
import com.example.zoria.model.AssistantState

/**
 * Visual core of ZORIA.
 * Responsively renders the actual assistant state:
 * - IDLE: Gentle breathing ambient core.
 * - LISTENING: Scales and ripples based on real hardware microphone RMS amplitude.
 * - PROCESSING: Rotating neural pulse ring.
 * - SPEAKING: Sonic frequency resonance.
 * - ERROR: Amber-red alert aura.
 *
 * Adheres strictly to the requirement: No fake audio simulation animations.
 */
@Composable
fun ZoriaOrbVisualizer(
    state: AssistantState,
    rmsDecibels: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "zoria_core_transition")

    // Ambient breathing pulse for IDLE state
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_pulse"
    )

    // Rotation angle for PROCESSING state
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "processing_rotation"
    )

    // Harmonic wave pulse for SPEAKING state
    val speakingWave by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_wave"
    )

    // Smooth transition between state colors
    val coreColor by animateColorAsState(
        targetValue = when (state) {
            AssistantState.IDLE -> StateIdleColor
            AssistantState.LISTENING -> StateListeningColor
            AssistantState.PROCESSING -> StateProcessingColor
            AssistantState.SPEAKING -> StateSpeakingColor
            AssistantState.ERROR -> StateErrorColor
        },
        animationSpec = tween(400),
        label = "core_color"
    )

    // Microphone amplitude responsive scaling (real decibels from hardware, clamped 0f..10f)
    val micScaleFactor by animateFloatAsState(
        targetValue = if (state == AssistantState.LISTENING) {
            1f + (rmsDecibels / 12f).coerceIn(0f, 0.9f)
        } else {
            1f
        },
        animationSpec = tween(80),
        label = "mic_scale"
    )

    Box(
        modifier = modifier
            .size(190.dp)
            .testTag("zoria_orb_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 4f

            when (state) {
                AssistantState.IDLE -> {
                    val radius = baseRadius * idlePulse
                    // Outer ambient halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(coreColor.copy(alpha = 0.35f), Color.Transparent),
                            center = center,
                            radius = radius * 1.8f
                        ),
                        radius = radius * 1.8f,
                        center = center
                    )
                    // Inner glowing orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, coreColor),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }

                AssistantState.LISTENING -> {
                    // Truthful reactive rings driven directly by microphone RMS levels
                    val activeRadius = baseRadius * micScaleFactor

                    // Expanding decibel resonance ring
                    drawCircle(
                        color = coreColor.copy(alpha = 0.25f),
                        radius = activeRadius * 1.6f,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    // Outer mic wave
                    drawCircle(
                        color = coreColor.copy(alpha = 0.5f),
                        radius = activeRadius * 1.3f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Reactive glowing core
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, coreColor),
                            center = center,
                            radius = activeRadius
                        ),
                        radius = activeRadius,
                        center = center
                    )
                }

                AssistantState.PROCESSING -> {
                    val radius = baseRadius * 1.05f

                    // Neural spinning arcs
                    drawCircle(
                        color = coreColor.copy(alpha = 0.2f),
                        radius = radius * 1.5f,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawArc(
                        color = coreColor,
                        startAngle = rotationAngle,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx()),
                        topLeft = Offset(center.x - radius * 1.5f, center.y - radius * 1.5f),
                        size = androidx.compose.ui.geometry.Size(radius * 3f, radius * 3f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, coreColor),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }

                AssistantState.SPEAKING -> {
                    val radius = baseRadius * speakingWave

                    // Resonating voice waves
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(coreColor.copy(alpha = 0.45f), Color.Transparent),
                            center = center,
                            radius = radius * 1.9f
                        ),
                        radius = radius * 1.9f,
                        center = center
                    )
                    drawCircle(
                        color = coreColor.copy(alpha = 0.6f),
                        radius = radius * 1.4f,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, coreColor),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }

                AssistantState.ERROR -> {
                    // Amber-red warning aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(coreColor.copy(alpha = 0.35f), Color.Transparent),
                            center = center,
                            radius = baseRadius * 1.7f
                        ),
                        radius = baseRadius * 1.7f,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, coreColor),
                            center = center,
                            radius = baseRadius
                        ),
                        radius = baseRadius,
                        center = center
                    )
                }
            }
        }
    }
}
