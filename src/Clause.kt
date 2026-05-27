data class Clause(
    val lits: IntArray,
    var w1: Int = 0,
    var w2: Int = if (lits.size > 1) 1 else 0
)