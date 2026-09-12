package com.cc.railshot.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cc.railshot.SoundManager
import com.cc.railshot.game.Fighter

private sealed interface AppScreen {
  data object Select : AppScreen

  data class Versus(val first: Fighter, val second: Fighter) : AppScreen

  data class Match(val first: Fighter, val second: Fighter) : AppScreen
}

@Composable
fun RailshotApp() {
  LaunchedEffect(Unit) {
    SoundManager.instance.switchBGM(SoundManager.BGM_MATCH)
  }
  var screen by remember { mutableStateOf<AppScreen>(AppScreen.Select) }
  when (val current = screen) {
    AppScreen.Select ->
      CharacterSelectScreen { first, second ->
        screen = AppScreen.Versus(first, second)
      }
    is AppScreen.Versus ->
      VsScreen(first = current.first, second = current.second) {
        screen = AppScreen.Match(current.first, current.second)
      }
    is AppScreen.Match -> GameScreen(you = current.first, rival = current.second)
  }
}
