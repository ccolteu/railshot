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
  }
}
