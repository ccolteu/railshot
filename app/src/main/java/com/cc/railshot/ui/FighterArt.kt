package com.cc.railshot.ui

import com.cc.railshot.game.BallTailBand
import com.cc.railshot.game.Fighter
import com.cc.railshot.game.PaddlePose

data class FighterArt(
  val selectFullBody: String? = null,
  val face: String? = null,
  val vsLeft: String? = null,
  val vsRight: String? = null,
  val nameSelect: String? = null,
  val nameVs: String? = null,
  val idleLeft: String? = null,
  val walkLeft: String? = null,
  val hitLeft: String? = null,
  val idleRight: String? = null,
  val walkRight: String? = null,
  val hitRight: String? = null,
  val ending: String? = null,
  val wins: String? = null,
)

object UiArt {
  const val PLAYER_SELECT = "ui_player_select.png"
  const val VS = "ui_vs.png"
  const val SELECT_BG = "ui_select_bg.png"
  const val VS_BG = "ui_vs_bg.png"
  const val FIGHT = "ui_fight.png"
  const val ROUND = "ui_round.png"
  const val TITLE = "ui_title.png"
  const val BADGE_1P = "ui_1p.png"
  const val BADGE_2P = "ui_2p.png"
  const val BTN_SELECT = "ui_btn_select.png"
  const val BTN_START = "ui_btn_start.png"
  const val ARROW_LEFT = "ui_arrow_left.png"
  const val FACE_FRAME_ON = "ui_face_frame_on.png"
  const val FACE_FRAME_OFF = "ui_face_frame_off.png"
  const val BALL = "ui_ball.png"

  /** Packed KEY hole radius in every `ui_ball_tail_*.png`. */
  const val TAIL_HOLE_R = 56f

  fun ballTail(band: BallTailBand, left: Boolean): String {
    val length =
      when (band) {
        BallTailBand.SHORT -> "short"
        BallTailBand.MEDIUM -> "medium"
        BallTailBand.LONG -> "long"
      }
    val side = if (left) "left" else "right"
    return "ui_ball_tail_${length}_$side.png"
  }

  /** Pocket center in PNG pixels (KEY hole, radius [TAIL_HOLE_R]). */
  fun ballTailPocket(path: String, width: Int, height: Int): Pair<Float, Float> {
    val fromRight =
      when {
        path.contains("short_left") -> 66f
        path.contains("short_right") -> 81f
        path.contains("medium_left") -> 69f
        path.contains("medium_right") -> 76f
        path.contains("long_left") -> 68f
        path.contains("long_right") -> 67f
        else -> 68f
      }
    return (width - fromRight) to (height / 2f)
  }

  fun roundNum(n: Int): String = "ui_num_${n.coerceIn(1, 3)}.png"
}

fun FighterArt.courtFrame(left: Boolean, pose: PaddlePose): String? =
  when (pose) {
    PaddlePose.IDLE -> if (left) idleLeft else idleRight
    PaddlePose.WALK -> if (left) walkLeft else walkRight
    PaddlePose.HIT -> if (left) hitLeft else hitRight
  }

