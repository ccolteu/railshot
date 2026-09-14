package com.cc.railshot.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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
import com.cc.railshot.game.PaddlePose
import com.cc.railshot.game.Phase
import com.cc.railshot.game.SELECT_LINGER_MS
import com.cc.railshot.game.SelectSession
import com.cc.railshot.game.SelectStep
import com.cc.railshot.game.Side
import com.cc.railshot.game.World
import kotlin.math.atan2
import kotlin.math.min

/**
 * One Choreographer SurfaceView for title, demo, select, VS, and match (WW2 Blitz shape).
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Choreographer.FrameCallback {

  private enum class Screen {
    TITLE,
    DEMO,
    SELECT,
    VS,
    MATCH,
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
  private val select = SelectSession()
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
  private val faceTiles = Array(Fighter.roster.size) { RectF() }

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

  init {
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
        titleT += dt
        if (titleT >= TITLE_HOLD_S) goDemo()
      }
      Screen.DEMO -> stepDemo(dt)
      Screen.SELECT -> {
        if (select.step == SelectStep.LOCKED) {
          lingerT += dt
          if (lingerT >= SELECT_LINGER_MS / 1000f) goVs()
        } else {
          selectIdleT += dt
          if (selectIdleT >= SELECT_IDLE_S) goTitle()
        }
      }
      Screen.VS -> {}
      Screen.MATCH -> stepMatch(dt)
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
      Screen.TITLE, Screen.DEMO -> if (event.actionMasked == MotionEvent.ACTION_DOWN) goSelect()
      Screen.SELECT ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
          if (select.step != SelectStep.LOCKED) selectIdleT = 0f
          touchSelect(x, y, sw, sh)
        }
      Screen.VS -> if (event.actionMasked == MotionEvent.ACTION_DOWN) touchVs(x, y, sw, sh)
      Screen.MATCH -> touchMatch(event, x, y, sw, sh)
    }
    return true
  }

  private fun goTitle() {
    screen = Screen.TITLE
    titleT = 0f
    demoT = 0f
    lingerT = 0f
    selectIdleT = 0f
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

  private fun goSelect() {
    if (screen == Screen.SELECT) return
    screen = Screen.SELECT
    select.reset()
    lingerT = 0f
    selectIdleT = 0f
    resultCardT = 0f
  }

  private fun goVs() {
    val first = select.firstPick ?: return
    val second = select.secondPick ?: return
    you = first
    rival = second
    lingerT = 0f
    cpuLevel = CpuLevel.EASY
    screen = Screen.VS
  }

  private fun goMatch() {
    youFrames = loadCourtFrames(you.art(), left = true)
    rivalFrames = loadCourtFrames(rival.art(), left = false)
    world = World(you = you, rival = rival, cpuLevel = cpuLevel)
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
    if (demoT >= DEMO_HOLD_S) goTitle()
  }

  private fun stepMatch(dt: Float) {
    world.step(dt)
    if (world.phase == Phase.PLAYING && !world.impactFrozen()) {
      tailTravel += world.ballSpeed() * dt
      if (world.starLive()) iceTailTravel += world.starSpeed() * dt
    }
    for (sfx in world.drainSfx()) {
      when (sfx) {
        GameSfx.SHIELD -> SoundManager.instance.playSFX(SoundManager.SFX_SHIELD, 1.05f, 1.22f)
        GameSfx.CHIP -> SoundManager.instance.playSFX(SoundManager.SFX_CHIP, 1.12f, 0.82f)
        GameSfx.ICE -> SoundManager.instance.playSFX(SoundManager.SFX_ICE, 1.08f, 1.0f)
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
        goSelect()
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
      SoundManager.instance.playSFX(SoundManager.SFX_ARROW)
      select.moveLeft()
      return
    }
    if (rightArrow.contains(x, y)) {
      SoundManager.instance.playSFX(SoundManager.SFX_ARROW)
      select.moveRight()
      return
    }
    if (selectBtn.contains(x, y)) {
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
    if (leftArrow.contains(x, y) || rightArrow.contains(x, y)) {
      cpuLevel = if (cpuLevel == CpuLevel.HARD) CpuLevel.EASY else CpuLevel.HARD
      SoundManager.instance.playSFX(SoundManager.SFX_ARROW)
      return
    }
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
            goSelect()
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
      Screen.TITLE -> blitFill(canvas, opaque(UiArt.TITLE), 0f, 0f, sw, sh)
      Screen.DEMO -> drawDemo(canvas, sw, sh)
      Screen.SELECT -> drawSelect(canvas, sw, sh)
      Screen.VS -> drawVs(canvas, sw, sh)
      Screen.MATCH -> drawMatch(canvas, sw, sh)
    }
    canvas.restore()
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
    val tile = (rowWTiles - gapT * (Fighter.roster.size - 1)) / Fighter.roster.size
    val rowTop = sh - padV - tile
    val colH = (rowTop - colT - 10f * dp).coerceAtLeast(1f)
    val selectBmp = keyed(UiArt.PLAYER_SELECT)
    val nameH = 28f * dp
    val flavorH = 15f * dp
    val ctrlH = 56f * dp
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
    val arrow = 56f * dp
    val btnH = 56f * dp
    val btnW = btnH * (264f / 150f)
    val rowW = arrow * 2f + btnW + 8f * dp
    var x = colL + (colW - rowW) / 2f
    val locked = select.step == SelectStep.LOCKED
    val alpha = if (locked) 102 else 255
    leftArrow.set(x, y, x + arrow, y + arrow)
    blitFit(canvas, keyed(UiArt.ARROW_LEFT), x, y, arrow, arrow, alpha)
    x += arrow + 4f * dp
    selectBtn.set(x, y, x + btnW, y + btnH)
    blitFit(canvas, keyed(UiArt.BTN_SELECT), x, y, btnW, btnH, alpha)
    x += btnW + 4f * dp
    rightArrow.set(x, y, x + arrow, y + arrow)
    blitFitFlipped(canvas, keyed(UiArt.ARROW_LEFT), x, y, arrow, arrow, alpha)

    Fighter.roster.forEachIndexed { i, fighter ->
      val l = padH + i * (tile + gapT)
      faceTiles[i].set(l, rowTop, l + tile, rowTop + tile)
      drawFaceTile(canvas, fighter, i, faceTiles[i], dp)
    }
  }

  private fun drawFaceTile(canvas: Canvas, fighter: Fighter, index: Int, box: RectF, dp: Float) {
    fillPaint.color = INK
    canvas.drawRect(box, fillPaint)
    val inset = 8f * dp
    fighter.art().face?.let { path ->
      blitCrop(canvas, keyed(path), box.left + inset, box.top + inset, box.width() - inset * 2f, box.height() - inset * 2f)
    }
    val selected = index == select.cursorIndex
    val frame = if (selected) UiArt.FACE_FRAME_ON else UiArt.FACE_FRAME_OFF
    blitFit(canvas, keyed(frame, hole = true), box.left, box.top, box.width(), box.height())
    val badge =
      when {
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
    val btnH = 56f * dp
    val nameH = 40f * dp
    val nameBtnGap = 14f * dp
    val barH = barPadT + nameH + nameBtnGap + btnH + barPadB
    val bustH = sh - barH
    val totalW = 1f + 0.42f + 1f
    val leftW = sw * (1f / totalW)
    val vsW = sw * (0.42f / totalW)
    you.art().vsLeft?.let { path ->
      blitFit(canvas, keyed(path), 8f * dp, 12f * dp, leftW - 16f * dp, bustH - 24f * dp)
    }
    val vsBmp = keyed(UiArt.VS)
    val vsDrawH = min(140f * dp, bustH)
    val vsDrawW = min(vsW, vsDrawH * (vsBmp.width / vsBmp.height.toFloat()))
    blitFit(canvas, vsBmp, leftW + (vsW - vsDrawW) / 2f, (bustH - vsDrawH) / 2f, vsDrawW, vsDrawH)
    rival.art().vsRight?.let { path ->
      blitFit(canvas, keyed(path), leftW + vsW + 8f * dp, 12f * dp, leftW - 16f * dp, bustH - 24f * dp)
    }
    val nameY = bustH + barPadT
    val namePad = 10f * dp
    val nw = leftW - namePad * 2f
    you.art().nameVs?.let { path ->
      blitFit(canvas, keyed(path), namePad, nameY, nw, nameH)
    }
    rival.art().nameVs?.let { path ->
      blitFit(canvas, keyed(path), leftW + vsW + namePad, nameY, nw, nameH)
    }
    layoutVsHits(sw, sh)
    blitFit(canvas, keyed(UiArt.ARROW_LEFT), leftArrow.left, leftArrow.top, leftArrow.width(), leftArrow.height())
    val diffBtn = if (cpuLevel == CpuLevel.HARD) UiArt.BTN_HARD else UiArt.BTN_EASY
    blitFit(canvas, keyed(diffBtn), startBtn.left, startBtn.top, startBtn.width(), startBtn.height())
    blitFitFlipped(canvas, keyed(UiArt.ARROW_LEFT), rightArrow.left, rightArrow.top, rightArrow.width(), rightArrow.height())
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
        val banner = keyed(UiArt.FIGHT)
        val bannerW = courtW * 0.92f
        val bannerH = bannerW * (banner.height / banner.width.toFloat())
        blitFit(canvas, banner, courtL + (courtW - bannerW) / 2f, courtT + (courtH - bannerH) / 2f, bannerW, bannerH)
      }
      Phase.SET_WIN -> {
        if (world.setWinBannerVisible()) {
          val banner = keyed(UiArt.WIN)
          val bannerW = courtW * 0.72f
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
    drawWellText(canvas, World.TIME_INSET, world.timeDisplay(), CREAM, 0.72f, vw, vh)
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
    val btnH = 56f * dp
    val btnW = btnH * (264f / 150f)
    val btnY = vh - pad - btnH
    winner.art().ending?.let { path ->
      blitFillHeightEnd(canvas, keyed(path), railW, 0f, vw - railW, vh)
    }
    winner.art().wins?.let { path ->
      val bmp = keyed(path)
      val maxW = railW * 0.92f
      val maxH = (btnY - pad * 2f).coerceAtLeast(1f)
      val aspect = bmp.width / bmp.height.toFloat()
      var dw = maxW
      var dh = dw / aspect
      if (dh > maxH) {
        dh = maxH
        dw = dh * aspect
      }
      blitFit(canvas, bmp, (railW - dw) / 2f, pad + (maxH - dh) / 2f, dw, dh)
    }
    if (resultCardT < RESULT_LOCK_S) return
    blitFit(canvas, keyed(UiArt.BTN_CONTINUE), (railW - btnW) / 2f, btnY, btnW, btnH)
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
    val bmp = frames[pose] ?: frames[PaddlePose.IDLE]
    layoutFighterDest(dst, paddleX, paddleY, leftCourt, courtL, courtT, courtW, courtH)
    if (bmp != null) {
      blitFit(canvas, bmp, dst.left, dst.top, dst.width(), dst.height())
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
    textPaint.textSize = min(h * 0.68f, w / (text.length.coerceAtLeast(1) * 0.56f))
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
  ) {
    val l = inset.leftF * vw
    val t = inset.topF * vh
    val wellW = inset.widthF * vw
    val wellH = inset.heightF * vh
    textPaint.color = color
    textPaint.textSize = min(wellH * heightFrac, wellW / (text.length.coerceAtLeast(1) * 0.62f))
    val fm = textPaint.fontMetrics
    canvas.drawText(text, l + wellW / 2f, t + wellH / 2f - (fm.ascent + fm.descent) / 2f, textPaint)
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
    val tile = (rowWTiles - gapT * (Fighter.roster.size - 1)) / Fighter.roster.size
    val rowTop = sh - padV - tile
    val colH = (rowTop - colT - 10f * dp).coerceAtLeast(1f)
    val selectBmp = keyed(UiArt.PLAYER_SELECT)
    val nameH = 28f * dp
    val flavorH = 15f * dp
    val ctrlH = 56f * dp
    val minGap = 10f * dp
    val slots = 5f
    val bannerCap = (colH - nameH - flavorH - ctrlH - minGap * slots).coerceAtLeast(24f * dp)
    val bannerH = min(colW * (selectBmp.height / selectBmp.width.toFloat()), bannerCap)
    val gap = ((colH - bannerH - nameH - flavorH - ctrlH) / slots).coerceAtLeast(0f)
    val y = colT + gap + bannerH + gap + nameH + gap + flavorH + gap + 6f * dp
    val arrow = 56f * dp
    val btnH = 56f * dp
    val btnW = btnH * (264f / 150f)
    val rowW = arrow * 2f + btnW + 8f * dp
    var x = colL + (colW - rowW) / 2f
    leftArrow.set(x, y, x + arrow, y + arrow)
    x += arrow + 4f * dp
    selectBtn.set(x, y, x + btnW, y + btnH)
    x += btnW + 4f * dp
    rightArrow.set(x, y, x + arrow, y + arrow)
    Fighter.roster.forEachIndexed { i, _ ->
      val l = padH + i * (tile + gapT)
      faceTiles[i].set(l, rowTop, l + tile, rowTop + tile)
    }
  }

  private fun layoutVsHits(sw: Float, sh: Float) {
    val dp = sh / 360f
    val barPadB = 16f * dp
    val barPadT = 8f * dp
    val btnH = 56f * dp
    val btnW = btnH * (264f / 150f)
    val nameH = 40f * dp
    val nameBtnGap = 14f * dp
    val barH = barPadT + nameH + nameBtnGap + btnH + barPadB
    val bustH = sh - barH
    val arrow = 56f * dp
    val rowW = arrow * 2f + btnW + 8f * dp
    val y = bustH + barPadT + nameH + nameBtnGap
    var x = (sw - rowW) / 2f
    leftArrow.set(x, y, x + arrow, y + arrow)
    x += arrow + 4f * dp
    startBtn.set(x, y, x + btnW, y + btnH)
    x += btnW + 4f * dp
    rightArrow.set(x, y, x + arrow, y + arrow)
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

  private fun blitFill(canvas: Canvas, bmp: Bitmap, l: Float, t: Float, w: Float, h: Float) {
    blit(canvas, bmp, l, t, w, h)
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
    const val DEMO_HOLD_S = 10f
    const val SELECT_IDLE_S = 45f
    const val RESULT_LOCK_S = 0.8f
    const val RESULT_IDLE_S = 10f
    const val DEMO_FLASH_S = 0.45f
    const val DEMO_RED = 0xFFE01414.toInt()
    const val CABINET = 0xFF0C1822.toInt()
    const val INK = 0xFF071018.toInt()
    const val CREAM = 0xFFF3EFE4.toInt()
    const val YOU = 0xFF5EE0C8.toInt()
    const val CPU = 0xFFFF6B6B.toInt()
    const val MUTE_DIM = 0x598FA3B0
    const val TAIL_FLICKER_DIST = 0.04f
    const val DOUBLE_TAP_MS = 280L
  }
}
