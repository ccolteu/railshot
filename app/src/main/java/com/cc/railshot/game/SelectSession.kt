package com.cc.railshot.game

enum class SelectStep {
  PICK_FIRST,
  PICK_SECOND,
  LOCKED,
}

const val SELECT_LINGER_MS = 1500L

class SelectSession {
  var cursorIndex: Int = 0
    private set
  var firstPick: Fighter? = null
    private set
  var secondPick: Fighter? = null
    private set

  val step: SelectStep
    get() =
      when {
        firstPick == null -> SelectStep.PICK_FIRST
        secondPick == null -> SelectStep.PICK_SECOND
        else -> SelectStep.LOCKED
      }

  val highlighted: Fighter
    get() = Fighter.roster[cursorIndex]

  fun moveLeft() {
    if (step == SelectStep.LOCKED) return
    cursorIndex = (cursorIndex + Fighter.roster.size - 1) % Fighter.roster.size
  }

  fun moveRight() {
    if (step == SelectStep.LOCKED) return
    cursorIndex = (cursorIndex + 1) % Fighter.roster.size
  }

  fun setCursor(index: Int) {
    if (step == SelectStep.LOCKED) return
    cursorIndex = index.mod(Fighter.roster.size)
  }

  fun confirm(): Boolean {
    if (step == SelectStep.LOCKED) return true
    if (firstPick == null) {
      firstPick = highlighted
    } else {
      secondPick = highlighted
    }
    return step == SelectStep.LOCKED
  }

  fun reset() {
    cursorIndex = 0
    firstPick = null
    secondPick = null
  }
}
