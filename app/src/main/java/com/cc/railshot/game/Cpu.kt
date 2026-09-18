package com.cc.railshot.game

/** How the 2P brain plays the paddle. */
enum class CpuStyle {
  /** Chases the ball, overcommits, recovers late. */
  SLUGGER,
  /** Holds a gate, smaller motion, cuts the line. */
  WALL,
  ;

  companion object {
    fun forFighter(fighter: Fighter): CpuStyle =
      when (fighter) {
        Fighter.MARU,
        Fighter.HEX,
        -> WALL
        Fighter.RIVET,
        Fighter.ASH,
        Fighter.KITE,
        Fighter.QUILL,
        -> SLUGGER
      }
  }
}

enum class CpuLevel {
  EASY,
  HARD,
  ;

  /** 1-based EEPROM table id, same shape as WW2 Blitz. */
  val table: Int get() = ordinal + 1
}
