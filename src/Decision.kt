data class Decision(
    val lit: Int,
    val trailSize: Int,
    var flipped: Boolean = false
)