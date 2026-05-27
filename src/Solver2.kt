import kotlin.math.abs

private const val UNASSIGNED = 0
private const val TRUE = 1
private const val FALSE = -1

class Solver2(private val numVars: Int, initialClauses: List<Clause>) {

    private data class DecisionFrame(
        val variable: Int,
        var phase: Int, // 0 = try true next, 1 = try false next, 2 = done
        val trailSize: Int,
        val qhead: Int
    )

    private val clauses = initialClauses.toMutableList()
    private val assignment = IntArray(numVars + 1)
    private val trail = mutableListOf<Int>()
    private var qhead = 0
    private val stack = mutableListOf<DecisionFrame>()

    private val watches =
        Array(2 * (numVars + 1) + 2) {
            mutableListOf<Clause>()
        }

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

                if (!enqueue(c.lits[0])) {
                    return false
                }
            }
        }

        if (propagate() != null) {
            return false
        }

        while (true) {
            val conflict = propagate()
            if (conflict != null) {
                var resolved = false
                while (stack.isNotEmpty()) {
                    val frame = stack.last()
                    undo(frame.trailSize)
                    qhead = frame.qhead

                    when (frame.phase) {
                        // TRUE branch failed -> try FALSE
                        0 -> {
                            frame.phase = 1
                            if (enqueue(-frame.variable)) {
                                resolved = true
                                break
                            }
                        }
                        // FALSE branch failed -> pop
                        1 -> {
                            frame.phase = 2
                            stack.removeLast()
                        }
                    }
                }

                if (!resolved) {
                    return false
                }

                continue
            }

            val decision = chooseLiteral()

            if (decision == 0) { //All assigned
                return true
            }

            stack.add(
                DecisionFrame(
                    variable = decision,
                    phase = 0,
                    trailSize = trail.size,
                    qhead = qhead
                )
            )
            enqueue(decision)
        }
    }

    // =====================================================
    // BCP
    // =====================================================

    private fun propagate(): Clause? {
        while (qhead < trail.size) {
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

                if (moved) {
                    continue
                }

                // no replacement possible

                when (literalValue(otherLit)) {
                    FALSE -> { return clause }
                    UNASSIGNED -> { if (!enqueue(otherLit)) return clause }
                }
                i++
            }
        }

        return null
    }

    // =====================================================
    // ASSIGNMENT
    // =====================================================

    private fun enqueue(lit: Int): Boolean {
        val v = abs(lit)
        val value = if (lit > 0) TRUE else FALSE
        val current = assignment[v]

        if (current != UNASSIGNED) {
            return current == value
        }

        assignment[v] = value
        trail.add(lit)
        return true
    }

    private fun undo(targetSize: Int) {
        while (trail.size > targetSize) {
            val lit = trail.removeLast()
            assignment[abs(lit)] = UNASSIGNED
        }
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