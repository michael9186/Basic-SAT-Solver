import java.io.File

class DimacsParser {

    fun parse(path: String): Pair<Int, MutableList<Clause>> {

        val clauses = mutableListOf<Clause>()
        var numVars = 0

        File(path).forEachLine { line ->

            if (line.startsWith("c") || line.isBlank()) {
                return@forEachLine
            }

            if (line.startsWith("p")) {

                val parts = line.split(" ")
                numVars = parts[2].toInt()

                return@forEachLine
            }

            val lits = line
                .trim()
                .split(Regex("\\s+"))
                .map { it.toInt() }
                .takeWhile { it != 0 }
                .toIntArray()

            clauses.add(Clause(lits))
        }
        return numVars to clauses
    }
}