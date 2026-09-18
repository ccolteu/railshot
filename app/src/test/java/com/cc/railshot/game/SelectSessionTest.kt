package com.cc.railshot.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectSessionTest {
  @Test
  fun arrowsWrapAndDoNotConfirm() {
    val session = SelectSession()
    assertEquals(Fighter.ASH, session.highlighted)
    session.moveLeft()
    assertEquals(Fighter.MARU, session.highlighted)
    session.moveRight()
    session.moveRight()
    assertEquals(Fighter.KITE, session.highlighted)
    assertEquals(SelectStep.PICK_FIRST, session.step)
    assertEquals(null, session.firstPick)
  }

  @Test
  fun firstSelectThenSecondLocksIncludingMirrorMatch() {
    val session = SelectSession()
    assertTrue(!session.confirm())
    assertEquals(Fighter.ASH, session.firstPick)
    assertEquals(SelectStep.PICK_SECOND, session.step)
    session.moveRight()
    session.moveRight()
    session.moveRight()
    session.moveRight()
    session.moveRight()
    session.moveRight()
    assertEquals(Fighter.ASH, session.highlighted)
    assertTrue(session.confirm())
    assertEquals(Fighter.ASH, session.secondPick)
    assertEquals(SelectStep.LOCKED, session.step)
  }

  @Test
  fun arcadeLocksOnFirstSelectAndPicksTheNextRival() {
    val session = SelectSession()
    session.reset(arcade = true)
    assertEquals(Fighter.ASH, session.highlighted)
    assertTrue(session.confirm())
    assertEquals(Fighter.ASH, session.firstPick)
    assertEquals(Fighter.KITE, session.secondPick)
    assertEquals(SelectStep.LOCKED, session.step)
  }

  @Test
  fun arcadeHexFightsQuillNext() {
    val session = SelectSession()
    session.reset(arcade = true)
    session.moveRight()
    session.moveRight()
    session.moveRight()
    assertEquals(Fighter.HEX, session.highlighted)
    assertTrue(session.confirm())
    assertEquals(Fighter.QUILL, session.secondPick)
  }

  @Test
  fun arcadeLadderSkipsYouAndEndsAfterTheLoop() {
    assertEquals(Fighter.KITE, Fighter.arcadeRival(Fighter.ASH))
    assertEquals(Fighter.RIVET, Fighter.nextArcadeRival(Fighter.ASH, Fighter.KITE))
    assertEquals(Fighter.HEX, Fighter.nextArcadeRival(Fighter.ASH, Fighter.RIVET))
    assertEquals(Fighter.QUILL, Fighter.nextArcadeRival(Fighter.ASH, Fighter.HEX))
    assertEquals(Fighter.MARU, Fighter.nextArcadeRival(Fighter.ASH, Fighter.QUILL))
    assertEquals(null, Fighter.nextArcadeRival(Fighter.ASH, Fighter.MARU))
    assertEquals(Fighter.MARU, Fighter.nextArcadeRival(Fighter.HEX, Fighter.QUILL))
    assertEquals(null, Fighter.nextArcadeRival(Fighter.HEX, Fighter.RIVET))
  }

  @Test
  fun lockedSessionIgnoresArrows() {
    val session = SelectSession()
    session.confirm()
    session.moveRight()
    session.confirm()
    val locked = session.cursorIndex
    session.moveLeft()
    session.moveRight()
    assertEquals(locked, session.cursorIndex)
    assertEquals(SelectStep.LOCKED, session.step)
  }
}
