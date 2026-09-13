package com.cc.railshot.game

/** Court feel for a roster pick. Tiny numeric knobs, same sport. */
data class FighterKit(
  val paddleLen: Float,
  val moveMul: Float,
  val shieldPop: Float,
  val sliceEdge: Float,
  val sliceAngle: Float,
  val swipeMul: Float,
  val flavor: String,
) {
  companion object {
    const val BASE_LEN = 0.20f
    const val BASE_POP = 1.06f
    const val BASE_EDGE = 0.32f
    const val BASE_ANGLE = 0.90f

    fun of(fighter: Fighter): FighterKit =
      when (fighter) {
        Fighter.RIVET ->
          FighterKit(
            paddleLen = BASE_LEN,
            moveMul = 1f,
            shieldPop = 1.18f,
            sliceEdge = BASE_EDGE,
            sliceAngle = BASE_ANGLE,
            swipeMul = 1f,
            flavor = "Hot bounce. Same reach as the rest.",
          )
        Fighter.ASH ->
          FighterKit(
            paddleLen = BASE_LEN,
            moveMul = 1.05f,
            shieldPop = BASE_POP,
            sliceEdge = BASE_EDGE,
            sliceAngle = BASE_ANGLE,
            swipeMul = 1.35f,
            flavor = "Drag the shield. The ball follows.",
          )
        Fighter.KITE ->
          FighterKit(
            paddleLen = 0.24f,
            moveMul = 1.22f,
            shieldPop = BASE_POP,
            sliceEdge = BASE_EDGE,
            sliceAngle = BASE_ANGLE,
            swipeMul = 1f,
            flavor = "Long shield. Fast on the rail.",
          )
        Fighter.MARU ->
          FighterKit(
            paddleLen = 0.25f,
            moveMul = 0.78f,
            shieldPop = BASE_POP,
            sliceEdge = 0.42f,
            sliceAngle = 0.78f,
            swipeMul = 0.85f,
            flavor = "Slow. Fat slice. Holds the gate.",
          )
        Fighter.QUILL ->
          FighterKit(
            paddleLen = BASE_LEN,
            moveMul = 1.08f,
            shieldPop = BASE_POP,
            sliceEdge = 0.38f,
            sliceAngle = 1.05f,
            swipeMul = 1.10f,
            flavor = "Steep rims. Hunts the high line.",
          )
        Fighter.HEX ->
          FighterKit(
            paddleLen = 0.17f,
            moveMul = 0.92f,
            shieldPop = 0.96f,
            sliceEdge = 0.30f,
            sliceAngle = BASE_ANGLE,
            swipeMul = 1f,
            flavor = "Short shield. Softer pop. Tricky.",
          )
      }
  }
}
