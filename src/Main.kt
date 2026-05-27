fun main(args: Array<String>) {

    if (args.isEmpty() || args.size != 2) {
        println("Usage: solver <1/2> <file>")
        return
    }

    val solverVersion = args[0]

    val parser = DimacsParser()
    val (numVars, clauses) = parser.parse(args[1])

    val result = when (solverVersion) {
        "1" -> {
            val solver = Solver1(numVars, clauses)
            solver.solve()
        }
        "2" -> {
            val solver = Solver2(numVars, clauses)
            solver.solve()
        }
        else -> {
            println("Unknown solver version: $solverVersion")
            return
        }
    }

    if (result) {
        println("SAT")
    } else {
        println("UNSAT")
    }
}