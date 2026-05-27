import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

private const val TIMEOUT_SECONDS = 10L
private val solverJar = File("out/artifacts/SAT_Solver_jar/SAT_Solver.jar").absolutePath
private const val SOLVER_VERSION = "2"

data class TestResult(
    val fileName: String,
    val expected: String,
    val actual: String,
    val runtimeMs: Double,
    val success: Boolean,
    val timeout: Boolean = false,
    val error: Boolean = false,
)

fun main(args: Array<String>) {

    val formulaDir = File("test-formulas")
    val files = formulaDir.listFiles()

    if (files.isEmpty()) {
        println("No test formulas found.")
        return
    }

    println("========================================")
    println("SAT Solver Parallel Tests")
    println("========================================")
    println()

    println("Timeout: ${TIMEOUT_SECONDS}s")
    println("Tests: ${files.size}")
    println("Solver: $SOLVER_VERSION")
    println()

    val executor = Executors.newWorkStealingPool()
    val futures = mutableListOf<Future<TestResult>>()

    val totalStart = System.nanoTime()

    for (file in files) {
        val future = executor.submit(
            Callable {
                val solutionFile = File("solutions/${file.name}.sol")

                if (!solutionFile.exists()) {
                    return@Callable TestResult(
                        fileName = file.name,
                        expected = "UNKNOWN",
                        actual = "MISSING SOLUTION",
                        runtimeMs = 0.0,
                        success = false,
                        error = true,
                    )
                }

                val expected = solutionFile.readText().trim()
                val start = System.nanoTime()

                try {
                    val process = ProcessBuilder(
                        "java",
                        "-jar",
                        solverJar,
                        SOLVER_VERSION,
                        file.absolutePath
                    ).redirectErrorStream(true).start()

                    val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)

                    val runtimeMs = (System.nanoTime() - start) / 1_000_000.0

                    if (!finished) {
                        process.destroyForcibly()

                        return@Callable TestResult(
                            fileName = file.name,
                            expected = expected,
                            actual = "TIMEOUT",
                            runtimeMs = runtimeMs,
                            success = false,
                            timeout = true
                        )
                    }

                    val output = process.inputStream.bufferedReader().readText()

                    val actual = when {
                        "UNSAT" in output -> "UNSAT"
                        "SAT" in output -> "SAT"
                        else -> "UNKNOWN"
                    }

                    TestResult(
                        fileName = file.name,
                        expected = expected,
                        actual = actual,
                        runtimeMs = runtimeMs,
                        success = (actual == expected)
                    )

                } catch (e: Exception) {
                    val runtimeMs = (System.nanoTime() - start) / 1_000_000.0
                    TestResult(
                        fileName = file.name,
                        expected = expected,
                        actual = "CRASH",
                        runtimeMs = runtimeMs,
                        success = false,
                        error = true
                    )
                }
            }
        )

        futures.add(future)
    }

    executor.shutdown()

    val results = futures.map { it.get() }.sortedBy { it.fileName }

    val totalRuntimeMs = (System.nanoTime() - totalStart) / 1_000_000.0

    var passed = 0
    var failed = 0
    var timeouts = 0
    var errors = 0

    var solverRuntimeMs = 0.0

    for (r in results) {

        println("----------------------------------------")
        println("Formula: ${r.fileName}")
        println("Expected: ${r.expected}")
        println("Actual:   ${r.actual}")
        when {
            r.timeout -> {
                timeouts++
                println(
                    "Result: TIMEOUT (${TIMEOUT_SECONDS}s)"
                )
            }
            r.error -> {
                errors++
                println("Result: ERROR")
            }
            r.success -> {
                passed++
                println("Result: PASS")
            }
            else -> {
                failed++
                println("Result: FAIL")
            }
        }

        println("Runtime: %.3f ms".format(r.runtimeMs))
        solverRuntimeMs += r.runtimeMs
    }

    println()
    println("========================================")
    println("SUMMARY")
    println("========================================")

    println("Passed:   $passed")
    println("Failed:   $failed")
    println("Timeouts: $timeouts")
    println("Errors:   $errors")
    println()

    println("Solver Runtime Sum: %.3f s".format(solverRuntimeMs/1000))
    println("Wall Clock Runtime: %.3f s".format(totalRuntimeMs/1000))

    if (results.isNotEmpty()) {
        println("Average Solver Runtime: %.3f s".format(solverRuntimeMs / results.size / 1000))
    }
}