package com.cc.railshot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cc.railshot.game.Fighter
import com.cc.railshot.game.SELECT_LINGER_MS
import com.cc.railshot.game.SelectSession
import com.cc.railshot.game.SelectStep
import kotlinx.coroutines.delay

@Composable
fun CharacterSelectScreen(onBothSelected: (Fighter, Fighter) -> Unit) {
  val session = remember { SelectSession() }
  var epoch by remember { mutableIntStateOf(0) }
  epoch
  val highlighted = session.highlighted
  val art = highlighted.art()
  val locked = session.step == SelectStep.LOCKED

  LaunchedEffect(epoch, session.step) {
    if (session.step == SelectStep.LOCKED) {
      delay(SELECT_LINGER_MS)
      val first = session.firstPick ?: return@LaunchedEffect
      val second = session.secondPick ?: return@LaunchedEffect
      onBothSelected(first, second)
    }
  }

  Letterbox43 {
    Box(modifier = Modifier.fillMaxSize()) {
      RailshotStampWallpaper()
      MagentaKeyedPortrait(
        assetPath = art.selectFullBody,
        label = highlighted.displayName,
        modifier =
          Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .fillMaxWidth(0.58f)
            .padding(vertical = 12.dp),
        contentScale = ContentScale.FillHeight,
      )
      Column(
        modifier =
          Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth(0.44f)
            .padding(start = 16.dp, top = 18.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        MagentaKeyedImage(
          assetPath = UiArt.PLAYER_SELECT,
          contentDescription = "PLAYER SELECT",
          modifier = Modifier.fillMaxWidth().aspectRatio(960f / 240f),
          contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.height(12.dp))
        FighterNameplate(
          fighter = highlighted,
          kind = NameplateKind.SELECT,
          modifier = Modifier.fillMaxWidth(0.92f).height(40.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        ArcadeButton(
          label = "SELECT",
          enabled = !locked,
          onClick = {
            session.confirm()
            epoch++
          },
        )
      }
      Row(
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        StripArrow("◀", enabled = !locked) {
          session.moveLeft()
          epoch++
        }
        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Fighter.roster.forEachIndexed { index, fighter ->
            FaceTile(
              fighter = fighter,
              selected = index == session.cursorIndex,
              p1 = session.firstPick == fighter,
              showP2 =
                (session.step == SelectStep.PICK_SECOND || session.step == SelectStep.LOCKED) &&
                  (index == session.cursorIndex || session.secondPick == fighter),
              modifier = Modifier.weight(1f),
            )
          }
        }
        StripArrow("▶", enabled = !locked) {
          session.moveRight()
          epoch++
        }
      }
      Text(
        text = "CREDIT 00",
        color = NameBlue,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier =
          Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 10.dp, bottom = 2.dp),
      )
    }
  }
}

@Composable
private fun MagentaKeyedPortrait(
  assetPath: String?,
  label: String,
  modifier: Modifier,
  contentScale: ContentScale = ContentScale.Fit,
) {
  if (assetPath != null) {
        MagentaKeyedImage(
          assetPath = assetPath,
          contentDescription = label,
          modifier = modifier,
          contentScale = contentScale,
          alignment = Alignment.BottomEnd,
        )
  } else {
    Box(
      modifier =
        modifier
          .background(PlaceholderFill)
          .border(2.dp, TileBorder),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        color = Cream,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun ArcadeButton(
  label: String,
  enabled: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Text(
    text = label,
    color = Ink,
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Black,
    fontSize = 16.sp,
    modifier =
      modifier
        .background(if (enabled) SelectYellow else Mute, RoundedCornerShape(4.dp))
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 8.dp),
  )
}

@Composable
private fun StripArrow(label: String, enabled: Boolean, onClick: () -> Unit) {
  Box(
    modifier =
      Modifier
        .size(width = 36.dp, height = 72.dp)
        .background(if (enabled) SelectYellow else Mute, RoundedCornerShape(4.dp))
        .clickable(enabled = enabled, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Text(label, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
  }
}

@Composable
private fun FaceTile(
  fighter: Fighter,
  selected: Boolean,
  p1: Boolean,
  showP2: Boolean,
  modifier: Modifier = Modifier,
) {
  val face = fighter.art().face
  Box(
    modifier =
      modifier
        .aspectRatio(1f)
        .border(3.dp, if (selected) SelectYellow else TileBorder)
        .background(Ink),
  ) {
    if (face != null) {
      MagentaKeyedImage(
        assetPath = face,
        contentDescription = fighter.displayName,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
    } else {
      Box(Modifier.fillMaxSize().background(PlaceholderFill), contentAlignment = Alignment.Center) {
        Text(
          fighter.displayName.take(1),
          color = Cream,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
        )
      }
    }
    val badge =
      when {
        showP2 -> "2P"
        p1 -> "1P"
        selected -> "1P"
        else -> null
      }
    if (badge != null) {
      Text(
        text = badge,
        color = Cream,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
        modifier =
          Modifier
            .align(Alignment.TopStart)
            .background(SelectRed)
            .padding(horizontal = 4.dp, vertical = 1.dp),
      )
    }
  }
}