fun FighterArt.assetPaths(): List<String> =
  listOfNotNull(
    selectFullBody,
    face,
    vsLeft,
    vsRight,
    nameSelect,
    nameVs,
    idleLeft,
    walkLeft,
    hitLeft,
    idleRight,
    walkRight,
    hitRight,
    ending,
    wins,
  )

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
        idleLeft = "rivet/rivet_game_idle_left.png",
        walkLeft = "rivet/rivet_game_walk_left.png",
        hitLeft = "rivet/rivet_game_hit_left.png",
        idleRight = "rivet/rivet_game_idle_right.png",
        walkRight = "rivet/rivet_game_walk_right.png",
        hitRight = "rivet/rivet_game_hit_right.png",
        ending = "rivet/rivet_ending.png",
        wins = "rivet/rivet_wins.png",
      )
    Fighter.ASH ->
      FighterArt(
        selectFullBody = "ash/ash_select_fullbody.png",
        face = "ash/ash_face.png",
        vsLeft = "ash/ash_vs_left.png",
        vsRight = "ash/ash_vs_right.png",
        nameSelect = "ash/ash_name_select.png",
        nameVs = "ash/ash_name_vs.png",
        idleLeft = "ash/ash_game_idle_left.png",
        walkLeft = "ash/ash_game_walk_left.png",
        hitLeft = "ash/ash_game_hit_left.png",
        idleRight = "ash/ash_game_idle_right.png",
        walkRight = "ash/ash_game_walk_right.png",
        hitRight = "ash/ash_game_hit_right.png",
        ending = "ash/ash_ending.png",
        wins = "ash/ash_wins.png",
      )
    Fighter.KITE ->
      FighterArt(
        selectFullBody = "kite/kite_select_fullbody.png",
        face = "kite/kite_face.png",
        vsLeft = "kite/kite_vs_left.png",
        vsRight = "kite/kite_vs_right.png",
        nameSelect = "kite/kite_name_select.png",
        nameVs = "kite/kite_name_vs.png",
        idleLeft = "kite/kite_game_idle_left.png",
        walkLeft = "kite/kite_game_walk_left.png",
        hitLeft = "kite/kite_game_hit_left.png",
        idleRight = "kite/kite_game_idle_right.png",
        walkRight = "kite/kite_game_walk_right.png",
        hitRight = "kite/kite_game_hit_right.png",
        ending = "kite/kite_ending.png",
        wins = "kite/kite_wins.png",
      )
    Fighter.MARU ->
      FighterArt(
        selectFullBody = "maru/maru_select_fullbody.png",
        face = "maru/maru_face.png",
        vsLeft = "maru/maru_vs_left.png",
        vsRight = "maru/maru_vs_right.png",
        nameSelect = "maru/maru_name_select.png",
        nameVs = "maru/maru_name_vs.png",
        idleLeft = "maru/maru_game_idle_left.png",
        walkLeft = "maru/maru_game_walk_left.png",
        hitLeft = "maru/maru_game_hit_left.png",
        idleRight = "maru/maru_game_idle_right.png",
        walkRight = "maru/maru_game_walk_right.png",
        hitRight = "maru/maru_game_hit_right.png",
        ending = "maru/maru_ending.png",
        wins = "maru/maru_wins.png",
      )
    Fighter.QUILL ->
      FighterArt(
        selectFullBody = "quill/quill_select_fullbody.png",
        face = "quill/quill_face.png",
        vsLeft = "quill/quill_vs_left.png",
        vsRight = "quill/quill_vs_right.png",
        nameSelect = "quill/quill_name_select.png",
        nameVs = "quill/quill_name_vs.png",
        idleLeft = "quill/quill_game_idle_left.png",
        walkLeft = "quill/quill_game_walk_left.png",
        hitLeft = "quill/quill_game_hit_left.png",
        idleRight = "quill/quill_game_idle_right.png",
        walkRight = "quill/quill_game_walk_right.png",
        hitRight = "quill/quill_game_hit_right.png",
        ending = "quill/quill_ending.png",
        wins = "quill/quill_wins.png",
      )
    Fighter.HEX ->
      FighterArt(
        selectFullBody = "hex/hex_select_fullbody.png",
        face = "hex/hex_face.png",
        vsLeft = "hex/hex_vs_left.png",
        vsRight = "hex/hex_vs_right.png",
        nameSelect = "hex/hex_name_select.png",
        nameVs = "hex/hex_name_vs.png",
        idleLeft = "hex/hex_game_idle_left.png",
        walkLeft = "hex/hex_game_walk_left.png",
        hitLeft = "hex/hex_game_hit_left.png",
        idleRight = "hex/hex_game_idle_right.png",
        walkRight = "hex/hex_game_walk_right.png",
        hitRight = "hex/hex_game_hit_right.png",
        ending = "hex/hex_ending.png",
        wins = "hex/hex_wins.png",
      )
  }
