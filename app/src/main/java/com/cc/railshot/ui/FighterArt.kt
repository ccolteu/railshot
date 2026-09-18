package com.cc.railshot.ui

import com.cc.railshot.game.BallTailBand
import com.cc.railshot.game.Fighter
import com.cc.railshot.game.PaddlePose
import com.cc.railshot.game.Side

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
  /** Match floor. Loaded from the 2P / rival fighter. */
  val court: String? = null,
  /** Cabinet overlay. Loaded from the 2P / rival fighter. */
  val cabinet: String? = null,
  /** Standing P1 gutter gate. Loaded from the 2P / rival. */
  val chipYou: String? = null,
  /** Standing P2 gutter gate. Loaded from the 2P / rival. */
  val chipCpu: String? = null,
  /** Flattened P1 gutter gate. Loaded from the 2P / rival. */
  val chipYouDown: String? = null,
  /** Flattened P2 gutter gate. Loaded from the 2P / rival. */
  val chipCpuDown: String? = null,
)

object UiArt {
  const val PLAYER_SELECT = "ui_player_select.png"
  const val VS = "ui_vs.png"
  const val SELECT_BG = "ui_select_bg.png"
  const val VS_BG = "ui_vs_bg.png"
  const val FIGHT = "ui_fight.png"
  const val ROUND = "ui_round.png"
  const val YOU_WIN = "ui_you_win.png"
  const val YOU_LOSE = "ui_you_lose.png"
  const val TITLE = "ui_title.png"
  const val BADGE_1P = "ui_1p.png"
  const val BADGE_2P = "ui_2p.png"
  const val BTN_SELECT = "ui_btn_select.png"
  const val BTN_CONTINUE = "ui_btn_continue.png"
  const val BTN_START = "ui_btn_start.png"
  const val BTN_EASY = "ui_btn_easy.png"
  const val BTN_HARD = "ui_btn_hard.png"
  const val ARROW_LEFT = "ui_arrow_left.png"
  const val FACE_FRAME_ON = "ui_face_frame_on.png"
  const val FACE_FRAME_OFF = "ui_face_frame_off.png"
  const val BALL = "ui_ball.png"
  const val ICE_BALL = "ui_ice_ball.png"

  fun courtWall(frame: Int): String =
    when (frame.coerceIn(0, 2)) {
      0 -> "ash/ash_court_wall_a.png"
      1 -> "ash/ash_court_wall_b.png"
      else -> "ash/ash_court_wall_c.png"
    }

  const val COURT_TRACE = "rivet/rivet_court_trace.png"
  const val HEX_CAR = "hex/hex_car.png"
  const val KITE_X = "kite/kite_court_x.png"
  const val MARU_LOG = "maru/maru_court_log.png"

  fun courtHawk(frame: Int): String =
    when (frame.coerceIn(0, 2)) {
      0 -> "quill/quill_court_hawk_a.png"
      1 -> "quill/quill_court_hawk_c.png"
      else -> "quill/quill_court_hawk_b.png"
    }

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

  fun iceBallTail(band: BallTailBand, left: Boolean): String =
    "ui_ice_ball_tail_" +
      when (band) {
        BallTailBand.SHORT -> "short"
        BallTailBand.MEDIUM -> "medium"
        BallTailBand.LONG -> "long"
      } +
      if (left) "_left.png" else "_right.png"

  fun iceBurst(frame: Int): String =
    when (frame.coerceIn(0, 2)) {
      0 -> "ui_ice_burst_a.png"
      1 -> "ui_ice_burst_b.png"
      else -> "ui_ice_burst_c.png"
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

fun FighterArt.stageCourt(): String = court ?: "rivet/rivet_court.png"

fun FighterArt.stageCabinet(): String = cabinet ?: "rivet/rivet_ui_cabinet.png"

fun FighterArt.stageChip(side: Side, standing: Boolean): String {
  val path =
    when {
      standing && side == Side.YOU -> chipYou
      standing -> chipCpu
      side == Side.YOU -> chipYouDown
      else -> chipCpuDown
    }
  return path
    ?: when {
      standing && side == Side.YOU -> "rivet/rivet_ui_chip_you.png"
      standing -> "rivet/rivet_ui_chip_cpu.png"
      side == Side.YOU -> "rivet/rivet_ui_chip_you_down.png"
      else -> "rivet/rivet_ui_chip_cpu_down.png"
    }
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
        court = "rivet/rivet_court.png",
        cabinet = "rivet/rivet_ui_cabinet.png",
        chipYou = "rivet/rivet_ui_chip_you.png",
        chipCpu = "rivet/rivet_ui_chip_cpu.png",
        chipYouDown = "rivet/rivet_ui_chip_you_down.png",
        chipCpuDown = "rivet/rivet_ui_chip_cpu_down.png",
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
        court = "ash/ash_court.png",
        cabinet = "ash/ash_ui_cabinet.png",
        chipYou = "ash/ash_ui_chip_you.png",
        chipCpu = "ash/ash_ui_chip_cpu.png",
        chipYouDown = "ash/ash_ui_chip_you_down.png",
        chipCpuDown = "ash/ash_ui_chip_cpu_down.png",
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
        court = "kite/kite_court.png",
        cabinet = "kite/kite_ui_cabinet.png",
        chipYou = "kite/kite_ui_chip_you.png",
        chipCpu = "kite/kite_ui_chip_cpu.png",
        chipYouDown = "kite/kite_ui_chip_you_down.png",
        chipCpuDown = "kite/kite_ui_chip_cpu_down.png",
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
        court = "maru/maru_court.png",
        cabinet = "maru/maru_ui_cabinet.png",
        chipYou = "maru/maru_ui_chip_you.png",
        chipCpu = "maru/maru_ui_chip_cpu.png",
        chipYouDown = "maru/maru_ui_chip_you_down.png",
        chipCpuDown = "maru/maru_ui_chip_cpu_down.png",
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
        court = "quill/quill_court.png",
        cabinet = "quill/quill_ui_cabinet.png",
        chipYou = "quill/quill_ui_chip_you.png",
        chipCpu = "quill/quill_ui_chip_cpu.png",
        chipYouDown = "quill/quill_ui_chip_you_down.png",
        chipCpuDown = "quill/quill_ui_chip_cpu_down.png",
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
        court = "hex/hex_court.png",
        cabinet = "hex/hex_ui_cabinet.png",
        chipYou = "hex/hex_ui_chip_you.png",
        chipCpu = "hex/hex_ui_chip_cpu.png",
        chipYouDown = "hex/hex_ui_chip_you_down.png",
        chipCpuDown = "hex/hex_ui_chip_cpu_down.png",
      )
  }
