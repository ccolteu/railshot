package com.cc.railshot.game

enum class Phase {
  SERVE,
  PLAYING,
  YOU_WIN,
  CPU_WIN,
}

enum class Side {
  YOU,
  CPU,
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

class World {
  var phase: Phase = Phase.SERVE
    private set
  var youScore: Int = 0
    private set
  var cpuScore: Int = 0
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

  init {
    dealChips()
    parkBall()
  }

  fun youChipsLeft(): Int = chips.count { it.side == Side.YOU && it.alive }

  fun cpuChipsLeft(): Int = chips.count { it.side == Side.CPU && it.alive }

  fun moveYouPaddle(normalizedY: Float) {
    youPaddleY = normalizedY.coerceIn(PADDLE_LEN / 2f, 1f - PADDLE_LEN / 2f)
    if (phase == Phase.SERVE) parkBall()
  }

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
    if (phase != Phase.PLAYING) return
    var left = dt.coerceAtMost(0.05f)
    val slice = 1f / 120f
    while (left > 0f && phase == Phase.PLAYING) {
      val step = if (left < slice) left else slice
      steerCpu(step)
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

  internal fun killAllBut(side: Side, keep: Chip) {
    chips.forEach { chip ->
      if (chip.side == side && chip !== keep) chip.alive = false
    }
  }

  private fun resetMatch() {
    youScore = 0
    cpuScore = 0
    youPaddleY = 0.5f
    cpuPaddleY = 0.5f
    dealChips()
    phase = Phase.SERVE
    parkBall()
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
    if (ballX - BALL_R <= 0f) {
      ballX = BALL_R
      vx = kotlin.math.abs(vx)
    } else if (ballX + BALL_R >= 1f) {
      ballX = 1f - BALL_R
      vx = -kotlin.math.abs(vx)
    }
    bouncePaddle(youPaddleY, YOU_PADDLE_X, incomingLeft = true)
    bouncePaddle(cpuPaddleY, CPU_PADDLE_X, incomingLeft = false)
    bounceChips()
    if (cpuChipsLeft() == 0) {
      phase = Phase.YOU_WIN
      return
    }
    if (youChipsLeft() == 0) {
      phase = Phase.CPU_WIN
    }
  }

  private fun bouncePaddle(paddleY: Float, paddleX: Float, incomingLeft: Boolean) {
    if (incomingLeft && vx >= 0f) return
    if (!incomingLeft && vx <= 0f) return
    val top = paddleY - PADDLE_LEN / 2f
    val bottom = top + PADDLE_LEN
    if (ballX + BALL_R < paddleX || ballX - BALL_R > paddleX + PADDLE_THICK) return
    if (ballY < top - BALL_R || ballY > bottom + BALL_R) return
    if (incomingLeft) {
      ballX = paddleX + PADDLE_THICK + BALL_R
    } else {
      ballX = paddleX - BALL_R
    }
    val hit = ((ballY - paddleY) / (PADDLE_LEN / 2f)).coerceIn(-1f, 1f)
    val speed = BALL_SPEED * 1.03f
    vy = hit * speed * 0.9f
    vx = if (incomingLeft) speed else -speed
    normalize(speed)
  }

  private fun bounceChips() {
    val hit =
      chips.firstOrNull { chip ->
        chip.alive && overlaps(chip)
      } ?: return
    hit.alive = false
    if (hit.side == Side.CPU) youScore += 1 else cpuScore += 1
    val cx = hit.x + hit.w / 2f
    val cy = hit.y + hit.h / 2f
    val dx = ballX - cx
    val dy = ballY - cy
    val px = hit.w / 2f + BALL_R
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
    val dx = ballX - closestX
    val dy = ballY - closestY
    return dx * dx + dy * dy <= BALL_R * BALL_R
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
    ballX = YOU_PADDLE_X + PADDLE_THICK + BALL_R + 0.004f
    ballY = youPaddleY
  }

  private fun normalize(speed: Float) {
    val mag = kotlin.math.sqrt(vx * vx + vy * vy)
    if (mag < 0.001f) return
    vx = vx / mag * speed
    vy = vy / mag * speed
  }

  companion object {
    const val BALL_R = 0.018f
    const val BALL_SPEED = 0.72f
    const val PADDLE_LEN = 0.20f
    const val PADDLE_THICK = 0.018f
    const val CHIP_COUNT = 6
    const val CHIP_W = 0.046f
    const val YOU_CHIP_X = 0.012f
    const val CPU_CHIP_X = 1f - 0.012f - CHIP_W
    const val YOU_PADDLE_X = YOU_CHIP_X + CHIP_W + 0.032f
    const val CPU_PADDLE_X = CPU_CHIP_X - 0.032f - PADDLE_THICK
    const val CPU_SPEED = 0.28f
    const val CPU_REACT_X = 0.52f
    const val CPU_AIM_BIAS = 0.06f
  }
}
