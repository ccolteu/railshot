package com.cc.railshot

import android.app.Application
import com.cc.railshot.game.HighScoreManager

class RailshotApp : Application() {
  override fun onCreate() {
    super.onCreate()
    HighScoreManager.loadHighScores(this)
  }
}
