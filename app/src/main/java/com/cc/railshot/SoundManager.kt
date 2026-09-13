package com.cc.railshot

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Low-latency SFX via [SoundPool]. Gapless BGM via dual [MediaPlayer]
 * chaining, same pattern as the other arcade titles in this set.
 */
class SoundManager private constructor() : AudioManager.OnAudioFocusChangeListener {

  private val lock = Any()
  private val loadedIds = IntArray(SFX_COUNT)
  private val sfxReady = BooleanArray(SFX_COUNT)
  private var appContext: Context? = null
  private var audioManager: AudioManager? = null
  private var soundPool: SoundPool? = null
  private var activeBgmPlayer: MediaPlayer? = null
  private var nextBgmPlayer: MediaPlayer? = null
  private var bgmAttrs: AudioAttributes? = null
  private var focusRequest: AudioFocusRequest? = null
  private var currentBgmRes = 0
  private var bgmWasPlaying = false
  private var paused = false
  private var ducking = false
  private var calloutDuck = false
  private var initialized = false
  private var bgmCompleting = false
  private var pendingChainRes = 0
  private val bgmHandler = Handler(Looper.getMainLooper())
  private val chainNextLoopRunnable = Runnable { runChainNextLoop() }
  private val unduckCalloutRunnable = Runnable {
    synchronized(lock) {
      calloutDuck = false
      applyBgmVolumeLocked()
    }
  }

  private val onBgmComplete = MediaPlayer.OnCompletionListener { mp ->
    synchronized(lock) {
      onBgmCompletedLocked(mp)
    }
  }

  fun initialize(context: Context) {
    synchronized(lock) {
      if (initialized) return
      val app = context.applicationContext
      appContext = app
      audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
      val attrs =
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      soundPool = SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(attrs).build()
      val pool = soundPool ?: return
      pool.setOnLoadCompleteListener { _, soundId, status ->
        if (status != 0) return@setOnLoadCompleteListener
        synchronized(lock) {
          var i = 0
          while (i < SFX_COUNT) {
            if (loadedIds[i] == soundId) {
              sfxReady[i] = true
              break
            }
            i++
          }
        }
      }
      var i = 0
      while (i < SFX_COUNT) {
        sfxReady[i] = false
        loadedIds[i] = pool.load(app, SFX_RAW[i], 1)
        i++
      }
      val musicAttrs =
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .build()
      bgmAttrs = musicAttrs
      activeBgmPlayer = createBgmPlayerLocked(musicAttrs)
      nextBgmPlayer = createBgmPlayerLocked(musicAttrs)
      requestFocusLocked()
      applyBgmVolumeLocked()
      initialized = true
    }
  }

  fun playSFX(id: Int) {
    playSFX(id, 1f, 1f)
  }

  fun playSFX(id: Int, gain: Float, rate: Float) {
    synchronized(lock) {
      if (!initialized || paused) return
      if (id < 0 || id >= SFX_COUNT) return
      val pool = soundPool ?: return
      val sid = loadedIds[id]
      if (sid == 0 || !sfxReady[id]) return
      val callout = isCallout(id)
      val vol =
        if (callout) CALLOUT_VOLUME
        else if (ducking) DUCK_VOLUME * SFX_VOLUME
        else SFX_VOLUME
      val out = (vol * gain).coerceIn(0f, 1f)
      val playback = rate.coerceIn(0.5f, 2f)
      val priority = if (callout) 3 else if (id == SFX_CHIP) 2 else 1
      if (callout) {
        calloutDuck = true
        applyBgmVolumeLocked()
        bgmHandler.removeCallbacks(unduckCalloutRunnable)
        val holdMs = if (id == SFX_FIGHT) 550L else 1200L
        bgmHandler.postDelayed(unduckCalloutRunnable, holdMs)
      }
      pool.play(sid, out, out, priority, 0, playback)
    }
  }

  fun stopBGM() {
    synchronized(lock) {
      if (!initialized) return
      cancelChainNextLoopLocked()
      bgmWasPlaying = false
      currentBgmRes = 0
      activeBgmPlayer?.let { resetPlayerLocked(it) }
      nextBgmPlayer?.let { resetPlayerLocked(it) }
    }
  }

