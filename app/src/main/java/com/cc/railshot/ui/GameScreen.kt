package com.cc.railshot.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cc.railshot.game.CabinetInset
import com.cc.railshot.game.Chip
import com.cc.railshot.game.Fighter
import com.cc.railshot.game.PaddlePose
import com.cc.railshot.game.Phase
import com.cc.railshot.game.Side
import com.cc.railshot.game.World
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun GameScreen(you: Fighter = Fighter.RIVET, rival: Fighter = Fighter.RIVET) {
  val world = remember { World() }
  var frame by remember { mutableIntStateOf(0) }
  val context = LocalContext.current
  val youFrames = remember(you) { loadCourtBitmaps(context, you.art(), left = true) }
  val rivalFrames = remember(rival) { loadCourtBitmaps(context, rival.art(), left = false) }
  val courtFloor = remember { loadOpaqueAsset(context, "court_circuit.png") }
  val ballSprite = remember { loadKeyedAsset(context, "ui_ball.png") }
  val youChipSprite = remember { loadKeyedAsset(context, "ui_chip_you.png") }
  val cpuChipSprite = remember { loadKeyedAsset(context, "ui_chip_cpu.png") }

  LaunchedEffect(Unit) {
    var last = 0L
    while (true) {
      withFrameNanos { now ->
        val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
        last = now
        world.step(dt)
        frame++
      }
    }
  }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(Cabinet)
        .statusBarsPadding()
        .navigationBarsPadding(),
  ) {
    val tick = frame
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      val courtMod =
        if (maxWidth / maxHeight >= 4f / 3f) {
          Modifier.fillMaxHeight().aspectRatio(4f / 3f)
        } else {
          Modifier.fillMaxWidth().aspectRatio(4f / 3f)
        }
      Box(modifier = courtMod.background(Ink)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val padL = maxWidth * World.FRAME_LEFT
          val padT = maxHeight * World.FRAME_TOP
          val padR = maxWidth * World.FRAME_RIGHT
          val padB = maxHeight * World.FRAME_BOTTOM
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(start = padL, top = padT, end = padR, bottom = padB)
                .pointerInput(Unit) {
                  detectTapGestures(onTap = { world.launch() })
                }
                .pointerInput(Unit) {
                  detectDragGestures { change, _ ->
                    change.consume()
                    world.moveYouPaddle((change.position.y / size.height).coerceIn(0f, 1f))
                  }
                },
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              tick
              val w = size.width
              val h = size.height
              drawImage(
                image = courtFloor,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(courtFloor.width, courtFloor.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                filterQuality = FilterQuality.None,
              )
              world.chips.forEach { chip ->
                drawChip(
                  sprite = if (chip.side == Side.YOU) youChipSprite else cpuChipSprite,
                  chip = chip,
                  courtW = w,
                  courtH = h,
                )
              }
              drawFighter(
                frames = youFrames,
                pose = world.youPose(),
                paddleX = World.YOU_PADDLE_X,
                paddleTop = world.youPaddleTop(),
                leftCourt = true,
                fallback = YouPaddle,
                courtW = w,
                courtH = h,
              )
              drawFighter(
                frames = rivalFrames,
                pose = world.cpuPose(),
                paddleX = World.CPU_PADDLE_X,
                paddleTop = world.cpuPaddleTop(),
                leftCourt = false,
                fallback = CpuPaddle,
                courtW = w,
                courtH = h,
              )
              drawBall(
                sprite = ballSprite,
                ballX = world.ballX,
                ballY = world.ballY,
                courtW = w,
                courtH = h,
              )
            }
            when (world.phase) {
              Phase.ROUND -> {
                Column(
                  modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.88f),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  MagentaKeyedImage(
                    assetPath = UiArt.ROUND,
                    contentDescription = "ROUND",
                    modifier = Modifier.fillMaxWidth().aspectRatio(960f / 547f),
                    contentScale = ContentScale.Fit,
                  )
                  MagentaKeyedImage(
                    assetPath = UiArt.roundNum(world.roundNumber()),
                    contentDescription = world.roundNumber().toString(),
                    modifier = Modifier.fillMaxWidth(0.28f).aspectRatio(240f / 300f).padding(top = 4.dp),
                    contentScale = ContentScale.Fit,
                  )
                }
              }
              Phase.SERVE -> {
                MagentaKeyedImage(
                  assetPath = UiArt.FIGHT,
                  contentDescription = "FIGHT",
                  modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.92f).aspectRatio(960f / 547f),
                  contentScale = ContentScale.Fit,
                )
              }
              Phase.YOU_WIN -> {
                Text(
                  text = "WIN\nTAP TO RESTART",
                  color = Cream,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  fontSize = 28.sp,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.align(Alignment.Center),
                )
              }
              Phase.CPU_WIN -> {
                Text(
                  text = "CPU WINS\nTAP TO RESTART",
                  color = Cream,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  fontSize = 28.sp,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.align(Alignment.Center),
                )
              }
              Phase.PLAYING -> Unit
            }
          }
        }
        MagentaKeyedImage(
          assetPath = "ui_cabinet.png",
          contentDescription = null,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.FillBounds,
          keyHoleBleed = true,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          CabinetWell(World.P1_SCORE_INSET) {
            WellText(world.youScore.toString().padStart(2, '0'), YouPaddle, 0.72f)
          }
          CabinetWell(World.P1_NAME_INSET) {
            WellText(you.displayName.uppercase(), Cream, 0.42f)
          }
          CabinetWell(World.P1_SETS_INSET) { SetDots(world.youSets, YouPaddle) }
          CabinetWell(World.TIME_INSET) {
            WellText(world.timeDisplay(), Cream, 0.72f)
          }
          CabinetWell(World.P2_SETS_INSET) { SetDots(world.cpuSets, CpuPaddle) }
          CabinetWell(World.P2_NAME_INSET) {
            WellText(rival.displayName.uppercase(), Cream, 0.42f)
          }
          CabinetWell(World.P2_SCORE_INSET) {
            WellText(world.cpuScore.toString().padStart(2, '0'), CpuPaddle, 0.72f)
          }
        }
      }
    }
  }
}

