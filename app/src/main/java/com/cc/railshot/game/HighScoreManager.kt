package com.cc.railshot.game

import android.content.Context

/** Two EEPROM tables (EASY / HARD) × top 10, WW2 Blitz shape. */
object HighScoreManager {

  const val SLOT_COUNT = 10
  const val DIFF_TABLES = 2

  private val topScores = IntArray(DIFF_TABLES * SLOT_COUNT)
  private val topNames = CharArray(DIFF_TABLES * SLOT_COUNT * 3) { 'A' }
  private val topFights = IntArray(DIFF_TABLES * SLOT_COUNT)
  private val nameWriteBuf = StringBuilder(3)
  private var hydrated = false

  private const val PREFS_NAME = "arcade_leaderboard"

  private val FALLBACK_NAMES =
    arrayOf("ASH", "KIT", "RIV", "HEX", "QUL", "MAR", "ACE", "AAA", "AAA", "AAA")

  init {
    var table = 0
    while (table < DIFF_TABLES) {
      seedTable(table)
      table++
    }
  }

  fun scoreAt(difficultyIndex: Int, index: Int): Int = topScores[slot(difficultyIndex, index)]

  fun fightsAt(difficultyIndex: Int, index: Int): Int = topFights[slot(difficultyIndex, index)]

  fun nameChar(difficultyIndex: Int, index: Int, charIndex: Int): Char =
    topNames[slot(difficultyIndex, index) * 3 + charIndex]

  fun loadHighScores(context: Context) {
    if (hydrated) return
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var table = 0
    while (table < DIFF_TABLES) {
      val dip = table + 1
      var i = 0
      while (i < SLOT_COUNT) {
        val si = slot(dip, i)
        topScores[si] = prefs.getInt(scoreKey(dip, i), fallbackScore(table, i))
        topFights[si] = prefs.getInt(fightKey(dip, i), fallbackFight(i))
        writeName(si, prefs.getString(nameKey(dip, i), FALLBACK_NAMES[i]))
        i++
      }
      table++
    }
    hydrated = true
    persist(context)
  }

  fun checkIfQualifies(newScore: Int, difficultyIndex: Int): Boolean {
    val score = newScore.coerceIn(0, 99_999_999)
    return score > topScores[slot(difficultyIndex, SLOT_COUNT - 1)]
  }

  fun checkAndInsertNewScore(
    context: Context,
    newScore: Int,
    char1: Char,
    char2: Char,
    char3: Char,
    fightsWon: Int,
    difficultyIndex: Int,
  ): Boolean {
    if (!insertInMemory(newScore, char1, char2, char3, fightsWon, difficultyIndex)) return false
    persist(context)
    return true
  }

  internal fun insertInMemory(
    newScore: Int,
    char1: Char,
    char2: Char,
    char3: Char,
    fightsWon: Int,
    difficultyIndex: Int,
  ): Boolean {
    val score = newScore.coerceIn(0, 99_999_999)
    val fights = fightsWon.coerceAtLeast(0)
    val last = slot(difficultyIndex, SLOT_COUNT - 1)
    if (score <= topScores[last]) return false
    var targetIndex = -1
    var scan = 0
    while (scan < SLOT_COUNT) {
      if (score > topScores[slot(difficultyIndex, scan)]) {
        targetIndex = scan
        break
      }
      scan++
    }
    if (targetIndex < 0) return false
    var i = SLOT_COUNT - 2
    while (i >= targetIndex) {
      copyRow(difficultyIndex, i, i + 1)
      i--
    }
    val dest = slot(difficultyIndex, targetIndex)
    topScores[dest] = score
    topFights[dest] = fights
    val nameBase = dest * 3
    topNames[nameBase] = char1
    topNames[nameBase + 1] = char2
    topNames[nameBase + 2] = char3
    return true
  }

  internal fun resetTablesForTest() {
    hydrated = false
    var table = 0
    while (table < DIFF_TABLES) {
      seedTable(table)
      table++
    }
  }

  private fun copyRow(difficultyIndex: Int, fromIndex: Int, toIndex: Int) {
    val src = slot(difficultyIndex, fromIndex)
    val dst = slot(difficultyIndex, toIndex)
    topScores[dst] = topScores[src]
    topFights[dst] = topFights[src]
    val sb = src * 3
    val db = dst * 3
    topNames[db] = topNames[sb]
    topNames[db + 1] = topNames[sb + 1]
    topNames[db + 2] = topNames[sb + 2]
  }

  private fun persist(context: Context) {
    val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
    var table = 0
    while (table < DIFF_TABLES) {
      val dip = table + 1
      var i = 0
      while (i < SLOT_COUNT) {
        val si = slot(dip, i)
        editor.putInt(scoreKey(dip, i), topScores[si])
        editor.putInt(fightKey(dip, i), topFights[si])
        nameWriteBuf.setLength(0)
        val base = si * 3
        nameWriteBuf.append(topNames[base])
        nameWriteBuf.append(topNames[base + 1])
        nameWriteBuf.append(topNames[base + 2])
        editor.putString(nameKey(dip, i), nameWriteBuf.toString())
        i++
      }
      table++
    }
    editor.apply()
  }

  private fun seedTable(table: Int) {
    var i = 0
    while (i < SLOT_COUNT) {
      val si = slot(table + 1, i)
      topScores[si] = fallbackScore(table, i)
      topFights[si] = fallbackFight(i)
      writeFallbackName(si, i)
      i++
    }
  }

  private fun fallbackScore(table: Int, i: Int): Int {
    val easy = intArrayOf(50000, 40000, 30000, 25000, 20000, 15000, 10000, 8000, 5000, 2000)
    val hard = intArrayOf(120000, 100000, 80000, 60000, 45000, 30000, 20000, 15000, 10000, 5000)
    val row = if (table == 0) easy else hard
    return row[i]
  }

  private fun fallbackFight(i: Int): Int =
    intArrayOf(5, 4, 4, 3, 3, 2, 2, 1, 1, 1)[i]

  private fun writeName(si: Int, stored: String?) {
    val base = si * 3
    if (stored != null && stored.length >= 3) {
      topNames[base] = stored[0]
      topNames[base + 1] = stored[1]
      topNames[base + 2] = stored[2]
    } else {
      topNames[base] = 'A'
      topNames[base + 1] = 'A'
      topNames[base + 2] = 'A'
    }
  }

  private fun writeFallbackName(si: Int, i: Int) {
    val base = si * 3
    val fb = FALLBACK_NAMES[i]
    topNames[base] = fb[0]
    topNames[base + 1] = fb[1]
    topNames[base + 2] = fb[2]
  }

  private fun slot(difficultyIndex: Int, index: Int): Int {
    var d = difficultyIndex - 1
    if (d < 0) d = 0
    if (d >= DIFF_TABLES) d = DIFF_TABLES - 1
    var i = index
    if (i < 0) i = 0
    if (i >= SLOT_COUNT) i = SLOT_COUNT - 1
    return d * SLOT_COUNT + i
  }

  private fun scoreKey(dip: Int, i: Int): String = "d${dip}_score_$i"

  private fun fightKey(dip: Int, i: Int): String = "d${dip}_fight_$i"

  private fun nameKey(dip: Int, i: Int): String = "d${dip}_name_$i"
}
