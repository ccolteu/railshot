package com.cc.railshot.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.cc.railshot.SoundManager
import com.cc.railshot.game.CabinetInset
import com.cc.railshot.game.CpuLevel
import com.cc.railshot.game.Fighter
import com.cc.railshot.game.FighterKit
import com.cc.railshot.game.GameSfx
import com.cc.railshot.game.HighScoreManager
import com.cc.railshot.game.PaddlePose
import com.cc.railshot.game.PlayMode
import com.cc.railshot.game.Phase
import com.cc.railshot.game.SELECT_LINGER_MS
import com.cc.railshot.game.SelectSession
import com.cc.railshot.game.SelectStep
import com.cc.railshot.game.Side
import com.cc.railshot.game.World
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sin

/**
 * One Choreographer SurfaceView for title, demo, select, VS, and match (WW2 Blitz shape).
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Choreographer.FrameCallback {

  private enum class Screen {
    TITLE,
    DEMO,
    RANKING,
    SELECT,
    VS,
    MATCH,
    REGISTER,
  }

  private val choreographer = Choreographer.getInstance()
  private val bitmaps = HashMap<String, Bitmap>()
  private var running = false
  private var lastNanos = 0L
  private var screen = Screen.TITLE
  private var titleT = 0f
  private var demoT = 0f
  private var lingerT = 0f
  private var selectIdleT = 0f
  private var resultCardT = 0f
  private var vsAnimT = 0f
  private val select = SelectSession()
  private var playMode = PlayMode.VS
  private var arcadeSkill = 0
  private var arcadeWins = 0
  private var rankingT = 0f
  private var registerT = 0f
  private var registerCharIndex = 0
  private var registerChar = 'A'
  private val registerName = CharArray(3) { 'A' }
  private val registerLeft = RectF()
  private val registerRight = RectF()
  private val registerSet = RectF()
  private val rankLine = StringBuilder(32)
  private var you: Fighter = Fighter.RIVET
  private var rival: Fighter = Fighter.RIVET
  private var cpuLevel: CpuLevel = CpuLevel.EASY
  private var world = World()
  private var announced: Phase? = null
  private var dragging = false
  private var grabOffsetY = 0f
  private var grabPointerId = -1
  private var lastCourtTapMs = 0L
  private val youGrab = RectF()
  private var youFrames: Map<PaddlePose, Bitmap> = emptyMap()
  private var rivalFrames: Map<PaddlePose, Bitmap> = emptyMap()
  private var tailTravel = 0f
  private var iceTailTravel = 0f

  private val stage = RectF()
  private val src = Rect()
  private val dst = RectF()
  private val leftArrow = RectF()
  private val rightArrow = RectF()
  private val selectBtn = RectF()
  private val startBtn = RectF()
  private val arcadeBtn = RectF()
  private val vsBtn = RectF()
  private val diffBtn = RectF()
  private val faceTiles = Array(Fighter.selectOrder.size) { RectF() }
  private val titleSlots = Array(Fighter.selectOrder.size) { RectF() }

  private val pixel =
    Paint().apply {
      isFilterBitmap = false
      isAntiAlias = false
      isDither = false
    }
  private val fillPaint = Paint().apply { isAntiAlias = false }
  private val textPaint =
    Paint().apply {
      isAntiAlias = false
      isFakeBoldText = true
      typeface = Typeface.MONOSPACE
      textAlign = Paint.Align.CENTER
    }
  private val dotPaint = Paint().apply { isAntiAlias = false }
  private val popupPaint =
    Paint().apply {
      typeface = Typeface.DEFAULT_BOLD
      isAntiAlias = false
      isFakeBoldText = true
    }
  private val popupShadowPaint =
    Paint().apply {
      color = Color.BLACK
      typeface = Typeface.DEFAULT_BOLD
      isAntiAlias = false
      isFakeBoldText = true
    }
  private val goldPaint =
    Paint().apply {
      color = Color.YELLOW
      typeface = Typeface.DEFAULT_BOLD
      isAntiAlias = false
      isFakeBoldText = true
      textAlign = Paint.Align.CENTER
    }
  private val goldShadowPaint =
    Paint().apply {
      color = Color.BLACK
      typeface = Typeface.DEFAULT_BOLD
      isAntiAlias = false
      isFakeBoldText = true
      textAlign = Paint.Align.CENTER
    }
  private val goldRimPaint =
    Paint().apply {
      color = 0xFFB35400.toInt()
      typeface = Typeface.DEFAULT_BOLD
      isAntiAlias = false
      isFakeBoldText = true
      textAlign = Paint.Align.CENTER
    }
  private val uiSelectIdleStrokePaint =
    Paint().apply {
      color = 0xFF6E6E6E.toInt()
      style = Paint.Style.STROKE
      strokeWidth = 5f
      strokeJoin = Paint.Join.MITER
      isAntiAlias = false
    }
  private val uiSelectIdleInnerPaint =
    Paint().apply {
      color = 0xFF3A3A3A.toInt()
      style = Paint.Style.STROKE
      strokeWidth = 2f
      isAntiAlias = false
    }
  private val uiSelectFocusStrokePaint =
    Paint().apply {
      color = 0xFFFFD54A.toInt()
      style = Paint.Style.STROKE
      strokeWidth = 6f
      strokeJoin = Paint.Join.MITER
      isAntiAlias = false
    }
  private val uiSelectFocusInnerPaint =
    Paint().apply {
      color = 0xFF00E5FF.toInt()
      style = Paint.Style.STROKE
      strokeWidth = 2f
      isAntiAlias = false
    }
  private var arcadeTypeface: Typeface? = null
  private var titleFocus = 2
  private var titleCommitT = -1f
  private var titleCommitMode: PlayMode? = null
  private var selectFocus = 1
  private val vsPortraitL = RectF()
  private val vsPortraitR = RectF()

  init {
    HighScoreManager.loadHighScores(context)
    try {
      arcadeTypeface = Typeface.createFromAsset(context.assets, "fonts/arcade_font.ttf")
    } catch (_: Exception) {
    }
    val face = arcadeTypeface ?: Typeface.DEFAULT_BOLD
    goldPaint.typeface = face
    goldShadowPaint.typeface = face
    goldRimPaint.typeface = face
    textPaint.typeface = face
    popupPaint.typeface = face
    popupShadowPaint.typeface = face
    holder.addCallback(this)
    isFocusable = true
    isFocusableInTouchMode = true
    setWillNotDraw(true)
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    running = true
    lastNanos = 0L
    choreographer.postFrameCallback(this)
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    running = false
    choreographer.removeFrameCallback(this)
  }

  override fun onDetachedFromWindow() {
    running = false
    choreographer.removeFrameCallback(this)
    super.onDetachedFromWindow()
  }

  override fun doFrame(frameTimeNanos: Long) {
    if (!running) return
    val dt =
      if (lastNanos == 0L) 0f
      else ((frameTimeNanos - lastNanos).coerceIn(0L, MAX_FRAME_NS) / 1_000_000_000f)
    lastNanos = frameTimeNanos
    when (screen) {
      Screen.TITLE -> {
        if (titleCommitT >= 0f) {
          titleCommitT += dt
          if (titleCommitT >= TITLE_COMMIT_S) {
            goSelect(titleCommitMode)
            titleCommitT = -1f
            titleCommitMode = null
          }
        } else {
          titleT += dt
          if (titleT >= TITLE_HOLD_S) goDemo()
        }
      }
      Screen.DEMO -> stepDemo(dt)
      Screen.RANKING -> {
        rankingT += dt
        if (rankingT >= RANKING_HOLD_S) goTitle()
      }
      Screen.SELECT -> {
        if (select.step == SelectStep.LOCKED) {
          lingerT += dt
          if (lingerT >= SELECT_LINGER_MS / 1000f) goVs()
        } else {
          selectIdleT += dt
          if (selectIdleT >= SELECT_IDLE_S) goTitle()
        }
      }
      Screen.VS -> vsAnimT += dt
      Screen.MATCH -> stepMatch(dt)
      Screen.REGISTER -> registerT += dt
    }
    val canvas = lockGameCanvas()
    if (canvas != null) {
      try {
        drawFrame(canvas)
      } finally {
        holder.unlockCanvasAndPost(canvas)
      }
    }
    choreographer.postFrameCallback(this)
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    layoutStage()
    val x = event.x - stage.left
    val y = event.y - stage.top
    val sw = stage.width()
    val sh = stage.height()
    if (event.actionMasked != MotionEvent.ACTION_DOWN &&
      event.actionMasked != MotionEvent.ACTION_POINTER_DOWN &&
      event.actionMasked != MotionEvent.ACTION_MOVE &&
      event.actionMasked != MotionEvent.ACTION_UP &&
      event.actionMasked != MotionEvent.ACTION_POINTER_UP &&
      event.actionMasked != MotionEvent.ACTION_CANCEL
    ) {
      return true
    }
    when (screen) {
      Screen.TITLE ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN) touchTitle(x, y, sw, sh)
      Screen.DEMO, Screen.RANKING ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN) goTitle()
      Screen.SELECT ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
          if (select.step != SelectStep.LOCKED) selectIdleT = 0f
          touchSelect(x, y, sw, sh)
        }
      Screen.VS -> if (event.actionMasked == MotionEvent.ACTION_DOWN) touchVs(x, y, sw, sh)
      Screen.MATCH -> touchMatch(event, x, y, sw, sh)
      Screen.REGISTER ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN) touchRegister(x, y, sw, sh)
    }
    return true
  }

  private fun goTitle() {
    screen = Screen.TITLE
    titleT = 0f
    titleFocus = 2
    titleCommitT = -1f
    titleCommitMode = null
    demoT = 0f
    lingerT = 0f
    selectIdleT = 0f
    rankingT = 0f
  }

  private fun goDemo() {
    you = Fighter.roster.random()
    rival = Fighter.roster.random()
    youFrames = loadCourtFrames(you.art(), left = true)
    rivalFrames = loadCourtFrames(rival.art(), left = false)
    world = World(you = you, rival = rival, cpuLevel = CpuLevel.HARD, attract = true)
    announced = null
    dragging = false
    grabOffsetY = 0f
    grabPointerId = -1
    lastCourtTapMs = 0L
    tailTravel = 0f
    iceTailTravel = 0f
    demoT = 0f
    screen = Screen.DEMO
  }

  private fun goSelect(mode: PlayMode? = null) {
    if (screen == Screen.SELECT) return
    if (mode != null) playMode = mode
    if (playMode == PlayMode.ARCADE) {
      arcadeSkill = 0
      arcadeWins = 0
    }
    screen = Screen.SELECT
    select.reset(arcade = playMode == PlayMode.ARCADE)
    lingerT = 0f
    selectIdleT = 0f
    resultCardT = 0f
  }

  private fun leaveMatchResult() {
    if (playMode != PlayMode.ARCADE) {
      goTitle()
      return
    }
    arcadeSkill = world.youSkill
    if (world.phase != Phase.YOU_WIN) {
      endArcadeCredit()
      return
    }
    arcadeWins += 1
    val next = Fighter.nextArcadeRival(you, rival)
    if (next == null) {
      endArcadeCredit()
      return
    }
    rival = next
    goVs(fromSelect = false)
  }

  private fun endArcadeCredit() {
    val dip = cpuLevel.table
    if (HighScoreManager.checkIfQualifies(arcadeSkill, dip)) goRegister() else goRanking()
  }

  private fun goRanking() {
    rankingT = 0f
    screen = Screen.RANKING
  }

  private fun goRegister() {
    registerCharIndex = 0
    registerChar = 'A'
    registerName[0] = 'A'
    registerName[1] = 'A'
    registerName[2] = 'A'
    registerT = 0f
    screen = Screen.REGISTER
  }

  private fun goVs(fromSelect: Boolean = true) {
    if (fromSelect) {
      val first = select.firstPick ?: return
      val second = select.secondPick ?: return
      you = first
      rival = second
    }
    lingerT = 0f
    vsAnimT = 0f
    screen = Screen.VS
  }

  private fun goMatch() {
    youFrames = loadCourtFrames(you.art(), left = true)
    rivalFrames = loadCourtFrames(rival.art(), left = false)
    world = World(you = you, rival = rival, cpuLevel = cpuLevel, startSkill = if (playMode == PlayMode.ARCADE) arcadeSkill else 0)
    announced = null
    dragging = false
    grabOffsetY = 0f
    grabPointerId = -1
    lastCourtTapMs = 0L
    tailTravel = 0f
    iceTailTravel = 0f
    resultCardT = 0f
    screen = Screen.MATCH
  }

  private fun stepDemo(dt: Float) {
    demoT += dt
    world.step(dt)
    if (world.phase == Phase.PLAYING && !world.impactFrozen()) {
      tailTravel += world.ballSpeed() * dt
      if (world.starLive()) iceTailTravel += world.starSpeed() * dt
    }
    world.drainSfx()
    if (demoT >= DEMO_HOLD_S) goRanking()
  }

  private fun stepMatch(dt: Float) {
    world.step(dt)
    if (world.phase == Phase.PLAYING && !world.impactFrozen()) {
      tailTravel += world.ballSpeed() * dt
      if (world.starLive()) iceTailTravel += world.starSpeed() * dt
    }
    for (sfx in world.drainSfx()) {
      when (sfx) {
        GameSfx.SHIELD ->
          SoundManager.instance.playSFX(
            SoundManager.SFX_SHIELD,
            world.shieldSfxGain(),
            world.shieldSfxRate(),
          )
        GameSfx.CHIP -> SoundManager.instance.playSFX(SoundManager.SFX_CHIP, 1.12f, 0.82f)
        GameSfx.ICE -> SoundManager.instance.playSFX(SoundManager.SFX_ICE, 1.08f, 1.0f)
        GameSfx.WALL -> SoundManager.instance.playSFX(SoundManager.SFX_WALL, 1.15f, 1.0f)
      }
    }
    val phase = world.phase
    if (phase != announced) {
      announced = phase
      when (phase) {
        Phase.ROUND -> SoundManager.instance.playSFX(SoundManager.roundCall(world.roundNumber()))
        Phase.SERVE -> SoundManager.instance.playSFX(SoundManager.SFX_FIGHT)
        else -> {}
      }
    }
    if (phase == Phase.YOU_WIN || phase == Phase.CPU_WIN) {
      resultCardT += dt
      if (resultCardT >= RESULT_IDLE_S) {
        SoundManager.instance.playSFX(SoundManager.SFX_SELECT)
        leaveMatchResult()
      }
    } else {
      resultCardT = 0f
    }
  }

  private fun touchSelect(x: Float, y: Float, sw: Float, sh: Float) {
    layoutSelectHits(sw, sh)
    val locked = select.step == SelectStep.LOCKED
    if (locked) return
    if (leftArrow.contains(x, y)) {
      selectFocus = 0
      SoundManager.instance.playSFX(SoundManager.SFX_ARROW)
      select.moveLeft()
      return
    }
    if (rightArrow.contains(x, y)) {
      selectFocus = 2
      SoundManager.instance.playSFX(SoundManager.SFX_ARROW)
      select.moveRight()
      return
    }
    if (selectBtn.contains(x, y)) {
      selectFocus = 1
      SoundManager.instance.playSFX(SoundManager.SFX_SELECT)
      select.confirm()
      lingerT = 0f
      return
    }
    for (i in faceTiles.indices) {
      if (faceTiles[i].contains(x, y)) {
        if (i != select.cursorIndex) {
          SoundManager.instance.playSFX(SoundManager.SFX_ARROW)
          select.setCursor(i)
        }
        return
      }
    }
  }

  private fun touchVs(x: Float, y: Float, sw: Float, sh: Float) {
    layoutVsHits(sw, sh)
    if (startBtn.contains(x, y)) {
      SoundManager.instance.playSFX(SoundManager.SFX_SELECT)
      goMatch()
    }
  }

  private fun touchMatch(event: MotionEvent, x: Float, y: Float, sw: Float, sh: Float) {
    val courtL = World.FRAME_LEFT * sw
    val courtT = World.FRAME_TOP * sh
    val courtR = sw - World.FRAME_RIGHT * sw
    val courtB = sh - World.FRAME_BOTTOM * sh
    val courtW = (courtR - courtL).coerceAtLeast(1f)
    val courtH = (courtB - courtT).coerceAtLeast(1f)
    layoutYouGrab(courtL, courtT, courtW, courtH)
    val slop = 18f * resources.displayMetrics.density
    youGrab.inset(-slop, -slop)
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        if (world.phase == Phase.YOU_WIN || world.phase == Phase.CPU_WIN) {
          if (resultCardT >= RESULT_LOCK_S) {
            SoundManager.instance.playSFX(SoundManager.SFX_SELECT)
            leaveMatchResult()
          }
          return
        }
        val playing = world.phase == Phase.PLAYING
        val inCourt = x in courtL..courtR && y in courtT..courtB
        if (tryGrab(event.getPointerId(0), x, y, courtT, courtH)) {
          // Rail held.
        } else {
          clearGrab()
          if (playing && inCourt) onCourtTap(y, courtT, courtH)
        }
        if (inCourt) world.launch()
      }
      MotionEvent.ACTION_POINTER_DOWN -> {
        val i = event.actionIndex
        val px = event.getX(i) - stage.left
        val py = event.getY(i) - stage.top
        val inCourt = px in courtL..courtR && py in courtT..courtB
        if (!dragging && tryGrab(event.getPointerId(i), px, py, courtT, courtH)) return
        if (dragging && event.getPointerId(i) == grabPointerId) return
        if (world.phase == Phase.PLAYING && inCourt && !youGrab.contains(px, py)) {
          onCourtTap(py, courtT, courtH)
        }
      }
      MotionEvent.ACTION_MOVE -> {
        if (!dragging) return
        val idx = event.findPointerIndex(grabPointerId)
        if (idx < 0) return
        val py = event.getY(idx) - stage.top
        val touchY = ((py - courtT) / courtH).coerceIn(0f, 1f)
        world.moveYouPaddle(touchY + grabOffsetY)
      }
      MotionEvent.ACTION_POINTER_UP -> {
        if (event.getPointerId(event.actionIndex) == grabPointerId) clearGrab()
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        clearGrab()
        if (event.actionMasked == MotionEvent.ACTION_CANCEL) lastCourtTapMs = 0L
      }
    }
  }

  private fun tryGrab(pointerId: Int, x: Float, y: Float, courtT: Float, courtH: Float): Boolean {
    if (!youGrab.contains(x, y)) return false
    dragging = true
    grabPointerId = pointerId
    val touchY = ((y - courtT) / courtH).coerceIn(0f, 1f)
    grabOffsetY = world.youPaddleY - touchY
    return true
  }

  private fun clearGrab() {
    dragging = false
    grabPointerId = -1
    grabOffsetY = 0f
  }

  private fun onCourtTap(y: Float, courtT: Float, courtH: Float) {
    val now = SystemClock.uptimeMillis()
    val aimY = ((y - courtT) / courtH).coerceIn(0f, 1f)
    if (lastCourtTapMs != 0L && now - lastCourtTapMs <= DOUBLE_TAP_MS) {
      lastCourtTapMs = 0L
      world.callYouSpecial(aimY)
      return
    }
    lastCourtTapMs = now
  }

  private fun layoutYouGrab(courtL: Float, courtT: Float, courtW: Float, courtH: Float) {
    layoutFighterDest(
      youGrab,
      World.YOU_PADDLE_X,
      world.youPaddleY,
      leftCourt = true,
      courtL,
      courtT,
      courtW,
      courtH,
    )
  }

  private fun layoutFighterDest(
    out: RectF,
    paddleX: Float,
    paddleY: Float,
    leftCourt: Boolean,
    courtL: Float,
    courtT: Float,
    courtW: Float,
    courtH: Float,
  ) {
    val spriteH = World.PADDLE_LEN * courtH
    val spriteW = spriteH * (World.SPRITE_W / World.SPRITE_H.toFloat())
    val top = courtT + (paddleY - World.PADDLE_LEN / 2f) * courtH
    val left =
      if (leftCourt) courtL + paddleX * courtW
      else courtL + paddleX * courtW + World.PADDLE_THICK * courtW - spriteW
    out.set(left, top, left + spriteW, top + spriteH)
  }

  private fun drawFrame(canvas: Canvas) {
    layoutStage()
    canvas.drawColor(CABINET)
    canvas.save()
    canvas.translate(stage.left, stage.top)
    val sw = stage.width()
    val sh = stage.height()
    when (screen) {
      Screen.TITLE -> drawTitle(canvas, sw, sh)
      Screen.DEMO -> drawDemo(canvas, sw, sh)
      Screen.RANKING -> drawRanking(canvas, sw, sh)
      Screen.SELECT -> drawSelect(canvas, sw, sh)
      Screen.VS -> drawVs(canvas, sw, sh)
      Screen.MATCH -> drawMatch(canvas, sw, sh)
      Screen.REGISTER -> drawRegister(canvas, sw, sh)
    }
    canvas.restore()
  }

  private fun drawTitle(canvas: Canvas, sw: Float, sh: Float) {
    blitFill(canvas, opaque(UiArt.SELECT_BG), 0f, 0f, sw, sh)
    layoutTitleRoster(sw, sh)
    layoutTitleHits(sw, sh)
    TITLE_DRAW_ORDER.forEach { i ->
      val path = Fighter.selectOrder[i].art().selectFullBody ?: return@forEach
      val bmp = keyedCropped(path)
      val box = titleSlots[i]
      if (i < TITLE_LEFT) blitFitFlipped(canvas, bmp, box.left, box.top, box.width(), box.height())
      else blitFit(canvas, bmp, box.left, box.top, box.width(), box.height())
    }
    val dp = sh / 360f
    val gap = 5f * dp
    val margin = 12f * dp
    val rail = keyedCropped(UiArt.WORD_RAIL)
    val shot = keyedCropped(UiArt.WORD_SHOT)
    val rivet = titleSlots[2]
    val hex = titleSlots[3]
    // AABB inner edges are Rivet's axe / Hex's shield; the V opening is the torsos.
    val leftClear = rivet.left + rivet.width() * 0.55f + margin
    val rightClear = hex.left + hex.width() * 0.38f - margin
    val cx = (leftClear + rightClear) / 2f
    val availW = (rightClear - leftClear).coerceAtLeast(1f)
    val aspect = maxOf(rail.width / rail.height.toFloat(), shot.width / shot.height.toFloat())
    val pocketH = (titleSlots[2].top + titleSlots[2].height() * 0.18f - titleSlots[0].top).coerceAtLeast(1f)
    var wordH = availW / aspect
    val stack = wordH * 2f + gap
    if (stack > pocketH) wordH = ((pocketH - gap) / 2f).coerceAtLeast(1f)
    val railW = wordH * (rail.width / rail.height.toFloat())
    val shotW = wordH * (shot.width / shot.height.toFloat())
    val top = titleSlots[0].top
    blitFit(canvas, rail, cx - railW / 2f, top, railW, wordH)
    blitFit(
      canvas,
      shot,
      cx - shotW / 2f + 3f * dp,
      top + wordH + gap - 1f * dp,
      shotW,
      wordH,
    )
    layoutTitleHits(sw, sh)
    val size = sh * 0.042f
    val modeY = sh - sh * 0.105f
    val dipY = sh - sh * 0.048f
    drawMenuItem(canvas, "ARCADE", arcadeBtn.centerX(), modeY, size, titleFocus == 0)
    drawMenuItem(canvas, "VS", vsBtn.centerX(), modeY, size, titleFocus == 2)
    val dip = if (cpuLevel == CpuLevel.HARD) "HARD" else "EASY"
    drawMenuItem(canvas, dip, diffBtn.centerX(), dipY, size, titleFocus == 1)
  }

  private fun layoutTitleRoster(sw: Float, sh: Float) {
    val dp = sh / 360f
    val menuBand = sh * 0.18f
    val pad = 10f * dp
    val step = 24f * dp
    val charH = (sh - pad - step * 2f - menuBand).coerceAtLeast(1f)
    val overlapL = 0.24f * charH
    val overlapR = 0.17f * charH
    val widths =
      FloatArray(Fighter.selectOrder.size) { i ->
        val path = Fighter.selectOrder[i].art().selectFullBody ?: return@FloatArray charH * 0.45f
        val bmp = keyedCropped(path)
        charH * (bmp.width / bmp.height.toFloat())
      }
    var x = pad
    for (i in 0 until TITLE_LEFT) {
      val y = pad + step * i
      titleSlots[i].set(x, y, x + widths[i], y + charH)
      x += overlapL
    }
    val maruX = sw - pad - widths[5]
    titleSlots[5].set(maruX, pad, maruX + widths[5], pad + charH)
    val quillX = maruX - overlapR
    titleSlots[4].set(quillX, pad + step, quillX + widths[4], pad + step + charH)
    val hexX = quillX - overlapR
    titleSlots[3].set(hexX, pad + step * 2f, hexX + widths[3], pad + step * 2f + charH)
  }

  private fun touchTitle(x: Float, y: Float, sw: Float, sh: Float) {
    layoutTitleHits(sw, sh)
    if (arcadeBtn.contains(x, y)) {
      armTitleCommit(PlayMode.ARCADE, 0)
      return
    }
    if (vsBtn.contains(x, y)) {
      armTitleCommit(PlayMode.VS, 2)
      return
    }
    if (titleCommitT >= 0f) return
    if (diffBtn.contains(x, y)) {
      titleFocus = 1
      cpuLevel = if (cpuLevel == CpuLevel.HARD) CpuLevel.EASY else CpuLevel.HARD
      SoundManager.instance.playSFX(SoundManager.SFX_ARROW)
      titleT = 0f
    }
  }

  private fun armTitleCommit(mode: PlayMode, focus: Int) {
    titleFocus = focus
    if (titleCommitMode != mode) {
      SoundManager.instance.playSFX(SoundManager.SFX_SELECT)
    }
    titleCommitMode = mode
    titleCommitT = 0f
    titleT = 0f
  }

  private fun layoutTitleHits(sw: Float, sh: Float) {
    val size = sh * 0.042f
    val modeY = sh - sh * 0.105f
    val dipY = sh - sh * 0.048f
    placeMenuHit(arcadeBtn, "ARCADE", sw * 0.28f, modeY, size)
    placeMenuHit(vsBtn, "VS", sw * 0.72f, modeY, size)
    val dip = if (cpuLevel == CpuLevel.HARD) "HARD" else "EASY"
    placeMenuHit(diffBtn, dip, sw * 0.50f, dipY, size)
  }

  private fun drawGoldLine(canvas: Canvas, text: CharSequence, cx: Float, y: Float, size: Float, color: Int = GOLD) {
    goldShadowPaint.textSize = size
    goldRimPaint.textSize = size
    goldPaint.textSize = size
    goldPaint.color = color
    goldRimPaint.color = if (color == GOLD || color == Color.YELLOW) 0xFFB35400.toInt() else 0xFF3A3A3A.toInt()
    val drop = size * 0.08f
    val rim = size * 0.04f
    canvas.drawText(text, 0, text.length, cx + drop, y + drop, goldShadowPaint)
    canvas.drawText(text, 0, text.length, cx + rim, y + rim, goldRimPaint)
    canvas.drawText(text, 0, text.length, cx, y, goldPaint)
    goldPaint.color = GOLD
  }

  private fun drawChevrons(canvas: Canvas, cx: Float, y: Float, textW: Float, size: Float) {
    goldPaint.textSize = size
    val markW = goldPaint.measureText(">")
    val gap = size * 0.85f
    drawGoldLine(canvas, ">", cx - textW * 0.5f - gap - markW * 0.5f, y, size)
    drawGoldLine(canvas, "<", cx + textW * 0.5f + gap + markW * 0.5f, y, size)
  }

  private fun placeMenuHit(out: RectF, label: String, cx: Float, y: Float, size: Float) {
    goldPaint.textSize = size
    val w = goldPaint.measureText(label)
    val fm = goldPaint.fontMetrics
    out.set(cx - w * 0.5f, y + fm.ascent, cx + w * 0.5f, y + fm.descent)
    out.inset(-size * 0.9f, -size * 0.5f)
  }

  private fun drawMenuItem(
    canvas: Canvas,
    label: String,
    cx: Float,
    y: Float,
    size: Float,
    focused: Boolean,
    color: Int = GOLD,
  ) {
    goldPaint.textSize = size
    val w = goldPaint.measureText(label)
    drawGoldLine(canvas, label, cx, y, size, color)
    if (focused) drawChevrons(canvas, cx, y, w, size)
  }

  private fun drawArcadeSelectFrame(canvas: Canvas, rect: RectF, focused: Boolean, sh: Float) {
    val s = (sh / 1080f).coerceAtLeast(0.5f)
    uiSelectIdleStrokePaint.strokeWidth = 5f * s
    uiSelectIdleInnerPaint.strokeWidth = 2f * s
    uiSelectFocusStrokePaint.strokeWidth = 6f * s
    uiSelectFocusInnerPaint.strokeWidth = 2f * s
    val outer = if (focused) uiSelectFocusStrokePaint else uiSelectIdleStrokePaint
    val inner = if (focused) uiSelectFocusInnerPaint else uiSelectIdleInnerPaint
    canvas.drawRect(rect, outer)
    val inset = 8f * s
    canvas.drawRect(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset, inner)
  }

  private fun drawRanking(canvas: Canvas, sw: Float, sh: Float) {
    blitFill(canvas, opaque(UiArt.SELECT_BG), 0f, 0f, sw, sh)
    val cx = sw * 0.5f
    val dip = cpuLevel.table
    drawGoldLine(canvas, "TOP SCORES", cx, sh * 0.09f, sh * 0.055f)
    val diff = if (cpuLevel == CpuLevel.EASY) "EASY" else "HARD"
    drawGoldLine(canvas, diff, cx, sh * 0.148f, sh * 0.036f, CREAM)
    val rowTop = sh * 0.22f
    val rowStep = sh * 0.068f
    val size = sh * 0.036f
    var i = 0
    while (i < HighScoreManager.SLOT_COUNT) {
      rankLine.setLength(0)
      if (i < 9) rankLine.append(' ')
      rankLine.append(i + 1)
      rankLine.append(' ')
      val score = HighScoreManager.scoreAt(dip, i).coerceIn(0, 99_999_999)
      var digits = 1
      var tally = score
      while (tally >= 10) {
        tally /= 10
        digits++
      }
      var pad = 8 - digits
      while (pad > 0) {
        rankLine.append('0')
        pad--
      }
      rankLine.append(score)
      rankLine.append("  ")
      rankLine.append(HighScoreManager.nameChar(dip, i, 0))
      rankLine.append(HighScoreManager.nameChar(dip, i, 1))
      rankLine.append(HighScoreManager.nameChar(dip, i, 2))
      rankLine.append("  FT")
      rankLine.append(HighScoreManager.fightsAt(dip, i))
      drawGoldLine(canvas, rankLine, cx, rowTop + i * rowStep, size, CREAM)
      i++
    }
    if ((System.currentTimeMillis() / 600L) % 2L == 0L) {
      drawGoldLine(canvas, "1P START", cx, sh * 0.94f, sh * 0.038f)
    }
  }

  private fun layoutRegisterHits(sw: Float, sh: Float) {
    registerLeft.set(0f, sh * 0.25f, sw * 0.45f, sh * 0.65f)
    registerRight.set(sw * 0.55f, sh * 0.25f, sw, sh * 0.65f)
    registerSet.set(sw * 0.10f, sh * 0.75f, sw * 0.90f, sh * 0.88f)
  }

  private fun drawRegister(canvas: Canvas, sw: Float, sh: Float) {
    blitFill(canvas, opaque(UiArt.SELECT_BG), 0f, 0f, sw, sh)
    layoutRegisterHits(sw, sh)
    val cx = sw * 0.5f
    drawGoldLine(canvas, "REGISTRATION", cx, sh * 0.16f, sh * 0.07f, CPU)
    drawGoldLine(canvas, "HI-SCORE ENTRY", cx, sh * 0.24f, sh * 0.04f, CREAM)
    val diff = if (cpuLevel == CpuLevel.EASY) "EASY" else "HARD"
    drawGoldLine(canvas, diff, cx, sh * 0.30f, sh * 0.04f)
    val letterSize = sh * 0.12f
    val spacing = sh * 0.18f
    val startX = cx - spacing
    val letterY = sh * 0.50f
    val blink = sin(registerT * 14f) * 0.5f + 0.5f
    val blinkAlpha = (80f + blink * 175f).toInt()
    val wing = sh * 0.08f
    var idx = 0
    while (idx < 3) {
      val slotX = startX + idx * spacing
      val ch = if (idx == registerCharIndex) registerChar else registerName[idx]
      if (idx == registerCharIndex) {
        goldPaint.alpha = blinkAlpha
        goldShadowPaint.alpha = blinkAlpha
        drawGoldLine(canvas, "<", slotX - wing, letterY, letterSize * 0.45f, CREAM)
        drawGoldLine(canvas, ">", slotX + wing, letterY, letterSize * 0.45f, CREAM)
      }
      drawGoldLine(canvas, ch.toString(), slotX, letterY, letterSize)
      goldPaint.alpha = 255
      goldShadowPaint.alpha = 255
      idx++
    }
    drawGoldLine(canvas, "[ TAP TO LOCK INITIAL ]", cx, sh * 0.82f, sh * 0.032f)
  }

  private fun touchRegister(x: Float, y: Float, sw: Float, sh: Float) {
    layoutRegisterHits(sw, sh)
    if (registerSet.contains(x, y)) {
      confirmRegisterLetter()
      return
    }
    if (registerLeft.contains(x, y)) {
      bumpRegisterChar(false)
      return
    }
    if (registerRight.contains(x, y)) {
      bumpRegisterChar(true)
    }
  }

  private fun bumpRegisterChar(up: Boolean) {
    var code = registerChar.code
    if (up) {
      code++
      if (code > 'Z'.code) code = 'A'.code
    } else {
      code--
      if (code < 'A'.code) code = 'Z'.code
    }
    registerChar = code.toChar()
    SoundManager.instance.playSFX(SoundManager.SFX_ARROW)
  }

  private fun confirmRegisterLetter() {
    registerName[registerCharIndex] = registerChar
    registerCharIndex++
    SoundManager.instance.playSFX(SoundManager.SFX_SELECT)
    if (registerCharIndex >= 3) {
      HighScoreManager.checkAndInsertNewScore(
        context,
        arcadeSkill,
        registerName[0],
        registerName[1],
        registerName[2],
        arcadeWins,
        cpuLevel.table,
      )
      goRanking()
    } else {
      registerChar = 'A'
    }
  }

  private fun drawDemo(canvas: Canvas, sw: Float, sh: Float) {
    drawMatch(canvas, sw, sh)
    if ((demoT / DEMO_FLASH_S).toInt() % 2 == 1) return
    val courtL = World.FRAME_LEFT * sw
    val courtT = World.FRAME_TOP * sh
    val courtW = sw - World.FRAME_LEFT * sw - World.FRAME_RIGHT * sw
    val courtH = sh - World.FRAME_TOP * sh - World.FRAME_BOTTOM * sh
    val cx = courtL + courtW / 2f
    val cy = courtT + courtH / 2f
    val label = "DEMO"
    textPaint.textSize = courtH * 0.09f
    val fm = textPaint.fontMetrics
    val ty = cy - (fm.ascent + fm.descent) / 2f
    textPaint.color = DEMO_RED
    canvas.drawText(label, cx, ty, textPaint)
  }

  private fun drawSelect(canvas: Canvas, sw: Float, sh: Float) {
    val dp = sh / 360f
    blitFill(canvas, opaque(UiArt.SELECT_BG), 0f, 0f, sw, sh)
    val highlighted = select.highlighted
    highlighted.art().selectFullBody?.let { path ->
      val pad = 12f * dp
      val pl = sw * 0.42f
      val pt = pad
      val pw = sw * 0.58f
      val ph = sh - pad * 2f
      canvas.save()
      canvas.clipRect(pl, pt, pl + pw, pt + ph)
      blitFillHeightEnd(canvas, keyed(path), pl, pt, pw, ph)
      canvas.restore()
    }
    val colL = 16f * dp
    val colT = 14f * dp
    val colW = sw * 0.50f - colL - 12f * dp
    val padH = 8f * dp
    val padV = 8f * dp
    val gapT = 6f * dp
    val rowWTiles = sw - padH * 2f
    val tile = (rowWTiles - gapT * (Fighter.selectOrder.size - 1)) / Fighter.selectOrder.size
    val rowTop = sh - padV - tile
    val colH = (rowTop - colT - 10f * dp).coerceAtLeast(1f)
    val selectBmp = keyed(UiArt.PLAYER_SELECT)
    val nameH = 28f * dp
    val flavorH = 15f * dp
    val ctrlH = sh * 0.07f
    val minGap = 10f * dp
    val slots = 5f
    val bannerCap = (colH - nameH - flavorH - ctrlH - minGap * slots).coerceAtLeast(24f * dp)
    val bannerH = min(colW * (selectBmp.height / selectBmp.width.toFloat()), bannerCap)
    val gap = ((colH - bannerH - nameH - flavorH - ctrlH) / slots).coerceAtLeast(0f)
    var y = colT + gap
    blitFit(canvas, selectBmp, colL, y, colW, bannerH)
    y += bannerH + gap
    highlighted.art().nameSelect?.let { path ->
      val nw = colW * 0.55f
      blitFit(canvas, keyed(path), colL + (colW - nw) / 2f, y, nw, nameH)
    }
    y += nameH + gap
    drawFlavorLine(canvas, FighterKit.of(highlighted).flavor, colL, y, colW, flavorH)
    y += flavorH + gap + 6f * dp
    val size = sh * 0.038f
    val locked = select.step == SelectStep.LOCKED
    val selCx = colL + colW * 0.50f
    val leftCx = colL + colW * 0.08f
    val rightCx = colL + colW * 0.92f
    val labelY = y + ctrlH * 0.55f
    if (!locked) {
      drawMenuItem(canvas, "L", leftCx, labelY, size, selectFocus == 0)
      drawMenuItem(canvas, "SELECT", selCx, labelY, size, selectFocus == 1)
      drawMenuItem(canvas, "R", rightCx, labelY, size, selectFocus == 2)
    }
    placeMenuHit(leftArrow, "L", leftCx, labelY, size)
    placeMenuHit(selectBtn, "SELECT", selCx, labelY, size)
    placeMenuHit(rightArrow, "R", rightCx, labelY, size)

    Fighter.selectOrder.forEachIndexed { i, fighter ->
      val l = padH + i * (tile + gapT)
      faceTiles[i].set(l, rowTop, l + tile, rowTop + tile)
      drawFaceTile(canvas, fighter, i, faceTiles[i], dp)
    }
  }

  private fun drawFaceTile(canvas: Canvas, fighter: Fighter, index: Int, box: RectF, dp: Float) {
    fillPaint.color = Color.BLACK
    canvas.drawRect(box, fillPaint)
    fighter.art().face?.let { path ->
      blitCrop(canvas, keyed(path), box.left, box.top, box.width(), box.height())
    }
    val selected = index == select.cursorIndex
    drawArcadeSelectFrame(canvas, box, focused = false, sh = box.height() * 3.2f)
    if (selected && (System.currentTimeMillis() / 400L) % 2L == 0L) {
      drawArcadeSelectFrame(canvas, box, focused = true, sh = box.height() * 3.2f)
    }
    val badge =
      when {
        select.arcade ->
          if (select.firstPick == fighter || (select.firstPick == null && selected)) UiArt.BADGE_1P
          else null
        (select.step == SelectStep.PICK_SECOND || select.step == SelectStep.LOCKED) &&
          (index == select.cursorIndex || select.secondPick == fighter) -> UiArt.BADGE_2P
        select.firstPick == fighter -> UiArt.BADGE_1P
        selected -> UiArt.BADGE_1P
        else -> null
      }
    if (badge != null) {
      val bw = box.width() * 0.42f
      blitFit(canvas, keyed(badge), box.left + 2f * dp, box.top + 2f * dp, bw, bw)
    }
  }

  private fun drawVs(canvas: Canvas, sw: Float, sh: Float) {
    val dp = sh / 360f
    blitFill(canvas, opaque(UiArt.VS_BG), 0f, 0f, sw, sh)
    val barPadB = 16f * dp
    val barPadT = 8f * dp
    val startSize = sh * 0.045f
    val nameH = 40f * dp
    val nameBtnGap = 18f * dp
    val barH = barPadT + nameH + nameBtnGap + startSize * 1.8f + barPadB
    val bustH = sh - barH
    val totalW = 1f + 0.42f + 1f
    val leftW = sw * (1f / totalW)
    val vsW = sw * (0.42f / totalW)
    val enter = 1f - easeOutCubic((vsAnimT / VS_PORTRAIT_S).coerceIn(0f, 1f))
    val fromLeft = -enter * leftW
    val fromRight = enter * leftW
    val boxPad = 10f * dp
    vsPortraitL.set(boxPad + fromLeft, 10f * dp, leftW - boxPad + fromLeft, bustH - 10f * dp)
    vsPortraitR.set(leftW + vsW + boxPad + fromRight, 10f * dp, sw - boxPad + fromRight, bustH - 10f * dp)
    fillPaint.color = Color.BLACK
    canvas.drawRect(vsPortraitL, fillPaint)
    canvas.drawRect(vsPortraitR, fillPaint)
    drawArcadeSelectFrame(canvas, vsPortraitL, focused = false, sh = sh)
    drawArcadeSelectFrame(canvas, vsPortraitR, focused = false, sh = sh)
    you.art().vsLeft?.let { path ->
      blitSelectPreview(canvas, keyed(path), vsPortraitL)
    }
    val vsBmp = keyed(UiArt.VS)
    val vsDrawH = min(140f * dp, bustH)
    val vsDrawW = min(vsW, vsDrawH * (vsBmp.width / vsBmp.height.toFloat()))
    val vsScale = easeOutBack(((vsAnimT - VS_PORTRAIT_S) / VS_MARK_S).coerceIn(0f, 1f))
    if (vsScale > 0.01f) {
      val vsL = leftW + (vsW - vsDrawW) / 2f
      val vsT = (bustH - vsDrawH) / 2f
      val cx = vsL + vsDrawW / 2f
      val cy = vsT + vsDrawH / 2f
      val dw = vsDrawW * vsScale
      val dh = vsDrawH * vsScale
      blitFit(canvas, vsBmp, cx - dw / 2f, cy - dh / 2f, dw, dh)
    }
    rival.art().vsRight?.let { path ->
      blitSelectPreview(canvas, keyed(path), vsPortraitR)
    }
    val blink = (System.currentTimeMillis() / 400L) % 2L == 0L
    if (blink && vsAnimT >= VS_PORTRAIT_S) {
      drawArcadeSelectFrame(canvas, vsPortraitL, focused = true, sh = sh)
      drawArcadeSelectFrame(canvas, vsPortraitR, focused = true, sh = sh)
    }
    val nameY = bustH + barPadT
    val namePad = 10f * dp
    val nw = leftW - namePad * 2f
    you.art().nameVs?.let { path ->
      blitFit(canvas, keyed(path), namePad + fromLeft, nameY, nw, nameH)
    }
    rival.art().nameVs?.let { path ->
      blitFit(canvas, keyed(path), leftW + vsW + namePad + fromRight, nameY, nw, nameH)
    }
    layoutVsHits(sw, sh)
    drawMenuItem(canvas, "START", startBtn.centerX(), startBtn.centerY(), startSize, focused = true)
  }

  private fun blitSelectPreview(canvas: Canvas, bmp: Bitmap, box: RectF) {
    val maxW = box.width() * 0.78f
    val maxH = box.height() * 0.78f
    val srcW = bmp.width.toFloat().coerceAtLeast(1f)
    val srcH = bmp.height.toFloat().coerceAtLeast(1f)
    val scale = (maxW / srcW).coerceAtMost(maxH / srcH)
    val dw = srcW * scale
    val dh = srcH * scale
    val px = box.centerX()
    val py = box.centerY()
    blitFit(canvas, bmp, px - dw * 0.5f, py - dh * 0.5f, dw, dh)
  }

  private fun drawMatch(canvas: Canvas, vw: Float, vh: Float) {
    val winner =
      when (world.phase) {
        Phase.YOU_WIN -> you
        Phase.CPU_WIN -> rival
        else -> null
      }
    if (winner != null) {
      drawEnding(canvas, winner, vw, vh)
      return
    }
    canvas.drawColor(INK)
    canvas.save()
    canvas.translate(world.cabinetShakeX(vw), world.cabinetShakeY(vh))
    val courtL = World.FRAME_LEFT * vw
    val courtT = World.FRAME_TOP * vh
    val courtR = vw - World.FRAME_RIGHT * vw
    val courtB = vh - World.FRAME_BOTTOM * vh
    val courtW = courtR - courtL
    val courtH = courtB - courtT
    blitFill(canvas, opaqueOrFallback(rival.art().stageCourt(), Fighter.RIVET.art().stageCourt()), courtL, courtT, courtW, courtH)
    if (world.hexCarLive()) {
      drawHexCar(canvas, courtL, courtT, courtW, courtH)
    }
    if (world.hawkLive()) {
      drawHawk(canvas, courtL, courtT, courtW, courtH)
    }
    if (world.kiteXLive()) {
      drawKiteX(canvas, courtL, courtT, courtW, courtH)
    }
    if (world.maruLogLive()) {
      drawMaruLogs(canvas, courtL, courtT, courtW, courtH)
    }
    for (chip in world.chips) {
      drawChip(
        canvas,
        keyedOrFallback(
          rival.art().stageChip(chip.side, standing = chip.alive),
          Fighter.RIVET.art().stageChip(chip.side, standing = chip.alive),
        ),
        chip.x,
        chip.y,
        chip.w,
        chip.h,
        courtL,
        courtT,
        courtW,
        courtH,
        standing = chip.alive,
        flip = chip.side == Side.CPU,
      )
    }
    if (world.postLive()) {
      for (slot in 0 until 3) {
        if (!world.wellVisible(slot)) continue
        drawPost(
          canvas,
          slot,
          courtL,
          courtT,
          courtW,
          courtH,
        )
      }
    }
    if (world.traceLive()) {
      drawTrace(canvas, courtL, courtT, courtW, courtH)
    }
    drawFighter(
      canvas,
      youFrames,
      world.youPose(),
      World.YOU_PADDLE_X,
      world.youPaddleY,
      true,
      YOU,
      courtL,
      courtT,
      courtW,
      courtH,
    )
    drawFighter(
      canvas,
      rivalFrames,
      world.cpuPose(),
      World.CPU_PADDLE_X,
      world.cpuPaddleY,
      false,
      CPU,
      courtL,
      courtT,
      courtW,
      courtH,
    )
    drawBall(canvas, courtL, courtT, courtW, courtH)
    drawSkillPops(canvas, courtL, courtT, courtW, courtH)
    when (world.phase) {
      Phase.ROUND -> {
        val banner = keyed(UiArt.ROUND)
        val num = keyed(UiArt.roundNum(world.roundNumber()))
        val gap = 8f * (vh / 360f)
        val maxW = courtW * 0.88f
        val maxH = courtH * 0.86f
        val numToBanner = 0.28f / 0.88f
        var bannerW = maxW
        var bannerH = bannerW * (banner.height / banner.width.toFloat())
        var numW = bannerW * numToBanner
        var numH = numW * (num.height / num.width.toFloat())
        val rawH = bannerH + gap + numH
        if (rawH > maxH) {
          val s = maxH / rawH
          bannerW *= s
          bannerH *= s
          numW *= s
          numH *= s
        }
        val top = courtT + (courtH - bannerH - gap - numH) / 2f
        blitFit(canvas, banner, courtL + (courtW - bannerW) / 2f, top, bannerW, bannerH)
        blitFit(canvas, num, courtL + (courtW - numW) / 2f, top + bannerH + gap, numW, numH)
      }
      Phase.SERVE -> {
        if (world.fightBannerVisible()) {
          val banner = keyed(UiArt.FIGHT)
          val bannerW = courtW * 0.92f
          val bannerH = bannerW * (banner.height / banner.width.toFloat())
          blitFit(canvas, banner, courtL + (courtW - bannerW) / 2f, courtT + (courtH - bannerH) / 2f, bannerW, bannerH)
        }
      }
      Phase.SET_WIN -> {
        if (world.setWinBannerVisible()) {
          val banner = keyed(if (world.setWinner == Side.YOU) UiArt.YOU_WIN else UiArt.YOU_LOSE)
          val bannerW = courtW * 0.88f
          val bannerH = bannerW * (banner.height / banner.width.toFloat())
          blitFit(canvas, banner, courtL + (courtW - bannerW) / 2f, courtT + (courtH - bannerH) / 2f, bannerW, bannerH)
        }
      }
      else -> {}
    }
    blitFill(canvas, keyedOrFallback(rival.art().stageCabinet(), Fighter.RIVET.art().stageCabinet(), hole = true), 0f, 0f, vw, vh)
    drawWellText(canvas, World.P1_SCORE_INSET, world.youScore.toString().padStart(2, '0'), YOU, 0.72f, vw, vh)
    drawWellText(canvas, World.P1_NAME_INSET, you.displayName.uppercase(), CREAM, 0.42f, vw, vh)
    drawSetDots(canvas, World.P1_SETS_INSET, world.youSets, YOU, vw, vh)
    drawWellText(canvas, World.TIME_INSET, world.youSkill.toString(), YOU, 0.72f, vw, vh, rows = 2, row = 0)
    drawWellText(canvas, World.TIME_INSET, world.timeDisplay(), CREAM, 0.72f, vw, vh, rows = 2, row = 1)
    drawSetDots(canvas, World.P2_SETS_INSET, world.cpuSets, CPU, vw, vh)
    drawWellText(canvas, World.P2_NAME_INSET, rival.displayName.uppercase(), CREAM, 0.42f, vw, vh)
    drawWellText(canvas, World.P2_SCORE_INSET, world.cpuScore.toString().padStart(2, '0'), CPU, 0.72f, vw, vh)
    canvas.restore()
  }

  private fun drawEnding(canvas: Canvas, winner: Fighter, vw: Float, vh: Float) {
    blitFill(canvas, opaque(UiArt.SELECT_BG), 0f, 0f, vw, vh)
    val dp = vh / 360f
    val pad = 12f * dp
    val railW = vw * 0.48f
    val right = railW - pad
    winner.art().ending?.let { path ->
      blitFillHeightEnd(canvas, keyed(path), railW, 0f, vw - railW, vh)
    }
    winner.art().wins?.let { path ->
      val bmp = keyed(path)
      val maxW = (right - pad).coerceAtLeast(1f)
      val maxH = (vh * 0.42f).coerceAtLeast(1f)
      val aspect = bmp.width / bmp.height.toFloat()
      var dw = maxW
      var dh = dw / aspect
      if (dh > maxH) {
        dh = maxH
        dw = dh * aspect
      }
      val x = right - dw
      val y = (vh - dh) / 2f
      blitFit(canvas, bmp, x, y, dw, dh)
    }
    if (resultCardT < RESULT_LOCK_S) return
    val size = vh * 0.042f
    drawMenuItem(canvas, "[ CONTINUE ]", vw * 0.25f, vh - 32f * dp, size, focused = true)
  }

  private fun drawFighter(
    canvas: Canvas,
    frames: Map<PaddlePose, Bitmap>,
    pose: PaddlePose,
    paddleX: Float,
    paddleY: Float,
    leftCourt: Boolean,
    fallback: Int,
    courtL: Float,
    courtT: Float,
    courtW: Float,
    courtH: Float,
  ) {
    layoutFighterDest(dst, paddleX, paddleY, leftCourt, courtL, courtT, courtW, courtH)
    val bmp = frames[pose] ?: frames[PaddlePose.IDLE]
    if (bmp != null) {
      blitFill(canvas, bmp, dst.left, dst.top, dst.width(), dst.height())
    } else {
      val hitX = if (leftCourt) World.YOU_HIT_X else World.CPU_HIT_X
      fillPaint.color = fallback
      canvas.drawRect(
        courtL + hitX * courtW,
        dst.top,
        courtL + hitX * courtW + World.PADDLE_THICK * courtW,
        dst.bottom,
        fillPaint,
      )
    }
  }

  private fun drawPost(
    canvas: Canvas,
    slot: Int,
    courtL: Float,
    courtT: Float,
    courtW: Float,
    courtH: Float,
  ) {
    val left = courtL + world.wellX() * courtW
    val top = courtT + world.wellY(slot) * courtH
    val w = (world.wellW() * courtW).coerceAtLeast(1f)
    val h = (world.wellH() * courtH).coerceAtLeast(1f)
    blitFill(canvas, keyed(UiArt.courtWall(world.postWallFrame(slot))), left, top, w, h)
  }

  private fun drawTrace(
    canvas: Canvas,
    courtL: Float,
    courtT: Float,
    courtW: Float,
    courtH: Float,
  ) {
    val left = courtL + world.traceX() * courtW
    val top = courtT + world.traceY() * courtH
    val w = (world.traceW() * courtW).coerceAtLeast(1f)
    val h = (world.traceH() * courtH).coerceAtLeast(1f)
    blitFill(canvas, keyed(UiArt.COURT_TRACE), left, top, w, h)
  }

  private fun drawHexCar(
    canvas: Canvas,
    courtL: Float,
    courtT: Float,
    courtW: Float,
    courtH: Float,
  ) {
    val left = courtL + world.hexCarX() * courtW
    val top = courtT + world.hexCarY() * courtH
    val w = (world.hexCarW() * courtW).coerceAtLeast(1f)
    val h = (world.hexCarH() * courtH).coerceAtLeast(1f)
    canvas.save()
    canvas.clipRect(courtL, courtT, courtL + courtW, courtT + courtH)
    blitFill(canvas, keyed(UiArt.HEX_CAR), left, top, w, h)
    canvas.restore()
  }

  private fun drawHawk(
    canvas: Canvas,
    courtL: Float,
    courtT: Float,
    courtW: Float,
    courtH: Float,
  ) {
    val left = courtL + world.hawkX() * courtW
    val top = courtT + world.hawkY() * courtH
    val w = (world.hawkW() * courtW).coerceAtLeast(1f)
    val h = (world.hawkH() * courtH).coerceAtLeast(1f)
    canvas.save()
    canvas.clipRect(courtL, courtT, courtL + courtW, courtT + courtH)
    val bmp = keyed(UiArt.courtHawk(world.hawkFlapFrame()))
    val cx = left + w / 2f
    val cy = top + h / 2f
    canvas.save()
    canvas.rotate(world.hawkHeadingDeg(), cx, cy)
    blitFill(canvas, bmp, left, top, w, h)
    canvas.restore()
    canvas.restore()
  }

  private fun drawKiteX(
    canvas: Canvas,
    courtL: Float,
    courtT: Float,
    courtW: Float,
    courtH: Float,
  ) {
    val left = courtL + world.kiteXX() * courtW
    val top = courtT + world.kiteXY() * courtH
    val w = (world.kiteXW() * courtW).coerceAtLeast(1f)
    val h = (world.kiteXH() * courtH).coerceAtLeast(1f)
    canvas.save()
    canvas.clipRect(courtL, courtT, courtL + courtW, courtT + courtH)
    val cx = left + w / 2f
    val cy = top + h / 2f
    canvas.save()
    canvas.rotate(world.kiteXDeg(), cx, cy)
    blitFill(canvas, keyed(UiArt.KITE_X), left, top, w, h)
    canvas.restore()
    canvas.restore()
  }

  private fun drawMaruLogs(
    canvas: Canvas,
    courtL: Float,
    courtT: Float,
    courtW: Float,
    courtH: Float,
  ) {
    val bmp = keyed(UiArt.MARU_LOG)
    canvas.save()
    canvas.clipRect(courtL, courtT, courtL + courtW, courtT + courtH)
    for (slot in 0 until world.maruLogCount()) {
      val left = courtL + world.maruLogX(slot) * courtW
      val top = courtT + world.maruLogY(slot) * courtH
      val w = (world.maruLogW(slot) * courtW).coerceAtLeast(1f)
      val h = (world.maruLogH(slot) * courtH).coerceAtLeast(1f)
      val cx = left + w / 2f
      val cy = top + h / 2f
      canvas.save()
      canvas.rotate(world.maruLogDeg(slot), cx, cy)
      blitFill(canvas, bmp, left, top, w, h)
      canvas.restore()
    }
    canvas.restore()
  }

  private fun drawSkillPops(
    canvas: Canvas,
    courtL: Float,
    courtT: Float,
    courtW: Float,
    courtH: Float,
  ) {
    if (world.skillPopups.isEmpty()) return
    val size = courtH * (36f / 864f)
    val drop = courtH * (2f / 864f)
    popupPaint.textSize = size
    popupShadowPaint.textSize = size
    for (pop in world.skillPopups) {
      val fade = (1f - pop.age / World.SKILL_POP_LIFE).coerceIn(0f, 1f)
      val alpha = (255f * fade).toInt()
      popupPaint.color = if (pop.you) YOU else CPU
      popupPaint.alpha = alpha
      popupShadowPaint.alpha = alpha
      val text = pop.value.toString()
      val x = courtL + pop.x * courtW
      val y = courtT + pop.y * courtH
      val w = popupPaint.measureText(text)
      canvas.drawText(text, x - w * 0.5f + drop, y + drop, popupShadowPaint)
      canvas.drawText(text, x - w * 0.5f, y, popupPaint)
    }
    popupPaint.alpha = 255
    popupShadowPaint.alpha = 255
  }

  private fun drawChip(
    canvas: Canvas,
    sprite: Bitmap,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    courtL: Float,
    courtT: Float,
    courtW: Float,
    courtH: Float,
    standing: Boolean,
    flip: Boolean,
  ) {
    val slotL = courtL + x * courtW
    val slotT = courtT + y * courtH
    val slotW = w * courtW
    val slotH = h * courtH
    val left: Float
    val top: Float
    val fw: Float
    val fh: Float
    if (standing) {
      fh = slotH
      fw = (fh * sprite.width / sprite.height.toFloat()).coerceAtLeast(1f)
      // Court-facing edge of the gutter so the bar is the blocker.
      left = if (flip) slotL else slotL + slotW - fw
      top = slotT
    } else {
      left = slotL
      top = slotT
      fw = slotW.coerceAtLeast(1f)
      fh = slotH.coerceAtLeast(1f)
    }
    if (flip) {
      val cx = left + fw / 2f
      canvas.save()
      canvas.scale(-1f, 1f, cx, top + fh / 2f)
      blit(canvas, sprite, left, top, fw, fh)
      canvas.restore()
    } else {
      blit(canvas, sprite, left, top, fw, fh)
    }
  }

  private fun drawBall(canvas: Canvas, courtL: Float, courtT: Float, courtW: Float, courtH: Float) {
    val radius = World.BALL_R * courtH
    val side = (radius * 2f).coerceAtLeast(2f)
    val cx = courtL + world.ballX * courtW
    val cy = courtT + world.ballY * courtH
    val flash = world.bounceFlash()
    if (flash > 0f) {
      fillPaint.color = ((200 * flash).toInt().coerceIn(0, 255) shl 24) or 0x00FFF8DC
      canvas.drawCircle(cx, cy, radius * (1.12f + 0.28f * flash), fillPaint)
    }
    drawComet(
      canvas,
      courtL,
      courtT,
      courtW,
      courtH,
      cx,
      cy,
      radius,
      side,
      world.ballSpeed(),
      world.ballVx(),
      world.ballVy(),
      tailTravel,
      ice = false,
    )
    if (world.starLive()) {
      val sx = courtL + world.starX() * courtW
      val sy = courtT + world.starY() * courtH
      drawComet(
        canvas,
        courtL,
        courtT,
        courtW,
        courtH,
        sx,
        sy,
        radius,
        side,
        world.starSpeed(),
        world.starVx(),
        world.starVy(),
        iceTailTravel,
        ice = true,
      )
    }
    if (world.iceBurstLive()) {
      val bx = courtL + world.iceBurstX() * courtW
      val by = courtT + world.iceBurstY() * courtH
      val burst = side * 2.4f
      blitFit(
        canvas,
        keyed(UiArt.iceBurst(world.iceBurstFrame())),
        bx - burst / 2f,
        by - burst / 2f,
        burst,
        burst,
      )
    }
  }

  private fun drawComet(
    canvas: Canvas,
    courtL: Float,
    courtT: Float,
    courtW: Float,
    courtH: Float,
    cx: Float,
    cy: Float,
    radius: Float,
    side: Float,
    speed: Float,
    vx: Float,
    vy: Float,
    travel: Float,
    ice: Boolean,
  ) {
    if (speed > World.BALL_SPEED * 0.08f) {
      val left = ((travel / TAIL_FLICKER_DIST).toInt() and 1) == 0
      val band = World.tailBandForSpeed(speed)
      val path = if (ice) UiArt.iceBallTail(band, left) else UiArt.ballTail(band, left)
      val bmp = keyed(path)
      val (px, py) = UiArt.ballTailPocket(path, bmp.width, bmp.height)
      val scale = radius / UiArt.TAIL_HOLE_R
      val tailW = bmp.width * scale
      val tailH = bmp.height * scale
      val deg = Math.toDegrees(atan2(vy.toDouble(), vx.toDouble())).toFloat()
      canvas.save()
      canvas.clipRect(courtL, courtT, courtL + courtW, courtT + courtH)
      canvas.rotate(deg, cx, cy)
      blit(canvas, bmp, cx - px * scale, cy - py * scale, tailW, tailH)
      canvas.restore()
    }
    val orb = if (ice) UiArt.ICE_BALL else UiArt.BALL
    blitFit(canvas, keyed(orb), cx - side / 2f, cy - side / 2f, side, side)
  }

  private fun drawFlavorLine(canvas: Canvas, text: String, l: Float, t: Float, w: Float, h: Float) {
    textPaint.color = CREAM
    textPaint.textSize = min(h * 0.46f, w / (text.length.coerceAtLeast(1) * 0.62f))
    val fm = textPaint.fontMetrics
    canvas.drawText(text, l + w / 2f, t + h / 2f - (fm.ascent + fm.descent) / 2f, textPaint)
  }

  private fun drawWellText(
    canvas: Canvas,
    inset: CabinetInset,
    text: String,
    color: Int,
    heightFrac: Float,
    vw: Float,
    vh: Float,
    rows: Int = 1,
    row: Int = 0,
  ) {
    val l = inset.leftF * vw
    val t = inset.topF * vh
    val wellW = inset.widthF * vw
    val wellH = inset.heightF * vh
    val rowH = wellH / rows.coerceAtLeast(1)
    val rowT = t + rowH * row
    textPaint.color = color
    textPaint.textSize = min(rowH * heightFrac, wellW / (text.length.coerceAtLeast(1) * 0.62f))
    val fm = textPaint.fontMetrics
    canvas.drawText(text, l + wellW / 2f, rowT + rowH / 2f - (fm.ascent + fm.descent) / 2f, textPaint)
  }

  private fun drawSetDots(canvas: Canvas, inset: CabinetInset, won: Int, accent: Int, vw: Float, vh: Float) {
    val l = inset.leftF * vw
    val t = inset.topF * vh
    val wellW = inset.widthF * vw
    val wellH = inset.heightF * vh
    val dot = min(wellH, wellW / 3.2f) * 0.55f
    val gap = dot * 0.45f
    val total = World.SETS_TO_WIN * dot + (World.SETS_TO_WIN - 1) * gap
    var x = l + (wellW - total) / 2f + dot / 2f
    val cy = t + wellH / 2f
    repeat(World.SETS_TO_WIN) { i ->
      dotPaint.color = if (i < won) accent else MUTE_DIM
      canvas.drawCircle(x, cy, dot / 2f, dotPaint)
      x += dot + gap
    }
  }

  private fun layoutSelectHits(sw: Float, sh: Float) {
    val dp = sh / 360f
    val colL = 16f * dp
    val colT = 14f * dp
    val colW = sw * 0.50f - colL - 12f * dp
    val padH = 8f * dp
    val padV = 8f * dp
    val gapT = 6f * dp
    val rowWTiles = sw - padH * 2f
    val tile = (rowWTiles - gapT * (Fighter.selectOrder.size - 1)) / Fighter.selectOrder.size
    val rowTop = sh - padV - tile
    val colH = (rowTop - colT - 10f * dp).coerceAtLeast(1f)
    val selectBmp = keyed(UiArt.PLAYER_SELECT)
    val nameH = 28f * dp
    val flavorH = 15f * dp
    val ctrlH = sh * 0.07f
    val minGap = 10f * dp
    val slots = 5f
    val bannerCap = (colH - nameH - flavorH - ctrlH - minGap * slots).coerceAtLeast(24f * dp)
    val bannerH = min(colW * (selectBmp.height / selectBmp.width.toFloat()), bannerCap)
    val gap = ((colH - bannerH - nameH - flavorH - ctrlH) / slots).coerceAtLeast(0f)
    val y = colT + gap + bannerH + gap + nameH + gap + flavorH + gap + 6f * dp
    val size = sh * 0.038f
    val labelY = y + ctrlH * 0.55f
    val selCx = colL + colW * 0.50f
    val leftCx = colL + colW * 0.08f
    val rightCx = colL + colW * 0.92f
    placeMenuHit(leftArrow, "L", leftCx, labelY, size)
    placeMenuHit(selectBtn, "SELECT", selCx, labelY, size)
    placeMenuHit(rightArrow, "R", rightCx, labelY, size)
    Fighter.selectOrder.forEachIndexed { i, _ ->
      val l = padH + i * (tile + gapT)
      faceTiles[i].set(l, rowTop, l + tile, rowTop + tile)
    }
  }

  private fun layoutVsHits(sw: Float, sh: Float) {
    val dp = sh / 360f
    val barPadB = 16f * dp
    val barPadT = 8f * dp
    val startSize = sh * 0.045f
    val nameH = 40f * dp
    val nameBtnGap = 18f * dp
    val barH = barPadT + nameH + nameBtnGap + startSize * 1.8f + barPadB
    val bustH = sh - barH
    val y = bustH + barPadT + nameH + nameBtnGap + startSize * 0.35f
    placeMenuHit(startBtn, "START", sw * 0.5f, y, startSize)
  }

  private fun layoutStage() {
    val vw = width.toFloat().coerceAtLeast(1f)
    val vh = height.toFloat().coerceAtLeast(1f)
    val target = 4f / 3f
    if (vw / vh >= target) {
      val w = vh * target
      val x = (vw - w) / 2f
      stage.set(x, 0f, x + w, vh)
    } else {
      val h = vw / target
      val y = (vh - h) / 2f
      stage.set(0f, y, vw, y + h)
    }
  }

  private fun blitFill(canvas: Canvas, bmp: Bitmap, l: Float, t: Float, w: Float, h: Float, alpha: Int = 255) {
    val prev = pixel.alpha
    pixel.alpha = alpha
    blit(canvas, bmp, l, t, w, h)
    pixel.alpha = prev
  }

  private fun blitFit(canvas: Canvas, bmp: Bitmap, l: Float, t: Float, w: Float, h: Float, alpha: Int = 255) {
    val aspect = bmp.width / bmp.height.toFloat()
    var dw = w
    var dh = dw / aspect
    if (dh > h) {
      dh = h
      dw = dh * aspect
    }
    val prev = pixel.alpha
    pixel.alpha = alpha
    blit(canvas, bmp, l + (w - dw) / 2f, t + (h - dh) / 2f, dw, dh)
    pixel.alpha = prev
  }

  private fun blitFillFlipped(canvas: Canvas, bmp: Bitmap, l: Float, t: Float, w: Float, h: Float, alpha: Int = 255) {
    val cx = l + w / 2f
    canvas.save()
    canvas.scale(-1f, 1f, cx, t + h / 2f)
    blitFill(canvas, bmp, l, t, w, h, alpha)
    canvas.restore()
  }

  private fun blitFitFlipped(canvas: Canvas, bmp: Bitmap, l: Float, t: Float, w: Float, h: Float, alpha: Int = 255) {
    val cx = l + w / 2f
    canvas.save()
    canvas.scale(-1f, 1f, cx, t + h / 2f)
    blitFit(canvas, bmp, l, t, w, h, alpha)
    canvas.restore()
  }

  private fun blitFillHeightEnd(canvas: Canvas, bmp: Bitmap, l: Float, t: Float, w: Float, h: Float) {
    val scale = h / bmp.height.toFloat()
    val dw = bmp.width * scale
    blit(canvas, bmp, l + w - dw, t, dw, h)
  }

  private fun blitCrop(canvas: Canvas, bmp: Bitmap, l: Float, t: Float, w: Float, h: Float) {
    val scale = maxOf(w / bmp.width, h / bmp.height)
    val dw = bmp.width * scale
    val dh = bmp.height * scale
    val cx = l + w / 2f
    val cy = t + h / 2f
    canvas.save()
    canvas.clipRect(l, t, l + w, t + h)
    blit(canvas, bmp, cx - dw / 2f, cy - dh / 2f, dw, dh)
    canvas.restore()
  }

  private fun blit(canvas: Canvas, bmp: Bitmap, left: Float, top: Float, w: Float, h: Float) {
    src.set(0, 0, bmp.width, bmp.height)
    dst.set(left, top, left + w, top + h)
    canvas.drawBitmap(bmp, src, dst, pixel)
  }

  private fun keyed(path: String, hole: Boolean = false): Bitmap {
    val key = (if (hole) "h:" else "k:") + path
    return bitmaps.getOrPut(key) { loadKeyedBitmap(context, path, hole) }
  }

  private fun keyedCropped(path: String): Bitmap =
    bitmaps.getOrPut("c:$path") { cropOpaque(loadKeyedBitmap(context, path)) }

  private fun keyedOrFallback(path: String, fallback: String, hole: Boolean = false): Bitmap {
    return try {
      keyed(path, hole)
    } catch (_: Exception) {
      keyed(fallback, hole)
    }
  }

  private fun opaque(path: String): Bitmap = bitmaps.getOrPut("o:$path") { loadOpaqueBitmap(context, path) }

  private fun opaqueOrFallback(path: String, fallback: String): Bitmap {
    return try {
      opaque(path)
    } catch (_: Exception) {
      opaque(fallback)
    }
  }

  private fun loadCourtFrames(art: FighterArt, left: Boolean): Map<PaddlePose, Bitmap> {
    val out = LinkedHashMap<PaddlePose, Bitmap>()
    for (pose in arrayOf(PaddlePose.IDLE, PaddlePose.WALK, PaddlePose.HIT)) {
      val path = art.courtFrame(left, pose) ?: continue
      out[pose] = keyed(path)
    }
    return out
  }

  private fun lockGameCanvas(): Canvas? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      try {
        val hw = holder.lockHardwareCanvas()
        if (hw != null) return hw
      } catch (_: RuntimeException) {
      }
    }
    return holder.lockCanvas()
  }

  private companion object {
    const val MAX_FRAME_NS = 50_000_000L
    const val TITLE_HOLD_S = 5f
    const val TITLE_COMMIT_S = 1f
    const val TITLE_LEFT = 3
    val TITLE_DRAW_ORDER = intArrayOf(0, 1, 2, 5, 4, 3)
    const val DEMO_HOLD_S = 10f
    const val RANKING_HOLD_S = 4f
    const val SELECT_IDLE_S = 45f
    const val RESULT_LOCK_S = 0.8f
    const val RESULT_IDLE_S = 10f
    const val DEMO_FLASH_S = 0.45f
    const val DEMO_RED = 0xFFE01414.toInt()
    const val CABINET = 0xFF0C1822.toInt()
    const val INK = 0xFF071018.toInt()
    const val CREAM = 0xFFF3EFE4.toInt()
    const val GOLD = 0xFFFFFF00.toInt()
    const val YOU = 0xFF5EE0C8.toInt()
    const val CPU = 0xFFFF6B6B.toInt()
    const val MUTE_DIM = 0x598FA3B0
    const val TAIL_FLICKER_DIST = 0.04f
    const val DOUBLE_TAP_MS = 280L
    const val VS_PORTRAIT_S = 0.38f
    const val VS_MARK_S = 0.28f

    private fun easeOutCubic(u: Float): Float {
      val t = 1f - u.coerceIn(0f, 1f)
      return 1f - t * t * t
    }

    private fun easeOutBack(u: Float): Float {
      val t = u.coerceIn(0f, 1f)
      val c = 1.70158f
      val p = t - 1f
      return 1f + (c + 1f) * p * p * p + c * p * p
    }
  }
}
