import kotlin.math.abs

private const val UNASSIGNED = 0
private const val TRUE = 1
private const val FALSE = -1

class Solver2(private val numVars: Int, initialClauses: List<Clause>) {

    val stats = SolverStats()

    private val clauses = initialClauses.toMutableList()
    private val assignment = IntArray(numVars + 1)
    private val trail = mutableListOf<Int>()
    private var qhead = 0
    private val watches = Array(2 * (numVars + 1) + 2) { mutableListOf<Clause>() }

    private val level = IntArray(numVars + 1) // decision level where var was assigned
    private var currentLevel = 0
    private val reason = arrayOfNulls<Clause>(numVars + 1) // propagated variables will have the reason clause stored

    init {
        for (c in clauses) {
            attachClause(c)
        }
    }

    // =====================================================
    // PUBLIC
    // =====================================================

    fun solve(): Boolean {

        // initial unit clauses
        for (c in clauses) {
            if (c.lits.isEmpty()) {
                return false
            }

            if (c.lits.size == 1) {
                if (!enqueue(c.lits[0], c)) {
                    return false
                }
            }
        }

        while (true) {
            val conflict = propagate()
            if (conflict != null) {
                // root-level conflict
                if (currentLevel == 0) return false

                val learned = analyzeConflict(conflict)
                val backtrackLevel = computeBacktrackLevel(learned)
                backtrack(backtrackLevel)

                clauses.add(learned)
                stats.learnedClauses++
                attachClause(learned)

                enqueue(learned.lits[0], learned)

            } else {
                val decision = chooseLiteral()

                if (decision == 0) {
                    return true
                }

                currentLevel++
                stats.decisions++
                enqueue(decision, null)
            }
        }
    }

    // =====================================================
    // BCP
    // =====================================================

    private fun propagate(): Clause? {
        while (qhead < trail.size) {
            stats.propagations++
            val lit = trail[qhead++]
            val falseLit = -lit
            val watchList = watches[litIndex(falseLit)]
            var i = 0
            while (i < watchList.size) {
                val clause = watchList[i]
                val watch1Lit = clause.lits[clause.w1]

                val falseWatch: Int
                val otherWatch: Int

                if (watch1Lit == falseLit) {
                    falseWatch = clause.w1
                    otherWatch = clause.w2
                } else {
                    falseWatch = clause.w2
                    otherWatch = clause.w1
                }

                val otherLit = clause.lits[otherWatch]

                // clause already satisfied
                if (literalValue(otherLit) == TRUE) {
                    i++
                    continue
                }

                // try move watch
                var moved = false

                for (k in clause.lits.indices) {
                    if (k == clause.w1 || k == clause.w2) continue

                    val candidate = clause.lits[k]
                    if (literalValue(candidate) != FALSE) {

                        if (falseWatch == clause.w1) {
                            clause.w1 = k
                        } else {
                            clause.w2 = k
                        }

                        watchList[i] = watchList.last()
                        watchList.removeLast()

                        watches[litIndex(candidate)].add(clause)

                        moved = true
                        break
                    }
                }

                if (moved) continue

                // no replacement possible
                when (literalValue(otherLit)) {
                    FALSE -> {
                        stats.conflicts++
                        return clause
                    }
                    UNASSIGNED -> { if (!enqueue(otherLit, clause)) return clause }
                }
                i++
            }
        }

        return null
    }

    // =====================================================
    // ASSIGNMENT
    // =====================================================

    private fun enqueue(lit: Int, from: Clause?): Boolean {
        val v = abs(lit)
        val value = if (lit > 0) TRUE else FALSE
        val current = assignment[v]

        if (current != UNASSIGNED) {
            return current == value
        }

        assignment[v] = value
        trail.add(lit)
        level[v] = currentLevel
        reason[v] = from
        return true
    }

    // =====================================================
    // WATCHES
    // =====================================================

    private fun attachClause(clause: Clause) {
        when (clause.lits.size) {
            0 -> {}
            1 -> { watches[litIndex(clause.lits[0])].add(clause) }
            else -> {
                watches[litIndex(clause.lits[clause.w1])].add(clause)
                watches[litIndex(clause.lits[clause.w2])].add(clause)
            }
        }
    }

    // =====================================================
    // CONFLICT DRIVEN STUFF
    // =====================================================

    private fun analyzeConflict(conflict: Clause): Clause {
        val learned = mutableListOf<Int>()
        val seen = BooleanArray(numVars + 1)
        var pathC = 0
        var p = 0
        var clause: Clause = conflict
        var index = trail.size - 1

        while (true) {
            for (lit in clause.lits) {
                val v = abs(lit)

                if (v == abs(p)) continue // skip pivot variable itself
                if (seen[v]) continue

                seen[v] = true

                if (level[v] == currentLevel) {
                    pathC++
                } else {
                    learned.add(lit)
                }
            }

            // select latest assigned seen literal
            while (true) {
                p = trail[index--]
                if (seen[abs(p)]) {
                    break
                }
            }

            pathC--
            if (pathC <= 0) break // FIRST UIP FOUND

            clause = reason[abs(p)]
                ?: error("reason unexpectedly null")
        }

        learned.add(0, -p)
        return Clause(learned.toIntArray())
    }

    private fun computeBacktrackLevel(clause: Clause): Int {
        if (clause.lits.size <= 1) {
            return 0
        }
        var maxLevel = 0
        for (i in 1..<clause.lits.size) {
            val lit = clause.lits[i]
            val v = abs(lit)
            maxLevel = maxOf(maxLevel, level[v])
        }
        return maxLevel
    }

    private fun backtrack(targetLevel: Int) {
        while (
            trail.isNotEmpty() &&
            level[abs(trail.last())] > targetLevel
        ) {
            val lit = trail.removeLast()
            val v = abs(lit)
            assignment[v] = UNASSIGNED
            level[v] = 0
            reason[v] = null
        }
        qhead = trail.size
        currentLevel = targetLevel
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private fun chooseLiteral(): Int {
        for (v in 1..numVars) {
            if (assignment[v] == UNASSIGNED) {
                return v
            }
        }
        return 0
    }

    private fun literalValue(lit: Int): Int {
        val v = abs(lit)
        val assigned = assignment[v]
        if (assigned == UNASSIGNED) {
            return UNASSIGNED
        }
        return if (lit > 0) {
            assigned
        } else {
            -assigned
        }
    }

    private fun litIndex(lit: Int): Int {
        val v = abs(lit)
        return if (lit > 0) 2 * v else 2 * v + 1
    }

}