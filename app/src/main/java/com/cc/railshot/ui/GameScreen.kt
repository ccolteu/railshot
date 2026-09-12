package com.cc.railshot.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cc.railshot.game.Fighter
import com.cc.railshot.game.Phase
import com.cc.railshot.game.World
import kotlin.math.min

@Composable
fun GameScreen(you: Fighter = Fighter.RIVET, rival: Fighter = Fighter.RIVET) {
  val world = remember { World() }
  var frame by remember { mutableIntStateOf(0) }

  LaunchedEffect(Unit) {
    var last = 0L
    while (true) {
      withFrameNanos { now ->
        val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
        last = now
        world.step(dt)
        frame++
      }
    }
  }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(Cabinet)
        .statusBarsPadding()
        .navigationBarsPadding(),
  ) {
    val tick = frame
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Hud(you.displayName, world.youScore.toString().padStart(2, '0'), YouPaddle)
      Text(
        text = "RAILSHOT",
        color = Cream,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
      )
      Hud(rival.displayName, world.cpuScore.toString().padStart(2, '0'), CpuPaddle)
    }
    Box(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(vertical = 4.dp),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxHeight()
            .aspectRatio(4f / 3f, matchHeightConstraintsFirst = true)
            .background(Ink)
            .pointerInput(Unit) {
              detectTapGestures(onTap = { world.launch() })
            }
            .pointerInput(Unit) {
              detectDragGestures { change, _ ->
                change.consume()
                world.moveYouPaddle((change.position.y / size.height).coerceIn(0f, 1f))
              }
            },
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          tick
          val w = size.width
          val h = size.height
          val rail = h * 0.028f
          drawRect(color = Line, style = Stroke(width = 3f))
          drawRect(color = Rail, size = Size(w, rail))
          drawRect(color = Rail, topLeft = Offset(0f, h - rail), size = Size(w, rail))
          drawLine(
            color = Line.copy(alpha = 0.8f),
            start = Offset(w * 0.5f, rail + 6f),
            end = Offset(w * 0.5f, h - rail - 6f),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f)),
          )
          val mark = min(w, h) * 0.12f
          drawCircle(
            color = CenterMark.copy(alpha = 0.35f),
            radius = mark,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = 3f),
          )
          drawCircle(
            color = CenterMark.copy(alpha = 0.55f),
            radius = mark * 0.18f,
            center = Offset(w * 0.5f, h * 0.5f),
          )
          world.chips.forEach { chip ->
            val left = chip.x * w
            val top = chip.y * h
            val cw = chip.w * w
            val ch = chip.h * h
            if (chip.alive) {
              drawRoundRect(
                color = chipColor(chip.side, chip.slot),
                topLeft = Offset(left, top),
                size = Size(cw, ch),
                cornerRadius = CornerRadius(cw * 0.45f, ch * 0.45f),
              )
            } else {
              drawRoundRect(
                color = Line.copy(alpha = 0.55f),
                topLeft = Offset(left, top),
                size = Size(cw, ch),
                cornerRadius = CornerRadius(cw * 0.45f, ch * 0.45f),
                style = Stroke(width = 2f),
              )
            }
          }
          drawRoundRect(
            color = YouPaddle,
            topLeft = Offset(World.YOU_PADDLE_X * w, world.youPaddleTop() * h),
            size = Size(World.PADDLE_THICK * w, World.PADDLE_LEN * h),
            cornerRadius = CornerRadius(6f, 6f),
          )
          drawRoundRect(
            color = CpuPaddle,
            topLeft = Offset(World.CPU_PADDLE_X * w, world.cpuPaddleTop() * h),
            size = Size(World.PADDLE_THICK * w, World.PADDLE_LEN * h),
            cornerRadius = CornerRadius(6f, 6f),
          )
          drawCircle(
            color = Ball,
            radius = World.BALL_R * min(w, h),
            center = Offset(world.ballX * w, world.ballY * h),
          )
        }
        val banner =
          when (world.phase) {
            Phase.SERVE -> "FIGHT\nTAP TO SERVE"
            Phase.YOU_WIN -> "WIN\nTAP TO RESTART"
            Phase.CPU_WIN -> "CPU WINS\nTAP TO RESTART"
            Phase.PLAYING -> null
          }
        if (banner != null) {
          Text(
            text = banner,
            color = Cream,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
          )
        }
      }
    }
  }
}

@Composable
private fun Hud(label: String, value: String, accent: androidx.compose.ui.graphics.Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(label, color = Mute, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    Text(value, color = accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 26.sp)
  }
}
