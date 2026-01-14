# Sudoku Solver

## 🏃 How to Run

### From command line

```bash
# Compile
javac -cp "lib/*" -d bin src/model/*.java src/solver/*.java src/heuristic/*.java src/util/*.java src/benchmark/*.java src/Main.java

# Run demo mode
java -cp "bin;lib/*" src.Main demo

# Run full benchmark suite
java -cp "bin;lib/*" src.Main benchmark

# Show help / usage
java -cp "bin;lib/*" src.Main help

```

---

## 📋 Project Overview

A modular and well-structured architecture to solve Sudoku puzzles using:

✅ **Complete Solver**: Constraint Programming (Choco Solver)  
- Guarantees a solution if one exists  
- Advanced search strategies (DOM_OVER_WDEG, MIN_DOM_SIZE, ACTIVITY_BASED)
- Optimal for difficult puzzles
- External dependency: Choco Solver library

✅ **Incomplete Solver**: Backtracking + Heuristics  
- Pure Java, no external dependency  
- Three customizable heuristics: MRV, Degree, Hybrid (MRV + Degree)
- Constraint propagation for search space reduction
- Very efficient on easy/medium puzzles
- Fast iteration and backtracking tracking

---

## 📂 File Organization

```text
src/
├── model/
│   ├── SudokuGrid.java          # Grid representation
│   ├── SolverResult.java        # Solving result container
│
├── solver/
│   ├── SudokuSolver.java        # Abstract base class
│   ├── CompleteSolver.java      # Complete solver (Choco)
│   ├── IncompleteSolver.java    # Incomplete solver (Backtracking)
│
├── heuristic/
│   ├── CellHeuristic.java       # Heuristic interface
│   ├── MRVHeuristic.java        # Minimum Remaining Values
│   ├── DegreeHeuristic.java     # Degree-based heuristic
│   └── HybridHeuristic.java     # MRV + Degree hybrid
│
├── util/
│   ├── SudokuValidator.java     # Grid validation
│   ├── PuzzleParser.java        # File parsing
│
└── Main.java                    # Entry point (demo + benchmark)
```

---

## 🚀 Quick Start

### Using the Complete Solver

```java
// Load a grid
int[][] puzzleData = { /* ... */ };
SudokuGrid grid = new SudokuGrid(puzzleData, 3);

// Create and configure solver
CompleteSolver solver = new CompleteSolver(grid);
solver.setStrategy(CompleteSolver.SearchStrategy.DOM_OVER_WDEG);
solver.setTimeout(10);  // 10 seconds

// Solve
SolverResult result = solver.solve();
System.out.println("Solved: " + result.isSolved());
System.out.println("Time: " + result.getTimeMs() + " ms");
```

### Using the Incomplete Solver

```java
// Load a grid
SudokuGrid grid = new SudokuGrid(puzzleData, 3);

// Create and configure solver
IncompleteSolver solver = new IncompleteSolver(grid);
solver.setHeuristic(new MRVHeuristic());
solver.setPropagate(true);
solver.setMaxIterations(100000);

// Solve
SolverResult result = solver.solve();
System.out.println("Solved: " + result.isSolved());
System.out.println("Time: " + result.getTimeMs() + " ms");
```

### Parsing from a file

```java
// Simple text file
SudokuGrid grid = PuzzleParser.parseFromFile("puzzle.txt", 3);

// MiniZinc format
SudokuGrid grid = PuzzleParser.parseFromMiniZinc(new File("puzzle.dzn"));

// Create manually
int[][] data = { /* ... */ };
SudokuGrid grid = PuzzleParser.createFromArray(data, 3);
```

---

## 🎯 Complete Solver Strategies

| Strategy | Description | When to Use |
|----------|-------------|------------|
| `INPUT_ORDER` | Sequential variable ordering (baseline) | Comparisons, naive baseline |
| `DOM_OVER_WDEG` | Domain over weighted degree (recommended) | Most puzzles - excellent balance |
| `MIN_DOM_SIZE` | First-fail: smallest domain first | Easy to medium puzzles |

```java
CompleteSolver solver = new CompleteSolver(grid);
solver.setStrategy(CompleteSolver.SearchStrategy.DOM_OVER_WDEG);
solver.setTimeout(30);  // seconds
```

---

## 🧠 Incomplete Solver Heuristics

### 1️⃣ MRVHeuristic (Minimum Remaining Values)
- **Strategy**: Select cell with fewest candidate values
- **Effectiveness**: Very high - aggressively reduces branching
- **Speed**: Fast computation, minimal overhead
- **Best for**: Most Sudoku instances (easy/medium/hard)

