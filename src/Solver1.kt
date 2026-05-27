import kotlin.math.abs

private const val UNASSIGNED = 0
private const val TRUE = 1
private const val FALSE = -1

class Solver1(
    private val numVars: Int,
    private val clauses: List<Clause>

) {

    /*
        assignment[v]
        0  -> unassigned
        1  -> true
        -1 -> false
        (see constants above)
     */
    private val assignment = IntArray(numVars + 1)

    //watched literal -> clauses watching it
    private val watchList = HashMap<Int, MutableList<Clause>>()

    //propagation queue
    private val propagationQueue = ArrayDeque<Int>()

    //assignment trail
    private val trail = mutableListOf<Int>()

    //decision stack
    private val decisions = mutableListOf<Decision>()

    init {
        initializeWatchLists()
    }

    fun solve(): Boolean {

        //Handle initial unit clauses
        for (clause in clauses) {

            if (clause.lits.isEmpty()) {
                return false
            }

            if (clause.lits.size == 1) {

                val lit = clause.lits[0]

                if (!enqueue(lit)) {
                    return false
                }
            }
        }

        while (true) {

            val conflict = propagate()

            if (conflict != null) {

                if (!backtrack()) {
                    return false
                }

            } else {

                val nextVar = chooseVariable()

                if (nextVar == -1) {
                    return true
                }

                decide(nextVar)
            }
        }
    }

    private fun initializeWatchLists() {

        for (clause in clauses) {

            if (clause.lits.isEmpty()) {
                continue
            }

            val l1 = clause.lits[clause.w1]

            watchList
                .getOrPut(l1) { mutableListOf() }
                .add(clause)

            if (clause.lits.size > 1) {

                val l2 = clause.lits[clause.w2]

                watchList
                    .getOrPut(l2) { mutableListOf() }
                    .add(clause)
            }
        }
    }

    /*
        returns:
        null -> no conflict
        clause -> conflicting clause
     */
    private fun propagate(): Clause? {

        while (propagationQueue.isNotEmpty()) {

            val lit = propagationQueue.removeFirst()

            //literal that became FALSE
            val falseLit = -lit

            val watching = watchList[falseLit]?.toMutableList() ?: mutableListOf()

            for (clause in watching) {

                val watch1Lit = clause.lits[clause.w1]
                //val watch2Lit = clause.lits[clause.w2]

                val falseWatchIndex: Int
                val otherWatchIndex: Int

                if (watch1Lit == falseLit) {
                    falseWatchIndex = clause.w1
                    otherWatchIndex = clause.w2
                } else {
                    falseWatchIndex = clause.w2
                    otherWatchIndex = clause.w1
                }

                val otherLit = clause.lits[otherWatchIndex]


                //clause already satisfied
                if (literalValue(otherLit) == TRUE) {
                    continue
                }

                //try finding new watch
                var foundReplacement = false

                for (i in clause.lits.indices) {

                    if (i == clause.w1 || i == clause.w2) {
                        continue
                    }

                    val candidate = clause.lits[i]

                    if (literalValue(candidate) != FALSE) {

                        //move watch
                        if (falseWatchIndex == clause.w1) {
                            clause.w1 = i
                        } else {
                            clause.w2 = i
                        }

                        watchList[falseLit]?.remove(clause)

                        watchList
                            .getOrPut(candidate) { mutableListOf() }
                            .add(clause)

                        foundReplacement = true
                        break
                    }
                }

                if (foundReplacement) {
                    continue
                }

                //no replacement found
                when (literalValue(otherLit)) {

                    FALSE -> {
                        //both watched literals false => conflict
                        return clause
                    }

                    UNASSIGNED -> {
                        //unit clause
                        if (!enqueue(otherLit)) {
                            return clause
                        }
                    }
                }
            }
        }

        return null
    }

    private fun enqueue(lit: Int): Boolean {

        val variable = abs(lit)

        val value =
            if (lit > 0) TRUE
            else FALSE

        val current = assignment[variable]

        //already assigned
        if (current != UNASSIGNED) {
            return current == value
        }

        assignment[variable] = value

        trail.add(lit)

        propagationQueue.addLast(lit)

        return true
    }

    /*
        returns:
        1  -> literal true
        -1 -> literal false
        0  -> unassigned
     */
    private fun literalValue(lit: Int): Int {

        val variable = abs(lit)

        val assigned = assignment[variable]

        if (assigned == UNASSIGNED) {
            return UNASSIGNED
        }

        return if (lit > 0) assigned else -assigned
    }

    private fun chooseVariable(): Int {

        for (v in 1..numVars) {

            if (assignment[v] == 0) {
                return v
            }
        }

        return -1
    }

    private fun decide(variable: Int) {

        val lit = variable

        decisions.add(
            Decision(
                lit = lit,
                trailSize = trail.size,
                flipped = false
            )
        )

        enqueue(lit)
    }

    //chronological backtracking
    private fun backtrack(): Boolean {

        while (decisions.isNotEmpty()) {

            val decision = decisions.removeLast()

            //undo assignments
            while (trail.size > decision.trailSize) {

                val lit = trail.removeLast()

                assignment[abs(lit)] = 0
            }

            //clear propagation queue
            propagationQueue.clear()

            //try opposite branch
            if (!decision.flipped) {

                val opposite = -decision.lit

                decisions.add(
                    Decision(
                        lit = opposite,
                        trailSize = trail.size,
                        flipped = true
                    )
                )

                enqueue(opposite)

                return true
            }
        }

        return false
    }

    fun printAssignments() {

        for (v in 1..numVars) {

            when (assignment[v]) {

                TRUE -> println("$v = TRUE")

                FALSE -> println("$v = FALSE")

                else -> println("$v = UNASSIGNED")
            }
        }
    }
}