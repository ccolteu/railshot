package com.cc.railshot.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.cc.railshot.ui.Cream
import com.cc.railshot.ui.Ink
import com.cc.railshot.ui.YouPaddle

private val Scheme =
  darkColorScheme(
    primary = YouPaddle,
    onPrimary = Ink,
    background = Ink,
    onBackground = Cream,
    surface = Ink,
    onSurface = Cream,
  )

@Composable
fun RailshotTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = Scheme, content = content)
}