private fun loadCourtBitmaps(
  context: Context,
  art: FighterArt,
  left: Boolean,
): Map<PaddlePose, ImageBitmap> {
  val paths =
    mapOf(
      PaddlePose.IDLE to art.courtFrame(left, PaddlePose.IDLE),
      PaddlePose.WALK to art.courtFrame(left, PaddlePose.WALK),
      PaddlePose.HIT to art.courtFrame(left, PaddlePose.HIT),
    )
  return paths.mapNotNull { (pose, path) ->
    if (path == null) null else pose to loadKeyedAsset(context, path)
  }.toMap()
}

private fun DrawScope.drawFighter(
  frames: Map<PaddlePose, ImageBitmap>,
  pose: PaddlePose,
  paddleX: Float,
  paddleTop: Float,
  leftCourt: Boolean,
  fallback: Color,
  courtW: Float,
  courtH: Float,
) {
  val bmp = frames[pose] ?: frames[PaddlePose.IDLE]
  val spriteH = World.PADDLE_LEN * courtH
  val spriteW = spriteH * (World.SPRITE_W / World.SPRITE_H.toFloat())
  val top = paddleTop * courtH
  // Body sits toward midcourt so sprites stand in front of the chip rails, not over them.
  val left =
    if (leftCourt) {
      paddleX * courtW
    } else {
      paddleX * courtW + World.PADDLE_THICK * courtW - spriteW
    }
  if (bmp != null) {
    drawImage(
      image = bmp,
      srcOffset = IntOffset.Zero,
      srcSize = IntSize(bmp.width, bmp.height),
      dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
      dstSize = IntSize(spriteW.roundToInt(), spriteH.roundToInt()),
      filterQuality = FilterQuality.None,
    )
  } else {
    val hitX = if (leftCourt) World.YOU_HIT_X else World.CPU_HIT_X
    drawRoundRect(
      color = fallback,
      topLeft = Offset(hitX * courtW, top),
      size = Size(World.PADDLE_THICK * courtW, spriteH),
      cornerRadius = CornerRadius(6f, 6f),
    )
  }
}

private fun DrawScope.drawChip(
  sprite: ImageBitmap,
  chip: Chip,
  courtW: Float,
  courtH: Float,
) {
  val slotW = chip.w * courtW
  val slotH = chip.h * courtH
  val aspect = sprite.height / sprite.width.toFloat()
  var dw = slotW
  var dh = dw * aspect
  if (dh > slotH) {
    dh = slotH
    dw = dh / aspect
  }
  val left = (chip.x * courtW + (slotW - dw) / 2f).roundToInt()
  val top = (chip.y * courtH + (slotH - dh) / 2f).roundToInt()
  drawImage(
    image = sprite,
    srcOffset = IntOffset.Zero,
    srcSize = IntSize(sprite.width, sprite.height),
    dstOffset = IntOffset(left, top),
    dstSize = IntSize(dw.roundToInt().coerceAtLeast(1), dh.roundToInt().coerceAtLeast(1)),
    alpha = if (chip.alive) 1f else 0.22f,
    filterQuality = FilterQuality.None,
  )
}

private fun DrawScope.drawBall(
  sprite: ImageBitmap,
  ballX: Float,
  ballY: Float,
  courtW: Float,
  courtH: Float,
) {
  val radius = World.BALL_R * min(courtW, courtH) * 1.45f
  val cx = ballX * courtW
  val cy = ballY * courtH
  val side = (radius * 2f).roundToInt().coerceAtLeast(2)
  val left = (cx - side / 2f).roundToInt()
  val top = (cy - side / 2f).roundToInt()
  drawImage(
    image = sprite,
    srcOffset = IntOffset.Zero,
    srcSize = IntSize(sprite.width, sprite.height),
    dstOffset = IntOffset(left, top),
    dstSize = IntSize(side, side),
    filterQuality = FilterQuality.None,
  )
}

@Composable
private fun BoxWithConstraintsScope.CabinetWell(
  inset: CabinetInset,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier =
      Modifier
        .align(Alignment.TopStart)
        .offset(maxWidth * inset.leftF, maxHeight * inset.topF)
        .size(maxWidth * inset.widthF, maxHeight * inset.heightF),
    contentAlignment = Alignment.Center,
    content = content,
  )
}

@Composable
private fun WellText(text: String, color: Color, heightFrac: Float) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    val byHeight = maxHeight.value * heightFrac
    val byWidth = maxWidth.value / (text.length.coerceAtLeast(1) * 0.62f)
    val size = min(byHeight, byWidth)
    Text(
      text = text,
      color = color,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      fontSize = size.sp,
      maxLines = 1,
      overflow = TextOverflow.Clip,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
    )
  }
}

@Composable
private fun SetDots(won: Int, accent: Color) {
  BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    val dot = (min(maxHeight.value, maxWidth.value / 3.2f) * 0.55f).dp
    Row(
      horizontalArrangement = Arrangement.spacedBy(dot * 0.45f),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      repeat(World.SETS_TO_WIN) { i ->
        Box(
          modifier =
            Modifier
              .size(dot)
              .background(if (i < won) accent else Mute.copy(alpha = 0.35f), CircleShape),
        )
      }
    }
  }
}
