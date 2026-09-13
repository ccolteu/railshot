package com.cc.railshot.game

enum class Phase {
  ROUND,
  SERVE,
  PLAYING,
  YOU_WIN,
  CPU_WIN,
}

enum class Side {
  YOU,
  CPU,
}

enum class PaddlePose {
  IDLE,
  WALK,
  HIT,
}

enum class GameSfx {
  SHIELD,
  CHIP,
}

enum class BallTailBand {
  SHORT,
  MEDIUM,
  LONG,
}

data class Chip(
  val x: Float,
  val y: Float,
  val w: Float,
  val h: Float,
  val slot: Int,
  val side: Side,
  var alive: Boolean = true,
)

/** Pixel well on the 1440×1080 cabinet. Right/bottom exclusive. */
data class CabinetInset(val left: Int, val top: Int, val right: Int, val bottom: Int) {
  val leftF: Float get() = left / CABINET_W
  val topF: Float get() = top / CABINET_H
  val rightF: Float get() = right / CABINET_W
  val bottomF: Float get() = bottom / CABINET_H
  val widthF: Float get() = (right - left) / CABINET_W
  val heightF: Float get() = (bottom - top) / CABINET_H

  private companion object {
    const val CABINET_W = 1440f
    const val CABINET_H = 1080f
  }
}

class World {
  var phase: Phase = Phase.ROUND
    private set
  var youScore: Int = 0
    private set
  var cpuScore: Int = 0
    private set
  var youSets: Int = 0
    private set
  var cpuSets: Int = 0
    private set
  var timeLeft: Float = SET_TIME
    private set
  var suddenDeath: Boolean = false
    private set
  var youPaddleY: Float = 0.5f
    private set
  var cpuPaddleY: Float = 0.5f
    private set
  var ballX: Float = 0f
    private set
  var ballY: Float = 0.5f
    private set
  val chips: MutableList<Chip> = mutableListOf()

  private var vx = 0f
  private var vy = 0f
  private var youHitT = 0f
  private var cpuHitT = 0f
  private var youWalkT = 0f
  private var cpuMoved = false
  private var youPaddleYPrev = 0.5f
  private var youPaddleVy = 0f
  private var cpuPaddleVy = 0f
  private var roundHold = ROUND_HOLD
  private val pendingSfx = ArrayDeque<GameSfx>()

  init {
    dealChips()
    parkBall()
  }

  fun roundNumber(): Int = (youSets + cpuSets + 1).coerceIn(1, 3)

  fun drainSfx(): List<GameSfx> {
    if (pendingSfx.isEmpty()) return emptyList()
    val out = pendingSfx.toList()
    pendingSfx.clear()
    return out
  }

  fun timeDisplay(): String = timeLeft.toInt().coerceIn(0, SET_TIME.toInt()).toString().padStart(2, '0')

  fun youChipsLeft(): Int = chips.count { it.side == Side.YOU && it.alive }

  fun cpuChipsLeft(): Int = chips.count { it.side == Side.CPU && it.alive }

  fun moveYouPaddle(normalizedY: Float) {
    val next = normalizedY.coerceIn(PADDLE_LEN / 2f, 1f - PADDLE_LEN / 2f)
    if (kotlin.math.abs(next - youPaddleY) > WALK_EPS) youWalkT = WALK_HOLD
    youPaddleY = next
    if (phase == Phase.SERVE || phase == Phase.ROUND) parkBall()
  }

  fun youPose(): PaddlePose = pose(youHitT, youWalkT > 0f)

  fun cpuPose(): PaddlePose = pose(cpuHitT, cpuMoved)

  fun launch() {
    if (phase == Phase.YOU_WIN || phase == Phase.CPU_WIN) {
      resetMatch()
      return
    }
    if (phase != Phase.SERVE) return
    val speed = BALL_SPEED
    val offset = ((ballY - youPaddleY) / (PADDLE_LEN / 2f)).coerceIn(-1f, 1f)
    vy = offset * speed * 0.7f
    vx = speed
    if (kotlin.math.abs(vy) < speed * 0.18f) vy = speed * 0.18f * if (offset >= 0) 1f else -1f
    normalize(speed)
    phase = Phase.PLAYING
  }

