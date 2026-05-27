import os
from pathlib import Path

from pysat.formula import CNF
from pysat.solvers import Solver

INPUT_DIR = "test-formulas"
OUTPUT_DIR = "solutions"

SOLVER_NAME = "g4"

Path(OUTPUT_DIR).mkdir(exist_ok=True)

files = sorted(os.listdir(INPUT_DIR))

for filename in files:

    formula_path = os.path.join(INPUT_DIR, filename)

    print(f"Solving {filename} ...")

    try:

        cnf = CNF(from_file=formula_path)

        with Solver(name=SOLVER_NAME, bootstrap_with=cnf) as solver:

            satisfiable = solver.solve()

        answer = "SAT" if satisfiable else "UNSAT"

        output_file = os.path.join(
            OUTPUT_DIR,
            filename + ".sol"
        )

        with open(output_file, "w") as f:
            f.write(answer + "\n")

        print(f" -> {answer}")

    except Exception as e:

        print(f" -> ERROR: {e}")

print()
print("Done.")