package com.cc.railshot.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

@Composable
fun MagentaKeyedImage(
  assetPath: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Fit,
  alignment: Alignment = Alignment.Center,
) {
  val context = LocalContext.current
  val bitmap =
    remember(assetPath) {
      context.assets.open(assetPath).use { stream ->
        chromaKeyMagenta(BitmapFactory.decodeStream(stream)).asImageBitmap()
      }
    }
  Image(
    bitmap = bitmap,
    contentDescription = contentDescription,
    modifier = modifier,
    contentScale = contentScale,
    alignment = alignment,
    filterQuality = FilterQuality.None,
  )
}

internal fun chromaKeyMagenta(src: Bitmap): Bitmap {
  val bmp = src.copy(Bitmap.Config.ARGB_8888, true)
  val w = bmp.width
  val h = bmp.height
  val px = IntArray(w * h)
  bmp.getPixels(px, 0, w, 0, 0, w, h)
  for (i in px.indices) {
    val c = px[i]
    val a = (c ushr 24) and 0xFF
    if (a < 8) {
      px[i] = 0
      continue
    }
    val r = (c shr 16) and 0xFF
    val g = (c shr 8) and 0xFF
    val b = c and 0xFF
    if (isKeyMagenta(r, g, b)) px[i] = 0
  }
  bmp.setPixels(px, 0, w, 0, 0, w, h)
  return bmp
}

/** Engine key: only true magenta #FF00FF, plus a few levels of PNG rounding. */
internal fun isKeyMagenta(r: Int, g: Int, b: Int): Boolean {
  return r >= 248 && b >= 248 && g <= 8
}
