fun main(args: Array<String>) {

    if (args.isEmpty()) {
        println("Usage: <cnf file>")
        return
    }

    val parser = DimacsParser()

    for (i in 0..13) {
        val (numVars, clauses) = parser.parse("test-formulas/sat$i.in")
        println("test-formulas/sat$i.in")
        println("Variables: $numVars")
        /*
        for (c in clauses) {
            println(c.lits.joinToString(" "))
        }
        */

        val solver = Solver(numVars, clauses)

        val result = solver.solve()

        if (result) {
            println("SAT")
            solver.printAssignments()
        } else {
            println("UNSAT")
        }
    }

}

