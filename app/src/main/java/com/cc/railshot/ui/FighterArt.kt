package com.cc.railshot.ui

import com.cc.railshot.game.Fighter

data class FighterArt(
  val selectFullBody: String? = null,
  val face: String? = null,
  val vsLeft: String? = null,
  val vsRight: String? = null,
  val nameSelect: String? = null,
  val nameVs: String? = null,
)

object UiArt {
  const val PLAYER_SELECT = "ui_player_select.png"
  const val VS = "ui_vs.png"
}

fun Fighter.art(): FighterArt =
  when (this) {
    Fighter.RIVET ->
      FighterArt(
        selectFullBody = "rivet/rivet_select_fullbody.png",
        face = "rivet/rivet_face.png",
        vsLeft = "rivet/rivet_vs_left.png",
        vsRight = "rivet/rivet_vs_right.png",
        nameSelect = "rivet/rivet_name_select.png",
        nameVs = "rivet/rivet_name_vs.png",
      )
    Fighter.ASH ->
      FighterArt(
        selectFullBody = "ash/ash_select_fullbody.png",
        face = "ash/ash_face.png",
        vsLeft = "ash/ash_vs_left.png",
        vsRight = "ash/ash_vs_right.png",
        nameSelect = "ash/ash_name_select.png",
        nameVs = "ash/ash_name_vs.png",
      )
    Fighter.KITE ->
      FighterArt(nameSelect = "kite/kite_name_select.png", nameVs = "kite/kite_name_vs.png")
    Fighter.MARU ->
      FighterArt(nameSelect = "maru/maru_name_select.png", nameVs = "maru/maru_name_vs.png")
    Fighter.QUILL ->
      FighterArt(nameSelect = "quill/quill_name_select.png", nameVs = "quill/quill_name_vs.png")
    Fighter.HEX ->
      FighterArt(nameSelect = "hex/hex_name_select.png", nameVs = "hex/hex_name_vs.png")
  }
