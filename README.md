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
- Advanced search algorithms  
- Multiple strategies available  

✅ **Incomplete Solver**: Backtracking + Heuristics  
- Pure Java, no external dependency required  
- Efficient search algorithms  
- Customizable heuristics (MRV, Degree)  

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

| Strategy         | Description                                   |
|-----------------|-----------------------------------------------|
| `INPUT_ORDER`    | Naive variable order (baseline)              |
| `DOM_OVER_WDEG`  | Domain over weighted degree (recommended)    |
| `MIN_DOM_SIZE`   | First-fail: smallest domain first            |
| `ACTIVITY_BASED` | Activity-based search                        |

---

## 🧠 Incomplete Solver Heuristics

### MRVHeuristic (Minimum Remaining Values)

- Selects the cell with the fewest candidates  
- Very effective to reduce search space  
- Good balance between time and memory  

### DegreeHeuristic

- First uses number of candidates (MRV)  
- On ties, selects the cell that constrains the most neighbors  
- More sophisticated but slightly slower  

### Propagation

- Automatically removes impossible candidates  
- Strongly reduces the search space  
- Recommended: enabled by default  

---

## 📊 SolverResult Class

Each solving call returns a `SolverResult` containing:

```java
result.isSolved()      // boolean: puzzle solved?
result.getTimeMs()     // long: time in milliseconds
result.getIterations() // long: number of iterations
result.getBacktracks() // long: number of backtracks
result.getSolution()   // int[][]: solution grid (if solved)
result.getSolverName() // String: solver name
```

---

## 🔍 SudokuGrid Class

Full grid management:

```java
// Value access
int value = grid.get(row, col);
grid.set(row, col, 5);
boolean empty = grid.isEmpty(row, col);

// Candidates (for incomplete solver)
int candidates = grid.getCandidates(row, col);
grid.removeCandidate(row, col, 5);
int count = grid.countCandidates(row, col);

// General info
int N = grid.getSize();         // 9 for a 9x9
int n = grid.getBlockSize();    // 3 for a 9x9
boolean full = grid.isFull();
int clues = grid.countClues();
grid.print();                   // Print grid to console
```

---

## 💾 SudokuValidator Class

Advanced validation:

```java
// Full grid validation
boolean valid = SudokuValidator.isValid(grid);

// Check a placement before applying it
boolean canPlace = SudokuValidator.isValidPlacement(grid, row, col, value);

// Partial checks
boolean rowValid   = SudokuValidator.isRowValid(grid, row);
boolean colValid   = SudokuValidator.isColumnValid(grid, col);
boolean blockValid = SudokuValidator.isBlockValid(grid, blockRow, blockCol);
```

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
