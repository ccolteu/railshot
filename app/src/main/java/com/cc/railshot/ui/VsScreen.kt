package com.cc.railshot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cc.railshot.game.Fighter

@Composable
fun VsScreen(
  first: Fighter,
  second: Fighter,
  onContinue: () -> Unit,
) {
  val left = first.art().vsLeft
  val right = second.art().vsRight
  Letterbox43 {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .clickable(onClick = onContinue),
    ) {
      ArcadeWallpaper(UiArt.VS_BG)
      Row(modifier = Modifier.fillMaxSize()) {
        VsSlot(
          fighter = first,
          bust = left,
          modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        Box(
          modifier = Modifier.fillMaxHeight().weight(0.42f),
          contentAlignment = Alignment.Center,
        ) {
          MagentaKeyedImage(
            assetPath = UiArt.VS,
            contentDescription = "VS",
            modifier = Modifier.fillMaxWidth().height(140.dp),
            contentScale = ContentScale.Fit,
          )
        }
        VsSlot(
          fighter = second,
          bust = right,
          modifier = Modifier.weight(1f).fillMaxHeight(),
        )
      }
      Text(
        text = "TAP TO FIGHT",
        color = Cream,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 10.dp),
      )
      Text(
        text = "CREDIT 00",
        color = NameBlue,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier =
          Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 10.dp, bottom = 8.dp),
      )
    }
  }
}

@Composable
private fun VsSlot(fighter: Fighter, bust: String?, modifier: Modifier) {
  Column(
    modifier = modifier.padding(horizontal = 8.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth(),
      contentAlignment = Alignment.BottomCenter,
    ) {
      if (bust != null) {
        MagentaKeyedImage(
          assetPath = bust,
          contentDescription = fighter.displayName,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Fit,
        )
      } else {
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(24.dp)
              .background(PlaceholderFill)
              .border(2.dp, TileBorder),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = fighter.displayName,
            color = Cream,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
    FighterNameplate(
      fighter = fighter,
      kind = NameplateKind.VS,
      modifier = Modifier.padding(top = 8.dp, bottom = 20.dp).height(36.dp).fillMaxWidth(0.85f),
    )
  }
}