  fun step(dt: Float) {
    val clamped = dt.coerceAtMost(0.05f)
    tickHits(clamped)
    youPaddleVy = (youPaddleY - youPaddleYPrev) / clamped.coerceAtLeast(0.0001f)
    youPaddleYPrev = youPaddleY
    if (phase == Phase.ROUND) {
      cpuMoved = false
      cpuPaddleVy = 0f
      roundHold -= clamped
      if (roundHold <= 0f) phase = Phase.SERVE
      return
    }
    if (phase != Phase.PLAYING) {
      cpuMoved = false
      cpuPaddleVy = 0f
      return
    }
    tickClock(clamped)
    if (phase != Phase.PLAYING) {
      cpuMoved = false
      cpuPaddleVy = 0f
      return
    }
    var left = clamped
    val slice = 1f / 120f
    cpuMoved = false
    while (left > 0f && phase == Phase.PLAYING) {
      val step = if (left < slice) left else slice
      val beforeCpu = cpuPaddleY
      steerCpu(step)
      cpuPaddleVy = (cpuPaddleY - beforeCpu) / step
      cpuMoved = cpuMoved || kotlin.math.abs(cpuPaddleY - beforeCpu) > WALK_EPS
      advance(step)
      left -= step
    }
  }

  fun youPaddleTop(): Float = youPaddleY - PADDLE_LEN / 2f

  fun cpuPaddleTop(): Float = cpuPaddleY - PADDLE_LEN / 2f

  internal fun placeBall(x: Float, y: Float, vx: Float, vy: Float) {
    ballX = x
    ballY = y
    this.vx = vx
    this.vy = vy
    phase = Phase.PLAYING
  }

  internal fun ballSpeed(): Float = kotlin.math.sqrt(vx * vx + vy * vy)

  internal fun ballVx(): Float = vx

  internal fun ballVy(): Float = vy

  fun ballTailBand(): BallTailBand = tailBandForSpeed(ballSpeed())

  internal fun setTimeLeft(seconds: Float) {
    timeLeft = seconds.coerceAtLeast(0f)
  }

  internal fun killAllBut(side: Side, keep: Chip) {
    chips.forEach { chip ->
      if (chip.side == side && chip !== keep) chip.alive = false
    }
  }

  private fun resetMatch() {
    youSets = 0
    cpuSets = 0
    resetSet()
  }

  private fun resetSet() {
    youScore = 0
    cpuScore = 0
    youPaddleY = 0.5f
    cpuPaddleY = 0.5f
    youHitT = 0f
    cpuHitT = 0f
    youWalkT = 0f
    cpuMoved = false
    suddenDeath = false
    timeLeft = SET_TIME
    dealChips()
    callRound()
  }

  private fun callRound() {
    roundHold = ROUND_HOLD
    phase = Phase.ROUND
    parkBall()
  }

  internal fun skipToServe() {
    if (phase == Phase.ROUND) phase = Phase.SERVE
  }

  private fun steerCpu(dt: Float) {
    if (vx <= 0f || ballX < CPU_REACT_X) return
    val max = CPU_SPEED * dt
    val target = ballY + CPU_AIM_BIAS
    val delta = (target - cpuPaddleY).coerceIn(-max, max)
    cpuPaddleY = (cpuPaddleY + delta).coerceIn(PADDLE_LEN / 2f, 1f - PADDLE_LEN / 2f)
  }

