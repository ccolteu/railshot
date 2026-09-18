package com.cc.railshot.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldTest {
  @Test
  fun launchLeavesServeAndMovesTheBallRight() {
    val world = World()
    assertEquals(Phase.ROUND, world.phase)
    world.skipToServe()
    assertEquals(Phase.SERVE, world.phase)
    world.launch()
    assertEquals(Phase.PLAYING, world.phase)
    val x = world.ballX
    world.step(0.05f)
    assertTrue(world.ballX > x)
  }

  @Test
  fun arcadeStartSkillCarriesIntoTheMatch() {
    val world = World(startSkill = 700)
    assertEquals(700, world.youSkill)
  }

  @Test
  fun hittingACpuChipScoresForYou() {
    val world = World()
    val target = world.chips.first { it.side == Side.CPU && it.slot == 2 && it.alive }
    world.placeBall(target.x - World.BALL_R_X - 0.001f, target.y + target.h / 2f, 0.9f, 0f)
    var guard = 0
    while (world.youScore == 0 && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertEquals(1, world.youScore)
    assertEquals(World.CHIP_COUNT - 1, world.cpuChipsLeft())
    assertTrue(world.drainSfx().contains(GameSfx.CHIP))
    assertTrue(world.impactFrozen())
    assertEquals(100, world.youSkill)
    val frozenX = world.ballX
    world.step(1f / 60f)
    assertEquals(frozenX, world.ballX, 0.0001f)
  }

  @Test
  fun missingTheYouPaddleKnocksOffAYouChip() {
    val world = World()
    val target = world.chips.first { it.side == Side.YOU && it.slot == 0 && it.alive }
    world.moveYouPaddle(0.85f)
    world.placeBall(0.22f, target.y + target.h / 2f, -0.9f, 0f)
    var guard = 0
    while (world.cpuScore == 0 && world.phase == Phase.PLAYING && guard++ < 80) {
      world.step(1f / 60f)
    }
    assertEquals(1, world.cpuScore)
    assertEquals(World.CHIP_COUNT - 1, world.youChipsLeft())
  }

  @Test
  fun clearingCpuChipsWinsASetNotTheMatch() {
    val world = World()
    val keep = world.chips.first { it.side == Side.CPU && it.slot == 2 && it.alive }
    world.killAllBut(Side.CPU, keep)
    world.placeBall(keep.x - World.BALL_R_X - 0.001f, keep.y + keep.h / 2f, 0.9f, 0f)
    var guard = 0
    while (world.youSets == 0 && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertEquals(1, world.youSets)
    assertEquals(0, world.cpuSets)
    assertEquals(Phase.SET_WIN, world.phase)
    assertEquals(Side.YOU, world.setWinner)
    assertEquals(0, world.cpuChipsLeft())
    world.skipSetWin()
    assertEquals(Phase.ROUND, world.phase)
    assertEquals(2, world.roundNumber())
  }

  @Test
  fun twoSetsWinsTheMatch() {
    val world = World()
    repeat(2) {
      val keep = world.chips.first { it.side == Side.CPU && it.slot == 2 && it.alive }
      world.killAllBut(Side.CPU, keep)
      world.placeBall(keep.x - World.BALL_R_X - 0.001f, keep.y + keep.h / 2f, 0.9f, 0f)
      var guard = 0
      while (world.phase == Phase.PLAYING && guard++ < 40) {
        world.step(1f / 60f)
      }
      world.skipSetWin()
    }
    assertEquals(2, world.youSets)
    assertEquals(Phase.YOU_WIN, world.phase)
  }

  @Test
  fun timeExpiryAwardsTheSetToTheSideWithMoreChips() {
    val world = World()
    world.skipToServe()
    world.launch()
    val keep = world.chips.first { it.side == Side.CPU && it.slot == 0 }
    world.killAllBut(Side.CPU, keep)
    world.setTimeLeft(0.01f)
    world.step(1f / 30f)
    assertEquals(1, world.youSets)
    assertEquals(0, world.cpuSets)
    assertEquals(Phase.SET_WIN, world.phase)
    assertEquals(Side.YOU, world.setWinner)
    world.skipSetWin()
    assertEquals(Phase.ROUND, world.phase)
  }

  @Test
  fun timeExpiryAwardsTheSetToCpuWhenCpuHasMoreChips() {
    val world = World()
    world.skipToServe()
    world.launch()
    val keep = world.chips.first { it.side == Side.YOU && it.slot == 0 }
    world.killAllBut(Side.YOU, keep)
    world.setTimeLeft(0.01f)
    world.step(1f / 30f)
    assertEquals(0, world.youSets)
    assertEquals(1, world.cpuSets)
    assertEquals(Phase.SET_WIN, world.phase)
    assertEquals(Side.CPU, world.setWinner)
  }

  @Test
  fun setWinHoldsTheCourtThenShowsWinThenNextRound() {
    val world = World()
    val keep = world.chips.first { it.side == Side.CPU && it.slot == 2 && it.alive }
    world.killAllBut(Side.CPU, keep)
    world.placeBall(keep.x - World.BALL_R_X - 0.001f, keep.y + keep.h / 2f, 0.9f, 0f)
    var guard = 0
    while (world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertEquals(Phase.SET_WIN, world.phase)
    assertEquals(Side.YOU, world.setWinner)
    assertEquals(false, world.setWinBannerVisible())
    repeat((World.SET_WIN_FREEZE / 0.05f).toInt() + 2) { world.step(0.05f) }
    assertEquals(true, world.setWinBannerVisible())
    repeat((World.SET_WIN_BANNER / 0.05f).toInt() + 2) { world.step(0.05f) }
    assertEquals(Phase.ROUND, world.phase)
    assertEquals(World.CHIP_COUNT, world.cpuChipsLeft())
  }

  @Test
  fun roundCallBecomesServeAfterTheHold() {
    val world = World()
    assertEquals(Phase.ROUND, world.phase)
    assertEquals(1, world.roundNumber())
    repeat(40) { world.step(World.ROUND_HOLD) }
    assertEquals(Phase.SERVE, world.phase)
  }

  @Test
  fun fightBannerClearsWhileStillWaitingToServe() {
    val world = World()
    world.skipToServe()
    assertTrue(world.fightBannerVisible())
    repeat((World.FIGHT_HOLD / 0.05f).toInt() + 2) { world.step(0.05f) }
    assertEquals(Phase.SERVE, world.phase)
    assertFalse(world.fightBannerVisible())
  }

  @Test
  fun deadChipsLeaveHolesInPlace() {
    val world = World()
    val keep = world.chips.first { it.side == Side.YOU && it.slot == 4 }
    val y = keep.y
    world.killAllBut(Side.YOU, keep)
    assertEquals(y, keep.y, 0.0001f)
    assertEquals(4, keep.slot)
    assertEquals(1, world.youChipsLeft())
  }

  @Test
  fun cpuPaddleTracksAnIncomingBall() {
    val world = World()
    world.placeBall(0.60f, 0.20f, 0.7f, 0f)
    val start = world.cpuPaddleY
    repeat(40) { world.step(1f / 60f) }
    assertTrue(world.cpuPaddleY < start)
  }

  @Test
  fun cpuDoesNotChaseUntilTheBallEntersItsHalf() {
    val world = World()
    world.placeBall(0.30f, 0.15f, 0.4f, 0f)
    val start = world.cpuPaddleY
    world.step(1f / 60f)
    assertEquals(start, world.cpuPaddleY, 0.0001f)
  }

  @Test
  fun paddleStartsIdleAndWalksWhenMoved() {
    val world = World()
    assertEquals(PaddlePose.IDLE, world.youPose())
    world.moveYouPaddle(0.7f)
    assertEquals(PaddlePose.WALK, world.youPose())
  }

  @Test
  fun paddleShowsHitAfterContact() {
    val world = World()
    world.moveYouPaddle(0.5f)
    world.placeBall(
      world.youFrontX() + World.BALL_R_X + 0.001f,
      world.youPaddleY,
      -0.8f,
      0f,
    )
    var guard = 0
    while (world.youPose() != PaddlePose.HIT && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertEquals(PaddlePose.HIT, world.youPose())
    assertTrue(world.drainSfx().contains(GameSfx.SHIELD))
    assertTrue(world.impactFrozen())
  }

  @Test
  fun fireballHitsTheCourtRailWithAWallCue() {
    val world = World()
    world.placeBall(0.50f, World.BALL_R + 0.004f, 0.12f, -0.9f)
    var guard = 0
    while (world.ballVy() < 0f && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.ballVy() > 0f)
    assertTrue(world.drainSfx().contains(GameSfx.WALL))
  }

  @Test
  fun iceCometHitsTheCourtRailWithAWallCue() {
    val world = World()
    world.placeBall(0.40f, 0.50f, -0.2f, 0f)
    world.placeStar(0.50f, World.STAR_R + 0.004f, 0.12f, -0.9f)
    var guard = 0
    while (world.starVy() < 0f && world.starLive() && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.starLive())
    assertTrue(world.starVy() > 0f)
    assertTrue(world.drainSfx().contains(GameSfx.WALL))
  }

  @Test
  fun centerHitStaysNearBaseSpeed() {
    val world = World()
    val speed = youReturnSpeed(world, paddleY = 0.5f, ballY = 0.5f)
    assertEquals(World.BALL_SPEED * World.SLICE_CENTER * world.youKit.shieldPop, speed, 0.04f)
  }

  @Test
  fun edgeHitIsFasterThanACenterHit() {
    val center = youReturnSpeed(World(), paddleY = 0.5f, ballY = 0.5f)
    val edge = youReturnSpeed(World(), paddleY = 0.5f, ballY = 0.58f)
    assertTrue(edge > center * 1.12f)
    assertTrue(edge <= World.BALL_SPEED * World.SLICE_CAP + 0.001f)
  }

  @Test
  fun swipingThroughContactAddsSpeed() {
    val still = youReturnSpeed(World(), paddleY = 0.5f, ballY = 0.5f)
    val world = World()
    world.moveYouPaddle(0.5f)
    world.step(1f / 60f)
    world.moveYouPaddle(0.64f)
    world.placeBall(
      world.youFrontX() + World.BALL_R_X + 0.001f,
      world.youPaddleY,
      -0.8f,
      0f,
    )
    waitForYouHit(world)
    assertTrue(world.ballSpeed() > still * 1.08f)
    assertTrue(world.ballSpeed() <= World.BALL_SPEED * World.SLICE_CAP + 0.001f)
  }

  private fun youReturnSpeed(world: World, paddleY: Float, ballY: Float): Float {
    world.moveYouPaddle(paddleY)
    world.step(1f / 60f)
    world.placeBall(world.youFrontX() + World.BALL_R_X + 0.001f, ballY, -0.8f, 0f)
    waitForYouHit(world)
    return world.ballSpeed()
  }

  private fun waitForYouHit(world: World) {
    var guard = 0
    while (world.youPose() != PaddlePose.HIT && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertEquals(PaddlePose.HIT, world.youPose())
  }

  @Test
  fun tailBandsSplitTheLiveSpeedRangeInThirds() {
    val min = World.TAIL_SPEED_MIN
    val max = World.TAIL_SPEED_MAX
    val span = max - min
    assertEquals(BallTailBand.SHORT, World.tailBandForSpeed(min))
    assertEquals(BallTailBand.SHORT, World.tailBandForSpeed(min + span * 0.32f))
    assertEquals(BallTailBand.MEDIUM, World.tailBandForSpeed(min + span * 0.50f))
    assertEquals(BallTailBand.LONG, World.tailBandForSpeed(min + span * 0.80f))
    assertEquals(BallTailBand.LONG, World.tailBandForSpeed(max))
  }

  @Test
  fun serveParksTheBallInFrontOfTheYouSprite() {
    val world = World()
    assertEquals(World.YOU_FRONT_X + World.BALL_R_X + 0.004f, world.ballX, 0.0001f)
  }

  @Test
  fun youFrontIsInsideTheDrawnSpriteBox() {
    val destRight = World.YOU_PADDLE_X + World.SPRITE_SPAN
    assertTrue(World.YOU_FRONT_X < destRight)
    assertTrue(World.YOU_FRONT_X > World.YOU_PADDLE_X)
  }

  @Test
  fun playfieldHoleIsInsideTheCabinet() {
    assertTrue(World.FRAME_LEFT > 0f)
    assertTrue(World.FRAME_TOP > 0f)
    assertTrue(World.FRAME_RIGHT > 0f)
    assertTrue(World.FRAME_BOTTOM > 0f)
    assertEquals(4f / 3f, World.COURT_ASPECT, 0.0001f)
    assertEquals(
      (World.HOLE_RIGHT_PX - World.HOLE_LEFT_PX).toFloat() /
        (World.HOLE_BOTTOM_PX - World.HOLE_TOP_PX).toFloat(),
      World.COURT_ASPECT,
      0.0001f,
    )
  }

  @Test
  fun maruAndHexAreWallsKiteIsASlugger() {
    assertEquals(CpuStyle.WALL, CpuStyle.forFighter(Fighter.MARU))
    assertEquals(CpuStyle.WALL, CpuStyle.forFighter(Fighter.HEX))
    assertEquals(CpuStyle.SLUGGER, CpuStyle.forFighter(Fighter.KITE))
  }

  @Test
  fun easyCpuLeavesAHighBallUncovered() {
    val world = World(CpuStyle.SLUGGER, CpuLevel.EASY)
    world.placeBall(0.70f, 0.08f, 0.55f, 0f)
    val start = world.cpuPaddleY
    repeat(18) { world.step(1f / 60f) }
    assertTrue(kotlin.math.abs(world.cpuPaddleY - 0.5f) < 0.12f)
    assertTrue(kotlin.math.abs(world.cpuPaddleY - 0.08f) > kotlin.math.abs(start - 0.08f) - 0.02f)
  }

  @Test
  fun hardWallMovesTowardAThreatenedGate() {
    val world =
      World(you = Fighter.RIVET, rival = Fighter.ASH, cpuLevel = CpuLevel.HARD, cpuStyle = CpuStyle.WALL)
    val gate = world.chips.first { it.side == Side.CPU && it.slot == 0 && it.alive }
    val gateY = gate.y + gate.h / 2f
    world.placeBall(0.40f, gateY, 0.55f, 0f)
    repeat(24) { world.step(1f / 60f) }
    assertTrue(world.cpuPaddleY < 0.42f)
    assertTrue(kotlin.math.abs(world.cpuPaddleY - gateY) < kotlin.math.abs(0.5f - gateY))
  }

  @Test
  fun hardCpuCutsADownwardAngleBeforeTheBallArrives() {
    val world =
      World(
        you = Fighter.RIVET,
        rival = Fighter.ASH,
        cpuLevel = CpuLevel.HARD,
        cpuStyle = CpuStyle.SLUGGER,
      )
    world.placeBall(0.38f, 0.50f, 0.55f, 0.42f)
    repeat(12) { world.step(1f / 60f) }
    assertTrue(world.cpuPaddleY > 0.52f)
  }

  @Test
  fun rivetReturnsHotterThanAsh() {
    val rivet = youReturnSpeed(World(you = Fighter.RIVET), paddleY = 0.5f, ballY = 0.5f)
    val ash = youReturnSpeed(World(you = Fighter.ASH), paddleY = 0.5f, ballY = 0.5f)
    assertTrue(rivet > ash * 1.06f)
  }

  @Test
  fun kiteReachesABallHexMisses() {
    val y = 0.370f
    val kite = World(you = Fighter.KITE)
    kite.moveYouPaddle(0.5f)
    kite.placeBall(kite.youFrontX() + World.BALL_R_X + 0.001f, y, -0.8f, 0f)
    waitForYouHit(kite)
    val hex = World(you = Fighter.HEX)
    hex.moveYouPaddle(0.5f)
    hex.placeBall(hex.youFrontX() + World.BALL_R_X + 0.001f, y, -0.8f, 0f)
    var guard = 0
    while (hex.youPose() != PaddlePose.HIT && hex.phase == Phase.PLAYING && guard++ < 40) {
      hex.step(1f / 60f)
    }
    assertTrue(hex.youPose() != PaddlePose.HIT)
  }

  @Test
  fun kiteCpuClosesFasterThanMaru() {
    fun closed(rival: Fighter): Float {
      val world = World(you = Fighter.ASH, rival = rival, cpuLevel = CpuLevel.HARD)
      world.placeBall(0.70f, 0.22f, 0.7f, 0f)
      repeat(18) { world.step(1f / 60f) }
      return kotlin.math.abs(world.cpuPaddleY - 0.22f)
    }
    assertTrue(closed(Fighter.KITE) < closed(Fighter.MARU))
  }

  @Test
  fun attractSteersTheLeftPaddle() {
    val world =
      World(you = Fighter.ASH, rival = Fighter.MARU, cpuLevel = CpuLevel.HARD, attract = true)
    world.placeBall(0.30f, 0.22f, -0.7f, 0f)
    val start = world.youPaddleY
    repeat(18) { world.step(1f / 60f) }
    assertTrue(world.youPaddleY < start)
    assertTrue(kotlin.math.abs(world.youPaddleY - 0.22f) < kotlin.math.abs(start - 0.22f))
  }

  @Test
  fun quillSteepRimIsSteeperThanRivet() {
    val rivet = World(you = Fighter.RIVET)
    val quill = World(you = Fighter.QUILL)
    val ballY = 0.58f
    youReturnSpeed(rivet, 0.5f, ballY)
    val rivetVy = kotlin.math.abs(rivet.ballVy())
    youReturnSpeed(quill, 0.5f, ballY)
    val quillVy = kotlin.math.abs(quill.ballVy())
    assertTrue(quillVy > rivetVy)
    assertEquals(BallTailBand.LONG, quill.ballTailBand())
  }

  @Test
  fun rivetSecondHitIsHotter() {
    val world = World(you = Fighter.RIVET)
    val first = youReturnSpeed(world, paddleY = 0.5f, ballY = 0.5f)
    thaw(world)
    youBounceAgain(world, paddleY = 0.5f, ballY = 0.5f)
    assertTrue(world.ballSpeed() > first * 1.12f)
    assertTrue(world.ballTailBand() != BallTailBand.SHORT)
  }

  @Test
  fun maruCenterDumpsSpeed() {
    val center = youReturnSpeed(World(you = Fighter.MARU), paddleY = 0.5f, ballY = 0.5f)
    val rim = youReturnSpeed(World(you = Fighter.MARU), paddleY = 0.5f, ballY = 0.62f)
    assertTrue(center < rim * 0.92f)
    val dumped = World(you = Fighter.MARU)
    youReturnSpeed(dumped, paddleY = 0.5f, ballY = 0.5f)
    assertEquals(BallTailBand.SHORT, dumped.ballTailBand())
  }

  @Test
  fun kiteAfterburnAfterALongDash() {
    val still = youReturnSpeed(World(you = Fighter.KITE), paddleY = 0.5f, ballY = 0.5f)
    val world = World(you = Fighter.KITE)
    world.moveYouPaddle(0.5f)
    world.step(1f / 60f)
    world.moveYouPaddle(0.72f)
    world.placeBall(world.youFrontX() + World.BALL_R_X + 0.001f, world.youPaddleY, -0.8f, 0f)
    waitForYouBounce(world)
    assertTrue(world.ballSpeed() > still * 1.12f)
    assertEquals(BallTailBand.LONG, world.ballTailBand())
  }

  @Test
  fun calledSpecialFiresOnceTowardTheTappedGate() {
    val world = World(you = Fighter.RIVET)
    world.placeBall(0.5f, 0.5f, 0.4f, 0f)
    assertTrue(world.callYouSpecial(0.12f))
    assertTrue(world.starLive())
    assertTrue(world.starVy() < -0.05f)
    assertEquals(Fighter.RIVET, world.starFighter())
    assertFalse(world.callYouSpecial(0.88f))
  }

  @Test
  fun shieldSmashDestroysTheCalledOrb() {
    val world = World(you = Fighter.RIVET)
    world.placeBall(0.40f, 0.50f, -0.4f, 0f)
    assertTrue(world.callYouSpecial(0.50f))
    val chips = world.cpuChipsLeft()
    world.placeStar(world.cpuFrontX() - World.STAR_R_X - 0.001f, world.cpuPaddleY, 0.6f, 0f)
    var guard = 0
    while (world.starLive() && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertFalse(world.starLive())
    assertEquals(chips, world.cpuChipsLeft())
    assertTrue(world.drainSfx().contains(GameSfx.ICE))
    assertTrue(world.iceBurstLive())
  }

  @Test
  fun shieldSmashDoesNotLockTheSmasher() {
    val world = World(you = Fighter.RIVET, cpuLevel = CpuLevel.EASY)
    world.placeBall(0.50f, 0.50f, 0.55f, 0f)
    world.placeStar(world.youFrontX() + World.STAR_R_X + 0.001f, world.youPaddleY, -0.7f, 0f)
    var guard = 0
    while (world.starLive() && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertFalse(world.starLive())
    val x0 = world.ballX
    world.step(1f / 60f)
    assertTrue(world.ballX > x0)
    world.moveYouPaddle(0.82f)
    assertEquals(0.82f, world.youPaddleY, 0.0001f)
  }

  @Test
  fun raisedPostBouncesTheBallWithoutScoring() {
    val world = World(rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    world.raisePost(1)
    val chips = world.cpuChipsLeft()
    world.placeBall(World.POST_X - World.BALL_R_X - 0.002f, 0.50f, 0.8f, 0f)
    var guard = 0
    while (world.ballVx() > 0f && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.ballVx() < 0f)
    assertEquals(0, world.youScore)
    assertEquals(chips, world.cpuChipsLeft())
    val postSfx = world.drainSfx()
    assertFalse(postSfx.contains(GameSfx.CHIP))
    assertTrue(postSfx.contains(GameSfx.WALL))
  }

  @Test
  fun raisedPostBouncesTheIceComet() {
    val world = World(you = Fighter.RIVET, rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    world.raisePost(1)
    world.placeBall(0.35f, 0.20f, -0.3f, 0f)
    world.placeStar(World.POST_X - World.STAR_R_X - 0.002f, 0.50f, 0.7f, 0f)
    var guard = 0
    while (world.starVx() > 0f && world.starLive() && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.starLive())
    assertTrue(world.starVx() < 0f)
    assertEquals(World.CHIP_COUNT, world.cpuChipsLeft())
    assertTrue(world.drainSfx().contains(GameSfx.WALL))
  }

  @Test
  fun iceCometFlattensYourGateAfterAWallBounce() {
    val world = World(you = Fighter.RIVET, rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    world.raisePost(1)
    world.moveYouPaddle(0.14f)
    world.placeBall(0.35f, 0.20f, -0.3f, 0f)
    world.placeStar(World.POST_X - World.STAR_R_X - 0.002f, 0.50f, 0.7f, 0f)
    var guard = 0
    while (world.starLive() && world.youChipsLeft() == World.CHIP_COUNT && guard++ < 200) {
      world.step(1f / 60f)
    }
    assertFalse(world.starLive())
    assertEquals(World.CHIP_COUNT - 1, world.youChipsLeft())
    assertEquals(1, world.cpuScore)
    assertTrue(world.iceBurstLive())
    val sfx = world.drainSfx()
    assertTrue(sfx.contains(GameSfx.ICE))
    assertTrue(sfx.contains(GameSfx.CHIP))
  }

  @Test
  fun nextWellRaisesAsTheLastOneSinks() {
    val world = World(rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    world.raisePost(0)
    world.placeBall(0.50f, 0.50f, 0f, 0f)
    val dt = 1f / 60f
    repeat((World.POST_UP / dt).toInt() + 8) { world.step(dt) }
    assertTrue(world.wellVisible(0))
    assertTrue(world.wellVisible(1))
    assertFalse(world.wellVisible(2))
  }

  @Test
  fun postsStayOffWhenTheCourtIsNotAsh() {
    val world = World(you = Fighter.RIVET, rival = Fighter.MARU, cpuLevel = CpuLevel.EASY)
    world.raisePost(1)
    assertFalse(world.postLive())
    world.placeBall(World.POST_X - World.BALL_R_X - 0.002f, 0.50f, 0.8f, 0f)
    val vx0 = world.ballVx()
    repeat(12) { world.step(1f / 60f) }
    assertTrue(world.ballVx() > 0f)
    assertEquals(vx0, world.ballVx(), 0.0001f)
  }

  @Test
  fun rivetTraceBouncesTheBallWithoutScoring() {
    val world = World(rival = Fighter.RIVET, cpuLevel = CpuLevel.EASY)
    world.placeTrace(0.5f)
    val chips = world.cpuChipsLeft()
    world.placeBall(World.TRACE_X - World.BALL_R_X - 0.002f, 0.50f, 0.8f, 0f)
    var guard = 0
    while (world.ballVx() > 0f && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.ballVx() < 0f)
    assertEquals(0, world.youScore)
    assertEquals(chips, world.cpuChipsLeft())
    val traceSfx = world.drainSfx()
    assertFalse(traceSfx.contains(GameSfx.CHIP))
    assertTrue(traceSfx.contains(GameSfx.WALL))
  }

  @Test
  fun rivetTraceBouncesTheIceComet() {
    val world = World(you = Fighter.ASH, rival = Fighter.RIVET, cpuLevel = CpuLevel.EASY)
    world.placeTrace(0.5f)
    world.placeBall(0.35f, 0.20f, -0.3f, 0f)
    world.placeStar(World.TRACE_X - World.STAR_R_X - 0.002f, 0.50f, 0.7f, 0f)
    var guard = 0
    while (world.starVx() > 0f && world.starLive() && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.starLive())
    assertTrue(world.starVx() < 0f)
    assertEquals(World.CHIP_COUNT, world.cpuChipsLeft())
    assertTrue(world.drainSfx().contains(GameSfx.WALL))
  }

  @Test
  fun rivetTraceStaysOffOnOtherCourts() {
    val world = World(rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    world.placeTrace(0.5f)
    assertFalse(world.traceLive())
  }

  @Test
  fun hexCarStaysOffOnOtherCourts() {
    val world = World(rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    world.placeHexCar()
    assertFalse(world.hexCarLive())
  }

  @Test
  fun hexCarPausesOnTheXThenDrivesOffTheTop() {
    val world = World(rival = Fighter.HEX, cpuLevel = CpuLevel.EASY)
    world.placeHexCar(hold = true)
    assertTrue(world.hexCarLive())
    val parked = 0.5f - World.HEX_CAR_H / 2f
    assertEquals(parked, world.hexCarY(), 0.0001f)
    val y0 = world.hexCarY()
    repeat(12) { world.step(1f / 60f) }
    assertEquals(y0, world.hexCarY(), 0.0001f)
    var hold = 0
    while (kotlin.math.abs(world.hexCarY() - y0) < 0.0001f && hold++ < 200) {
      world.step(1f / 60f)
    }
    assertTrue(world.hexCarLive())
    assertTrue(world.hexCarY() < y0)
    var guard = 0
    while (world.hexCarLive() && guard++ < 400) {
      world.step(1f / 60f)
    }
    assertFalse(world.hexCarLive())
    var wait = 0
    while (!world.hexCarLive() && wait++ < 200) {
      world.step(1f / 60f)
    }
    assertTrue(world.hexCarLive())
    assertTrue(world.hexCarY() > parked)
  }

  @Test
  fun hexCarBouncesTheBallWithoutScoring() {
    val world = World(rival = Fighter.HEX, cpuLevel = CpuLevel.EASY)
    world.placeHexCar(hold = true)
    val chips = world.cpuChipsLeft()
    world.placeBall(World.HEX_CAR_X - World.BALL_R_X - 0.002f, 0.50f, 0.8f, 0f)
    var guard = 0
    while (world.ballVx() > 0f && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.ballVx() < 0f)
    assertEquals(0, world.youScore)
    assertEquals(chips, world.cpuChipsLeft())
    val carSfx = world.drainSfx()
    assertFalse(carSfx.contains(GameSfx.CHIP))
    assertTrue(carSfx.contains(GameSfx.WALL))
  }

  @Test
  fun hexCarBouncesTheIceComet() {
    val world = World(you = Fighter.ASH, rival = Fighter.HEX, cpuLevel = CpuLevel.EASY)
    world.placeHexCar(hold = true)
    world.placeBall(0.35f, 0.20f, -0.3f, 0f)
    world.placeStar(World.HEX_CAR_X - World.STAR_R_X - 0.002f, 0.50f, 0.7f, 0f)
    var guard = 0
    while (world.starVx() > 0f && world.starLive() && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.starLive())
    assertTrue(world.starVx() < 0f)
    assertEquals(World.CHIP_COUNT, world.cpuChipsLeft())
    assertTrue(world.drainSfx().contains(GameSfx.WALL))
  }

  @Test
  fun quillHawkStaysOffOnOtherCourts() {
    val world = World(rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    world.placeHawk()
    assertFalse(world.hawkLive())
  }

  @Test
  fun quillHawkOrbitsTheRimAndSkipsTheX() {
    val world = World(rival = Fighter.QUILL, cpuLevel = CpuLevel.EASY)
    world.placeHawk(0f)
    assertTrue(world.hawkLive())
    var sawHigh = false
    var sawLow = false
    var sawLeft = false
    var sawRight = false
    repeat((World.HAWK_LAP / (1f / 60f)).toInt() + 8) {
      world.step(1f / 60f)
      val cx = world.hawkX() + World.HAWK_W / 2f
      val cy = world.hawkY() + World.HAWK_H / 2f
      val dx = cx - 0.5f
      val dy = cy - 0.5f
      assertTrue(dx * dx + dy * dy > 0.12f * 0.12f)
      if (cy < World.HAWK_TOP_CY + 0.05f) sawHigh = true
      if (cy > World.HAWK_BOT_CY - 0.05f) sawLow = true
      if (cx < World.HAWK_LEFT_CX + 0.05f) sawLeft = true
      if (cx > World.HAWK_RIGHT_CX - 0.05f) sawRight = true
    }
    assertTrue(sawHigh)
    assertTrue(sawLow)
    assertTrue(sawLeft)
    assertTrue(sawRight)
  }

  @Test
  fun quillHawkHeadingFollowsThePath() {
    val world = World(rival = Fighter.QUILL, cpuLevel = CpuLevel.EASY)
    world.placeHawk(0f)
    assertEquals(0f, world.hawkHeadingDeg(), 8f)
    world.placeHawk(0.52f)
    val heading = world.hawkHeadingDeg()
    val leftward = heading > 150f || heading < -150f
    assertTrue(leftward)
  }

  @Test
  fun quillHawkBouncesTheBallWithoutScoring() {
    val world = World(rival = Fighter.QUILL, cpuLevel = CpuLevel.EASY)
    world.placeHawk(0.58f)
    val chips = world.cpuChipsLeft()
    val y = world.hawkY() + World.HAWK_H / 2f
    world.placeBall(world.hawkX() - World.BALL_R_X - 0.002f, y, 0.8f, 0f)
    var guard = 0
    while (world.ballVx() > 0f && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.ballVx() < 0f)
    assertEquals(0, world.youScore)
    assertEquals(chips, world.cpuChipsLeft())
    val hawkSfx = world.drainSfx()
    assertFalse(hawkSfx.contains(GameSfx.CHIP))
    assertTrue(hawkSfx.contains(GameSfx.WALL))
  }

  @Test
  fun quillHawkBouncesTheIceComet() {
    val world = World(you = Fighter.ASH, rival = Fighter.QUILL, cpuLevel = CpuLevel.EASY)
    world.placeHawk(0.58f)
    world.placeBall(0.35f, 0.20f, -0.3f, 0f)
    val y = world.hawkY() + World.HAWK_H / 2f
    world.placeStar(world.hawkX() - World.STAR_R_X - 0.002f, y, 0.7f, 0f)
    var guard = 0
    while (world.starVx() > 0f && world.starLive() && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.starLive())
    assertTrue(world.starVx() < 0f)
    assertEquals(World.CHIP_COUNT, world.cpuChipsLeft())
    assertTrue(world.drainSfx().contains(GameSfx.WALL))
  }

  @Test
  fun kiteXStaysOffOnOtherCourts() {
    val world = World(rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    world.placeKiteX()
    assertFalse(world.kiteXLive())
  }

  @Test
  fun kiteXSpinsOnKiteCourt() {
    val world = World(rival = Fighter.KITE, cpuLevel = CpuLevel.EASY)
    world.placeKiteX(0f)
    assertTrue(world.kiteXLive())
    assertEquals(World.KITE_X_CX, world.kiteXX() + World.KITE_X_W / 2f, 0.0001f)
    assertEquals(World.KITE_X_CY, world.kiteXY() + World.KITE_X_H / 2f, 0.0001f)
    assertEquals(0f, world.kiteXDeg(), 0.01f)
    repeat(20) { world.step(0.05f) }
    assertEquals(36f, world.kiteXDeg(), 1.5f)
  }

  @Test
  fun kiteXBouncesTheBallWithoutScoring() {
    val world = World(rival = Fighter.KITE, cpuLevel = CpuLevel.EASY)
    world.placeKiteX(-45f)
    val chips = world.cpuChipsLeft()
    val x =
      World.KITE_X_CX - World.KITE_X_ARM_T / World.COURT_ASPECT / 2f - World.BALL_R_X - 0.002f
    world.placeBall(x, World.KITE_X_CY, 0.8f, 0f)
    var guard = 0
    while (world.ballVx() > 0f && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.ballVx() < 0f)
    assertEquals(0, world.youScore)
    assertEquals(chips, world.cpuChipsLeft())
    val sfx = world.drainSfx()
    assertFalse(sfx.contains(GameSfx.CHIP))
    assertTrue(sfx.contains(GameSfx.WALL))
  }

  @Test
  fun kiteXBouncesTheIceComet() {
    val world = World(you = Fighter.ASH, rival = Fighter.KITE, cpuLevel = CpuLevel.EASY)
    world.placeKiteX(-45f)
    world.placeBall(0.35f, 0.20f, -0.3f, 0f)
    val x =
      World.KITE_X_CX - World.KITE_X_ARM_T / World.COURT_ASPECT / 2f - World.STAR_R_X - 0.002f
    world.placeStar(x, World.KITE_X_CY, 0.7f, 0f)
    var guard = 0
    while (world.starVx() > 0f && world.starLive() && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.starLive())
    assertTrue(world.starVx() < 0f)
    assertEquals(World.CHIP_COUNT, world.cpuChipsLeft())
    assertTrue(world.drainSfx().contains(GameSfx.WALL))
  }

  @Test
  fun maruLogsStayOffOnOtherCourts() {
    val world = World(rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    world.placeMaruLogs()
    assertFalse(world.maruLogLive())
  }

  @Test
  fun maruLogsDriftDownTheChasmAndWrap() {
    val world = World(rival = Fighter.MARU, cpuLevel = CpuLevel.EASY)
    world.placeMaruLogs(0f)
    assertTrue(world.maruLogLive())
    assertEquals(World.LOG_COUNT, world.maruLogCount())
    val y0 = world.maruLogY(0)
    val y1 = world.maruLogY(1)
    val y2 = world.maruLogY(2)
    assertTrue(y0 < y1)
    assertTrue(y1 < y2)
    val gap01 = y1 - y0
    val gap12 = y2 - y1
    assertTrue(kotlin.math.abs(gap01 - gap12) > 0.04f)
    assertTrue(kotlin.math.abs(world.maruLogW(0) - world.maruLogW(1)) > 0.01f)
    assertTrue(kotlin.math.abs(world.maruLogH(1) - world.maruLogH(2)) > 0.004f)
    repeat(20) { world.step(0.05f) }
    assertTrue(world.maruLogY(0) > y0)
    world.placeMaruLogs(0.98f)
    val nearBot = world.maruLogY(0)
    repeat(20) { world.step(0.05f) }
    assertTrue(world.maruLogY(0) < nearBot)
  }

  @Test
  fun maruLogsWeaveAroundTheBoulders() {
    val world = World(rival = Fighter.MARU, cpuLevel = CpuLevel.EASY)
    world.placeMaruLogAtY(0, 127f / 1080f)
    val rightOfTopRock = world.maruLogX(0) + world.maruLogW(0) / 2f
    world.placeMaruLogAtY(0, 447f / 1080f)
    val leftOfMidRock = world.maruLogX(0) + world.maruLogW(0) / 2f
    world.placeMaruLogAtY(0, 611f / 1080f)
    val rightOfLowRock = world.maruLogX(0) + world.maruLogW(0) / 2f
    assertTrue(rightOfTopRock > 0.50f)
    assertTrue(leftOfMidRock < rightOfTopRock - 0.02f)
    assertTrue(rightOfLowRock > leftOfMidRock + 0.02f)
    world.placeMaruLogAtY(0, 420f / 1080f)
    assertTrue(kotlin.math.abs(world.maruLogDeg(0) - 90f) > 8f)
  }

  @Test
  fun maruLogBouncesTheBallWithoutScoring() {
    val world = World(rival = Fighter.MARU, cpuLevel = CpuLevel.EASY)
    world.placeMaruLogAtY(0, 0.5f)
    val chips = world.cpuChipsLeft()
    val cx = world.maruLogX() + world.maruLogW() / 2f
    val cy = world.maruLogY(0) + world.maruLogH(0) / 2f
    world.placeBall(cx - World.BALL_R_X - 0.012f, cy, 0.8f, 0f)
    var guard = 0
    while (world.ballVx() > 0f && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.ballVx() < 0f)
    assertEquals(0, world.youScore)
    assertEquals(chips, world.cpuChipsLeft())
    val sfx = world.drainSfx()
    assertFalse(sfx.contains(GameSfx.CHIP))
    assertTrue(sfx.contains(GameSfx.WALL))
  }

  @Test
  fun maruLogBouncesTheIceComet() {
    val world = World(you = Fighter.ASH, rival = Fighter.MARU, cpuLevel = CpuLevel.EASY)
    world.placeMaruLogAtY(0, 0.5f)
    world.placeBall(0.35f, 0.20f, -0.3f, 0f)
    val cx = world.maruLogX() + world.maruLogW() / 2f
    val cy = world.maruLogY(0) + world.maruLogH(0) / 2f
    world.placeStar(cx - World.STAR_R_X - 0.012f, cy, 0.7f, 0f)
    var guard = 0
    while (world.starVx() > 0f && world.starLive() && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.starLive())
    assertTrue(world.starVx() < 0f)
    assertEquals(World.CHIP_COUNT, world.cpuChipsLeft())
    assertTrue(world.drainSfx().contains(GameSfx.WALL))
  }

  @Test
  fun skillPointsKeepOneThreeFiveIceDouble() {
    assertEquals(100, World.skillPoints(0))
    assertEquals(300, World.skillPoints(1))
    assertEquals(500, World.skillPoints(2))
    assertEquals(700, World.skillPoints(3))
    assertEquals(200, World.skillPoints(0, ice = true))
    assertEquals(600, World.skillPoints(1, ice = true))
    assertEquals(1000, World.skillPoints(2, ice = true))
  }

  @Test
  fun p1DirectChipAwards100Skill() {
    val world = World(you = Fighter.RIVET, rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    val target = world.chips.first { it.side == Side.CPU && it.slot == 2 && it.alive }
    world.armYouShot(0)
    world.placeBall(target.x - World.BALL_R_X - 0.001f, target.y + target.h / 2f, 0.9f, 0f)
    var guard = 0
    while (world.youScore == 0 && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertEquals(1, world.youScore)
    assertEquals(100, world.youSkill)
    assertEquals(1, world.skillPopups.size)
    assertEquals(100, world.skillPopups[0].value)
    val popY = world.skillPopups[0].y
    world.step(1f / 60f)
    assertTrue(world.skillPopups[0].y < popY)
  }

  @Test
  fun p1WallBankUsesThreeAndFiveTable() {
    val world = World(you = Fighter.RIVET, rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    val target = world.chips.first { it.side == Side.CPU && it.slot == 2 && it.alive }
    world.armYouShot(1)
    world.placeBall(target.x - World.BALL_R_X - 0.001f, target.y + target.h / 2f, 0.9f, 0f)
    var guard = 0
    while (world.youScore == 0 && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertEquals(300, world.youSkill)
    thaw(world)
    val next = world.chips.first { it.side == Side.CPU && it.alive }
    world.armYouShot(2)
    world.placeBall(next.x - World.BALL_R_X - 0.001f, next.y + next.h / 2f, 0.9f, 0f)
    guard = 0
    while (world.youScore == 1 && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertEquals(800, world.youSkill)
  }

  @Test
  fun p1OwnGoalStillBanksTheTimeWell() {
    val world = World(you = Fighter.RIVET, rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    val target = world.chips.first { it.side == Side.YOU && it.slot == 0 && it.alive }
    world.armYouShot(4)
    world.placeBall(target.x + target.w + World.BALL_R_X + 0.001f, target.y + target.h / 2f, -0.9f, 0f)
    var guard = 0
    while (world.cpuScore == 0 && world.phase == Phase.PLAYING && guard++ < 80) {
      world.step(1f / 60f)
    }
    assertEquals(1, world.cpuScore)
    assertEquals(World.skillPoints(4), world.youSkill)
    assertEquals(1, world.skillPopups.size)
    assertEquals(World.skillPoints(4), world.skillPopups[0].value)
  }

  @Test
  fun p1IceChipDoublesTheTable() {
    val world = World(you = Fighter.RIVET, rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    val target = world.chips.first { it.side == Side.CPU && it.slot == 2 && it.alive }
    world.placeBall(0.4f, 0.5f, -0.2f, 0f)
    world.armYouIce(0)
    world.placeStar(target.x - World.STAR_R_X - 0.001f, target.y + target.h / 2f, 0.8f, 0f)
    var guard = 0
    while (world.youScore == 0 && world.starLive() && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertEquals(1, world.youScore)
    assertEquals(200, world.youSkill)
    assertEquals(200, world.skillPopups[0].value)
  }

  @Test
  fun skillPersistsAcrossSets() {
    val world = World(you = Fighter.RIVET, rival = Fighter.ASH, cpuLevel = CpuLevel.EASY)
    val keep = world.chips.first { it.side == Side.CPU && it.slot == 2 && it.alive }
    world.killAllBut(Side.CPU, keep)
    world.armYouShot(0)
    world.placeBall(keep.x - World.BALL_R_X - 0.001f, keep.y + keep.h / 2f, 0.9f, 0f)
    var guard = 0
    while (world.youSets == 0 && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertEquals(100, world.youSkill)
    world.skipSetWin()
    assertEquals(0, world.youScore)
    assertEquals(100, world.youSkill)
  }

  @Test
  fun attractDoesNotBankSkill() {
    val world = World(you = Fighter.RIVET, rival = Fighter.ASH, cpuLevel = CpuLevel.EASY, attract = true)
    val target = world.chips.first { it.side == Side.CPU && it.slot == 2 && it.alive }
    world.armYouShot(2)
    world.placeBall(target.x - World.BALL_R_X - 0.001f, target.y + target.h / 2f, 0.9f, 0f)
    var guard = 0
    while (world.youScore == 0 && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertEquals(1, world.youScore)
    assertEquals(0, world.youSkill)
    assertEquals(1, world.skillPopups.size)
    assertEquals(World.skillPoints(2), world.skillPopups[0].value)
  }

  @Test
  fun calledOrbMissesAShieldOffTheLine() {
    val world = World(you = Fighter.RIVET, rival = Fighter.HEX, cpuLevel = CpuLevel.EASY)
    world.placeBall(0.40f, 0.50f, -0.4f, 0f)
    assertTrue(world.callYouSpecial(0.12f))
    var guard = 0
    while (world.starLive() && world.cpuChipsLeft() == World.CHIP_COUNT && guard++ < 300) {
      world.step(1f / 60f)
    }
    assertTrue(world.cpuChipsLeft() < World.CHIP_COUNT)
    assertTrue(world.iceBurstLive())
  }

  @Test
  fun hexStarFiresOncePerSet() {
    val world = World(you = Fighter.HEX)
    youReturnSpeed(world, paddleY = 0.5f, ballY = 0.5f)
    assertFalse(world.starLive())
    thaw(world)
    youBounceAgain(world, paddleY = 0.5f, ballY = 0.5f)
    assertFalse(world.starLive())
    assertTrue(world.callYouSpecial(0.12f))
    assertTrue(world.starLive())
    assertEquals(Fighter.HEX, world.starFighter())
    assertFalse(world.callYouSpecial(0.88f))
  }

  private fun thaw(world: World) {
    var guard = 0
    while (world.impactFrozen() && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
  }

  private fun youBounceAgain(world: World, paddleY: Float, ballY: Float) {
    world.moveYouPaddle(paddleY)
    world.placeBall(world.youFrontX() + World.BALL_R_X + 0.001f, ballY, -0.8f, 0f)
    waitForYouBounce(world)
  }

  private fun waitForYouBounce(world: World) {
    var guard = 0
    while (world.ballVx() <= 0f && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertTrue(world.ballVx() > 0f)
  }
}
