fun main(args: Array<String>) {

    if (args.isEmpty() || args.size != 2) {
        println("Usage: solver <1/2> <file>")
        return
    }

    val solverVersion = args[0]

    val parser = DimacsParser()
    val (numVars, clauses) = parser.parse(args[1])

    var stats: SolverStats? = null

    val result: Boolean
    when (solverVersion) {
        "1" -> {
            val solver = Solver1(numVars, clauses)
            result = solver.solve()
            stats = solver.stats
        }

        "2" -> {
            val solver = Solver2(numVars, clauses)
            result = solver.solve()
            stats = solver.stats
        }

        else -> {
            println("Unknown solver version: $solverVersion")
            result = false
        }
    }

    if (result) {
        println("SAT")
    } else {
        println("UNSAT")
    }

    if (stats != null) {
        println("Stats:")
        println("DECISIONS ${stats.decisions}")
        println("PROPAGATIONS ${stats.propagations}")
        println("CONFLICTS ${stats.conflicts}")
        if (solverVersion == "2") {println("LEARNED ${stats.learnedClauses}")}
    }
}