  private fun advance(dt: Float) {
    ballX += vx * dt
    ballY += vy * dt
    if (ballY - BALL_R <= 0f) {
      ballY = BALL_R
      vy = kotlin.math.abs(vy)
    } else if (ballY + BALL_R >= 1f) {
      ballY = 1f - BALL_R
      vy = -kotlin.math.abs(vy)
    }
    bounceSprite(youPaddleY, YOU_FRONT_X, incomingLeft = true, youSide = true)
    bounceSprite(cpuPaddleY, CPU_FRONT_X, incomingLeft = false, youSide = false)
    bounceChips()
    if (phase != Phase.PLAYING) return
    if (ballX - BALL_R_X <= 0f) {
      ballX = BALL_R_X
      vx = kotlin.math.abs(vx)
    } else if (ballX + BALL_R_X >= 1f) {
      ballX = 1f - BALL_R_X
      vx = -kotlin.math.abs(vx)
    }
    when {
      cpuChipsLeft() == 0 -> finishSet(Side.YOU)
      youChipsLeft() == 0 -> finishSet(Side.CPU)
    }
  }

  private fun bounceSprite(
    paddleY: Float,
    frontX: Float,
    incomingLeft: Boolean,
    youSide: Boolean,
  ) {
    if (incomingLeft && vx >= 0f) return
    if (!incomingLeft && vx <= 0f) return
    // Past the sprite toward the chip rail — do not collide from behind.
    if (incomingLeft && ballX < frontX) return
    if (!incomingLeft && ballX > frontX) return
    val top = paddleY - PADDLE_LEN / 2f
    val bottom = top + PADDLE_LEN
    if (incomingLeft) {
      if (ballX - BALL_R_X > frontX) return
    } else {
      if (ballX + BALL_R_X < frontX) return
    }
    if (ballY < top - BALL_R || ballY > bottom + BALL_R) return
    if (youSide) youHitT = HIT_HOLD else cpuHitT = HIT_HOLD
    pendingSfx += GameSfx.SHIELD
    ballX = if (incomingLeft) frontX + BALL_R_X else frontX - BALL_R_X
    val hit = ((ballY - paddleY) / (PADDLE_LEN / 2f)).coerceIn(-1f, 1f)
    val paddleVy = if (youSide) youPaddleVy else cpuPaddleVy
    val swipe = (kotlin.math.abs(paddleVy) / SLICE_SWIPE_REF).coerceIn(0f, 1f)
    val speed =
      (BALL_SPEED * (SLICE_CENTER + kotlin.math.abs(hit) * SLICE_EDGE + swipe * SLICE_SWIPE))
        .coerceAtMost(BALL_SPEED * SLICE_CAP)
    vy = hit * speed * 0.9f + paddleVy.coerceIn(-SLICE_SWIPE_REF, SLICE_SWIPE_REF) * 0.12f
    vx = if (incomingLeft) speed else -speed
    normalize(speed)
  }

  private fun bounceChips() {
    val hit =
      chips.firstOrNull { chip ->
        chip.alive && overlaps(chip)
      } ?: return
    hit.alive = false
    pendingSfx += GameSfx.CHIP
    if (hit.side == Side.CPU) youScore += 1 else cpuScore += 1
    if (suddenDeath) {
      finishSet(if (hit.side == Side.CPU) Side.YOU else Side.CPU)
      return
    }
    val cx = hit.x + hit.w / 2f
    val cy = hit.y + hit.h / 2f
    val dx = ballX - cx
    val dy = ballY - cy
    val px = hit.w / 2f + BALL_R_X
    val py = hit.h / 2f + BALL_R
    val ox = px - kotlin.math.abs(dx)
    val oy = py - kotlin.math.abs(dy)
    if (ox < oy) {
      vx = if (dx < 0) -kotlin.math.abs(vx) else kotlin.math.abs(vx)
      ballX += if (dx < 0) -ox else ox
    } else {
      vy = if (dy < 0) -kotlin.math.abs(vy) else kotlin.math.abs(vy)
      ballY += if (dy < 0) -oy else oy
    }
  }

