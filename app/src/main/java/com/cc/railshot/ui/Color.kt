package com.cc.railshot.ui

import androidx.compose.ui.graphics.Color
import com.cc.railshot.game.Side

val Ink = Color(0xFF071018)
val Cabinet = Color(0xFF0C1822)
val Cream = Color(0xFFF3EFE4)
val Mute = Color(0xFF8FA3B0)
val Line = Color(0xFF1C3A48)
val Rail = Color(0xFF243848)
val YouPaddle = Color(0xFF5EE0C8)
val CpuPaddle = Color(0xFFFF6B6B)
val Ball = Color(0xFFF7F4EA)
val CenterMark = Color(0xFFC9A227)
val StampPaper = Color(0xFF6B9A58)
val SelectYellow = Color(0xFFFFE14A)
val SelectRed = Color(0xFFE23B2E)
val NameBlue = Color(0xFF2A5CFF)
val VsGold = Color(0xFFFFC94A)
val TileBorder = Color(0xFF1A3A88)
val PlaceholderFill = Color(0x33071018)

fun chipColor(side: Side, slot: Int): Color {
  val you =
    arrayOf(
      Color(0xFF1F9A86),
      Color(0xFF2BB7A0),
      Color(0xFF3ED4B8),
      Color(0xFF5EE0C8),
      Color(0xFF7AF0D6),
      Color(0xFFA8F7E6),
    )
  val cpu =
    arrayOf(
      Color(0xFFB71C32),
      Color(0xFFE23D4A),
      Color(0xFFFF6B6B),
      Color(0xFFFF8A7A),
      Color(0xFFFF9A8C),
      Color(0xFFFFC1B6),
    )
  val palette = if (side == Side.YOU) you else cpu
  return palette[slot.coerceIn(0, palette.lastIndex)]
}