```java
IncompleteSolver solver = new IncompleteSolver(grid);
solver.setHeuristic(new MRVHeuristic());
```

### 2️⃣ DegreeHeuristic (MRV + Degree Tie-breaking)
- **Strategy**: Primary = fewest candidates (MRV); Tie-breaker = constrains most neighbors
- **Effectiveness**: Very high on hard puzzles
- **Speed**: Slightly slower due to degree computation
- **Best for**: Hard/sparse puzzles with multiple candidate ties

```java
IncompleteSolver solver = new IncompleteSolver(grid);
solver.setHeuristic(new DegreeHeuristic());
```

### ⚙️ Configuration Options

```java
IncompleteSolver solver = new IncompleteSolver(grid);
solver.setHeuristic(new HybridHeuristic());      // Choose heuristic
solver.setPropagate(true);                       // Enable candidate reduction
solver.setMaxIterations(100_000);                // Iteration limit
```

---

## 📊 Benchmark & Dashboard Tools

### 1️⃣ **Benchmark Suite** (`SudokuBenchmark.java`)
Runs comprehensive tests across:
- **Difficulties**: Easy, Medium, Hard
- **Grid Sizes**: 9×9, 16×16, 25×25, and other n×n variants
- **Complete Solver**: 3 strategies × N puzzles
- **Incomplete Solver**: 2 heuristics × N puzzles
- **Output**: CSV report with timing, iterations, backtracks, success rates

```bash
java -cp "bin;lib/*" src.Main benchmark
# Generates: benchmarks/benchmark_results.csv
```

**Puzzle Sources:**

#### Instance Generation
We generated custom Sudoku instances programmatically to ensure controlled difficulty levels and comprehensive solver evaluation:
- **Generation Method**: Instances were created using constraint-based generation algorithms (Z3 SMT solver) that start with a complete valid solution and selectively remove clues while maintaining puzzle uniqueness.
- **Grid Flexibility**: Supports n×n grids where n = k² (e.g., 9×9 with 3×3 blocks, 16×16 with 4×4 blocks, 25×25 with 5×5 blocks)
- **Difficulty Control**: Difficulty is determined by the number and placement of removed clues—fewer clues = harder puzzles. We generated instances across a spectrum:
  - **Easy**: ~60% clue density
  - **Medium**: ~40% clue density
  - **Hard**: ~20% clue density
- **Validation**: All generated instances are verified to have exactly one unique solution before benchmarking.

#### Online Benchmark Sources
Additional instances were sourced from established online repositories to validate performance against known difficult puzzles:
- Popular Sudoku benchmark datasets (e.g., Project Euler, Kaggle)
- Real-world hard instances with known solving times
- Cross-validation of solver performance against published results

### 2️⃣ **Benchmark Dashboard** (`benchmark_dashboard.html`)
Interactive web UI to visualize results:
- 📈 Charts: Time by solver, Success rates, Iterations, Complete vs Incomplete
- 📋 Detailed results table
- 🎯 KPIs: Success rate, fastest solver, slowest solver
- ✨ No server required (loads CSV via client-side JS)

**Usage:**
```bash
# Go to the root of the projetct
python -m http.server 8000

# Then open: http://localhost:8000/benchmark_dashboard.html
```

### 3️⃣ **Interactive Solver** (`sudoku_solver.html`)
Visual Sudoku solving interface:
- 🎮 Load puzzles from benchmarks or create custom grids (9×9, 16×16, 25×25, or other n×n sizes)
- ⚙️ Choose solver (Complete or Incomplete with any heuristic)
- 🔍 Watch real-time solving with statistics
- 📊 Performance metrics (time, iterations, backtracks)

**Usage:**
```bash
# Open directly in browser or via HTTP server
open sudoku_solver.html
# or
http://localhost:8000/sudoku_solver.html
```

## 🔬 Performance Analysis & Conclusion

Our benchmarks on "Hard" instances (e.g., `sudoku_p89.dzn`) reveal distinct performance characteristics for each approach:

1.  **Complete Solver (Choco)**:
    *   **Strategy Matters**: `INPUT_ORDER` fails quickly on hard instances, while `DOM_OVER_WDEG` solves them efficiently (e.g., ~4.9s for a hard puzzle).
    *   **Robustness**: The underlying Constraint Programming engine handles deep search trees well through advanced propagation and conflict learning.

