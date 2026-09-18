package com.cc.railshot.game

enum class Phase {
  ROUND,
  SERVE,
  PLAYING,
  SET_WIN,
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
  ICE,
  WALL,
}

enum class BallTailBand {
  SHORT,
  MEDIUM,
  LONG,
}

private enum class PostPhase {
  IDLE,
  RAISE,
  UP,
  SINK,
}

private enum class HexCarPhase {
  GAP,
  ENTER,
  HOLD,
  EXIT,
}

private data class AabbBounce(val x: Float, val y: Float, val vx: Float, val vy: Float)

private data class RayHit(val t: Float, val vertical: Boolean)

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

class World(
  val you: Fighter = Fighter.RIVET,
  val rival: Fighter = Fighter.MARU,
  val cpuLevel: CpuLevel = CpuLevel.HARD,
  val cpuStyle: CpuStyle = CpuStyle.forFighter(rival),
  val attract: Boolean = false,
) {
  constructor(cpuStyle: CpuStyle, cpuLevel: CpuLevel) : this(
    you = Fighter.RIVET,
    rival = Fighter.MARU,
    cpuLevel = cpuLevel,
    cpuStyle = cpuStyle,
  )

  val youKit: FighterKit = FighterKit.of(you)
  val cpuKit: FighterKit = FighterKit.of(rival)
  private val courtWells: Boolean = rival == Fighter.ASH
  private val courtTrace: Boolean = rival == Fighter.RIVET
  private val courtCar: Boolean = rival == Fighter.HEX
  private val courtHawk: Boolean = rival == Fighter.QUILL
  private val youStyle: CpuStyle = CpuStyle.forFighter(you)
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
  var setWinner: Side = Side.YOU
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
  private var fightHold = 0f
  private var setWinT = 0f
  private var freezeT = 0f
  private var shakeT = 0f
  private var shakeDur = 0f
  private var shakeAmp = 0f
  private var flashT = 0f
  private val pendingSfx = ArrayDeque<GameSfx>()
  private var rallyHits = 0
  private var lastPaddle: Side? = null
  private var chipSincePaddle = false
  private var youHeat = 0
  private var cpuHeat = 0
  private var youRail = 0f
  private var cpuRail = 0f
  private var youStarArmed = true
  private var cpuStarArmed = true
  private var cpuPaddleHits = 0
  private var starLive = false
  private var starFromYou = true
  private var starFighter = Fighter.RIVET
  private var starX = 0f
  private var starY = 0.5f
  private var starVx = 0f
  private var starVy = 0f
  private var burstT = 0f
  private var burstX = 0f
  private var burstY = 0.5f
  private var postWait = POST_FIRST
  private val postGrow = FloatArray(3)
  private val postPhase = Array(3) { PostPhase.IDLE }
  private val postClock = FloatArray(3)
  private var traceWait = TRACE_FIRST
  private var traceOn = false
  private var traceU = 0f
  private var traceDir = 1f
  private var hexCarPhase = HexCarPhase.GAP
  private var hexCarT = 0f
  private var hawkT = 0f
  private var hawkAnimT = 0f
  private var tailLock: BallTailBand? = null
  private var shieldGain = 1.05f
  private var shieldRate = 1.22f

  init {
    dealChips()
    parkBall()
    if (attract) {
      phase = Phase.SERVE
      launch()
    }
  }

  fun youFrontX(): Float = Companion.youFrontX(PADDLE_LEN)

  fun cpuFrontX(): Float = Companion.cpuFrontX(PADDLE_LEN)

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
    val half = youKit.paddleLen / 2f
    val next = normalizedY.coerceIn(half, 1f - half)
    noteRail(youSide = true, from = youPaddleY, to = next)
    if (kotlin.math.abs(next - youPaddleY) > WALK_EPS) youWalkT = WALK_HOLD
    youPaddleY = next
    if (phase == Phase.SERVE || phase == Phase.ROUND) parkBall()
  }

  fun youPose(): PaddlePose = pose(youHitT, youWalkT > 0f)

  fun cpuPose(): PaddlePose = pose(cpuHitT, cpuMoved)

  fun impactFrozen(): Boolean = freezeT > 0f && phase == Phase.PLAYING

  fun bounceFlash(): Float = if (flashT <= 0f) 0f else (flashT / FLASH_HOLD).coerceIn(0f, 1f)

  fun cabinetShakeX(vw: Float): Float = shakeAxis(vw, 73f)

  fun cabinetShakeY(vh: Float): Float = shakeAxis(vh, 51f)

  fun launch() {
    if (phase == Phase.YOU_WIN || phase == Phase.CPU_WIN) {
      resetMatch()
      return
    }
    if (phase == Phase.SET_WIN) return
    if (phase != Phase.SERVE) return
    val speed = BALL_SPEED
    val offset = ((ballY - youPaddleY) / (youKit.paddleLen / 2f)).coerceIn(-1f, 1f)
    vy = offset * speed * 0.7f
    vx = speed
    if (kotlin.math.abs(vy) < speed * 0.18f) vy = speed * 0.18f * if (offset >= 0) 1f else -1f
    normalize(speed)
    phase = Phase.PLAYING
    rallyHits = 0
    lastPaddle = null
    chipSincePaddle = false
    tailLock = null
    armCourtHazards()
  }

  fun step(dt: Float) {
    val clamped = dt.coerceAtMost(0.05f)
    if (
      phase == Phase.ROUND ||
        phase == Phase.SERVE ||
        phase == Phase.PLAYING ||
        phase == Phase.SET_WIN
    ) {
      tickHexCar(clamped)
      tickHawk(clamped)
    }
    tickHits(clamped)
    tickJuice(clamped)
    if (!attract) {
      youPaddleVy = (youPaddleY - youPaddleYPrev) / clamped.coerceAtLeast(0.0001f)
      youPaddleYPrev = youPaddleY
    }
    if (freezeT > 0f && phase == Phase.PLAYING) {
      cpuMoved = false
      cpuPaddleVy = 0f
      return
    }
    if (phase == Phase.ROUND) {
      cpuMoved = false
      cpuPaddleVy = 0f
      roundHold -= clamped
      if (roundHold <= 0f) startServe()
      return
    }
    if (phase == Phase.SERVE) {
      cpuMoved = false
      cpuPaddleVy = 0f
      if (fightHold > 0f) fightHold -= clamped
      return
    }
    if (phase == Phase.SET_WIN) {
      cpuMoved = false
      cpuPaddleVy = 0f
      setWinT -= clamped
      if (setWinT <= 0f) endSetWin()
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
    tickPost(clamped)
    tickTrace(clamped)
    var left = clamped
    val slice = 1f / 120f
    cpuMoved = false
    while (left > 0f && phase == Phase.PLAYING) {
      val step = if (left < slice) left else slice
      if (attract) {
        val beforeYou = youPaddleY
        steerPaddle(step, towardCpu = false)
        youPaddleVy = (youPaddleY - beforeYou) / step
        if (kotlin.math.abs(youPaddleY - beforeYou) > WALK_EPS) youWalkT = WALK_HOLD
      }
      val beforeCpu = cpuPaddleY
      steerPaddle(step, towardCpu = true)
      cpuPaddleVy = (cpuPaddleY - beforeCpu) / step
      cpuMoved = cpuMoved || kotlin.math.abs(cpuPaddleY - beforeCpu) > WALK_EPS
      advance(step)
      left -= step
      if (freezeT > 0f) break
    }
  }

  fun youPaddleTop(): Float = youPaddleY - youKit.paddleLen / 2f

  fun cpuPaddleTop(): Float = cpuPaddleY - cpuKit.paddleLen / 2f

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

  fun starLive(): Boolean = starLive

  fun starX(): Float = starX

  fun starY(): Float = starY

  fun starFighter(): Fighter = starFighter

  internal fun starVx(): Float = starVx

  internal fun starVy(): Float = starVy

  internal fun starSpeed(): Float = kotlin.math.sqrt(starVx * starVx + starVy * starVy)

  fun iceBurstLive(): Boolean = burstT > 0f

  fun iceBurstX(): Float = burstX

  fun iceBurstY(): Float = burstY

  fun postLive(): Boolean = courtWells && postGrow.any { it > 0.02f }

  fun wellVisible(slot: Int): Boolean = courtWells && postGrow.getOrElse(slot) { 0f } > 0.02f

  fun wellY(slot: Int): Float = POST_SLOTS[slot] - POST_H / 2f

  fun wellX(): Float = POST_X

  fun wellW(): Float = POST_W

  fun wellH(): Float = POST_H

  fun postWallFrame(slot: Int): Int {
    val grow = postGrow.getOrElse(slot) { 0f }
    return when {
      grow < 0.55f -> 0
      grow < 0.82f -> 1
      else -> 2
    }
  }

  internal fun raisePost(slot: Int = 1) {
    if (!courtWells) return
    armPost()
    postWait = 0f
    val i = slot.coerceIn(0, 2)
    postPhase[i] = PostPhase.UP
    postClock[i] = POST_UP
    postGrow[i] = 1f
  }

  fun traceLive(): Boolean = courtTrace && traceOn

  fun traceX(): Float = TRACE_X

  fun traceY(): Float = traceCy() - TRACE_H / 2f

  fun traceW(): Float = TRACE_W

  fun traceH(): Float = TRACE_H

  internal fun placeTrace(u: Float = 0.5f) {
    if (!courtTrace) return
    traceWait = 0f
    traceOn = true
    traceU = u.coerceIn(0f, 1f)
    traceDir = 1f
  }

  fun hexCarLive(): Boolean = courtCar && hexCarPhase != HexCarPhase.GAP

  fun hexCarX(): Float = hexCarLeft()

  fun hexCarY(): Float = hexCarTop()

  fun hexCarW(): Float = HEX_CAR_W

  fun hexCarH(): Float = HEX_CAR_H

  internal fun placeHexCar(hold: Boolean = true) {
    if (!courtCar) return
    hexCarT = 0f
    hexCarPhase = if (hold) HexCarPhase.HOLD else HexCarPhase.ENTER
  }

  fun hawkLive(): Boolean = courtHawk

  fun hawkX(): Float = hawkLeft()

  fun hawkY(): Float = hawkTop()

  fun hawkW(): Float = HAWK_W

  fun hawkH(): Float = HAWK_H

  fun hawkHeadingDeg(): Float {
    val p = hawkPose()
    return Math.toDegrees(kotlin.math.atan2(p.dy.toDouble(), p.dx.toDouble())).toFloat()
  }

  fun hawkFlapFrame(): Int {
    val step = ((hawkAnimT / HAWK_FLAP).toInt() % 4 + 4) % 4
    return when (step) {
      0 -> 0
      1 -> 1
      2 -> 2
      else -> 1
    }
  }

  internal fun placeHawk(u: Float = 0f) {
    if (!courtHawk) return
    hawkT = u.coerceIn(0f, 1f) * HAWK_LAP
    hawkAnimT = 0f
  }

  fun iceBurstFrame(): Int {
    val u = 1f - (burstT / ICE_BURST_S).coerceIn(0f, 1f)
    return when {
      u < 1f / 3f -> 0
      u < 2f / 3f -> 1
      else -> 2
    }
  }

  private fun popIceBurst() {
    burstX = starX
    burstY = starY
    burstT = ICE_BURST_S
  }

  internal fun placeStar(x: Float, y: Float, vx: Float, vy: Float) {
    starLive = true
    starX = x
    starY = y
    starVx = vx
    starVy = vy
  }

  fun callYouSpecial(aimY: Float): Boolean = spawnSpecial(youSide = true, aimY = aimY)

  fun ballTailBand(): BallTailBand = tailLock ?: tailBandForSpeed(ballSpeed())

  fun shieldSfxGain(): Float = shieldGain

  fun shieldSfxRate(): Float = shieldRate

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
    freezeT = 0f
    shakeT = 0f
    shakeDur = 0f
    shakeAmp = 0f
    flashT = 0f
    timeLeft = SET_TIME
    rallyHits = 0
    lastPaddle = null
    chipSincePaddle = false
    youHeat = 0
    cpuHeat = 0
    youRail = 0f
    cpuRail = 0f
    youStarArmed = true
    cpuStarArmed = true
    cpuPaddleHits = 0
    starLive = false
    burstT = 0f
    tailLock = null
    armCourtHazards()
    shieldGain = 1.05f
    shieldRate = 1.22f
    dealChips()
    callRound()
  }

  private fun callRound() {
    roundHold = ROUND_HOLD
    phase = Phase.ROUND
    parkBall()
  }

  fun fightBannerVisible(): Boolean = phase == Phase.SERVE && fightHold > 0f

  fun setWinBannerVisible(): Boolean = phase == Phase.SET_WIN && setWinT <= SET_WIN_BANNER

  internal fun skipSetWin() {
    if (phase == Phase.SET_WIN) endSetWin()
  }

  internal fun skipToServe() {
    if (phase == Phase.ROUND) startServe()
  }

  private fun startServe() {
    phase = Phase.SERVE
    fightHold = FIGHT_HOLD
    parkBall()
  }

  private fun steerPaddle(dt: Float, towardCpu: Boolean) {
    val style = if (towardCpu) cpuStyle else youStyle
    val kit = if (towardCpu) cpuKit else youKit
    val y = if (towardCpu) cpuPaddleY else youPaddleY
    val ballIncoming = if (towardCpu) vx > 0f else vx < 0f
    val orbIncoming = starLive && if (towardCpu) starVx > 0f else starVx < 0f
    if (!ballIncoming && !orbIncoming) return
    val chaseOrb =
      orbIncoming &&
        (!ballIncoming ||
          if (towardCpu) starX >= ballX else starX <= ballX)
    val threatX = if (chaseOrb) starX else ballX
    if (towardCpu && threatX < cpuReactX()) return
    if (!towardCpu && threatX > 1f - cpuReactX()) return
    val front = if (towardCpu) cpuFrontX() else youFrontX()
    val chase =
      if (chaseOrb) starY
      else if (cpuLevel == CpuLevel.HARD) interceptY(front, towardCpu)
      else ballY
    if (cpuLevel == CpuLevel.EASY && (chase < 0.17f || chase > 0.83f)) {
      // Leave the rails open — retreat toward mid instead of covering a high/low shot.
      val mid = 0.5f - y
      val max = cpuSpeed(style, kit) * 0.55f * dt
      val half = kit.paddleLen / 2f
      writePaddleY(towardCpu, (y + mid.coerceIn(-max, max)).coerceIn(half, 1f - half))
      return
    }
    val camp = threatenedGateY(if (towardCpu) Side.CPU else Side.YOU) ?: chase
    val mix = if (chaseOrb) 0f else campMix(style)
    var target = chase * (1f - mix) + camp * mix
    val error = target - y
    if (style == CpuStyle.SLUGGER && kotlin.math.abs(error) > 0.012f) {
      target += kotlin.math.sign(error) * overshoot()
    }
    val max = cpuSpeed(style, kit) * dt
    val step =
      if (style == CpuStyle.SLUGGER && kotlin.math.abs(error) > 0.008f) {
        kotlin.math.sign(error) * max
      } else {
        (target - y).coerceIn(-max, max)
      }
    val half = kit.paddleLen / 2f
    val next = (y + step).coerceIn(half, 1f - half)
    noteRail(youSide = !towardCpu, from = y, to = next)
    writePaddleY(towardCpu, next)
  }

  private fun writePaddleY(towardCpu: Boolean, y: Float) {
    if (towardCpu) cpuPaddleY = y else youPaddleY = y
  }

  private fun noteRail(youSide: Boolean, from: Float, to: Float) {
    val d = kotlin.math.abs(to - from)
    if (youSide && you == Fighter.KITE) youRail += d
    if (!youSide && rival == Fighter.KITE) cpuRail += d
  }

  private fun cpuSpeed(style: CpuStyle, kit: FighterKit): Float {
    val base =
      when {
        cpuLevel == CpuLevel.HARD && style == CpuStyle.SLUGGER -> 0.54f
        cpuLevel == CpuLevel.HARD -> 0.42f
        style == CpuStyle.SLUGGER -> 0.22f
        else -> 0.16f
      }
    return base * kit.moveMul
  }

  private fun cpuReactX(): Float = if (cpuLevel == CpuLevel.HARD) 0.32f else 0.68f

  private fun campMix(style: CpuStyle): Float =
    when {
      style == CpuStyle.WALL && cpuLevel == CpuLevel.HARD -> 0.62f
      style == CpuStyle.WALL -> 0.48f
      cpuLevel == CpuLevel.HARD -> 0.12f
      else -> 0.05f
    }

  private fun overshoot(): Float = if (cpuLevel == CpuLevel.EASY) 0.09f else 0.03f

  private fun threatenedGateY(side: Side): Float? {
    var bestY: Float? = null
    var bestD = Float.MAX_VALUE
    for (chip in chips) {
      if (chip.side != side || !chip.alive) continue
      val cy = chip.y + chip.h / 2f
      val d = kotlin.math.abs(cy - ballY)
      if (d < bestD) {
        bestD = d
        bestY = cy
      }
    }
    return bestY
  }

  private fun interceptY(frontX: Float, towardCpu: Boolean): Float {
    var x = ballX
    var y = ballY
    var svx = vx
    var svy = vy
    if (towardCpu && svx <= 0.01f) return y
    if (!towardCpu && svx >= -0.01f) return y
    var guard = 0
    val lo = BALL_R
    val hi = 1f - BALL_R
    while (guard++ < 32) {
      val approaching = if (towardCpu) x < frontX else x > frontX
      if (!approaching) break
      val tPaddle = (frontX - x) / svx
      if (tPaddle <= 0f) break
      var tHit = tPaddle
      var kind = 0
      if (svy > 0.001f) {
        val tWall = (hi - y) / svy
        if (tWall > 0.0001f && tWall < tHit) {
          tHit = tWall
          kind = 1
        }
      } else if (svy < -0.001f) {
        val tWall = (lo - y) / svy
        if (tWall > 0.0001f && tWall < tHit) {
          tHit = tWall
          kind = 2
        }
      }
      var postKind = 3
      for (slot in 0 until 3) {
        if (!wellSolid(slot)) continue
        val hit =
          rayAabb(
            x,
            y,
            svx,
            svy,
            slotLeft(slot) - BALL_R_X,
            slotTop(slot) - BALL_R,
            slotLeft(slot) + slotW(slot) + BALL_R_X,
            slotTop(slot) + slotH(slot) + BALL_R,
          )
        if (hit != null && hit.t > 0.0001f && hit.t < tHit) {
          tHit = hit.t
          postKind = if (hit.vertical) 3 else 4
          kind = postKind
        }
      }
      if (traceSolid()) {
        val hit =
          rayAabb(
            x,
            y,
            svx,
            svy,
            traceLeft() - BALL_R_X,
            traceTop() - BALL_R,
            traceLeft() + TRACE_W + BALL_R_X,
            traceTop() + TRACE_H + BALL_R,
          )
        if (hit != null && hit.t > 0.0001f && hit.t < tHit) {
          tHit = hit.t
          kind = if (hit.vertical) 3 else 4
        }
      }
      if (hexCarSolid()) {
        val hit =
          rayAabb(
            x,
            y,
            svx,
            svy,
            hexCarLeft() - BALL_R_X,
            hexCarTop() - BALL_R,
            hexCarLeft() + HEX_CAR_W + BALL_R_X,
            hexCarTop() + HEX_CAR_H + BALL_R,
          )
        if (hit != null && hit.t > 0.0001f && hit.t < tHit) {
          tHit = hit.t
          kind = if (hit.vertical) 3 else 4
        }
      }
      if (hawkSolid()) {
        val hit =
          rayAabb(
            x,
            y,
            svx,
            svy,
            hawkLeft() - BALL_R_X,
            hawkTop() - BALL_R,
            hawkLeft() + HAWK_W + BALL_R_X,
            hawkTop() + HAWK_H + BALL_R,
          )
        if (hit != null && hit.t > 0.0001f && hit.t < tHit) {
          tHit = hit.t
          kind = if (hit.vertical) 3 else 4
        }
      }
      if (kind == 0) return (y + svy * tHit).coerceIn(lo, hi)
      x += svx * tHit
      y += svy * tHit
      when (kind) {
        1 -> {
          y = hi
          svy = -kotlin.math.abs(svy)
        }
        2 -> {
          y = lo
          svy = kotlin.math.abs(svy)
        }
        3 -> svx = -svx
        else -> svy = -svy
      }
    }
    return y.coerceIn(lo, hi)
  }

  private fun advance(dt: Float) {
    ballX += vx * dt
    ballY += vy * dt
    if (ballY - BALL_R <= 0f) {
      ballY = BALL_R
      if (vy < 0f) {
        vy = kotlin.math.abs(vy)
        bumpWall()
      }
    } else if (ballY + BALL_R >= 1f) {
      ballY = 1f - BALL_R
      if (vy > 0f) {
        vy = -kotlin.math.abs(vy)
        bumpWall()
      }
    }
    bounceSprite(youPaddleY, youFrontX(), incomingLeft = true, youSide = true)
    bounceSprite(cpuPaddleY, cpuFrontX(), incomingLeft = false, youSide = false)
    bouncePost()
    bounceChips()
    advanceStar(dt)
    if (phase != Phase.PLAYING) return
    if (ballX - BALL_R_X <= 0f) {
      ballX = BALL_R_X
      if (vx < 0f) {
        vx = kotlin.math.abs(vx)
        bumpWall()
      }
    } else if (ballX + BALL_R_X >= 1f) {
      ballX = 1f - BALL_R_X
      if (vx > 0f) {
        vx = -kotlin.math.abs(vx)
        bumpWall()
      }
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
    val kit = if (youSide) youKit else cpuKit
    val half = kit.paddleLen / 2f
    val top = paddleY - half
    val bottom = top + kit.paddleLen
    if (incomingLeft) {
      if (ballX - BALL_R_X > frontX) return
    } else {
      if (ballX + BALL_R_X < frontX) return
    }
    if (ballY < top - BALL_R || ballY > bottom + BALL_R) return
    val side = if (youSide) Side.YOU else Side.CPU
    val prev = lastPaddle
    if (prev != null && prev != side && !chipSincePaddle) {
      if (prev == Side.YOU && you == Fighter.RIVET) youHeat = 0
      if (prev == Side.CPU && rival == Fighter.RIVET) cpuHeat = 0
    }
    if (youSide) youHitT = HIT_HOLD else cpuHitT = HIT_HOLD
    pendingSfx += GameSfx.SHIELD
    sting(SHIELD_FREEZE, 0f, 0f, FLASH_HOLD)
    ballX = if (incomingLeft) frontX + BALL_R_X else frontX - BALL_R_X
    val hit = ((ballY - paddleY) / half).coerceIn(-1f, 1f)
    val paddleVy = if (youSide) youPaddleVy else cpuPaddleVy
    val swipe = (kotlin.math.abs(paddleVy) / SLICE_SWIPE_REF).coerceIn(0f, 1f)
    val fighter = if (youSide) you else rival
    var pop = kit.shieldPop
    var edge = kit.sliceEdge
    var angle = kit.sliceAngle
    var swipeMul = kit.swipeMul
    var dumped = false
    var dived = false
    var smashed = false
    var heat = 0
    if (fighter == Fighter.RIVET) {
      heat = if (youSide) ++youHeat else ++cpuHeat
      pop *= 1f + (heat - 1).coerceAtLeast(0) * RIVET_HEAT
    }
    if (fighter == Fighter.MARU && kotlin.math.abs(hit) < MARU_CENTER) {
      dumped = true
      pop *= MARU_DUMP
      angle *= 0.55f
    }
    if (fighter == Fighter.QUILL && kotlin.math.abs(hit) >= QUILL_RIM) {
      dived = true
      angle *= QUILL_DIVE
    }
    if (fighter == Fighter.ASH && swipe >= ASH_SMASH_SWIPE) {
      smashed = true
      pop *= ASH_SMASH
    }
    var burn = 1f
    var burned = false
    if (fighter == Fighter.KITE) {
      val rail = if (youSide) youRail else cpuRail
      if (rail >= KITE_RAIL) {
        burned = true
        burn = KITE_BURN
      }
      if (youSide) youRail = 0f else cpuRail = 0f
    }
    val rally = 1f + rallyHits * RALLY_STEP
    rallyHits += 1
    val speed =
      (BALL_SPEED *
          (SLICE_CENTER + kotlin.math.abs(hit) * edge + swipe * SLICE_SWIPE * swipeMul) *
          pop *
          burn *
          rally)
        .coerceAtMost(BALL_SPEED * SLICE_CAP)
    vy = hit * speed * angle + paddleVy.coerceIn(-SLICE_SWIPE_REF, SLICE_SWIPE_REF) * 0.12f
    vx = if (incomingLeft) speed else -speed
    normalize(speed)
    lastPaddle = side
    chipSincePaddle = false
    noteKitTell(dumped, dived, smashed, burned, fighter, heat, speed)
    if (!youSide) {
      cpuPaddleHits += 1
      maybeCpuSpecial()
    }
  }

  private fun maybeCpuSpecial() {
    if (attract || starLive) return
    if (cpuPaddleHits < CPU_SPECIAL_HITS) return
    val aim = threatenedGateY(Side.YOU) ?: 0.5f
    spawnSpecial(youSide = false, aimY = aim)
  }

  private fun spawnSpecial(youSide: Boolean, aimY: Float): Boolean {
    if (phase != Phase.PLAYING || starLive) return false
    val armed = if (youSide) youStarArmed else cpuStarArmed
    if (!armed) return false
    val fighter = if (youSide) you else rival
    val target = aimCalledChip(youSide, aimY, fighter) ?: return false
    if (youSide) youStarArmed = false else cpuStarArmed = false
    starLive = true
    starFromYou = youSide
    starFighter = fighter
    starX = if (youSide) youFrontX() + BALL_R_X else cpuFrontX() - BALL_R_X
    starY = if (youSide) youPaddleY else cpuPaddleY
    val cx = target.x + target.w / 2f
    val cy = target.y + target.h / 2f
    val dx = cx - starX
    val dy = cy - starY
    val mag = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    val speed = specialSpeed()
    starVx = dx / mag * speed
    starVy = dy / mag * speed
    shieldGain = 1.05f
    shieldRate = 1.22f
    pendingSfx += GameSfx.SHIELD
    return true
  }

  private fun noteKitTell(
    dumped: Boolean,
    dived: Boolean,
    smashed: Boolean,
    burned: Boolean,
    fighter: Fighter,
    heat: Int,
    speed: Float,
  ) {
    val fromSpeed = tailBandForSpeed(speed)
    tailLock =
      when {
        dumped -> BallTailBand.SHORT
        burned || smashed || dived -> BallTailBand.LONG
        fighter == Fighter.RIVET && heat >= 3 -> hotterTail(fromSpeed, BallTailBand.LONG)
        fighter == Fighter.RIVET && heat >= 2 -> hotterTail(fromSpeed, BallTailBand.MEDIUM)
        else -> null
      }
    when {
      dumped -> {
        shieldGain = 0.88f
        shieldRate = 0.82f
      }
      burned -> {
        shieldGain = 1.18f
        shieldRate = 1.48f
      }
      smashed -> {
        shieldGain = 1.22f
        shieldRate = 0.90f
      }
      dived -> {
        shieldGain = 1.12f
        shieldRate = 1.35f
      }
      fighter == Fighter.RIVET && heat >= 2 -> {
        shieldGain = 1.12f
        shieldRate = (1.18f + 0.10f * heat).coerceAtMost(1.85f)
      }
      else -> {
        shieldGain = 1.05f
        shieldRate = 1.22f
      }
    }
  }

  private fun hotterTail(a: BallTailBand, b: BallTailBand): BallTailBand =
    if (a.ordinal >= b.ordinal) a else b

  private fun specialSpeed(): Float = (BALL_SPEED * STAR_SPEED).coerceAtMost(BALL_SPEED * SLICE_CAP)

  private fun aimCalledChip(youSide: Boolean, aimY: Float, fighter: Fighter): Chip? {
    val side = if (youSide) Side.CPU else Side.YOU
    val y = aimY.coerceIn(0f, 1f)
    var pool = chips.filter { it.alive && it.side == side }
    if (pool.isEmpty()) return null
    if (fighter == Fighter.QUILL) {
      val high = y < 0.5f
      val half = pool.filter { chip ->
        val cy = chip.y + chip.h / 2f
        if (high) cy < 0.5f else cy > 0.5f
      }
      if (half.isNotEmpty()) pool = half
    }
    return pool.minBy { chip -> kotlin.math.abs(chip.y + chip.h / 2f - y) }
  }

  private fun advanceStar(dt: Float) {
    if (!starLive || phase != Phase.PLAYING) return
    starX += starVx * dt
    starY += starVy * dt
    if (starY - STAR_R <= 0f) {
      starY = STAR_R
      if (starVy < 0f) {
        starVy = kotlin.math.abs(starVy)
        bumpWall()
      }
    } else if (starY + STAR_R >= 1f) {
      starY = 1f - STAR_R
      if (starVy > 0f) {
        starVy = -kotlin.math.abs(starVy)
        bumpWall()
      }
    }
    smashOrbOnPaddle(youPaddleY, youFrontX(), incomingLeft = true, youSide = true)
    if (!starLive) return
    smashOrbOnPaddle(cpuPaddleY, cpuFrontX(), incomingLeft = false, youSide = false)
    if (!starLive) return
    bounceStarPost()
    if (!starLive) return
    val hit =
      chips.firstOrNull { chip ->
        chip.alive && overlapsStar(chip)
      }
    if (hit != null) {
      hit.alive = false
      pendingSfx += GameSfx.CHIP
      chipSincePaddle = true
      sting(CHIP_FREEZE, SHAKE_CHIP, SHAKE_CHIP_T, FLASH_CHIP)
      pendingSfx += GameSfx.ICE
      if (hit.side == Side.CPU) youScore += 1 else cpuScore += 1
      popIceBurst()
      starLive = false
      if (suddenDeath) finishSet(if (hit.side == Side.CPU) Side.YOU else Side.CPU)
      return
    }
    if (starX < -0.05f || starX > 1.05f) starLive = false
  }

  private fun smashOrbOnPaddle(
    paddleY: Float,
    frontX: Float,
    incomingLeft: Boolean,
    youSide: Boolean,
  ) {
    if (!starLive) return
    if (incomingLeft && starVx >= 0f) return
    if (!incomingLeft && starVx <= 0f) return
    if (incomingLeft && starX < frontX) return
    if (!incomingLeft && starX > frontX) return
    val kit = if (youSide) youKit else cpuKit
    val half = kit.paddleLen / 2f
    val top = paddleY - half
    val bottom = top + kit.paddleLen
    if (incomingLeft) {
      if (starX - STAR_R_X > frontX) return
    } else {
      if (starX + STAR_R_X < frontX) return
    }
    if (starY < top - STAR_R || starY > bottom + STAR_R) return
    popIceBurst()
    starLive = false
    if (youSide) youHitT = HIT_HOLD else cpuHitT = HIT_HOLD
    pendingSfx += GameSfx.ICE
    sting(0f, 0f, 0f, FLASH_HOLD)
  }

  private fun overlapsStar(chip: Chip): Boolean {
    val closestX = starX.coerceIn(chip.x, chip.x + chip.w)
    val closestY = starY.coerceIn(chip.y, chip.y + chip.h)
    val nx = (starX - closestX) / STAR_R_X
    val ny = (starY - closestY) / STAR_R
    return nx * nx + ny * ny <= 1f
  }

  private fun bouncePost() {
    val hit = bounceAabb(ballX, ballY, vx, vy, BALL_R_X, BALL_R) ?: return
    val bounced = hit.vx != vx || hit.vy != vy
    ballX = hit.x
    ballY = hit.y
    vx = hit.vx
    vy = hit.vy
    if (bounced) bumpWall()
  }

  private fun bounceStarPost() {
    if (!starLive) return
    val hit = bounceAabb(starX, starY, starVx, starVy, STAR_R_X, STAR_R) ?: return
    val bounced = hit.vx != starVx || hit.vy != starVy
    starX = hit.x
    starY = hit.y
    starVx = hit.vx
    starVy = hit.vy
    if (bounced) bumpWall()
  }

  private fun bumpWall() {
    pendingSfx += GameSfx.WALL
  }

  private fun bounceAabb(
    px: Float,
    py: Float,
    pvx: Float,
    pvy: Float,
    rx: Float,
    ry: Float,
  ): AabbBounce? {
    var x = px
    var y = py
    var vx = pvx
    var vy = pvy
    var hitAny = false
    if (courtWells) {
      for (slot in 0 until 3) {
        val hit = bounceOneWell(slot, x, y, vx, vy, rx, ry) ?: continue
        x = hit.x
        y = hit.y
        vx = hit.vx
        vy = hit.vy
        hitAny = true
      }
    }
    if (traceSolid()) {
      val hit = bounceBox(traceLeft(), traceTop(), TRACE_W, TRACE_H, x, y, vx, vy, rx, ry)
      if (hit != null) {
        x = hit.x
        y = hit.y
        vx = hit.vx
        vy = hit.vy
        hitAny = true
      }
    }
    if (hexCarSolid()) {
      val hit = bounceBox(hexCarLeft(), hexCarTop(), HEX_CAR_W, HEX_CAR_H, x, y, vx, vy, rx, ry)
      if (hit != null) {
        x = hit.x
        y = hit.y
        vx = hit.vx
        vy = hit.vy
        hitAny = true
      }
    }
    if (hawkSolid()) {
      val hit = bounceBox(hawkLeft(), hawkTop(), HAWK_W, HAWK_H, x, y, vx, vy, rx, ry)
      if (hit != null) {
        x = hit.x
        y = hit.y
        vx = hit.vx
        vy = hit.vy
        hitAny = true
      }
    }
    return if (hitAny) AabbBounce(x, y, vx, vy) else null
  }

  private fun bounceOneWell(
    slot: Int,
    px: Float,
    py: Float,
    pvx: Float,
    pvy: Float,
    rx: Float,
    ry: Float,
  ): AabbBounce? {
    if (!wellSolid(slot)) return null
    return bounceBox(slotLeft(slot), slotTop(slot), slotW(slot), slotH(slot), px, py, pvx, pvy, rx, ry)
  }

  private fun bounceBox(
    left: Float,
    top: Float,
    w: Float,
    h: Float,
    px: Float,
    py: Float,
    pvx: Float,
    pvy: Float,
    rx: Float,
    ry: Float,
  ): AabbBounce? {
    val right = left + w
    val bot = top + h
    val closestX = px.coerceIn(left, right)
    val closestY = py.coerceIn(top, bot)
    val nx = (px - closestX) / rx
    val ny = (py - closestY) / ry
    if (nx * nx + ny * ny > 1f) return null
    val cx = left + w / 2f
    val cy = top + h / 2f
    val dx = px - cx
    val dy = py - cy
    val ox = w / 2f + rx - kotlin.math.abs(dx)
    val oy = h / 2f + ry - kotlin.math.abs(dy)
    var x = px
    var y = py
    var vx = pvx
    var vy = pvy
    if (ox < oy) {
      vx = if (dx < 0) -kotlin.math.abs(pvx) else kotlin.math.abs(pvx)
      x += if (dx < 0) -ox else ox
    } else {
      vy = if (dy < 0) -kotlin.math.abs(pvy) else kotlin.math.abs(pvy)
      y += if (dy < 0) -oy else oy
    }
    return AabbBounce(x, y, vx, vy)
  }

  private fun wellSolid(slot: Int): Boolean = courtWells && postGrow[slot] >= POST_SOLID

  private fun slotW(slot: Int): Float = POST_W * postGrow[slot]

  private fun slotH(slot: Int): Float = POST_H * postGrow[slot]

  private fun slotLeft(slot: Int): Float = wellCx() - slotW(slot) / 2f

  private fun slotTop(slot: Int): Float = POST_SLOTS[slot] - slotH(slot) / 2f

  private fun wellCx(): Float = POST_X + POST_W / 2f

  private fun armCourtHazards() {
    armPost()
    armTrace()
  }

  private fun armTrace() {
    traceWait = TRACE_FIRST
    traceOn = false
    traceU = 0f
    traceDir = 1f
  }

  private fun traceSolid(): Boolean = courtTrace && traceOn

  private fun traceLeft(): Float = TRACE_X

  private fun traceTop(): Float = traceCy() - TRACE_H / 2f

  private fun hexCarSolid(): Boolean = hexCarLive()

  private fun hexCarLeft(): Float = HEX_CAR_X

  private fun hexCarTop(): Float = hexCarCy() - HEX_CAR_H / 2f

  private fun hawkSolid(): Boolean = hawkLive()

  private fun hawkLeft(): Float = hawkCx() - HAWK_W / 2f

  private fun hawkTop(): Float = hawkCy() - HAWK_H / 2f

  private fun hawkCx(): Float = hawkPose().x

  private fun hawkCy(): Float = hawkPose().y

  private data class HawkPose(val x: Float, val y: Float, val dx: Float, val dy: Float)

  private fun hawkPose(): HawkPose = hawkPath(hawkT / HAWK_LAP)

  private fun hawkPath(uRaw: Float): HawkPose {
    val left = HAWK_LEFT_CX
    val right = HAWK_RIGHT_CX
    val top = HAWK_TOP_CY
    val bot = HAWK_BOT_CY
    val r = HAWK_CORNER_R
    val sh = (right - left - 2f * r).coerceAtLeast(0.02f)
    val sv = (bot - top - 2f * r).coerceAtLeast(0.02f)
    val quarter = (kotlin.math.PI.toFloat() * 0.5f) * r
    val total = 2f * sh + 2f * sv + 4f * quarter
    var s = ((uRaw % 1f + 1f) % 1f) * total
    fun corner(cx: Float, cy: Float, a0: Float, t: Float): HawkPose {
      val a = a0 + t * (kotlin.math.PI.toFloat() * 0.5f)
      val cos = kotlin.math.cos(a)
      val sin = kotlin.math.sin(a)
      return HawkPose(cx + r * cos, cy + r * sin, -sin, cos)
    }
    if (s <= sh) {
      val t = s / sh
      return HawkPose(left + r + t * sh, top, 1f, 0f)
    }
    s -= sh
    if (s <= quarter) return corner(right - r, top + r, -kotlin.math.PI.toFloat() * 0.5f, s / quarter)
    s -= quarter
    if (s <= sv) {
      val t = s / sv
      return HawkPose(right, top + r + t * sv, 0f, 1f)
    }
    s -= sv
    if (s <= quarter) return corner(right - r, bot - r, 0f, s / quarter)
    s -= quarter
    if (s <= sh) {
      val t = s / sh
      return HawkPose(right - r - t * sh, bot, -1f, 0f)
    }
    s -= sh
    if (s <= quarter) return corner(left + r, bot - r, kotlin.math.PI.toFloat() * 0.5f, s / quarter)
    s -= quarter
    if (s <= sv) {
      val t = s / sv
      return HawkPose(left, bot - r - t * sv, 0f, -1f)
    }
    s -= sv
    val t = (s / quarter).coerceIn(0f, 1f)
    return corner(left + r, top + r, kotlin.math.PI.toFloat(), t)
  }

  private fun tickHawk(dt: Float) {
    if (!courtHawk) {
      hawkT = 0f
      hawkAnimT = 0f
      return
    }
    hawkT += dt
    if (hawkT >= HAWK_LAP) hawkT -= HAWK_LAP
    hawkAnimT += dt
  }

  private fun traceCy(): Float = TRACE_CY0 + traceU * (TRACE_CY1 - TRACE_CY0)

  private fun tickTrace(dt: Float) {
    if (!courtTrace) {
      traceOn = false
      return
    }
    if (traceWait > 0f) {
      traceWait -= dt
      if (traceWait > 0f) return
      traceOn = true
      traceU = 0f
      traceDir = 1f
    }
    if (!traceOn) return
    traceU += traceDir * dt / TRACE_TRAVEL
    if (traceU >= 1f) {
      traceU = 1f
      traceDir = -1f
    } else if (traceU <= 0f) {
      traceU = 0f
      traceDir = 1f
    }
  }

  private fun hexCarCy(): Float {
    val start = 1f + HEX_CAR_H / 2f
    val mid = 0.5f
    val end = 0f - HEX_CAR_H / 2f
    return when (hexCarPhase) {
      HexCarPhase.GAP,
      HexCarPhase.ENTER,
      -> {
        val u = ease((hexCarT / HEX_CAR_ENTER).coerceIn(0f, 1f))
        start + (mid - start) * u
      }
      HexCarPhase.HOLD -> mid
      HexCarPhase.EXIT -> {
        val u = ease((hexCarT / HEX_CAR_EXIT).coerceIn(0f, 1f))
        mid + (end - mid) * u
      }
    }
  }

  private fun tickHexCar(dt: Float) {
    if (!courtCar) {
      hexCarPhase = HexCarPhase.GAP
      hexCarT = 0f
      return
    }
    hexCarT += dt
    when (hexCarPhase) {
      HexCarPhase.GAP -> {
        if (hexCarT < HEX_CAR_GAP) return
        hexCarPhase = HexCarPhase.ENTER
        hexCarT = 0f
      }
      HexCarPhase.ENTER -> {
        if (hexCarT < HEX_CAR_ENTER) return
        hexCarPhase = HexCarPhase.HOLD
        hexCarT = 0f
      }
      HexCarPhase.HOLD -> {
        if (hexCarT < HEX_CAR_HOLD) return
        hexCarPhase = HexCarPhase.EXIT
        hexCarT = 0f
      }
      HexCarPhase.EXIT -> {
        if (hexCarT < HEX_CAR_EXIT) return
        hexCarPhase = HexCarPhase.GAP
        hexCarT = 0f
      }
    }
  }

  private fun ease(u: Float): Float {
    val x = u.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
  }

  private fun armPost() {
    postWait = POST_FIRST
    for (i in 0 until 3) {
      postPhase[i] = PostPhase.IDLE
      postClock[i] = 0f
      postGrow[i] = 0f
    }
  }

  private fun startRaise(slot: Int) {
    postPhase[slot] = PostPhase.RAISE
    postClock[slot] = POST_RAISE
    postGrow[slot] = 0f
  }

  private fun tickPost(dt: Float) {
    if (!courtWells) {
      for (i in 0 until 3) postGrow[i] = 0f
      return
    }
    if (postWait > 0f) {
      postWait -= dt
      if (postWait > 0f) return
      startRaise(0)
    }
    var handoff = -1
    for (slot in 0 until 3) {
      if (postPhase[slot] == PostPhase.IDLE) continue
      postClock[slot] -= dt
      when (postPhase[slot]) {
        PostPhase.RAISE -> {
          val u = (1f - postClock[slot] / POST_RAISE).coerceIn(0f, 1f)
          postGrow[slot] = u
          if (postClock[slot] > 0f) continue
          postGrow[slot] = 1f
          postPhase[slot] = PostPhase.UP
          postClock[slot] = POST_UP
        }
        PostPhase.UP -> {
          postGrow[slot] = 1f
          if (postClock[slot] > 0f) continue
          postPhase[slot] = PostPhase.SINK
          postClock[slot] = POST_SINK
          handoff = (slot + 1) % 3
        }
        PostPhase.SINK -> {
          postGrow[slot] = (postClock[slot] / POST_SINK).coerceIn(0f, 1f)
          if (postClock[slot] > 0f) continue
          postGrow[slot] = 0f
          postPhase[slot] = PostPhase.IDLE
        }
        PostPhase.IDLE -> {}
      }
    }
    if (handoff >= 0 && postPhase[handoff] == PostPhase.IDLE) startRaise(handoff)
  }

  private fun rayAabb(
    ox: Float,
    oy: Float,
    dx: Float,
    dy: Float,
    left: Float,
    top: Float,
    right: Float,
    bot: Float,
  ): RayHit? {
    val inf = 1e6f
    val tx1 = if (kotlin.math.abs(dx) < 1e-8f) if (ox in left..right) -inf else inf else (left - ox) / dx
    val tx2 = if (kotlin.math.abs(dx) < 1e-8f) if (ox in left..right) inf else -inf else (right - ox) / dx
    val ty1 = if (kotlin.math.abs(dy) < 1e-8f) if (oy in top..bot) -inf else inf else (top - oy) / dy
    val ty2 = if (kotlin.math.abs(dy) < 1e-8f) if (oy in top..bot) inf else -inf else (bot - oy) / dy
    val tminX = kotlin.math.min(tx1, tx2)
    val tmaxX = kotlin.math.max(tx1, tx2)
    val tminY = kotlin.math.min(ty1, ty2)
    val tmaxY = kotlin.math.max(ty1, ty2)
    val tEnter = kotlin.math.max(tminX, tminY)
    val tExit = kotlin.math.min(tmaxX, tmaxY)
    if (tExit < 0f || tEnter > tExit || tEnter < 0f) return null
    return RayHit(tEnter, vertical = tminX >= tminY)
  }

  private fun bounceChips() {
    val hit =
      chips.firstOrNull { chip ->
        chip.alive && overlaps(chip)
      } ?: return
    hit.alive = false
    pendingSfx += GameSfx.CHIP
    chipSincePaddle = true
    sting(CHIP_FREEZE, SHAKE_CHIP, SHAKE_CHIP_T, FLASH_CHIP)
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
    ballX = youFrontX() + BALL_R_X + 0.004f
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
    setWinner = winner
    if (winner == Side.YOU) youSets += 1 else cpuSets += 1
    vx = 0f
    vy = 0f
    starLive = false
    armCourtHazards()
    setWinT = SET_WIN_FREEZE + SET_WIN_BANNER
    phase = Phase.SET_WIN
  }

  private fun endSetWin() {
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

  private fun sting(freeze: Float, shake: Float, shakeTime: Float, flash: Float) {
    freezeT = kotlin.math.max(freezeT, freeze)
    if (shakeT <= 0f || shake >= shakeAmp) {
      shakeAmp = shake
      shakeDur = shakeTime
    }
    shakeT = kotlin.math.max(shakeT, shakeTime)
    flashT = kotlin.math.max(flashT, flash)
  }

  private fun tickJuice(dt: Float) {
    if (freezeT > 0f) freezeT = (freezeT - dt).coerceAtLeast(0f)
    if (shakeT > 0f) shakeT = (shakeT - dt).coerceAtLeast(0f)
    if (flashT > 0f) flashT = (flashT - dt).coerceAtLeast(0f)
    if (burstT > 0f) burstT = (burstT - dt).coerceAtLeast(0f)
  }

  private fun shakeAxis(span: Float, freq: Float): Float {
    if (shakeT <= 0f || shakeDur <= 0f) return 0f
    val u = (shakeT / shakeDur).coerceIn(0f, 1f)
    return kotlin.math.cos(shakeT * freq) * shakeAmp * u * u * span
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
    const val RALLY_STEP = 0.04f
    const val RIVET_HEAT = 0.14f
    const val KITE_RAIL = 0.16f
    const val KITE_BURN = 1.20f
    const val ASH_SMASH_SWIPE = 0.55f
    const val ASH_SMASH = 1.16f
    const val MARU_CENTER = 0.40f
    const val MARU_DUMP = 0.70f
    const val QUILL_RIM = 0.70f
    const val QUILL_DIVE = 1.28f
    const val STAR_SPEED = 0.85f
    const val STAR_R = BALL_R
    const val CPU_SPECIAL_HITS = 3
    const val ICE_BURST_S = 0.24f
    /**
     * Ash court template on the 1440×1080 floor PNG.
     * Do not author art off this table.
     */
    const val COURT_GUTTER_W_PX = 101
    const val POST_W_PX = 96
    const val POST_H_PX = 176
    const val POST_X_PX = (1440 - POST_W_PX) / 2
    const val POST_W: Float = POST_W_PX / 1440f
    const val POST_H: Float = POST_H_PX / 1080f
    const val POST_X: Float = POST_X_PX / 1440f
    const val POST_FIRST = 2.2f
    const val POST_RAISE = 0.45f
    const val POST_UP = 3.4f
    const val POST_SINK = 0.40f
    const val POST_SOLID = 0.58f
    val POST_SLOTS: FloatArray = floatArrayOf(240f / 1080f, 540f / 1080f, 840f / 1080f)
    /**
     * Rivet court live dash on the 1440×1080 floor PNG.
     * Travels the gold center line; only this bead is solid.
     */
    const val TRACE_W_PX = 48
    const val TRACE_H_PX = 192
    const val TRACE_X_PX = (1440 - TRACE_W_PX) / 2
    const val TRACE_CY0_PX = 168
    const val TRACE_CY1_PX = 912
    const val TRACE_W: Float = TRACE_W_PX / 1440f
    const val TRACE_H: Float = TRACE_H_PX / 1080f
    const val TRACE_X: Float = TRACE_X_PX / 1440f
    const val TRACE_CY0: Float = TRACE_CY0_PX / 1080f
    const val TRACE_CY1: Float = TRACE_CY1_PX / 1080f
    const val TRACE_FIRST = 2.2f
    const val TRACE_TRAVEL = 3.6f
    /**
     * Hex street hovercar on the 1440×1080 floor PNG.
     * Visual only. Enters from the bottom, pauses on the gold X, exits the top, loops.
     */
    const val HEX_CAR_W_PX = 240
    const val HEX_CAR_H_PX = 320
    const val HEX_CAR_X_PX = (1440 - HEX_CAR_W_PX) / 2
    const val HEX_CAR_W: Float = HEX_CAR_W_PX / 1440f
    const val HEX_CAR_H: Float = HEX_CAR_H_PX / 1080f
    const val HEX_CAR_X: Float = HEX_CAR_X_PX / 1440f
    const val HEX_CAR_GAP = 1.8f
    const val HEX_CAR_ENTER = 3.2f
    const val HEX_CAR_HOLD = 1.4f
    const val HEX_CAR_EXIT = 2.8f
    /**
     * Quill aerie hawk. Rounded square around the playfield, in front of the
     * fighters — never the gold X. Packed 136×192.
     */
    const val HAWK_W_PX = 136
    const val HAWK_H_PX = 192
    const val HAWK_W: Float = HAWK_W_PX / 1440f
    const val HAWK_H: Float = HAWK_H_PX / 1080f
    const val HAWK_TOP_CY: Float = 0.16f
    const val HAWK_BOT_CY: Float = 0.84f
    const val HAWK_CORNER_R: Float = 0.10f
    const val HAWK_LAP = 12f
    const val HAWK_FLAP = 0.16f
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
    const val HIT_HOLD = 0.22f
    const val SHIELD_POP = 1.06f
    const val SHIELD_FREEZE = 0.045f
    const val CHIP_FREEZE = 0.10f
    const val SHAKE_CHIP = 0.0075f
    const val SHAKE_CHIP_T = 0.16f
    const val FLASH_HOLD = 0.12f
    const val FLASH_CHIP = 0.07f
    const val WALK_HOLD = 0.12f
    const val WALK_EPS = 0.0008f
    const val SPRITE_W = 192
    const val SPRITE_H = 233
    const val SET_TIME = 99f
    const val SETS_TO_WIN = 2
    const val ROUND_HOLD = 1.8f
    const val FIGHT_HOLD = 1.2f
    const val SET_WIN_FREEZE = 0.5f
    const val SET_WIN_BANNER = 1.4f
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
    val SPRITE_SPAN: Float = spriteSpan(PADDLE_LEN)
    val BALL_R_X: Float = BALL_R / COURT_ASPECT
    val STAR_R_X: Float = STAR_R / COURT_ASPECT
    /** Opaque court-facing edge of the left fighter (baseline length). */
    val YOU_FRONT_X: Float = youFrontX(PADDLE_LEN)
    /** Opaque court-facing edge of the right fighter (baseline length). */
    val CPU_FRONT_X: Float = cpuFrontX(PADDLE_LEN)
    val YOU_HIT_X: Float = YOU_FRONT_X - PADDLE_THICK
    val CPU_HIT_X: Float = CPU_FRONT_X
    val HAWK_LEFT_CX: Float = YOU_FRONT_X + 0.14f
    val HAWK_RIGHT_CX: Float = CPU_FRONT_X - 0.14f
    val TAIL_SPEED_MIN: Float = BALL_SPEED * SLICE_CENTER
    val TAIL_SPEED_MAX: Float = BALL_SPEED * SLICE_CAP

    fun spriteSpan(len: Float): Float = len * SPRITE_W / SPRITE_H / COURT_ASPECT

    fun youFrontX(len: Float): Float =
      YOU_PADDLE_X + spriteSpan(len) * (SPRITE_W - SPRITE_FRONT_PAD) / SPRITE_W

    fun cpuFrontX(len: Float): Float =
      CPU_PADDLE_X + PADDLE_THICK - spriteSpan(len) * (SPRITE_W - SPRITE_FRONT_PAD) / SPRITE_W

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
