package com.cc.railshot.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.cc.railshot.SoundManager
import com.cc.railshot.game.Fighter
import kotlinx.coroutines.delay

private sealed interface AppScreen {
  data object Title : AppScreen

  data object Select : AppScreen

  data class Versus(val first: Fighter, val second: Fighter) : AppScreen

  data class Match(val first: Fighter, val second: Fighter) : AppScreen
}

private const val TITLE_HOLD_MS = 5000L

@Composable
fun RailshotApp() {
  LaunchedEffect(Unit) {
    SoundManager.instance.switchBGM(SoundManager.BGM_MATCH)
  }
  var screen by remember { mutableStateOf<AppScreen>(AppScreen.Title) }
  when (val current = screen) {
    AppScreen.Title ->
      TitleScreen { screen = AppScreen.Select }
    AppScreen.Select ->
      CharacterSelectScreen { first, second ->
        screen = AppScreen.Versus(first, second)
      }
    is AppScreen.Versus ->
      VsScreen(first = current.first, second = current.second) {
        screen = AppScreen.Match(current.first, current.second)
      }
    is AppScreen.Match ->
      GameScreen(
        you = current.first,
        rival = current.second,
        onBackToSelect = { screen = AppScreen.Select },
      )
  }
}

@Composable
private fun TitleScreen(onFinished: () -> Unit) {
  var finished by remember { mutableStateOf(false) }
  fun finish() {
    if (finished) return
    finished = true
    onFinished()
  }
  LaunchedEffect(Unit) {
    delay(TITLE_HOLD_MS)
    finish()
  }
  Letterbox43 {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectTapGestures(onTap = { finish() })
          },
    ) {
      ArcadeWallpaper(UiArt.TITLE, modifier = Modifier.fillMaxSize())
    }
  }
}
