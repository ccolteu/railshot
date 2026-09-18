package com.cc.railshot.game

enum class Fighter(val key: String, val displayName: String) {
  RIVET("rivet", "RIVET"),
  ASH("ash", "ASH"),
  KITE("kite", "KITE"),
  MARU("maru", "MARU"),
  QUILL("quill", "QUILL"),
  HEX("hex", "HEX");

  companion object {
    val roster: List<Fighter> = entries
    val selectOrder: List<Fighter> = listOf(ASH, KITE, RIVET, HEX, QUILL, MARU)

    fun arcadeRival(you: Fighter): Fighter {
      val i = selectOrder.indexOf(you).coerceAtLeast(0)
      return selectOrder[(i + 1) % selectOrder.size]
    }

    /** Next CPU after a win, or null once every other fighter is beaten. */
    fun nextArcadeRival(you: Fighter, current: Fighter): Fighter? {
      val n = selectOrder.size
      if (n <= 1) return null
      val youI = selectOrder.indexOf(you).coerceAtLeast(0)
      val nextI = (selectOrder.indexOf(current).coerceAtLeast(0) + 1) % n
      if (nextI == youI) return null
      return selectOrder[nextI]
    }
  }
}
