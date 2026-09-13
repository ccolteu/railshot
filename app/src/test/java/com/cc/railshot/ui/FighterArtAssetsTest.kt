package com.cc.railshot.ui

import com.cc.railshot.game.Fighter
import java.io.File
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
}
