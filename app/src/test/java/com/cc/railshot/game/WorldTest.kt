package com.cc.railshot.game

import org.junit.Assert.assertEquals
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
    assertEquals(Phase.ROUND, world.phase)
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
      World.YOU_FRONT_X + World.BALL_R_X + 0.001f,
      world.youPaddleY,
      -0.8f,
      0f,
    )
    var guard = 0
    while (world.youPose() != PaddlePose.HIT && world.phase == Phase.PLAYING && guard++ < 40) {
      world.step(1f / 60f)
    }
    assertEquals(PaddlePose.HIT, world.youPose())
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
}
