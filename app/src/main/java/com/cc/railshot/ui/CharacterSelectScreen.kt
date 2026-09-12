package com.cc.railshot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
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
      ArcadeWallpaper(UiArt.SELECT_BG)
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
            .fillMaxWidth(0.50f)
            .fillMaxHeight(0.72f)
            .padding(start = 16.dp, top = 18.dp, end = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
      ) {
        MagentaKeyedImage(
          assetPath = UiArt.PLAYER_SELECT,
          contentDescription = "SELECT FIGHTER",
          modifier = Modifier.fillMaxWidth().aspectRatio(976f / 417f),
          contentScale = ContentScale.Fit,
        )
        FighterNameplate(
          fighter = highlighted,
          kind = NameplateKind.SELECT,
          modifier = Modifier.fillMaxWidth(0.55f).height(28.dp),
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          StripArrow(flip = false, enabled = !locked) {
            session.moveLeft()
            epoch++
          }
          SelectButton(
            enabled = !locked,
            onClick = {
              session.confirm()
              epoch++
            },
          )
          StripArrow(flip = true, enabled = !locked) {
            session.moveRight()
            epoch++
          }
        }
      }
      Row(
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
private fun SelectButton(
  enabled: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  MagentaKeyedImage(
    assetPath = UiArt.BTN_SELECT,
    contentDescription = "SELECT",
    modifier =
      modifier
        .height(56.dp)
        .aspectRatio(264f / 150f)
        .alpha(if (enabled) 1f else 0.4f)
        .clickable(enabled = enabled, onClick = onClick),
    contentScale = ContentScale.Fit,
  )
}

@Composable
private fun StripArrow(flip: Boolean, enabled: Boolean, onClick: () -> Unit) {
  MagentaKeyedImage(
    assetPath = UiArt.ARROW_LEFT,
    contentDescription = if (flip) "Next" else "Previous",
    modifier =
      Modifier
        .requiredSize(56.dp)
        .graphicsLayer { if (flip) scaleX = -1f }
        .alpha(if (enabled) 1f else 0.4f)
        .clickable(enabled = enabled, onClick = onClick),
    contentScale = ContentScale.FillBounds,
  )
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
  Box(modifier = modifier.aspectRatio(1f).background(Ink)) {
    if (face != null) {
      MagentaKeyedImage(
        assetPath = face,
        contentDescription = fighter.displayName,
        modifier = Modifier.fillMaxSize().padding(8.dp),
        contentScale = ContentScale.Crop,
      )
    } else {
      Box(
        Modifier.fillMaxSize().padding(8.dp).background(PlaceholderFill),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          fighter.displayName.take(1),
          color = Cream,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
        )
      }
    }
    MagentaKeyedImage(
      assetPath = if (selected) UiArt.FACE_FRAME_ON else UiArt.FACE_FRAME_OFF,
      contentDescription = null,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Fit,
    )
    val badge =
      when {
        showP2 -> UiArt.BADGE_2P
        p1 -> UiArt.BADGE_1P
        selected -> UiArt.BADGE_1P
        else -> null
      }
    if (badge != null) {
      MagentaKeyedImage(
        assetPath = badge,
        contentDescription = null,
        modifier =
          Modifier
            .align(Alignment.TopStart)
            .padding(2.dp)
            .fillMaxWidth(0.42f)
            .aspectRatio(1f),
        contentScale = ContentScale.Fit,
      )
    }
  }
}
