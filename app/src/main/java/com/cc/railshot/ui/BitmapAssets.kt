package com.cc.railshot.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

internal fun loadKeyedBitmap(
  context: Context,
  assetPath: String,
  keyHoleBleed: Boolean = false,
): Bitmap = chromaKeyMagenta(decodeAssetBitmap(context, assetPath), keyHoleBleed)

internal fun loadOpaqueBitmap(context: Context, assetPath: String): Bitmap =
  decodeAssetBitmap(context, assetPath)

private fun decodeAssetBitmap(context: Context, assetPath: String): Bitmap {
  val opts =
    BitmapFactory.Options().apply {
      inScaled = false
      inPreferredConfig = Bitmap.Config.ARGB_8888
    }
  return context.assets.open(assetPath).use { stream ->
    BitmapFactory.decodeStream(stream, null, opts) ?: error("missing asset $assetPath")
  }
}

internal fun chromaKeyMagenta(src: Bitmap, keyHoleBleed: Boolean = false): Bitmap {
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
    val drop = if (keyHoleBleed) isCabinetHoleBleed(r, g, b) else isKeyMagenta(r, g, b)
    if (drop) px[i] = 0
  }
  bmp.setPixels(px, 0, w, 0, 0, w, h)
  return bmp
}

internal fun isKeyMagenta(r: Int, g: Int, b: Int): Boolean {
  return r >= 248 && b >= 248 && g <= 8
}

internal fun isCabinetHoleBleed(r: Int, g: Int, b: Int): Boolean {
  if (isKeyMagenta(r, g, b)) return true
  if (r >= 160 && g <= 90 && b >= 70 && r > g + 40) return true
  if (g > 40 || r < 16) return false
  if (r < g) return false
  if (b < 4 && r < 28) return false
  if (r > b + 25 && g >= b) return false
  if (b > r + 10 && g >= (r * 7) / 10) return false
  return true
}
