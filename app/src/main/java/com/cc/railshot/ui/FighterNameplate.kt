package com.cc.railshot.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cc.railshot.game.Fighter

enum class NameplateKind {
  SELECT,
  VS,
}

@Composable
fun FighterNameplate(
  fighter: Fighter,
  kind: NameplateKind,
  modifier: Modifier = Modifier,
) {
  val art = fighter.art()
  val assetPath = if (kind == NameplateKind.SELECT) art.nameSelect else art.nameVs
  if (assetPath != null) {
    MagentaKeyedImage(
      assetPath = assetPath,
      contentDescription = fighter.displayName,
      modifier = modifier,
      contentScale = ContentScale.Fit,
    )
  } else {
    Text(
      text = fighter.displayName,
      color = NameBlue,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Black,
      fontSize = 20.sp,
      modifier = modifier,
    )
  }
}