  fun switchBGM(resId: Int): Boolean {
    synchronized(lock) {
      if (!initialized || resId == 0) return false
      val active = activeBgmPlayer ?: return false
      val next = nextBgmPlayer ?: return false
      if (resId == currentBgmRes && active.isPlaying) return true
      bgmWasPlaying = false
      cancelChainNextLoopLocked()
      return try {
        resetPlayerLocked(active)
        resetPlayerLocked(next)
        if (!loadBgmSourceLocked(active, resId)) {
          currentBgmRes = 0
          return false
        }
        active.prepare()
        currentBgmRes = resId
        applyBgmVolumeLocked()
        if (!prepareNextPlayerLocked(resId)) {
          try {
            active.setNextMediaPlayer(null)
            active.isLooping = true
          } catch (_: Exception) {
          }
        }
        if (!paused) {
          active.start()
          bgmWasPlaying = true
        }
        true
      } catch (_: Exception) {
        currentBgmRes = 0
        false
      }
    }
  }

  fun pauseAll() {
    synchronized(lock) {
      paused = true
      soundPool?.autoPause()
      pauseActiveBgmLocked(remember = true)
    }
  }

  fun resumeAll() {
    synchronized(lock) {
      paused = false
      requestFocusLocked()
      soundPool?.autoResume()
      resumeBgmLocked()
    }
  }

  fun release() {
    synchronized(lock) {
      abandonFocusLocked()
      soundPool?.release()
      soundPool = null
      var i = 0
      while (i < SFX_COUNT) {
        loadedIds[i] = 0
        sfxReady[i] = false
        i++
      }
      releasePlayerLocked(activeBgmPlayer)
      releasePlayerLocked(nextBgmPlayer)
      activeBgmPlayer = null
      nextBgmPlayer = null
      bgmAttrs = null
      currentBgmRes = 0
      bgmWasPlaying = false
      cancelChainNextLoopLocked()
      bgmHandler.removeCallbacks(unduckCalloutRunnable)
      paused = false
      ducking = false
      calloutDuck = false
      initialized = false
      appContext = null
      audioManager = null
      focusRequest = null
    }
  }

