package com.cc.railshot.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

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
fun ArcadeWallpaper(assetPath: String, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val bitmap = remember(assetPath) { loadOpaqueAsset(context, assetPath) }
  Image(
    bitmap = bitmap,
    contentDescription = null,
    modifier = modifier.fillMaxSize(),
    contentScale = ContentScale.FillBounds,
    filterQuality = FilterQuality.None,
  )
}
