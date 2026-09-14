package com.cc.railshot.game

/** Court feel for a roster pick. Readable knobs, same sport. */
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
            shieldPop = 1.28f,
            sliceEdge = BASE_EDGE,
            sliceAngle = BASE_ANGLE,
            swipeMul = 1f,
            flavor = "Hot bounce. Stacks on a rally.",
          )
        Fighter.ASH ->
          FighterKit(
            paddleLen = BASE_LEN,
            moveMul = 1.12f,
            shieldPop = BASE_POP,
            sliceEdge = BASE_EDGE,
            sliceAngle = BASE_ANGLE,
            swipeMul = 1.85f,
            flavor = "Smash swipe. Drag through contact.",
          )
        Fighter.KITE ->
          FighterKit(
            paddleLen = 0.27f,
            moveMul = 1.38f,
            shieldPop = BASE_POP,
            sliceEdge = BASE_EDGE,
            sliceAngle = BASE_ANGLE,
            swipeMul = 1f,
            flavor = "Long rail. Dash, then afterburn.",
          )
        Fighter.MARU ->
          FighterKit(
            paddleLen = 0.28f,
            moveMul = 0.68f,
            shieldPop = BASE_POP,
            sliceEdge = 0.50f,
            sliceAngle = 0.78f,
            swipeMul = 0.85f,
            flavor = "Body block. Only the rims slice.",
          )
        Fighter.QUILL ->
          FighterKit(
            paddleLen = BASE_LEN,
            moveMul = 1.10f,
            shieldPop = BASE_POP,
            sliceEdge = 0.48f,
            sliceAngle = 1.22f,
            swipeMul = 1.10f,
            flavor = "Dive rims. Steals the high line.",
          )
        Fighter.HEX ->
          FighterKit(
            paddleLen = 0.15f,
            moveMul = 1f,
            shieldPop = 0.88f,
            sliceEdge = 0.30f,
            sliceAngle = BASE_ANGLE,
            swipeMul = 1f,
            flavor = "Short shield. One orb per set.",
          )
      }
  }
}
