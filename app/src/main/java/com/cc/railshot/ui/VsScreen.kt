package com.cc.railshot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cc.railshot.SoundManager
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
    Box(modifier = Modifier.fillMaxSize()) {
      ArcadeWallpaper(UiArt.VS_BG)
      Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
          VsBust(
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
          VsBust(
            fighter = second,
            bust = right,
            modifier = Modifier.weight(1f).fillMaxHeight(),
          )
        }
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 20.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
              FighterNameplate(
                fighter = first,
                kind = NameplateKind.VS,
                modifier = Modifier.fillMaxWidth(0.85f).height(36.dp),
              )
            }
            Spacer(modifier = Modifier.weight(0.42f))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
              FighterNameplate(
                fighter = second,
                kind = NameplateKind.VS,
                modifier = Modifier.fillMaxWidth(0.85f).height(36.dp),
              )
            }
          }
          StartButton(
            onClick = onContinue,
            modifier = Modifier.align(Alignment.Center),
          )
        }
      }
    }
  }
}

@Composable
private fun StartButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  MagentaKeyedImage(
    assetPath = UiArt.BTN_START,
    contentDescription = "START",
    modifier =
      modifier
        .requiredHeight(56.dp)
        .aspectRatio(264f / 150f)
        .clickable {
          SoundManager.instance.playSFX(SoundManager.SFX_SELECT)
          onClick()
        },
    contentScale = ContentScale.Fit,
  )
}

@Composable
private fun VsBust(fighter: Fighter, bust: String?, modifier: Modifier) {
  Box(
    modifier = modifier.padding(horizontal = 8.dp, vertical = 12.dp),
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
}
