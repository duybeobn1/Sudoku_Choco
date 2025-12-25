# Sudoku CSP Benchmark and Solver

This repository contains several tools for modeling, solving, and generating Sudoku instances as Constraint Satisfaction Problems (CSPs). It includes:

- A Java solver and benchmark using the Choco constraint solver.
- A Python generator using Z3 to create Sudoku instances in MiniZinc (.dzn) format.
- Example Sudoku instances and helper scripts.

## Repository Structure

- `SudokuSolver.java`: Java program that models and solves a standard 9x9 Sudoku using Choco.
- `SudokuBenchmark.java`: Java benchmark to evaluate Sudoku solving performance (e.g., multiple runs / instances).
- `benchmark.py`: Python script that uses Z3 to generate large Sudoku instances (e.g., 16x16, 25x25) and saves them as `.dzn` files for MiniZinc. 
- `sudoku.txt`: Example Sudoku puzzle (grid) used as input or reference.
- `command.md`: Example commands to compile and run the Java solver with Choco.
- `choco-solver.jar`: Choco Solver library.

## Prerequisites

### Java / Choco

- Java 8 or later installed and available on your PATH.
- Choco Solver JAR file downloaded locally.

Update the classpath in the commands below with the actual path to your Choco JAR.

### Python / Z3

- Python 3.8+.
- The `z3-solver` package installed:

```bash
pip install z3-solver
```

- Standard libraries: `random`, `time`, `os` (part of the Python standard library).

## How the Code Works

### Java Sudoku Solver (Choco)

`SudokuSolver.java` models Sudoku as a CSP:

- **Variables**: A 9x9 matrix of integer variables \(X_{i,j}\) (rows and columns from 1 to 9) with domain 1–9.
- **Row constraints**: All cells in each row must take pairwise distinct values (`allDifferent`). 
- **Column constraints**: All cells in each column must take pairwise distinct values.
- **Block constraints**: Each 3×3 block (subgrid) must contain distinct values.
- **Clues**: Given digits from the puzzle (e.g., loaded from `sudoku.txt` or hard-coded) are posted as equality constraints on the corresponding variables.

`SudokuBenchmark.java` extends this model to:

- Run the solver on multiple instances or multiple times.
- Measure runtimes and collect basic performance statistics.

### Python Instance Generator (Z3 → MiniZinc)

`benchmark.py` creates Sudoku instances of size \(n \times n\) where \(n = b^2\) for a given block size \(b\):

- Builds a Z3 model with:
- Integer variables for each cell with domain 1–\(n\).
- Distinctness constraints on rows, columns, and all \(b \times b\) blocks.
- Uses Z3 to generate a complete valid solution grid.
- Randomly removes values to achieve a target **density** of given cells (e.g., 0.2, 0.3, 0.4, 0.6, 0.8).
- Writes instances to `.dzn` files using MiniZinc’s `array2d` format, with aligned numbers and comments describing the instance.

The main script generates batches of instances, for example:

- Sizes: 16×16 (`blocksize = 4`), 25×25 (`blocksize = 5`).
- Multiple densities and two instances per density.
- Output folder: `generated_instances/` (created automatically).

## Usage

### 1. Compile and Run the Java Solver

#### Compile

```bash
javac -cp choco-solver.jar SudokuSolver.java SudokuBenchmark.java
```

#### Run the Solver

```bash
java -cp choco-solver.jar SudokuSolver
```

This will:

- Build the CSP model for a 9×9 Sudoku.
- Apply all constraints (rows, columns, blocks, and clues).
- Invoke Choco to find one or more solutions and print them.

#### Run the Benchmark

```bash
java -cp choco-solver.jar SudokuBenchmark
```

This will:

- Execute the solver repeatedly (or on different instances).
- Measure solving times and potentially output statistics to the console.

### 2. Generate Sudoku Instances with Python

From the project root:

```bash
python benchmark.py
```

This will:

- Initialize `LargeSudokuGenerator` for each configured size (e.g., 16×16, 25×25).
- For each size and each density, generate 2 puzzle instances using Z3.
- Save each instance as a `.dzn` file under `generated_instances/`, with filenames such as:
  - `sudoku16x16_d02_1.dzn`
  - `sudoku25x25_d04_2.dzn`

These `.dzn` files can be consumed by MiniZinc models of Sudoku for further experimentation and benchmarking.

## Input Format (Example Sudoku)

The example file `sudoku.txt` contains a Sudoku puzzle that can be:

- Parsed by `SudokuSolver.java` (if configured to read from file).
- Used as a reference to hard-code clues in the Java solver.

Ensure that the representation (e.g., 0 or `.` for empty cells) matches the parsing logic inside `SudokuSolver.java`.

## Adapting the Project

- To change the Sudoku size in Python, modify `tasks` and `blocksize` values in `benchmark.py`. 
- To test different puzzles in Java, either:
  - Edit the clues in the source code, or
  - Implement/adjust file parsing to read new instances.
- To benchmark other solvers or models, reuse the generated `.dzn` instances with your own MiniZinc or CSP models.
