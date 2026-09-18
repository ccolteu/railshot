package com.cc.railshot.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HighScoreManagerTest {
  @Before
  fun reset() {
    HighScoreManager.resetTablesForTest()
  }

  @Test
  fun easyAndHardTablesStartApart() {
    assertTrue(HighScoreManager.scoreAt(CpuLevel.HARD.table, 0) > HighScoreManager.scoreAt(CpuLevel.EASY.table, 0))
  }

  @Test
  fun insertOnEasyLeavesHardUntouched() {
    val hardTop = HighScoreManager.scoreAt(CpuLevel.HARD.table, 0)
    val easyTop = HighScoreManager.scoreAt(CpuLevel.EASY.table, 0)
    val score = easyTop + 1
    assertTrue(HighScoreManager.checkIfQualifies(score, CpuLevel.EASY.table))
    assertTrue(
      HighScoreManager.insertInMemory(score, 'Z', 'E', 'D', fightsWon = 2, difficultyIndex = CpuLevel.EASY.table),
    )
    assertEquals(score, HighScoreManager.scoreAt(CpuLevel.EASY.table, 0))
    assertEquals('Z', HighScoreManager.nameChar(CpuLevel.EASY.table, 0, 0))
    assertEquals(2, HighScoreManager.fightsAt(CpuLevel.EASY.table, 0))
    assertEquals(hardTop, HighScoreManager.scoreAt(CpuLevel.HARD.table, 0))
  }

  @Test
  fun belowLastPlaceDoesNotQualify() {
    val last = HighScoreManager.scoreAt(CpuLevel.HARD.table, HighScoreManager.SLOT_COUNT - 1)
    assertFalse(HighScoreManager.checkIfQualifies(last, CpuLevel.HARD.table))
  }
}
