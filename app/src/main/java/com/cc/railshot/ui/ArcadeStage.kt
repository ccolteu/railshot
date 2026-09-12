package com.cc.railshot.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun Letterbox43(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier = modifier.fillMaxSize().background(Cabinet),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxHeight()
          .aspectRatio(4f / 3f, matchHeightConstraintsFirst = true),
      content = content,
    )
  }
}

@Composable
fun RailshotStampWallpaper(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.fillMaxSize()) {
    drawRect(StampPaper)
    val native = drawContext.canvas.nativeCanvas
    val paint =
      Paint().apply {
        color = 0xFF7CB56A.toInt()
        textSize = size.minDimension * 0.055f
        typeface = Typeface.DEFAULT_BOLD
        isFakeBoldText = true
        isAntiAlias = false
      }
    val stepX = size.width * 0.22f
    val stepY = size.height * 0.14f
    native.save()
    native.rotate(-32f, size.width * 0.5f, size.height * 0.5f)
    var y = -size.height
    while (y < size.height * 2f) {
      var x = -size.width
      var col = 0
      while (x < size.width * 2f) {
        native.drawText("RAILSHOT", x + if (col % 2 == 0) 0f else stepX * 0.35f, y, paint)
        x += stepX
        col++
      }
      y += stepY
    }
    native.restore()
  }
}
