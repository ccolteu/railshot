package com.cc.railshot.ui

import com.cc.railshot.game.Fighter
import com.cc.railshot.game.PaddlePose
import com.cc.railshot.game.World
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertTrue
import org.junit.Test

class FighterArtAssetsTest {
  @Test
  fun everyRosterArtPathExistsUnderArt() {
    val artRoot = File("../art")
    assertTrue("expected art dir at ${artRoot.canonicalPath}", artRoot.isDirectory)
    val missing = mutableListOf<String>()
    for (fighter in Fighter.roster) {
      val art = fighter.art()
      val paths = art.assetPaths()
      assertTrue("${fighter.name} should have a full art set", paths.size == 14)
      for (path in paths) {
        if (!File(artRoot, path).isFile) missing += "${fighter.key}/$path"
      }
    }
    assertTrue("missing art files:\n${missing.joinToString("\n")}", missing.isEmpty())
  }

  @Test
  fun courtFramesArePacked192x233() {
    val artRoot = File("../art")
    val wrong = mutableListOf<String>()
    for (fighter in Fighter.roster) {
      val art = fighter.art()
      for (left in arrayOf(true, false)) {
        for (pose in PaddlePose.entries) {
          val path = art.courtFrame(left, pose) ?: continue
          val (w, h) = pngSize(File(artRoot, path))
          if (w != World.SPRITE_W || h != World.SPRITE_H) {
            wrong += "$path ${w}x$h"
          }
        }
      }
    }
    assertTrue("court frames not 192x233:\n${wrong.joinToString("\n")}", wrong.isEmpty())
  }

  private fun pngSize(file: File): Pair<Int, Int> {
    val bytes = file.readBytes()
    val buf = ByteBuffer.wrap(bytes, 16, 8).order(ByteOrder.BIG_ENDIAN)
    return buf.int to buf.int
  }
}
