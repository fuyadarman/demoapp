package com.example.myandroidapp

import kotlin.OptIn
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.ExperimentalTextApi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlin.random.Random

private enum class GameState { READY, PLAYING, GAME_OVER }

private class Pipe(var x: Float, val gapCenter: Float) {
    var scored: Boolean = false
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTextApi::class)
@Composable
fun FlappyBirdGame() {
    val textMeasurer = rememberTextMeasurer()

    // Canvas / world size (pixels)
    var canvasW by remember { mutableStateOf(0f) }
    var canvasH by remember { mutableStateOf(0f) }

    // Bird physics
    var birdY by remember { mutableStateOf(0f) }
    var birdVel by remember { mutableStateOf(0f) }

    // Pipes
    val pipes = remember { mutableStateListOf<Pipe>() }
    var spawnTimer by remember { mutableStateOf(0f) }

    // Scoreboard / state
    var score by remember { mutableStateOf(0) }
    var best by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf(GameState.READY) }

    // Sizes derived from the canvas so the game scales to any screen
    val birdRadius = (canvasW * 0.045f).coerceAtLeast(16f)
    val pipeWidth = (canvasW * 0.16f).coerceAtLeast(56f)
    val gapHeight = (canvasH * 0.30f).coerceAtLeast(150f)
    val groundHeight = (canvasH * 0.09f).coerceAtLeast(48f)
    val birdX = canvasW * 0.28f

    // Speeds scaled to screen
    val gravity = canvasH * 1.8f
    val jumpVel = -canvasH * 0.62f
    val pipeSpeed = canvasW * 0.42f
    val spawnInterval = 1.55f

    // ---- Game loop ----
    LaunchedEffect(canvasW, canvasH) {
        if (canvasW <= 0f || canvasH <= 0f) return@LaunchedEffect
        birdY = canvasH * 0.45f
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                var dt = (now - last) / 1_000_000_000f
                last = now
                if (dt > 0.05f) dt = 0.05f

                if (state == GameState.PLAYING) {
                    // Bird physics
                    birdVel += gravity * dt
                    birdY += birdVel * dt

                    // Move pipes
                    for (p in pipes) p.x -= pipeSpeed * dt
                    while (pipes.isNotEmpty() && pipes.first().x + pipeWidth < -pipeWidth) {
                        pipes.removeAt(0)
                    }

                    // Spawn pipes
                    spawnTimer += dt
                    if (spawnTimer >= spawnInterval) {
                        spawnTimer -= spawnInterval
                        val margin = gapHeight / 2f + 60f
                        val range = canvasH - groundHeight - 2f * margin
                        val gapCenter =
                            if (range > 0f) margin + Random.nextFloat() * range else canvasH / 2f
                        pipes.add(Pipe(canvasW + pipeWidth, gapCenter))
                    }

                    // Scoring
                    for (p in pipes) {
                        if (!p.scored && p.x + pipeWidth < birdX - birdRadius) {
                            p.scored = true
                            score += 1
                        }
                    }

                    // Ceiling
                    if (birdY - birdRadius < 0f) {
                        birdY = birdRadius
                        birdVel = 0f
                    }

                    // Collision detection
                    var dead = birdY + birdRadius >= canvasH - groundHeight
                    if (!dead) {
                        for (p in pipes) {
                            val left = p.x
                            val right = p.x + pipeWidth
                            if (birdX + birdRadius > left && birdX - birdRadius < right) {
                                val gapTop = p.gapCenter - gapHeight / 2f
                                val gapBottom = p.gapCenter + gapHeight / 2f
                                if (birdY - birdRadius < gapTop || birdY + birdRadius > gapBottom) {
                                    dead = true
                                    break
                                }
                            }
                        }
                    }
                    if (dead) {
                        state = GameState.GAME_OVER
                        if (score > best) best = score
                    }
                }
            }
        }
    }

    // ---- Tap handling ----
    val onTap: () -> Unit = {
        when (state) {
            GameState.READY -> {
                pipes.clear()
                score = 0
                birdY = canvasH * 0.45f
                birdVel = jumpVel
                spawnTimer = 0.8f
                state = GameState.PLAYING
            }
            GameState.PLAYING -> {
                birdVel = jumpVel
            }
            GameState.GAME_OVER -> {
                pipes.clear()
                score = 0
                birdY = canvasH * 0.45f
                birdVel = 0f
                spawnTimer = 0f
                state = GameState.READY
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    canvasW = it.width.toFloat()
                    canvasH = it.height.toFloat()
                }
                .pointerInput(state) {
                    detectTapGestures(onTap = { onTap() })
                }
        ) {
            // --- Sky ---
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4EC0CA), Color(0xFF9BE7DC))
                ),
                size = size
            )

            // --- Pipes ---
            for (p in pipes) {
                val gapTop = p.gapCenter - gapHeight / 2f
                val gapBottom = p.gapCenter + gapHeight / 2f
                drawPipe(p.x, 0f, pipeWidth, gapTop.coerceAtLeast(0f))
                drawPipe(
                    p.x, gapBottom, pipeWidth,
                    (size.height - groundHeight - gapBottom).coerceAtLeast(0f)
                )
            }

            // --- Ground ---
            drawRect(
                color = Color(0xFFDED895),
                topLeft = Offset(0f, size.height - groundHeight),
                size = Size(size.width, groundHeight)
            )
            drawRect(
                color = Color(0xFF73C13B),
                topLeft = Offset(0f, size.height - groundHeight),
                size = Size(size.width, groundHeight * 0.25f)
            )

            // --- Bird ---
            val angle = (birdVel * 0.05f).coerceIn(-25f, 90f)
            rotate(degrees = angle, pivot = Offset(birdX, birdY)) {
                // body
                drawCircle(
                    color = Color(0xFFFFE14D),
                    radius = birdRadius,
                    center = Offset(birdX, birdY)
                )
                drawCircle(
                    color = Color(0xFF6D4C41),
                    radius = birdRadius,
                    center = Offset(birdX, birdY),
                    style = Stroke(width = birdRadius * 0.16f)
                )
                // wing
                drawOval(
                    color = Color(0xFFFDD835),
                    topLeft = Offset(birdX - birdRadius * 0.95f, birdY - birdRadius * 0.25f),
                    size = Size(birdRadius * 1.1f, birdRadius * 0.9f)
                )
                // eye
                drawCircle(
                    color = Color.White,
                    radius = birdRadius * 0.34f,
                    center = Offset(birdX + birdRadius * 0.35f, birdY - birdRadius * 0.35f)
                )
                drawCircle(
                    color = Color.Black,
                    radius = birdRadius * 0.16f,
                    center = Offset(birdX + birdRadius * 0.45f, birdY - birdRadius * 0.35f)
                )
                // beak
                val beak = Path().apply {
                    moveTo(birdX + birdRadius * 0.9f, birdY - birdRadius * 0.12f)
                    lineTo(birdX + birdRadius * 1.7f, birdY + birdRadius * 0.1f)
                    lineTo(birdX + birdRadius * 0.9f, birdY + birdRadius * 0.42f)
                    close()
                }
                drawPath(beak, Color(0xFFFF7043))
            }

            // --- Score during play ---
            if (state == GameState.PLAYING) {
                drawCenteredText(
                    textMeasurer, score.toString(), size.width, 60f,
                    TextStyle(color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                )
            }

            // --- Ready overlay ---
            if (state == GameState.READY) {
                drawCenteredText(
                    textMeasurer, "Flappy Bird", size.width, size.height * 0.26f,
                    TextStyle(color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                )
                drawCenteredText(
                    textMeasurer, "Tap to Start", size.width, size.height * 0.55f,
                    TextStyle(color = Color(0xFF14432A), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                )
            }

            // --- Game over overlay ---
            if (state == GameState.GAME_OVER) {
                drawRect(Color(0x66000000), size = size)
                drawCenteredText(
                    textMeasurer, "Game Over", size.width, size.height * 0.28f,
                    TextStyle(color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                )
                drawCenteredText(
                    textMeasurer, "Score: $score", size.width, size.height * 0.40f,
                    TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                )
                drawCenteredText(
                    textMeasurer, "Best: $best", size.width, size.height * 0.47f,
                    TextStyle(color = Color(0xFFFFF176), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                )
                drawCenteredText(
                    textMeasurer, "Tap to Restart", size.width, size.height * 0.60f,
                    TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

private fun DrawScope.drawPipe(x: Float, y: Float, w: Float, h: Float) {
    if (h <= 0f) return
    drawRect(
        color = Color(0xFF5CB733),
        topLeft = Offset(x, y),
        size = Size(w, h)
    )
    // highlight strip
    drawRect(
        color = Color(0xFF7CD04A),
        topLeft = Offset(x + w * 0.12f, y),
        size = Size(w * 0.18f, h)
    )
    // outline
    drawRect(
        color = Color(0xFF2E6B1E),
        topLeft = Offset(x, y),
        size = Size(w, h),
        style = Stroke(width = 4f)
    )
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawCenteredText(
    textMeasurer: TextMeasurer,
    text: String,
    canvasWidth: Float,
    topY: Float,
    style: TextStyle
) {
    val layout = textMeasurer.measure(text = text, style = style)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset((canvasWidth - layout.size.width) / 2f, topY)
    )
}
