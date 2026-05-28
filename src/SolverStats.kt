data class SolverStats(
    var decisions: Long = 0,
    var propagations: Long = 0,
    var conflicts: Long = 0,
    var learnedClauses: Long = 0
)