2.  **Incomplete Solver (Backtracking)**:
    *   **High Throughput**: Capable of processing over 1 million iterations in under 6 seconds.
    *   **Heuristic Limitations**: While `MRV` and `Degree` heuristics are vastly superior to naive backtracking, they may still get trapped in deep search sub-trees on specifically designed "Hard" instances, leading to timeouts despite high iteration speed.
    *   **Use Case**: Extremely efficient for Easy to Medium puzzles, but requires advanced techniques (like restarts or clause learning) to match Choco on the hardest instances.

**Conclusion**: For general-purpose solving, the Incomplete Solver is lightweight and fast. For guaranteed solving of complex, adversarial puzzles, the Complete Solver with `DOM_OVER_WDEG` remains the superior choice.

---

## 📝 Text File Formats

**Simple format (recommended):**

```text
5 3 0 0 7 0 0 0 0
6 0 0 1 9 5 0 0 0
0 9 8 0 0 0 0 6 0
...
```

- One line per Sudoku row  
- Space-separated values  
- 0 = empty cell  
- Lines starting with `#` can be used as comments  

**MiniZinc format:**

```text
% Comment
n = 3;
grid = array2d(1..9, 1..9, [
 5, 3, 0, 0, 7, 0, 0, 0, 0,
 ...
]);
```

---

## ⚡ Performance Tips

### For the Complete Solver:

1. Use `DOM_OVER_WDEG` or `ACTIVITY_BASED` for harder puzzles  
2. Increase timeout for large puzzles (16x16+)  
3. Compile with JVM optimization flags in production  

### For the Incomplete Solver:

1. Always enable propagation (`setPropagate(true)`)  
2. Use `MRVHeuristic` as default heuristic  
3. Use `DegreeHeuristic` for very sparse puzzles  
4. Set a reasonable iteration limit to avoid timeouts  

### General comparison:

- **Complete**: Slower but guarantees finding a solution  
- **Incomplete**: Very fast on easy/medium puzzles, may fail on very hard ones  

---

## 🧪 Full Usage Example

```java
import src.model.SudokuGrid;
import src.model.SolverResult;
import src.solver.CompleteSolver;
import src.solver.IncompleteSolver;
import src.heuristic.MRVHeuristic;

public class Example {
    public static void main(String[] args) {
        // Create a grid
        int[][] puzzle = { /* ... */ };
        SudokuGrid grid = new SudokuGrid(puzzle, 3);

        System.out.println("Original puzzle:");
        grid.print();

        // Solve with complete solver
        System.out.println("\n=== Complete Solver ===");
        CompleteSolver complete = new CompleteSolver(grid);
        SolverResult completeResult = complete.solve();
        System.out.println("Solved: " + completeResult.isSolved());
        System.out.println("Time: " + completeResult.getTimeMs() + " ms");

        // Solve with incomplete solver
        System.out.println("\n=== Incomplete Solver ===");
        IncompleteSolver incomplete = new IncompleteSolver(grid);
        incomplete.setHeuristic(new MRVHeuristic());
        SolverResult incompleteResult = incomplete.solve();
        System.out.println("Solved: " + incompleteResult.isSolved());
        System.out.println("Time: " + incompleteResult.getTimeMs() + " ms");

        // Compare performance
        System.out.println("\n=== Comparison ===");
        if (completeResult.isSolved() && incompleteResult.isSolved()) {
            System.out.println("Complete faster: " +
                    (completeResult.getTimeMs() < incompleteResult.getTimeMs()));
        }
    }
}
```

---

## 📦 Dependencies

### Required:

- Java 8+

### Optional:

- **Choco Solver** (for the complete solver)

The incomplete solver has **no external dependencies**.

---

## 🛠️ Extension and Customization

### Create a new heuristic:

```java
public class CustomHeuristic implements CellHeuristic {
    @Override
    public int[] selectCell(SudokuGrid grid) {
        // Your selection logic here
        return new int[]{row, col};
    }
}

// Use:
IncompleteSolver solver = new IncompleteSolver(grid);
solver.setHeuristic(new CustomHeuristic());
```

### Create a new solver:

```java
public class CustomSolver extends SudokuSolver {
    @Override
    public SolverResult solve() {
        startTime = System.currentTimeMillis();
        // Your algorithm here
        boolean solved = false;
        // ...
        finalizeResult(solved);
        return result;
    }
}
```

---

## 🎓 Notes

- **Separation of Concerns**: Each class has a single clear responsibility  
- **Design Patterns**: Strategy (heuristics), Template Method (solver base)  
- **Classical algorithms**: Backtracking, CSP, Constraint Propagation  
- **Optimizations**: MRV, degree heuristic, early conflict detection  
- **Scalability**: Supports 9x9, 16x16, and other \(n^2 \times n^2\) sizes