  override fun onAudioFocusChange(focusChange: Int) {
    synchronized(lock) {
      when (focusChange) {
        AudioManager.AUDIOFOCUS_GAIN -> {
          ducking = false
          applyBgmVolumeLocked()
          if (!paused) resumeBgmLocked()
        }
        AudioManager.AUDIOFOCUS_LOSS,
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
        -> {
          ducking = false
          pauseActiveBgmLocked(remember = true)
        }
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
          ducking = true
          applyBgmVolumeLocked()
        }
      }
    }
  }

  private fun createBgmPlayerLocked(musicAttrs: AudioAttributes): MediaPlayer {
    val player = MediaPlayer()
    player.setAudioAttributes(musicAttrs)
    player.isLooping = false
    applyVolumeToPlayerLocked(player)
    player.setOnCompletionListener(onBgmComplete)
    player.setOnErrorListener { _, _, _ ->
      synchronized(lock) { currentBgmRes = 0 }
      true
    }
    return player
  }

  private fun onBgmCompletedLocked(mp: MediaPlayer) {
    if (!initialized) return
    if (bgmCompleting) return
    if (mp !== activeBgmPlayer) return
    val resId = currentBgmRes
    if (resId == 0) return
    bgmCompleting = true
    pendingChainRes = resId
    val finished = activeBgmPlayer
    val incoming = nextBgmPlayer
    activeBgmPlayer = incoming
    nextBgmPlayer = finished
    applyBgmVolumeLocked()
    if (!paused) bgmWasPlaying = true
    bgmHandler.removeCallbacks(chainNextLoopRunnable)
    bgmHandler.postDelayed(chainNextLoopRunnable, CHAIN_PREPARE_DELAY_MS)
  }

  private fun runChainNextLoop() {
    synchronized(lock) {
      try {
        if (!initialized || !bgmCompleting) return
        val resId = pendingChainRes
        if (resId == 0 || resId != currentBgmRes) return
        prepareNextPlayerLocked(resId)
      } catch (_: Exception) {
      } finally {
        bgmCompleting = false
        pendingChainRes = 0
      }
    }
  }

  private fun cancelChainNextLoopLocked() {
    bgmHandler.removeCallbacks(chainNextLoopRunnable)
    bgmCompleting = false
    pendingChainRes = 0
  }

  private fun prepareNextPlayerLocked(resId: Int): Boolean {
    val active = activeBgmPlayer ?: return false
    val next = nextBgmPlayer ?: return false
    if (resId == 0) return false
    return try {
      resetPlayerLocked(next)
      if (!loadBgmSourceLocked(next, resId)) return false
      next.prepare()
      applyVolumeToPlayerLocked(next)
      try {
        active.setNextMediaPlayer(next)
      } catch (_: IllegalArgumentException) {
        active.setNextMediaPlayer(null)
        active.isLooping = true
      } catch (_: IllegalStateException) {
        active.setNextMediaPlayer(null)
        active.isLooping = true
      }
      true
    } catch (_: Exception) {
      false
    }
  }

  private fun loadBgmSourceLocked(player: MediaPlayer, resId: Int): Boolean {
    val ctx = appContext ?: return false
    val fd = ctx.resources.openRawResourceFd(resId) ?: return false
    try {
      player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
    } finally {
      fd.close()
    }
    return true
  }

  private fun resetPlayerLocked(player: MediaPlayer) {
    try {
      player.setNextMediaPlayer(null)
    } catch (_: Exception) {
    }
    try {
      if (player.isPlaying) player.stop()
    } catch (_: Exception) {
    }
    player.reset()
    val attrs = bgmAttrs
    if (attrs != null) player.setAudioAttributes(attrs)
    player.isLooping = false
    player.setOnCompletionListener(onBgmComplete)
    applyVolumeToPlayerLocked(player)
  }

  private fun pauseActiveBgmLocked(remember: Boolean) {
    val active = activeBgmPlayer ?: return
    try {
      if (active.isPlaying) {
        if (remember) bgmWasPlaying = true
        active.pause()
      }
    } catch (_: Exception) {
    }
  }

  private fun resumeBgmLocked() {
    val active = activeBgmPlayer ?: return
    if (paused || currentBgmRes == 0) return
    if (bgmWasPlaying || !active.isPlaying) {
      try {
        applyBgmVolumeLocked()
        active.start()
        bgmWasPlaying = true
      } catch (_: Exception) {
      }
    }
  }

  private fun applyBgmVolumeLocked() {
    applyVolumeToPlayerLocked(activeBgmPlayer)
    applyVolumeToPlayerLocked(nextBgmPlayer)
  }

  private fun applyVolumeToPlayerLocked(player: MediaPlayer?) {
    if (player == null) return
    val v =
      when {
        calloutDuck -> BGM_VOLUME * CALLOUT_BGM_DUCK
        ducking -> DUCK_VOLUME * BGM_VOLUME
        else -> BGM_VOLUME
      }
    try {
      player.setVolume(v, v)
    } catch (_: Exception) {
    }
  }

  private fun releasePlayerLocked(player: MediaPlayer?) {
    if (player == null) return
    try {
      player.setOnCompletionListener(null)
      player.setOnErrorListener(null)
      player.setNextMediaPlayer(null)
    } catch (_: Exception) {
    }
    try {
      player.reset()
    } catch (_: Exception) {
    }
    try {
      player.release()
    } catch (_: Exception) {
    }
  }

  private fun requestFocusLocked() {
    val am = audioManager ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val req =
        focusRequest
          ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            )
            .setOnAudioFocusChangeListener(this)
            .build()
            .also { focusRequest = it }
      am.requestAudioFocus(req)
    } else {
      @Suppress("DEPRECATION")
      am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }
  }

  private fun abandonFocusLocked() {
    val am = audioManager ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val req = focusRequest
      if (req != null) am.abandonAudioFocusRequest(req)
    } else {
      @Suppress("DEPRECATION")
      am.abandonAudioFocus(this)
    }
  }

  companion object {
    const val SFX_ARROW = 0
    const val SFX_SELECT = 1
    const val SFX_ROUND1 = 2
    const val SFX_ROUND2 = 3
    const val SFX_ROUND3 = 4
    const val SFX_FIGHT = 5
    const val SFX_SHIELD = 6
    const val SFX_CHIP = 7

    val BGM_MATCH: Int
      get() = R.raw.bgm_match

    private const val SFX_COUNT = 8
    private const val MAX_STREAMS = 8
    private const val DUCK_VOLUME = 0.45f
    private const val BGM_VOLUME = 0.58f
    private const val SFX_VOLUME = 0.95f
    private const val CALLOUT_VOLUME = 1.0f
    private const val CALLOUT_BGM_DUCK = 0.18f
    private const val CHAIN_PREPARE_DELAY_MS = 16L

    private fun isCallout(id: Int): Boolean =
      id == SFX_FIGHT || id == SFX_ROUND1 || id == SFX_ROUND2 || id == SFX_ROUND3

    private val SFX_RAW =
      intArrayOf(
        R.raw.sfx_arrow,
        R.raw.sfx_select,
        R.raw.sfx_round1,
        R.raw.sfx_round2,
        R.raw.sfx_round3,
        R.raw.sfx_fight,
        R.raw.sfx_shield,
        R.raw.sfx_chip,
      )

    val instance: SoundManager by lazy { SoundManager() }

    fun roundCall(roundNumber: Int): Int =
      when (roundNumber) {
        2 -> SFX_ROUND2
        3 -> SFX_ROUND3
        else -> SFX_ROUND1
      }
  }
}