  private fun overlaps(chip: Chip): Boolean {
    val closestX = ballX.coerceIn(chip.x, chip.x + chip.w)
    val closestY = ballY.coerceIn(chip.y, chip.y + chip.h)
    val nx = (ballX - closestX) / BALL_R_X
    val ny = (ballY - closestY) / BALL_R
    return nx * nx + ny * ny <= 1f
  }

  private fun dealChips() {
    chips.clear()
    val gap = 0.02f
    val h = (1f - gap * (CHIP_COUNT + 1)) / CHIP_COUNT
    val w = CHIP_W
    for (slot in 0 until CHIP_COUNT) {
      val y = gap + slot * (h + gap)
      chips +=
        Chip(
          x = YOU_CHIP_X,
          y = y,
          w = w,
          h = h,
          slot = slot,
          side = Side.YOU,
        )
      chips +=
        Chip(
          x = CPU_CHIP_X,
          y = y,
          w = w,
          h = h,
          slot = slot,
          side = Side.CPU,
        )
    }
  }

  private fun parkBall() {
    vx = 0f
    vy = 0f
    ballX = YOU_FRONT_X + BALL_R_X + 0.004f
    ballY = youPaddleY
  }

  private fun tickClock(dt: Float) {
    if (suddenDeath) return
    timeLeft = (timeLeft - dt).coerceAtLeast(0f)
    if (timeLeft > 0f) return
    val yours = youChipsLeft()
    val cpus = cpuChipsLeft()
    when {
      yours > cpus -> finishSet(Side.YOU)
      cpus > yours -> finishSet(Side.CPU)
      else -> suddenDeath = true
    }
  }

  private fun finishSet(winner: Side) {
    if (winner == Side.YOU) youSets += 1 else cpuSets += 1
    vx = 0f
    vy = 0f
    if (youSets >= SETS_TO_WIN) {
      phase = Phase.YOU_WIN
      return
    }
    if (cpuSets >= SETS_TO_WIN) {
      phase = Phase.CPU_WIN
      return
    }
    resetSet()
  }

  private fun tickHits(dt: Float) {
    if (youHitT > 0f) youHitT = (youHitT - dt).coerceAtLeast(0f)
    if (cpuHitT > 0f) cpuHitT = (cpuHitT - dt).coerceAtLeast(0f)
    if (youWalkT > 0f) youWalkT = (youWalkT - dt).coerceAtLeast(0f)
  }

  private fun pose(hitT: Float, moving: Boolean): PaddlePose {
    if (hitT > 0f) return PaddlePose.HIT
    if (moving) return PaddlePose.WALK
    return PaddlePose.IDLE
  }

  private fun normalize(speed: Float) {
    val mag = kotlin.math.sqrt(vx * vx + vy * vy)
    if (mag < 0.001f) return
    vx = vx / mag * speed
    vy = vy / mag * speed
  }

