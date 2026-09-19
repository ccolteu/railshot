package com.cc.railshot

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Stick / D-pad / face buttons for a landscape Android handheld.
 *
 * Rail Shot and Retroid-class shells are both landscape: stick axes are used
 * as the device reports them. No portrait remap. Left stick (next to P1)
 * steers the rail; right stick is a fallback if it is deflected more.
 */
class CabinetPad {

  var axisX = 0f
  var axisY = 0f
  var hatX = 0f
  var hatY = 0f

  private var dpadX = 0f
  private var dpadY = 0f
  private var smoothX = 0f
  private var smoothY = 0f
  private var menuLatchX = 0
  private var menuLatchY = 0
  private var menuRepeatX = 0f
  private var menuRepeatY = 0f

  fun screenStickX(): Float = (axisX + hatX).coerceIn(-1f, 1f)

  fun screenStickY(): Float = (axisY + hatY).coerceIn(-1f, 1f)

  fun pollMenuX(dt: Float): Int = pollLatched(screenStickX(), dt, horizontal = true)

  fun pollMenuY(dt: Float): Int = pollLatched(screenStickY(), dt, horizontal = false)

  fun menuStickActive(): Boolean =
    kotlin.math.abs(screenStickX()) > MENU_GATE || kotlin.math.abs(screenStickY()) > MENU_GATE

  private fun pollLatched(value: Float, dt: Float, horizontal: Boolean): Int {
    val dir =
      when {
        value > MENU_GATE -> 1
        value < -MENU_GATE -> -1
        else -> 0
      }
    return if (horizontal) {
      stepLatch(dir, dt, { menuLatchX }, { menuLatchX = it }, { menuRepeatX }, { menuRepeatX = it })
    } else {
      stepLatch(dir, dt, { menuLatchY }, { menuLatchY = it }, { menuRepeatY }, { menuRepeatY = it })
    }
  }

  private fun stepLatch(
    dir: Int,
    dt: Float,
    getLatch: () -> Int,
    setLatch: (Int) -> Unit,
    getRepeat: () -> Float,
    setRepeat: (Float) -> Unit,
  ): Int {
    if (dir == 0) {
      setLatch(0)
      setRepeat(0f)
      return 0
    }
    if (dir != getLatch()) {
      setLatch(dir)
      setRepeat(MENU_FIRST)
      return dir
    }
    setRepeat(getRepeat() - dt)
    if (getRepeat() <= 0f) {
      setRepeat(MENU_AGAIN)
      return dir
    }
    return 0
  }

  fun sampleSteer(): Boolean {
    val rx = (axisX + hatX).coerceIn(-1f, 1f)
    val ry = (axisY + hatY).coerceIn(-1f, 1f)
    val mag = sqrt(rx * rx + ry * ry)
    var tx = 0f
    var ty = 0f
    if (mag > DEAD) {
      val t = ((mag - DEAD) / (1f - DEAD)).coerceIn(0f, 1f)
      val gain = t.toDouble().pow(FINE_POW).toFloat()
      val inv = gain / mag
      tx = rx * inv
      ty = ry * inv
    }
    smoothX += (tx - smoothX) * SMOOTH
    smoothY += (ty - smoothY) * SMOOTH
    if (kotlin.math.abs(smoothX) < REST) smoothX = 0f
    if (kotlin.math.abs(smoothY) < REST) smoothY = 0f
    return mag2(steerX(), steerY()) >= REST * REST
  }

  fun steerX(): Float = clamp1(smoothX + dpadX)

  fun steerY(): Float = clamp1(smoothY + dpadY)

  fun ingestMotion(event: MotionEvent): Boolean {
    if (
      !event.isFromSource(InputDevice.SOURCE_CLASS_JOYSTICK) &&
      !event.isFromSource(InputDevice.SOURCE_JOYSTICK) &&
      !event.isFromSource(InputDevice.SOURCE_GAMEPAD)
    ) {
      return false
    }
    val lx = event.getAxisValue(MotionEvent.AXIS_X).coerceIn(-1f, 1f)
    val ly = event.getAxisValue(MotionEvent.AXIS_Y).coerceIn(-1f, 1f)
    val right = readRightStick(event)
    if (mag2(right[0], right[1]) > mag2(lx, ly)) {
      axisX = right[0]
      axisY = right[1]
    } else {
      axisX = lx
      axisY = ly
    }
    hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X).coerceIn(-1f, 1f)
    hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y).coerceIn(-1f, 1f)
    return true
  }

  fun ingestKey(down: Boolean, keyCode: Int): Boolean {
    when (keyCode) {
      KeyEvent.KEYCODE_DPAD_LEFT -> {
        dpadX = if (down) -1f else 0f
        return true
      }
      KeyEvent.KEYCODE_DPAD_RIGHT -> {
        dpadX = if (down) 1f else 0f
        return true
      }
      KeyEvent.KEYCODE_DPAD_UP -> {
        dpadY = if (down) -1f else 0f
        return true
      }
      KeyEvent.KEYCODE_DPAD_DOWN -> {
        dpadY = if (down) 1f else 0f
        return true
      }
      else ->
        return isConfirmKey(keyCode) ||
          isIceKey(keyCode) ||
          isBackKey(keyCode) ||
          isStartKey(keyCode) ||
          isDifficultyKey(keyCode)
    }
  }

  companion object {
    const val DEAD = 0.18f
    const val PAD_RAIL_SPEED = 1.28f
    private const val REST = 0.02f
    private const val SMOOTH = 0.32f
    private const val FINE_POW = 1.65
    private const val MENU_GATE = 0.42f
    private const val MENU_FIRST = 0.28f
    private const val MENU_AGAIN = 0.11f

    fun isConfirmKey(code: Int): Boolean =
      when (code) {
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_SPACE,
        -> true
        else -> false
      }

    fun isIceKey(code: Int): Boolean =
      when (code) {
        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BUTTON_X,
        KeyEvent.KEYCODE_SHIFT_LEFT,
        -> true
        else -> false
      }

    fun isBackKey(code: Int): Boolean =
      code == KeyEvent.KEYCODE_BUTTON_B || code == KeyEvent.KEYCODE_ESCAPE

    fun isStartKey(code: Int): Boolean =
      code == KeyEvent.KEYCODE_BUTTON_START || code == KeyEvent.KEYCODE_MENU

    fun isDifficultyKey(code: Int): Boolean = code == KeyEvent.KEYCODE_BUTTON_Y

    fun isVolumeOrSystem(code: Int): Boolean =
      when (code) {
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_APP_SWITCH,
        -> true
        else -> false
      }

    private fun readRightStick(event: MotionEvent): FloatArray {
      val z = event.getAxisValue(MotionEvent.AXIS_Z).coerceIn(-1f, 1f)
      val rz = event.getAxisValue(MotionEvent.AXIS_RZ).coerceIn(-1f, 1f)
      val rx = event.getAxisValue(MotionEvent.AXIS_RX).coerceIn(-1f, 1f)
      val ry = event.getAxisValue(MotionEvent.AXIS_RY).coerceIn(-1f, 1f)
      return if (mag2(rx, ry) >= mag2(z, rz)) floatArrayOf(rx, ry) else floatArrayOf(z, rz)
    }

    private fun mag2(x: Float, y: Float): Float = x * x + y * y

    private fun clamp1(v: Float): Float = v.coerceIn(-1f, 1f)
  }
}
