package com.cc.railshot.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectSessionTest {
  @Test
  fun arrowsWrapAndDoNotConfirm() {
    val session = SelectSession()
    assertEquals(Fighter.RIVET, session.highlighted)
    session.moveLeft()
    assertEquals(Fighter.HEX, session.highlighted)
    session.moveRight()
    session.moveRight()
    assertEquals(Fighter.ASH, session.highlighted)
    assertEquals(SelectStep.PICK_FIRST, session.step)
    assertEquals(null, session.firstPick)
  }

  @Test
  fun firstSelectThenSecondLocksIncludingMirrorMatch() {
    val session = SelectSession()
    assertTrue(!session.confirm())
    assertEquals(Fighter.RIVET, session.firstPick)
    assertEquals(SelectStep.PICK_SECOND, session.step)
    session.moveRight()
    session.moveRight()
    session.moveRight()
    session.moveRight()
    session.moveRight()
    session.moveRight()
    assertEquals(Fighter.RIVET, session.highlighted)
    assertTrue(session.confirm())
    assertEquals(Fighter.RIVET, session.secondPick)
    assertEquals(SelectStep.LOCKED, session.step)
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