  companion object {
    const val BALL_R = 0.027f
    const val BALL_SPEED = 0.72f
    const val SLICE_CENTER = 0.98f
    const val SLICE_EDGE = 0.32f
    const val SLICE_SWIPE = 0.20f
    const val SLICE_CAP = 1.5f
    const val SLICE_SWIPE_REF = 1.2f
    const val PADDLE_LEN = 0.20f
    const val PADDLE_THICK = 0.018f
    const val CHIP_COUNT = 6
    const val CHIP_W = 0.046f
    const val YOU_CHIP_X = 0.012f
    const val CPU_CHIP_X = 1f - 0.012f - CHIP_W
    /** Small gap from chip rail; fighter sprites extend inward toward midcourt. */
    const val PADDLE_LANE = 0.028f
    const val YOU_PADDLE_X = YOU_CHIP_X + CHIP_W + PADDLE_LANE
    const val CPU_PADDLE_X = CPU_CHIP_X - PADDLE_LANE - PADDLE_THICK
    const val CPU_SPEED = 0.28f
    const val CPU_REACT_X = 0.52f
    const val CPU_AIM_BIAS = 0.06f
    const val HIT_HOLD = 0.18f
    const val WALK_HOLD = 0.12f
    const val WALK_EPS = 0.0008f
    const val SPRITE_W = 192
    const val SPRITE_H = 233
    const val SET_TIME = 99f
    const val SETS_TO_WIN = 2
    const val ROUND_HOLD = 1.8f
    const val CABINET_W_PX = 1440
    const val CABINET_H_PX = 1080
    /** Playfield hole — 1152×864 = 4:3. Exclusive right/bottom. */
    const val HOLE_LEFT_PX = 144
    const val HOLE_TOP_PX = 128
    const val HOLE_RIGHT_PX = 1296
    const val HOLE_BOTTOM_PX = 992
    val FRAME_LEFT: Float = HOLE_LEFT_PX / CABINET_W_PX.toFloat()
    val FRAME_TOP: Float = HOLE_TOP_PX / CABINET_H_PX.toFloat()
    val FRAME_RIGHT: Float = (CABINET_W_PX - HOLE_RIGHT_PX) / CABINET_W_PX.toFloat()
    val FRAME_BOTTOM: Float = (CABINET_H_PX - HOLE_BOTTOM_PX) / CABINET_H_PX.toFloat()
    const val COURT_ASPECT = 4f / 3f
    const val WELL_TOP_PX = 32
    const val WELL_BOTTOM_PX = 100
    val P1_SCORE_INSET = CabinetInset(220, WELL_TOP_PX, 324, WELL_BOTTOM_PX)
    val P1_NAME_INSET = CabinetInset(340, WELL_TOP_PX, 528, WELL_BOTTOM_PX)
    val P1_SETS_INSET = CabinetInset(544, WELL_TOP_PX, 620, WELL_BOTTOM_PX)
    val TIME_INSET = CabinetInset(636, WELL_TOP_PX, 804, WELL_BOTTOM_PX)
    val P2_SETS_INSET = CabinetInset(820, WELL_TOP_PX, 896, WELL_BOTTOM_PX)
    val P2_NAME_INSET = CabinetInset(912, WELL_TOP_PX, 1100, WELL_BOTTOM_PX)
    val P2_SCORE_INSET = CabinetInset(1116, WELL_TOP_PX, 1220, WELL_BOTTOM_PX)
    /**
     * Magenta pad on the court-facing side of packed 192×233 frames
     * (Rivet idle opaque x=25..165).
     */
    const val SPRITE_FRONT_PAD = 26
    /** Dest width in X, matching GameScreen on the cabinet hole. */
    val SPRITE_SPAN: Float = PADDLE_LEN * SPRITE_W / SPRITE_H / COURT_ASPECT
    val BALL_R_X: Float = BALL_R / COURT_ASPECT
    /** Opaque court-facing edge of the left fighter. */
    val YOU_FRONT_X: Float = YOU_PADDLE_X + SPRITE_SPAN * (SPRITE_W - SPRITE_FRONT_PAD) / SPRITE_W
    /** Opaque court-facing edge of the right fighter. */
    val CPU_FRONT_X: Float = CPU_PADDLE_X + PADDLE_THICK - SPRITE_SPAN * (SPRITE_W - SPRITE_FRONT_PAD) / SPRITE_W
    val YOU_HIT_X: Float = YOU_FRONT_X - PADDLE_THICK
    val CPU_HIT_X: Float = CPU_FRONT_X
    val TAIL_SPEED_MIN: Float = BALL_SPEED * SLICE_CENTER
    val TAIL_SPEED_MAX: Float = BALL_SPEED * SLICE_CAP

    fun tailBandForSpeed(speed: Float): BallTailBand {
      val u = ((speed - TAIL_SPEED_MIN) / (TAIL_SPEED_MAX - TAIL_SPEED_MIN)).coerceIn(0f, 1f)
      return when {
        u < 1f / 3f -> BallTailBand.SHORT
        u < 2f / 3f -> BallTailBand.MEDIUM
        else -> BallTailBand.LONG
      }
    }
  }
}
